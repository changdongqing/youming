# QwenPaw 接入 pig 服务端 —— 实施计划

> **配套文档**：[architecture.md](./architecture.md)（架构方案）
>
> **使用方式**：按阶段顺序逐步实施，每个阶段独立可交付。`sync.enabled` 开关确保任何阶段未完成时不影响现有功能。
>
> **核心原则**：本地优先，服务端是母版与同步源。所有改动以"新增适配器 + 旁路同步"为主，不替换现有主干逻辑。

---

## 目录

- [总体路线图](#总体路线图)
- [通用约定](#通用约定)
- [阶段 0：基础设施搭建](#阶段-0基础设施搭建)
- [阶段 1：授权对接 pig OAuth2](#阶段-1授权对接-pig-oauth2)
- [阶段 2：记忆同步](#阶段-2记忆同步)
- [阶段 3：企业凭据拉取](#阶段-3企业凭据拉取)
- [阶段 4：企业技能分发](#阶段-4企业技能分发)
- [阶段 5：MCP 模板同步](#阶段-5mcp-模板同步)
- [交付物清单](#交付物清单)
- [测试策略](#测试策略)
- [回滚策略](#回滚策略)

---

## 总体路线图

```
阶段0  基础设施        ← 同步层骨架 + 配置模型 + pig HTTP 客户端
  │
  ▼
阶段1  授权对接        ← pig OAuth2 登录 + JWT 验签 (前置依赖)
  │
  ├──────────────────────┐──────────────────────┐
  ▼                      ▼                      ▼
阶段2  记忆同步     阶段3  凭据拉取        阶段4  技能分发
(价值最高)           (安全价值高)          (可并行)
  │                      │                      │
  └──────────┬───────────┘──────────────────────┘
             ▼
阶段5  MCP模板同步 (可选)
```

| 阶段 | 预估工期 | 依赖 | 可并行 |
|------|---------|------|--------|
| 0 基础设施 | 2-3 天 | 无 | — |
| 1 授权对接 | 3-5 天 | 阶段 0 | — |
| 2 记忆同步 | 5-7 天 | 阶段 1 | 与 3、4 并行 |
| 3 凭据拉取 | 2-3 天 | 阶段 1 | 与 2、4 并行 |
| 4 技能分发 | 3-4 天 | 阶段 1 | 与 2、3 并行 |
| 5 MCP 同步 | 2-3 天 | 阶段 1 | 最后 |

---

## 通用约定

### 命名规范

- 服务端同步相关代码统一放在 `src/qwenpaw/sync/` 目录
- 配置项统一在 `config.yaml` 的 `sync` 段下
- 环境变量前缀 `QWENPAW_SYNC_`（如 `QWENPAW_SYNC_ENABLED`）
- pig 相关类名使用 `Pig` 前缀（如 `PigClient`、`PigMemorySync`）

### 开关机制

所有同步功能受 `sync.enabled` 总开关控制：

```python
# 任何同步操作前的统一检查
from ..config import load_config
def is_sync_enabled() -> bool:
    cfg = load_config()
    return getattr(cfg, "sync", None) is not None and cfg.sync.enabled
```

`enabled: false` 或未配置 `sync` 段时，完全退化为现有纯本地模式，**零影响**。

### 错误处理原则

- 同步失败**永不阻塞**本地主流程
- 网络错误重试 3 次后放弃，记入失败队列
- 所有同步异常只记日志，不抛出到上层
- 断网时本地正常工作，联网后补传

---

## 阶段 0：基础设施搭建

**目标**：搭建同步层骨架，后续各阶段在此之上填充业务逻辑。

### 0.1 新增配置模型

**文件**：`src/qwenpaw/config/config.py`

在 `SecurityConfig` 之后新增 `SyncConfig` 及子配置：

```python
class PigAuthConfig(BaseModel):
    """pig OAuth2 认证配置。"""
    base_url: str = Field(default="", description="pig 服务端基础 URL")
    client_id: str = Field(default="qwenpaw")
    client_secret: str = Field(default="", description="加密存储，复用 Fernet")
    token_endpoint: str = Field(default="/oauth2/token")
    introspect_endpoint: str = Field(default="/oauth2/introspect")
    jwks_endpoint: str = Field(default="/oauth2/jwks", description="JWT 公钥端点")
    authorize_endpoint: str = Field(default="/oauth2/authorize")
    redirect_uri: str = Field(default="", description="回调地址，如 http://localhost:端口/api/sync/oauth/callback")

class MemorySyncConfig(BaseModel):
    sync_enabled: bool = Field(default=True)
    batch_size: int = Field(default=50, ge=1, le=500)
    retry_times: int = Field(default=3, ge=0, le=10)
    flush_on_exit: bool = Field(default=True)

class SkillSyncConfig(BaseModel):
    auto_pull_enterprise: bool = Field(default=True)

class CredentialSyncConfig(BaseModel):
    cache_enterprise: bool = Field(default=True)
    sync_interval_minutes: int = Field(default=30, ge=5, le=1440)

class SyncConfig(BaseModel):
    """顶层 sync 配置段。"""
    enabled: bool = Field(default=False, description="总开关，false 时纯本地模式")
    auth_mode: Literal["local", "pig"] = Field(default="local")
    pig: PigAuthConfig = Field(default_factory=PigAuthConfig)
    memory: MemorySyncConfig = Field(default_factory=MemorySyncConfig)
    skill: SkillSyncConfig = Field(default_factory=SkillSyncConfig)
    credential: CredentialSyncConfig = Field(default_factory=CredentialSyncConfig)
```

在顶层 `Config` 模型中挂载 `sync: SyncConfig`。

### 0.2 同步层骨架

**新建目录**：`src/qwenpaw/sync/`

```
src/qwenpaw/sync/
  ├── __init__.py          # 导出公共接口
  ├── client.py            # pig HTTP 客户端
  ├── auth_adapter.py      # OAuth2 token 管理
  ├── errors.py            # 同步层异常定义
  └── lifecycle.py         # 生命周期挂钩（登录/退出/定时）
```

**`client.py` — pig HTTP 客户端**：

```python
class PigClient:
    """pig 服务端 HTTP 客户端，封装 Bearer token、重试、退避。

    所有同步模块共用此客户端。
    """
    def __init__(self, config: PigAuthConfig, token_provider):
        ...
    async def get(self, path: str, params: dict = None) -> dict: ...
    async def post(self, path: str, json: dict = None) -> dict: ...
    async def download(self, path: str, dest: Path) -> None: ...
    async def upload(self, path: str, file: Path) -> dict: ...
```

关键实现要点：
- Bearer token 从 `auth_adapter` 获取，401 时自动刷新
- 重试：指数退避，最多 3 次，仅对网络错误重试（4xx 不重试）
- 超时：默认 30 秒，下载技能包时延长至 120 秒
- 使用 `httpx.AsyncClient`（项目已有 httpx 依赖链）

**`auth_adapter.py` — OAuth2 token 管理**：

```python
class PigTokenProvider:
    """管理 pig OAuth2 access_token / refresh_token 生命周期。

    - token 缓存在内存 + 持久化到 SECRET_DIR/pig_tokens.json（加密）
    - 过期前 60 秒自动刷新
    - 刷新失败时标记为需重新登录
    """
    async def get_access_token(self) -> str: ...
    async def refresh(self) -> bool: ...
    async def revoke(self) -> None: ...
```

**`lifecycle.py` — 生命周期挂钩**：

```python
class SyncLifecycle:
    """同步层生命周期管理，挂载到 app lifespan。"""
    async def on_login_success(self, user: str) -> None:
        """登录成功后并发拉取：技能 + 记忆 + 凭据 + MCP"""
        ...
    async def on_shutdown(self) -> None:
        """退出时 flush 记忆上行队列"""
        ...
    def start_periodic_sync(self) -> None:
        """启动定时同步任务（每30分钟检查技能/凭据变更）"""
        ...
```

### 0.3 接入 app lifespan

**文件**：`src/qwenpaw/app/_app.py`

在 `lifespan()` 函数（第 115 行）的 `_background_startup()` 中，skill pool auto-update 之后追加同步层初始化：

```python
# ---- Sync layer init (pig integration) ----
try:
    from ..sync.lifecycle import SyncLifecycle
    sync_lc = SyncLifecycle()
    app.state.sync_lifecycle = sync_lc
    await sync_lc.on_startup()
except Exception:
    logger.warning("Sync layer init skipped", exc_info=True)
```

在 `finally` 块中追加：

```python
# Flush sync queue on shutdown
sync_lc = getattr(app.state, "sync_lifecycle", None)
if sync_lc:
    await sync_lc.on_shutdown()
```

### 0.4 阶段 0 验收标准

- [ ] `SyncConfig` 可在 `config.yaml` 中配置，缺省时 `enabled=false`
- [ ] `PigClient` 能对 pig `/oauth2/introspect` 发起请求（即使返回 401）
- [ ] `PigTokenProvider` 能持久化/加载 token 文件
- [ ] app 启动/关闭时不因同步层报错
- [ ] `sync.enabled=false` 时零行为变化

---

## 阶段 1：授权对接 pig OAuth2

**目标**：用户可通过 pig SSO 登录 QwenPaw，pig JWT 可本地验签。

### 1.1 后端：JWT 验签

**文件**：`src/qwenpaw/app/auth.py`

新增 pig JWT 验签函数，保留现有本地 HMAC 作为 fallback：

```python
# 新增
_pig_jwks_cache: dict = {"keys": None, "fetched_at": 0}
_JWKS_TTL = 3600  # 公钥缓存1小时

async def _fetch_pig_jwks() -> dict:
    """从 pig 的 jwks_endpoint 拉取公钥，带缓存。"""
    ...

def verify_pig_jwt(token: str) -> Optional[str]:
    """验证 pig 签发的 JWT，返回 username（sub claim）。

    1. 解码 JWT header 获取 kid
    2. 从缓存的 JWKS 中找到匹配公钥
    3. 验证签名 + exp
    4. 返回 sub claim
    """
    ...

def is_pig_auth_mode() -> bool:
    """检查是否启用了 pig 认证模式。"""
    cfg = load_config()
    return cfg.sync.enabled and cfg.sync.auth_mode == "pig"
```

**改造 `verify_token()`**（第 173 行）：

```python
def verify_token(token: str) -> Optional[str]:
    if is_pig_auth_mode():
        return verify_pig_jwt(token)
    # 现有逻辑不变
    ...
```

> 注意：`verify_pig_jwt` 内部如果需要异步拉取 JWKS，需改造 `AuthMiddleware.dispatch()` 为 async 调用。当前 `verify_token` 是同步的，JWKS 拉取可放在后台定时刷新（类似 `_get_config_cached` 的 mtime 缓存模式），验签时只读缓存。

### 1.2 后端：OAuth2 授权码回调路由

**新建文件**：`src/qwenpaw/app/routers/sync_auth.py`

```python
router = APIRouter(prefix="/api/sync", tags=["sync"])

@router.get("/oauth/callback")
async def oauth_callback(code: str, state: str, request: Request):
    """pig OAuth2 授权码回调。

    1. 用 code 换 access_token + refresh_token
    2. 持久化 token
    3. 重定向到前端，带 token 参数
    """
    ...

@router.get("/oauth/authorize")
async def oauth_authorize(request: Request):
    """重定向到 pig 授权页。"""
    ...

@router.post("/oauth/logout")
async def oauth_logout(request: Request):
    """注销：撤销 pig token + 清除本地缓存。"""
    ...
```

在 `_app.py` 中 `include_router` 注册此路由。

**修改 `_PUBLIC_PATHS`**（auth.py 第 53 行）：追加 `/api/sync/oauth/callback`、`/api/sync/oauth/authorize`。

### 1.3 后端：auth status 扩展

**文件**：`src/qwenpaw/app/routers/auth.py`

扩展 `/api/auth/status` 返回值，增加 `auth_mode` 字段：

```python
class AuthStatusResponse(BaseModel):
    enabled: bool
    has_users: bool
    auth_mode: str = "local"  # "local" | "pig"
    pig_authorize_url: str = ""  # pig 模式下的授权页 URL
```

### 1.4 前端：登录页适配

**文件**：`console/src/pages/Login/index.tsx`

- 检查 `auth_status.auth_mode`：
  - `local`：现有用户名密码登录表单（不变）
  - `pig`：显示"使用 pig 登录"按钮，点击跳转 `pig_authorize_url`
- 回调页处理：从 URL 参数获取 token，存入 localStorage（复用现有 `getApiToken`/`setApiToken`）

**文件**：`console/src/api/modules/auth.ts`

新增 `getAuthStatus` 返回类型扩展，新增 `pigLogout` 方法。

### 1.5 阶段 1 验收标准

- [ ] pig 模式下，前端登录跳转 pig SSO 页面
- [ ] 授权回调后获得 JWT，存入 localStorage
- [ ] 后续 API 请求带 `Bearer <jwt>`，`AuthMiddleware` 验签通过
- [ ] JWT 过期时前端可感知并重新跳转登录
- [ ] `auth_mode=local` 时行为与当前完全一致
- [ ] pig 服务端不可达时，本地模式仍可正常使用

---

## 阶段 2：记忆同步

**目标**：登录时从服务端拉取记忆增量到本地 SQLite，工作中后台异步上行新增记忆，断网可正常工作。

> **本阶段价值最高，也最复杂。**

### 2.1 记忆上行：HistoryStore append hook

**文件**：`src/qwenpaw/agents/context/scroll/history.py`

在 `HistoryStore` 中新增可选同步 hook，**不改变现有 `append()` 返回值和逻辑**：

```python
class HistoryStore:
    def __init__(self, db_path, sync_hook=None):
        ...
        self._sync_hook = sync_hook  # Callable[[int, str, LogEntry, str|None], None] | None

    def append(self, *, session_id, entry, agent_id=None, dedup_key=None) -> int:
        seq = ...  # 现有逻辑完全不变
        # 新增：写成功后异步通知同步层（仅对新插入的行）
        if seq > 0 and self._sync_hook:
            try:
                self._sync_hook(seq, session_id, entry, dedup_key, agent_id)
            except Exception:
                logger.debug("sync hook failed, ignored")
        return seq
```

同样在 `append_many()` 末尾对新增行触发 hook。

### 2.2 记忆上行：同步管理器

**新建文件**：`src/qwenpaw/sync/memory_sync.py`

```python
class MemorySyncManager:
    """记忆双向同步管理器。

    - 上行：接收 HistoryStore hook，批量异步推送到 pig
    - 下行：登录时从 pig 拉取增量记忆到本地 SQLite
    """

    def __init__(self, pig_client: PigClient, history: HistoryStore,
                 config: MemorySyncConfig):
        self._queue: asyncio.Queue = asyncio.Queue()
        self._batch_size = config.batch_size
        self._worker: asyncio.Task | None = None
        self._last_sync_seq: int = 0  # 从本地元数据加载

    # --- 上行 ---

    def on_history_appended(self, seq, session_id, entry, dedup_key, agent_id):
        """HistoryStore hook 回调，非阻塞入队。"""
        self._queue.put_nowait({
            "seq": seq, "session_id": session_id,
            "entry": entry, "dedup_key": dedup_key, "agent_id": agent_id,
        })

    async def _uplink_worker(self):
        """后台 worker：攒批上行到 pig。"""
        while True:
            batch = await self._collect_batch()
            await self._upload_batch(batch)

    async def _upload_batch(self, batch: list[dict]):
        """上传一批记忆到服务端，失败重试。"""
        ...

    # --- 下行 ---

    async def pull_incremental(self):
        """登录时从 pig 拉取增量记忆。

        1. 读取本地 last_sync_seq
        2. GET /api/sync/memory?since={last_sync_seq}
        3. 用 history.append_many() 写入本地（dedup_key 幂等）
        4. 更新 last_sync_seq
        """
        ...

    # --- 生命周期 ---

    async def start(self):
        """启动上行 worker。"""
        self._worker = asyncio.create_task(self._uplink_worker())

    async def flush_and_stop(self):
        """退出时 flush 队列后停止。"""
        ...
```

### 2.3 同步元数据持久化

在 SQLite 中新增同步游标表（`history.py` 的 `_init_schema` 中追加）：

```sql
CREATE TABLE IF NOT EXISTS sync_meta (
    key   TEXT PRIMARY KEY,
    value TEXT
);
-- 存储: last_sync_seq, last_pull_at 等
```

### 2.4 HistoryStore 实例化时注入 hook

**文件**：`src/qwenpaw/agents/context/scroll/sync.py`（第 550 行 `HistoryStore(db_path)`）

或更上层的实例化点——需要找到 `HistoryStore` 的唯一/主要创建位置，在 `sync.enabled` 时注入 `MemorySyncManager.on_history_appended` 作为 `sync_hook`。

> 实施时需确认 `HistoryStore` 的所有实例化点，确保 hook 注入不遗漏。

### 2.5 pig 服务端接口约定（需 pig 侧实现）

```
GET  /api/sync/memory?since={seq}&limit=500
     → { entries: [...], next_seq: int, has_more: bool }

POST /api/sync/memory
     body: { entries: [...] }
     → { accepted: int, deduped: int }

GET  /api/sync/memory/meta
     → { max_seq: int }  # 服务端最新 seq，用于对账
```

服务端 `dedup_key` 处理：`ON CONFLICT DO NOTHING`（与本地一致）。

### 2.6 登录拉取挂钩

**文件**：`src/qwenpaw/sync/lifecycle.py`

`on_login_success()` 中追加：

```python
if cfg.sync.memory.sync_enabled:
    memory_sync = MemorySyncManager(pig_client, history_store, cfg.sync.memory)
    await memory_sync.pull_incremental()
    await memory_sync.start()
    app.state.memory_sync = memory_sync
```

### 2.7 阶段 2 验收标准

- [ ] 登录后本地 SQLite 包含服务端全部记忆（增量拉取）
- [ ] 工作中新产生的记忆异步上行到服务端（可查日志确认）
- [ ] 断网时本地记忆读写完全正常，上行队列堆积
- [ ] 联网后堆积的上行记录自动补传
- [ ] 同一 `dedup_key` 记忆不重复（幂等验证）
- [ ] 退出时上行队列 flush 完成
- [ ] `memory.sync_enabled=false` 时零行为变化
- [ ] pig 侧接口不可达时不影响本地任何操作

---

## 阶段 3：企业凭据拉取

**目标**：登录时拉取企业 API Key 到本地加密缓存，使用时个人 Key 优先、企业 Key 回退。

### 3.1 企业凭据缓存层

**新建文件**：`src/qwenpaw/sync/credential_sync.py`

```python
class EnterpriseCredentialCache:
    """企业凭据本地加密缓存。

    存储路径: SECRET_DIR/enterprise_credentials.yaml
    加密方式: 复用 secret_store.py 的 Fernet 加密
    """

    def __init__(self, pig_client: PigClient):
        self._path = SECRET_DIR / "enterprise_credentials.yaml"
        ...

    async def pull(self):
        """从 pig 拉取企业凭据，加密后写入本地缓存。"""
        remote = await self._pig_client.get("/api/sync/credentials")
        # 用 encrypt_dict_fields 加密 secrets 字段
        self._write_cache(remote)

    def get(self, ref: str) -> CredentialRecord | None:
        """从本地缓存读取企业凭据。"""
        ...

    def list_refs(self) -> list[str]:
        """列出企业凭据 ref。"""
        ...
```

### 3.2 AsyncCredentialStore fallback 改造

**文件**：`src/qwenpaw/drivers/credentials/store.py`

在 `AsyncCredentialStore` 中增加企业凭据 fallback（仅当 `sync.enabled` 时生效）：

```python
class AsyncCredentialStore:
    def __init__(self, credentials_path: Path, enterprise_cache=None):
        self._path = credentials_path
        self._enterprise_cache = enterprise_cache  # 新增，可为 None

    async def get(self, ref: str) -> CredentialRecord:
        # ① 现有逻辑：查本地 personal YAML
        try:
            return await asyncio.to_thread(self._get_sync, ref)
        except CredentialNotFoundError:
            pass
        # ② fallback：查企业凭据缓存
        if self._enterprise_cache:
            record = self._enterprise_cache.get(ref)
            if record:
                return record
        # ③ env: (现有逻辑已在 _get_sync 中处理)
        raise CredentialNotFoundError(ref)
```

> 注意：`env:` 引用在现有 `_get_sync` 中已处理（第 53 行），需确保 fallback 不会截获 `env:` ref。实施时将 `env:` 检查提前到 personal 查找之前。

### 3.3 凭据实例化点注入

找到 `AsyncCredentialStore` 的实例化位置，在 `sync.enabled` 时注入 `EnterpriseCredentialCache` 实例。

### 3.4 定时同步

**文件**：`src/qwenpaw/sync/lifecycle.py`

`start_periodic_sync()` 中追加：

```python
if cfg.sync.credential.cache_enterprise:
    interval = cfg.sync.credential.sync_interval_minutes * 60
    scheduler.add_job(enterprise_cache.pull, "interval", seconds=interval)
```

### 3.5 pig 服务端接口约定

```
GET /api/sync/credentials
   → { credentials: [{ ref, kind, public: {...}, secrets: {...}, meta: {...} }] }
```

### 3.6 阶段 3 验收标准

- [ ] 登录后企业凭据缓存到 `SECRET_DIR/enterprise_credentials.yaml`（加密）
- [ ] Agent 使用 API Key 时，个人 Key 优先，无个人 Key 时回退企业 Key
- [ ] `env:` 引用仍正常工作（优先级不被破坏）
- [ ] 企业凭据在服务端更新后，定时同步拉取到本地
- [ ] `credential.cache_enterprise=false` 时零行为变化
- [ ] 个人凭据永不上传服务端

---

## 阶段 4：企业技能分发

**目标**：登录时增量拉取企业技能到 `skill_pool/`（只读），个人技能独立于 `workspace/skills/`。

### 4.1 技能同步管理器

**新建文件**：`src/qwenpaw/sync/skill_sync.py`

```python
class SkillSyncManager:
    """企业技能增量同步。

    - 对比本地 manifest version 与服务端 version
    - 增量下载变更技能（zip 包）
    - 解压到 skill_pool/，标记 source: "enterprise"
    """

    async def pull_enterprise_skills(self):
        """登录时增量拉取企业技能。

        1. GET /api/sync/skills/manifest → 服务端 manifest（含 version, skills 列表）
        2. 对比本地 pool manifest version
        3. 对变更技能: GET /api/sync/skills/{name}/download → zip
        4. 解压到 skill_pool/{name}/，复用 _extract_and_validate_zip
        5. 更新本地 pool manifest，source: "enterprise"
        """
        ...

    async def upload_personal_skill(self, skill_name: str):
        """用户主动上传个人技能到服务端（云备份）。"""
        ...
```

### 4.2 manifest source 字段扩展

**文件**：`src/qwenpaw/agents/skill_system/store.py`

`classify_pool_skill_source()`（第 438 行）扩展返回值：

```python
# 现有: ("builtin" | "customized", bool)
# 扩展: 增加 "enterprise" 来源
def classify_pool_skill_source(...) -> tuple[str, bool]:
    if existing and _is_enterprise_entry(existing):
        return "enterprise", False
    # ... 现有逻辑
```

新增辅助函数：

```python
def is_enterprise_skill_entry(entry: dict | None) -> bool:
    """检查 manifest entry 是否为企业技能。"""
    normalized = normalize_skill_manifest_entry(entry)
    return bool(normalized) and str(normalized.get("source", "")) == "enterprise"
```

### 4.3 企业技能只读保护

在技能编辑/删除路由中，检查 `source == "enterprise"` 时拒绝写入：

**文件**：`src/qwenpaw/app/routers/skills.py`

在写入类路由（create/update/delete skill）中增加守卫：

```python
if is_enterprise_skill(skill_name):
    raise HTTPException(403, "Enterprise skills are read-only")
```

### 4.4 pig 服务端接口约定

```
GET /api/sync/skills/manifest
   → { version: int, skills: [{ name, version, md_hash, source: "enterprise" }] }

GET /api/sync/skills/{name}/download
   → binary (zip)

POST /api/sync/skills/upload  (个人技能云备份，可选)
   → multipart zip
```

### 4.5 阶段 4 验收标准

- [ ] 登录后 `skill_pool/` 包含服务端企业技能
- [ ] 企业技能在前端展示，标记为"企业"来源
- [ ] 企业技能不可编辑/删除（403）
- [ ] 个人技能（`workspace/skills/`）不受影响
- [ ] 同名时个人技能优先加载
- [ ] 技能 manifest version 未变时不重复下载
- [ ] `skill.auto_pull_enterprise=false` 时零行为变化
- [ ] 断网时使用本地已拉取的技能正常工作

---

## 阶段 5：MCP 模板同步

**目标**：登录时拉取企业 MCP 模板到本地缓存，用户可基于模板创建实例。

### 5.1 MCP 模板同步

**新建文件**：`src/qwenpaw/sync/mcp_sync.py`

```python
class McpTemplateSync:
    """企业 MCP 模板同步。

    - 拉取企业 MCP 驱动模板到本地缓存
    - 本地实例配置标记 source_template
    - 模板更新时前端提示用户
    """

    async def pull_templates(self):
        """从 pig 拉取 MCP 模板。"""
        templates = await self._pig_client.get("/api/sync/mcp/templates")
        self._write_cache(templates)
```

### 5.2 DriverConfigService 扩展

**文件**：`src/qwenpaw/app/driver_config_service.py`

- 本地驱动配置增加 `source_template` 字段，标记基于哪个企业模板创建
- 新增 `list_enterprise_templates()` 方法供前端展示

### 5.3 阶段 5 验收标准

- [ ] 登录后本地缓存企业 MCP 模板
- [ ] 前端可查看企业 MCP 模板列表
- [ ] 可基于模板创建本地 MCP 实例
- [ ] 本地实例配置不被模板更新覆盖
- [ ] 模板更新时前端有提示

---

## 交付物清单

### 新增文件

| 文件 | 阶段 | 说明 |
|------|------|------|
| `src/qwenpaw/sync/__init__.py` | 0 | 同步层包入口 |
| `src/qwenpaw/sync/client.py` | 0 | pig HTTP 客户端 |
| `src/qwenpaw/sync/auth_adapter.py` | 0 | OAuth2 token 管理 |
| `src/qwenpaw/sync/errors.py` | 0 | 同步层异常 |
| `src/qwenpaw/sync/lifecycle.py` | 0 | 生命周期挂钩 |
| `src/qwenpaw/app/routers/sync_auth.py` | 1 | OAuth2 回调路由 |
| `src/qwenpaw/sync/memory_sync.py` | 2 | 记忆同步管理器 |
| `src/qwenpaw/sync/credential_sync.py` | 3 | 企业凭据缓存 |
| `src/qwenpaw/sync/skill_sync.py` | 4 | 技能同步管理器 |
| `src/qwenpaw/sync/mcp_sync.py` | 5 | MCP 模板同步 |

### 修改文件

| 文件 | 阶段 | 改动范围 |
|------|------|---------|
| `src/qwenpaw/config/config.py` | 0 | 新增 `SyncConfig` 及子配置 |
| `src/qwenpaw/app/_app.py` | 0 | lifespan 中初始化同步层 |
| `src/qwenpaw/app/auth.py` | 1 | `verify_token()` 增加 pig JWT 分支 |
| `src/qwenpaw/app/routers/auth.py` | 1 | `AuthStatusResponse` 扩展 |
| `console/src/pages/Login/index.tsx` | 1 | pig SSO 跳转 |
| `console/src/api/modules/auth.ts` | 1 | auth status 类型扩展 |
| `src/qwenpaw/agents/context/scroll/history.py` | 2 | `append()` 增加 sync hook |
| `src/qwenpaw/drivers/credentials/store.py` | 3 | `get()` 增加企业凭据 fallback |
| `src/qwenpaw/agents/skill_system/store.py` | 4 | source 字段扩展 |
| `src/qwenpaw/app/routers/skills.py` | 4 | 企业技能只读保护 |
| `src/qwenpaw/app/driver_config_service.py` | 5 | `source_template` 字段 |

### pig 服务端需实现的接口

| 接口 | 阶段 | 说明 |
|------|------|------|
| `GET /oauth2/jwks` | 1 | JWT 公钥（Spring Authorization Server 自带） |
| `GET /api/sync/memory?since={seq}` | 2 | 记忆增量下行 |
| `POST /api/sync/memory` | 2 | 记忆上行 |
| `GET /api/sync/memory/meta` | 2 | 服务端最新 seq（对账） |
| `GET /api/sync/credentials` | 3 | 企业凭据列表 |
| `GET /api/sync/skills/manifest` | 4 | 技能 manifest |
| `GET /api/sync/skills/{name}/download` | 4 | 技能包下载 |
| `POST /api/sync/skills/upload` | 4 | 个人技能上传（可选） |
| `GET /api/sync/mcp/templates` | 5 | MCP 模板列表 |

---

## 测试策略

### 单元测试

每个阶段对应的 `tests/sync/` 下编写：

- `test_client.py`：PigClient 重试/退避/超时
- `test_auth_adapter.py`：token 刷新/过期/持久化
- `test_memory_sync.py`：上行批量/下行增量/dedup 幂等
- `test_credential_sync.py`：fallback 优先级/加密缓存
- `test_skill_sync.py`：manifest 比对/增量下载/只读保护

### 集成测试

- `e2e/sync/` 下编写端到端测试：
  - 模拟 pig 服务端（mock HTTP）
  - 登录 → 拉取 → 工作 → 上行 → 退出 全流程
  - 断网场景模拟（pig 不可达）

### 关键测试场景

| 场景 | 验证点 |
|------|--------|
| `sync.enabled=false` | 所有行为与当前版本一致 |
| pig 不可达启动 | 本地正常启动，同步层静默跳过 |
| 工作中断网 | 本地记忆正常读写，上行队列堆积 |
| 联网恢复 | 上行队列自动补传 |
| 多设备并发 | 同一 dedup_key 不重复 |
| JWT 过期 | 前端重新跳转登录 |
| 企业技能更新 | 增量下载，不影响个人技能 |

---

## 回滚策略

### 精细回滚

每个阶段可通过配置独立关闭：

```yaml
sync:
  enabled: true
  memory:
    sync_enabled: false    # 关闭记忆同步
  credential:
    cache_enterprise: false # 关闭凭据拉取
  skill:
    auto_pull_enterprise: false # 关闭技能拉取
```

### 全量回滚

```yaml
sync:
  enabled: false  # 一键退化为纯本地模式
```

或环境变量：

```bash
QWENPAW_SYNC_ENABLED=false
```

### 数据回滚

- 记忆：本地 SQLite 始终是完整副本，关闭同步后不丢失任何数据
- 凭据：个人凭据始终在本地 YAML，企业缓存可删除（`rm SECRET_DIR/enterprise_credentials.yaml`）
- 技能：个人技能始终在 `workspace/skills/`，企业技能在 `skill_pool/` 可保留或删除
- MCP：本地实例配置不受影响

---

*文档创建日期：2026-07-30*
*配套架构方案：[architecture.md](./architecture.md)*
