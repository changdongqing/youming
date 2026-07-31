# 阶段 3 详细设计：企业凭据拉取

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)
>
> **目标**：登录时拉取企业 API Key 到本地加密缓存，使用时个人 Key 优先、企业 Key 回退，个人凭据永不上传。
>
> **预估工期**：2-3 天 · **依赖**：阶段 0 + 阶段 1

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. 企业凭据缓存层](#2-企业凭据缓存层)
- [3. AsyncCredentialStore fallback 改造](#3-asynccredentialstore-fallback-改造)
- [4. 实例化注入](#4-实例化注入)
- [5. 定时同步](#5-定时同步)
- [6. pig 服务端接口约定](#6-pig-服务端接口约定)
- [7. 安全设计](#7-安全设计)
- [8. 验收标准](#8-验收标准)

---

## 1. 整体架构

### 1.1 凭据解析优先级

```
Agent 请求凭据 ref (如 "openai_api_key")
    │
    ▼
AsyncCredentialStore.get("openai_api_key")
    │
    ├─ ① env: 前缀? → 直接读环境变量 (现有逻辑, 第53行)
    │
    ├─ ② 查本地 personal YAML (现有逻辑, credentials.yaml)
    │     └─ 命中 → 返回 CredentialRecord
    │
    ├─ ③ 查企业凭据缓存 (新增 fallback)
    │     └─ 命中 → 返回 CredentialRecord
    │
    └─ 全未命中 → raise CredentialNotFoundError
```

### 1.2 数据流

```
┌─────────────────────────────────────────────────────┐
│                    QwenPaw 本地                      │
│                                                     │
│  credentials.yaml         enterprise_credentials   │
│  (个人凭据, 现有)          .yaml (企业凭据缓存)      │
│      │                         ▲                   │
│      │ Fernet加密                │ Fernet加密        │
│      ▼                         │                   │
│  AsyncCredentialStore ◄────────┘                   │
│  .get() 优先 personal → enterprise → env           │
└─────────────────┬───────────────────────────────────┘
                  │ 登录时拉取 + 定时同步
                  ▼
            ┌──────────┐
            │ pig 服务端│
            │ 企业凭据  │
            └──────────┘

个人凭据: 纯本地, 永不上传 ←── 安全隔离
企业凭据: 服务端管理, 本地加密缓存 ←── 只读拉取
```

---

## 2. 企业凭据缓存层

**新建文件**：`src/qwenpaw/sync/credential_sync.py`

### 2.1 类定义

```python
# -*- coding: utf-8 -*-
"""企业凭据本地加密缓存。

从 pig 拉取企业统一 API Key，加密缓存到本地。
与个人凭据（credentials.yaml）独立存储，使用时 personal 优先、enterprise 回退。

安全原则：
- 企业凭据在服务端加密存储，传输走 TLS
- 本地缓存复用 secret_store.py 的 Fernet 加密
- 个人凭据永不上传服务端
"""
from __future__ import annotations

import asyncio
import logging
import threading
from pathlib import Path
from typing import Any

import yaml

from ..config.config import CredentialSyncConfig
from ..constant import SECRET_DIR
from ..drivers.credentials.types import CredentialRecord
from ..drivers.errors import CredentialNotFoundError
from ..security.secret_store import decrypt, encrypt, is_encrypted
from .client import PigClient
from .errors import SyncError

logger = logging.getLogger(__name__)

_CACHE_FILE = SECRET_DIR / "enterprise_credentials.yaml"
_CACHE_VERSION = 1

# 企业凭据中的 secrets 字段需要加密的字段名
_ENTERPRISE_SECRET_FIELDS = frozenset({"api_key", "secret", "token", "password"})


class EnterpriseCredentialCache:
    """企业凭据本地加密缓存。

    存储路径: SECRET_DIR/enterprise_credentials.yaml
    加密方式: 复用 secret_store.py 的 Fernet 加密
    生命周期: 由 SyncLifecycle 持有。
    """

    def __init__(
        self,
        pig_client: PigClient,
        config: CredentialSyncConfig,
    ) -> None:
        self._pig = pig_client
        self._config = config
        self._lock = threading.RLock()
        self._cache: dict[str, dict] = {}
        self._load_cache()

    # ================================================================
    # 持久化
    # ================================================================

    def _load_cache(self) -> None:
        """从磁盘加载企业凭据缓存（解密 secrets）。"""
        if not _CACHE_FILE.is_file():
            return
        try:
            data = yaml.safe_load(
                _CACHE_FILE.read_text(encoding="utf-8"),
            )
            if not data or not isinstance(data, dict):
                return
            credentials = data.get("credentials", {})
            # 解密每条记录的 secrets
            for ref, entry in credentials.items():
                if isinstance(entry, dict):
                    secrets = entry.get("secrets", {})
                    if isinstance(secrets, dict):
                        entry["secrets"] = {
                            k: decrypt(v) if is_encrypted(v) else v
                            for k, v in secrets.items()
                        }
            self._cache = credentials
        except Exception as exc:
            logger.warning("Failed to load enterprise credentials: %s", exc)

    def _save_cache(self) -> None:
        """持久化企业凭据缓存到磁盘（加密 secrets）。"""
        _CACHE_FILE.parent.mkdir(parents=True, exist_ok=True)

        # 深拷贝并加密 secrets
        encrypted_cache: dict[str, dict] = {}
        for ref, entry in self._cache.items():
            if not isinstance(entry, dict):
                continue
            encrypted_entry = dict(entry)
            secrets = encrypted_entry.get("secrets", {})
            if isinstance(secrets, dict):
                encrypted_entry["secrets"] = {
                    k: encrypt(v) if v and not is_encrypted(v) else v
                    for k, v in secrets.items()
                }
            encrypted_cache[ref] = encrypted_entry

        payload = {
            "version": _CACHE_VERSION,
            "credentials": encrypted_cache,
        }
        _CACHE_FILE.write_text(
            yaml.dump(payload, allow_unicode=True, default_flow_style=False),
            encoding="utf-8",
        )
        try:
            _CACHE_FILE.chmod(0o600)
        except OSError:
            pass

    # ================================================================
    # 读取接口（供 AsyncCredentialStore fallback 调用）
    # ================================================================

    def get(self, ref: str) -> CredentialRecord | None:
        """从本地缓存读取企业凭据。

        Returns:
            CredentialRecord 或 None（未缓存）。
        """
        with self._lock:
            entry = self._cache.get(ref)
            if not entry or not isinstance(entry, dict):
                return None
            return CredentialRecord(
                ref=ref,
                kind=str(entry.get("kind", "")),
                public=dict(entry.get("public", {})),
                secrets=dict(entry.get("secrets", {})),
                meta={**dict(entry.get("meta", {})),
                      "source": "enterprise"},
            )

    def list_refs(self) -> list[str]:
        """列出已缓存的企业凭据 ref。"""
        with self._lock:
            return sorted(self._cache.keys())

    # ================================================================
    # 同步（从 pig 拉取）
    # ================================================================

    async def pull(self) -> int:
        """从 pig 拉取企业凭据，加密后写入本地缓存。

        全量替换策略：服务端为权威源，本地缓存整体替换。
        （企业凭据由管理员在 pig 端管理，本地不应有未同步的变更）

        Returns:
            拉取的凭据数量。
        """
        try:
            resp = await self._pig.get("/api/sync/credentials")
            credentials = resp.get("credentials", [])

            with self._lock:
                self._cache = {}
                for cred in credentials:
                    ref = cred.get("ref", "")
                    if not ref:
                        continue
                    self._cache[ref] = {
                        "kind": cred.get("kind", ""),
                        "public": dict(cred.get("public", {})),
                        "secrets": dict(cred.get("secrets", {})),
                        "meta": dict(cred.get("meta", {})),
                    }
                self._save_cache()

            count = len(self._cache)
            logger.info(
                "Enterprise credentials pulled: %d entries",
                count,
            )
            return count

        except SyncError as exc:
            logger.warning("Enterprise credential pull failed: %s", exc)
            return 0
        except Exception:
            logger.warning(
                "Enterprise credential pull unexpected error",
                exc_info=True,
            )
            return 0

    # ================================================================
    # 清除
    # ================================================================

    def clear(self) -> None:
        """清除本地企业凭据缓存。"""
        with self._lock:
            self._cache = {}
            try:
                _CACHE_FILE.unlink(missing_ok=True)
            except OSError:
                pass
```

### 2.2 缓存文件格式

```yaml
# ~/.qwenpaw.secret/enterprise_credentials.yaml (权限 0600)
version: 1
credentials:
  openai_api_key:
    kind: static
    public:
      provider: openai
    secrets:
      api_key: ENC:gAAAAABm...  # Fernet 加密
    meta:
      source: enterprise
      managed_by: pig
  anthropic_api_key:
    kind: static
    public:
      provider: anthropic
    secrets:
      api_key: ENC:gAAAAABm...
    meta:
      source: enterprise
```

---

## 3. AsyncCredentialStore fallback 改造

### 3.1 构造函数扩展

**文件**：`src/qwenpaw/drivers/credentials/store.py`

```python
class AsyncCredentialStore:
    """Async per-workspace YAML credential store.

    支持企业凭据 fallback：当 sync 启用时，
    get() 在个人凭据未命中时回退查询企业凭据缓存。
    """

    def __init__(
        self,
        credentials_path: Path,
        enterprise_cache=None,  # 新增: EnterpriseCredentialCache | None
    ) -> None:
        self._path = credentials_path
        self._lock = threading.RLock()
        self._enterprise_cache = enterprise_cache
```

### 3.2 get() 方法改造

现有 `get()` → `_get_sync()` 的链路改造：

```python
async def get(self, ref: str) -> CredentialRecord:
    """Read one CredentialRecord and decrypt values under secrets.

    解析优先级:
    1. env: 前缀 → 环境变量 (现有逻辑)
    2. 本地 personal YAML (现有逻辑)
    3. 企业凭据缓存 (新增 fallback, 仅 sync 启用时)
    """
    return await asyncio.to_thread(self._get_with_fallback, ref)


def _get_with_fallback(self, ref: str) -> CredentialRecord:
    """同步读取，带企业凭据 fallback。"""
    # env: 前缀优先（现有逻辑，不经过企业缓存）
    if ref.startswith("env:"):
        return self._get_sync(ref)

    # 查本地个人凭据
    try:
        return self._get_sync(ref)
    except CredentialNotFoundError:
        pass

    # fallback: 查企业凭据缓存
    if self._enterprise_cache:
        record = self._enterprise_cache.get(ref)
        if record is not None:
            return record

    # 全未命中
    raise CredentialNotFoundError(ref)
```

### 3.3 list_refs() 改造（可选）

```python
async def list_refs(self) -> list[str]:
    """Return sorted credential refs (个人 + 企业)。"""
    personal = await asyncio.to_thread(self._list_refs_sync)
    if self._enterprise_cache:
        enterprise = self._enterprise_cache.list_refs()
        return sorted(set(personal) | set(enterprise))
    return personal
```

### 3.4 不改动的部分

以下方法**完全不变**，确保个人凭据的写入/删除不受影响：

- `put()` → `_put_sync()`：只写本地 YAML
- `delete()` → `_delete_sync()`：只删本地 YAML
- `_encrypt_secrets()` / `_decrypt_secrets()`：加密逻辑不变

> 企业凭据是只读的，`put` / `delete` 不支持操作企业凭据 ref。

---

## 4. 实例化注入

### 4.1 实例化点清单

`AsyncCredentialStore` 有 2 个实例化点：

| # | 文件 | 行号 | 用途 |
|---|------|------|------|
| ① | `src/qwenpaw/app/workspace/service_factories.py` | 35 | **主路径**：workspace 的 DriverManager |
| ② | `src/qwenpaw/app/driver_config_service.py` | 47 | 驱动配置服务 |

### 4.2 注入方案

**文件**：`src/qwenpaw/app/workspace/service_factories.py`（主路径）

```python
async def create_driver_service(ws: "Workspace", _service):
    from ...drivers.adapters.mcp_legacy_config import (
        migrate_legacy_mcp_if_needed,
    )
    from ...drivers.credentials.store import AsyncCredentialStore
    from ...drivers.handlers import MCPDriverHandler
    from ...drivers.handlers.mcp import validate_mcp_endpoint
    from ...drivers.manager import DriverManager
    from ..approvals.driver_gate import QwenPawDriverApprovalGate

    # 获取企业凭据缓存（如果 sync 启用）
    enterprise_cache = _get_enterprise_cache()

    credential_store = AsyncCredentialStore(
        ws.workspace_dir / "credentials.yaml",
        enterprise_cache=enterprise_cache,  # 新增
    )
    driver_manager = DriverManager(
        ws.workspace_dir / "drivers",
        credential_store,
        approval_gate=QwenPawDriverApprovalGate(),
    )
    # ... 后续不变 ...
```

### 4.3 获取 EnterpriseCredentialCache

```python
# src/qwenpaw/app/workspace/service_factories.py 新增

def _get_enterprise_cache():
    """获取企业凭据缓存实例（如果 sync 启用）。

    从 app.state.sync_lifecycle 获取已注册的 EnterpriseCredentialCache。
    如果 sync 未启用或尚未初始化，返回 None。
    """
    try:
        from ...config import load_config
        cfg = load_config()
        if not (cfg.sync.enabled and cfg.sync.credential.cache_enterprise):
            return None
    except Exception:
        return None

    # 从全局获取（SyncLifecycle 注册的实例）
    try:
        from ...sync.lifecycle import get_sync_lifecycle
        sync_lc = get_sync_lifecycle()
        if sync_lc and sync_lc._credential_sync:
            return sync_lc._credential_sync
    except Exception:
        pass
    return None
```

**文件**：`src/qwenpaw/app/driver_config_service.py`（第 47 行同理注入）。

### 4.4 SyncLifecycle 注册

**文件**：`src/qwenpaw/sync/lifecycle.py`

在 `on_startup()` 中创建 `EnterpriseCredentialCache`：

```python
async def on_startup(self) -> None:
    # ... PigClient 初始化 ...

    # 创建企业凭据缓存
    if cfg.sync.credential.cache_enterprise:
        try:
            from .credential_sync import EnterpriseCredentialCache
            self._credential_sync = EnterpriseCredentialCache(
                self._pig_client,
                cfg.sync.credential,
            )
            self.register_credential_sync(self._credential_sync)
        except Exception:
            logger.warning("Enterprise credential cache init failed",
                           exc_info=True)
```

在 `on_login_success()` 中触发拉取：

```python
async def on_login_success(self, user: str) -> None:
    # ...
    if cfg.sync.credential.cache_enterprise and self._credential_sync:
        tasks.append((
            "credentials",
            asyncio.create_task(self._credential_sync.pull()),
        ))
    # ...
```

---

## 5. 定时同步

### 5.1 定时拉取

**文件**：`src/qwenpaw/sync/lifecycle.py`

`start_periodic_sync()` 中已预留（阶段 0 设计），实际实现：

```python
async def _periodic_loop(self) -> None:
    cfg = load_config()
    interval = cfg.sync.credential.sync_interval_minutes * 60

    while True:
        await asyncio.sleep(interval)
        try:
            if self._credential_sync:
                count = await self._credential_sync.pull()
                logger.debug(
                    "Periodic credential sync: %d entries",
                    count,
                )
        except Exception:
            logger.warning(
                "Periodic credential sync failed",
                exc_info=True,
            )
```

### 5.2 定时策略

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `sync_interval_minutes` | 30 | 每 30 分钟检查一次 |
| 拉取方式 | 全量替换 | 服务端为权威源 |
| 失败处理 | 静默重试 | 下次定时继续 |

> 全量替换的原因：企业凭据由管理员在 pig 端集中管理，本地不应有未同步的变更。全量替换确保始终与服务端一致。

---

## 6. pig 服务端接口约定

### 6.1 拉取企业凭据

```
GET /api/sync/credentials

Headers:
  Authorization: Bearer <access_token>

Response 200:
{
  "credentials": [
    {
      "ref": "openai_api_key",
      "kind": "static",
      "public": {
        "provider": "openai",
        "base_url": "https://api.openai.com/v1"
      },
      "secrets": {
        "api_key": "sk-xxxxxxxxxxxx"
      },
      "meta": {
        "managed_by": "pig",
        "updated_at": "2026-07-30T10:00:00Z"
      }
    },
    {
      "ref": "anthropic_api_key",
      "kind": "static",
      "public": {
        "provider": "anthropic"
      },
      "secrets": {
        "api_key": "sk-ant-xxxxxxxxxx"
      },
      "meta": {}
    }
  ]
}
```

### 6.2 pig 服务端实现要点（Java/Spring）

```java
// pig 侧需新建表
CREATE TABLE qwenpaw_enterprise_credentials (
    id          BIGSERIAL PRIMARY KEY,
    ref         VARCHAR(255) NOT NULL,          -- 凭据引用名
    kind        VARCHAR(50) NOT NULL DEFAULT 'static',
    public_data JSONB DEFAULT '{}',              -- 公开信息
    secret_data JSONB DEFAULT '{}',              -- 加密的 secrets
    meta_data   JSONB DEFAULT '{}',
    user_id     VARCHAR(255),                    -- 所属用户(可为空=全员可用)
    tenant_id   VARCHAR(255),                    -- 租户ID
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(ref, user_id, tenant_id)
);
```

> `secret_data` 在服务端应使用 pig 的加密能力加密存储，不应明文存库。传输给 QwenPaw 时走 TLS。

### 6.3 pig 端凭据管理界面

pig 管理后台需提供凭据管理页面，供管理员：

- 添加/编辑/删除企业 API Key
- 按 ref 名引用（如 `openai_api_key`）
- 查看 last_updated 时间

> 这部分属于 pig 侧的前端开发，不在 QwenPaw 范围内。

---

## 7. 安全设计

### 7.1 加密层级

```
┌─────────────────────────────────────────────┐
│ 传输层: TLS (HTTPS)                          │
│   pig ←→ QwenPaw 之间的凭据传输加密            │
├─────────────────────────────────────────────┤
│ 服务端存储层: pig 侧加密                       │
│   secret_data 在 PostgreSQL 中加密存储         │
│   (pig 的加密能力, 非 QwenPaw 范围)            │
├─────────────────────────────────────────────┤
│ 本地存储层: Fernet 加密 (复用现有)             │
│   enterprise_credentials.yaml 中 secrets 加密 │
│   主密钥: OS keychain / .master_key           │
├─────────────────────────────────────────────┤
│ 内存层: 最小化驻留                             │
│   仅在 get() 调用时解密, 不持久化明文          │
└─────────────────────────────────────────────┘
```

### 7.2 安全隔离

| 凭据类型 | 存储 | 上传 | 下行 |
|---------|------|------|------|
| 个人凭据 | 本地 YAML (Fernet加密) | **永不上传** | 不拉取 |
| 企业凭据 | 本地缓存 (Fernet加密) | 不可上传(只读) | 服务端拉取 |
| env: 引用 | 环境变量 | 不涉及 | 不涉及 |

### 7.3 文件权限

```python
# enterprise_credentials.yaml 权限 0600 (仅所有者可读写)
_CACHE_FILE.chmod(0o600)

# 位于 SECRET_DIR (权限 0700)
# ~/.qwenpaw.secret/
#   ├── .master_key          # Fernet 主密钥 (现有)
#   ├── auth.json            # 本地 auth (现有)
#   ├── pig_tokens.json      # pig OAuth2 token (阶段0)
#   └── enterprise_credentials.yaml  # 企业凭据缓存 (新增)
```

### 7.4 安全审计

- 企业凭据缓存文件有 `source: enterprise` 标记，前端可区分来源
- 日志中不输出凭据明文（`CredentialRecord.__repr__` 已 redact secrets）
- 企业凭据 ref 不可通过 `put` / `delete` 修改（只读保护）

---

## 8. 验收标准

### 功能验收

- [ ] 登录后 `enterprise_credentials.yaml` 生成在 `SECRET_DIR/`，权限 0600
- [ ] 文件中 secrets 字段以 `ENC:` 前缀加密存储
- [ ] Agent 使用 API Key 时，个人 Key 优先，无个人 Key 时回退企业 Key
- [ ] `env:` 引用仍正常工作（优先级不被破坏，`env:` 不经过企业缓存）
- [ ] 企业凭据缓存中 `meta.source = "enterprise"`，前端可区分来源
- [ ] 企业凭据 ref 不可通过 `put` / `delete` 修改

### 同步验收

- [ ] 登录时自动拉取企业凭据，日志显示 `Enterprise credentials pulled: N entries`
- [ ] 每 30 分钟定时同步一次（可配 `sync_interval_minutes`）
- [ ] pig 端企业凭据更新后，下次定时同步拉取到本地
- [ ] pig 不可达时本地正常使用已缓存的企业凭据

### 安全验收

- [ ] 个人凭据永不上传服务端（`put` 只写本地 YAML）
- [ ] 企业凭据缓存文件权限 0600
- [ ] 企业凭据缓存文件中 secrets 以 `ENC:` 加密
- [ ] 日志中不输出凭据明文

### Fallback 验收

- [ ] `credential.cache_enterprise=false` 时 `enterprise_cache=None`，零行为变化
- [ ] `sync.enabled=false` 时 `enterprise_cache=None`，`get()` 仅查本地
- [ ] 企业凭据缓存为空时，`get()` 回退到 `CredentialNotFoundError`

### 优先级验收

- [ ] 同名 ref：个人凭据优先于企业凭据
- [ ] `env:` ref：不查个人也不查企业，直接读环境变量
- [ ] `list_refs()` 返回个人 + 企业去重后的合集

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)*
