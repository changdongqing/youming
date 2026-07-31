# QwenPaw 端适配调整清单

> **文档定位**：pig 服务端对接 QwenPaw 已完成（见 `pig-side-implementation-plan.md`），本文档列出 QwenPaw 客户端基于其改造文档（`改造qwenpaw文档参考/phase0-5`）落地时，**因 pig 实际架构与文档假设不符、或路径/字段需对齐而必须或建议调整的地方**。纯客户端内部逻辑（不涉及与服务端契约的）不列入。
>
> **配套文档**：[qwenpaw-adaptation.md](./qwenpaw-adaptation.md) · [pig-side-implementation-plan.md](./pig-side-implementation-plan.md) · [改造qwenpaw文档参考/](./改造qwenpaw文档参考/)
>
> **制定日期**：2026-07-31

---

## 级别说明

| 级别 | 含义 |
|------|------|
| 🔴 必须改 | 不改则无法对接，pig 与 QwenPaw 契约不兼容 |
| 🟡 建议改 | 不改能跑，但语义/健壮性受损，存在潜在坑 |
| 🟢 无需改 | pig 实现与文档假设已对齐，列出供确认 |

---

## 一、🔴 必须改（最小可对接集：1 + 2 + 3）

### 1. 认证模式：从「JWT 本地验签」改为「opaque token 持有」

**文档假设**（phase1 §2.1-2.2）：从 pig 的 `/oauth2/jwks` 拉取 RS256 公钥，用 PyJWT 本地验签，检查 `sub`/`exp` claim。

**pig 实际**：pig-auth 用 `OAuth2TokenFormat.REFERENCE`（opaque 不透明令牌，格式 `client:username:uuid`），资源服务器经 `PigCustomOpaqueTokenIntrospector` 通过 Redis 内省。**无 `JwtDecoder`，无 JWKS 端点可用，token 无法本地解码。**

**调整内容**：
- `src/qwenpaw/app/auth.py`：
  - 删除 `_fetch_pig_jwks_async`、`_verify_pig_jwt_sync`、`_verify_pig_jwt_async` 整套 JWKS 验签逻辑
  - `verify_token()` 在 pig 模式下**不做本地验签**——仅校验 `Authorization: Bearer <token>` 存在即放行
  - `AuthMiddleware.dispatch`：pig 模式下 token 有效性由 pig 服务端在首次 401 时反馈，客户端据此触发刷新
- `src/qwenpaw/sync/auth_adapter.py`：
  - `PigTokenProvider.get_access_token()` 保持原逻辑（持有 + 过期前刷新）
  - 刷新依赖 `expires_at`（授权码流程换 token 时 pig 返回的 `expires_in` 计算），不依赖 JWT 解码
- `pyproject.toml`：`PyJWT` 依赖**不再需要**（除非项目别处用到 cryptography 的其他功能）

**验证点**：pig 模式下，QwenPaw 带 opaque Bearer token 调 `/sync/memory`，pig 的 opaque-token 过滤链内省通过即 200；token 失效时 pig 返回 401，QwenPaw 触发 refresh。

---

### 2. 路径前缀：从 `/api/sync/...` 改为 `/sync/...`（配合 base_url 带 `/admin`）

**文档假设**：调用 `/api/sync/memory` 等，pig 有 `/api/` 命名空间。

**pig 实际**：pig-boot context-path = `/admin`，controller 映射 `/sync/...`，外部路径 `/admin/sync/memory`。**pig 全仓无 `/api/` 前缀。**

**调整方案**（推荐方案 A）：
- `config.yaml` 的 `sync.pig.base_url` 设为 `http://<pig-host>:9999/admin`
- 客户端代码所有 `/api/sync/...` 字符串改为 `/sync/...`（base_url 已带 `/admin`，拼接后为 `/admin/sync/...`）

> 备选方案 B：base_url 设为 `http://<host>:9999`，客户端路径从 `/api/sync/...` 改为 `/admin/sync/...`。两方案二选一，方案 A 改动更少。

**涉及文件与改动点**：

| 文件 | 原路径 | 改为 |
|------|--------|------|
| `src/qwenpaw/sync/memory_sync.py` | `/api/sync/memory`（GET 下行） | `/sync/memory` |
| `src/qwenpaw/sync/memory_sync.py` | `/api/sync/memory`（POST 上行） | `/sync/memory` |
| `src/qwenpaw/sync/memory_sync.py` | `/api/sync/memory/meta` | `/sync/memory/meta` |
| `src/qwenpaw/sync/credential_sync.py` | `/api/sync/credentials` | `/sync/credentials` |
| `src/qwenpaw/sync/skill_sync.py` | `/api/sync/skills/manifest` | `/sync/skills/manifest` |
| `src/qwenpaw/sync/skill_sync.py` | `/api/sync/skills/{name}/download` | `/sync/skills/{name}/download` |
| `src/qwenpaw/sync/skill_sync.py` | `/api/sync/skills/upload` | `/sync/skills/upload` |
| `src/qwenpaw/sync/mcp_sync.py` | `/api/sync/mcp/templates` | `/sync/mcp/templates` |

**验证点**：`GET http://<host>:9999/admin/sync/memory/meta` 带 Bearer 返回 `{max_seq: 0}`（新用户）即路径通。

---

### 3. OAuth2 redirect_uri 对齐

**文档假设**（phase1 §3.1 + pig 侧 V23 种子）：回调地址 `http://localhost:8000/api/sync/oauth/callback`。

**pig 实际**：QwenPaw 自身路由 `sync_auth.py` 用 `prefix="/api/sync/oauth"`，但 pig 无 `/api` 前缀约定。且 pig 侧 `sys_oauth_client_details.web_server_redirect_uri` 必须与 QwenPaw 实际回调 URL **完全一致**，否则授权码换 token 时 pig 拒绝。

**调整内容**：
- QwenPaw 侧 `src/qwenpaw/app/routers/sync_auth.py`：`APIRouter(prefix="/api/sync/oauth")` → `prefix="/sync/oauth"`（与第 2 项保持一致）
- QwenPaw 侧 `_PUBLIC_PATHS` 放行路径同步去掉 `/api`
- pig 侧 V23 种子 `web_server_redirect_uri` 同步改为 `http://localhost:8000/sync/oauth/callback`（**此项由 pig 侧改，已确认**）
- 若 QwenPaw 部署在非 8000 端口或带反向代理，按实际回调地址调整两侧

**验证点**：浏览器走授权码流程 → pig 登录页 → 回调 QwenPaw `/sync/oauth/callback?code=xxx` → 换到 opaque token。

---

## 二、🟡 建议改（不改能跑，但有坑）

### 4. 记忆上行的 `seq` 语义

**文档假设**（phase2 §2.5）：客户端上传 `entries[].seq` 是本地 seq，服务端自增 `server_seq` 独立分配（暗示连续递增整数）。

**pig 实际**：服务端 `seq` 用**雪花 ID**（MyBatis-Plus `IdType.ASSIGN_ID`，非自增整数），客户端上传的 `seq` 存入 `client_seq` 字段，服务端 `seq` 独立生成。

**影响与调整**：
- 客户端拉取时拿到的 `next_seq` 是雪花大整数（如 `1893456789012345`），**非连续、非小整数**
- `last_sync_seq` 初始值 0 仍有效（`seq > 0` 能拉全量）
- Python `int` 无溢出，游标逻辑无碍
- **建议**：`memory_sync.py` 的日志/UI 不要把 seq 当成"第 N 条记录"展示；增量比对只依赖 `seq > since` 语义，不假设连续性
- **无需改代码逻辑**，仅注意展示与日志措辞

---

### 5. 技能 manifest 的 `version` 字段

**文档假设**（phase4 §7.1）：`{version: 15, skills: [...]}`，version 是整体递增计数器。

**pig 实际**：`version` = `max(skills.enterprise_version)`，无记录时为 0。不是独立的递增计数器。

**影响与调整**：
- 客户端用本地 `manifest.version` 与服务端 `version` 比对——**语义基本一致**（version 变大 = 有技能更新）
- 但若某技能 `enterprise_version` 回退（理论上不会发生，但防御性考虑），`version` 可能下降
- **建议**：`skill_sync.py` 的 `pull_enterprise_skills` 比对逻辑保持 `server_ev > local_ev`（当前代码已如此，正确），**不要额外加"version 下降 = 无需更新"的短路判断**
- 逐技能比对 `enterprise_version` 是最可靠的方式，整体 `version` 仅作快速判断

---

### 6. 凭据 `secrets` 传输安全

**pig 实现**：`secret_data` 在库中由 Jasypt `StringEncryptor` 加密，**返回时已解密**为明文 Map，响应体里 `secrets` 直接是明文。

**文档假设**（phase3 §6.1）：响应 `secrets: {api_key: "sk-xxx"}` 明文。**契约一致，QwenPaw 无需解密。**

**建议**：
- 传输层**必须走 TLS**——pig 侧无法强制，需 QwenPaw 确保生产环境 `pig.base_url` 是 `https://`
- 本地开发可用 `http://`，但生产环境严禁明文走 HTTP
- QwenPaw 本地缓存的 `enterprise_credentials.yaml` 仍用 Fernet 加密（phase3 原设计不变）

---

## 三、🟢 无需改（已对齐，列出供确认）

以下契约 pig 实现已与 QwenPaw 文档假设完全一致，**无需任何调整**：

| 契约项 | 对齐情况 |
|--------|---------|
| 记忆条目字段名 | `session_id/agent_id/kind/role/name/content/tool_call_id/tool_input/tool_state/headline/blocks/metadata/created_at/dedup_key` —— pig 实体 camelCase 字段完全一致 |
| 幂等去重 | `ON CONFLICT (session_id, dedup_key, user_id) DO NOTHING` —— pig mapper 实现一致 |
| 记忆下行响应 | `{entries, next_seq, has_more}` —— pig VO 字段名一致 |
| 记忆上行响应 | `{accepted, deduped}` —— pig VO 一致 |
| 记忆对账响应 | `{max_seq}` —— pig VO 一致 |
| 凭据响应结构 | `{credentials:[{ref, kind, public, secrets, meta}]}` —— pig VO 一致（public/secrets/meta 均为 Map） |
| 凭据可见范围 | 用户专属 + `user_id IS NULL` 全员可用 —— pig 实现一致 |
| 技能 manifest 响应 | `{version, skills:[{name, version, md_hash, enterprise_version, description}]}` —— pig VO 一致 |
| 技能下载 | `application/zip` 流式 + `Content-Disposition: attachment` —— pig 实现一致 |
| 技能上传 | `multipart/form-data`，字段 `file` —— pig 实现一致 |
| 技能上传响应 | `{name, stored, version}` —— pig VO 一致 |
| MCP 模板响应 | `{templates:[{name, endpoint, description, version, protocol, transport, credential_ref, default_policy, capabilities}]}` —— pig VO 一致（capabilities 为 List） |
| userId 提取 | 从 Bearer token 经 pig 内省得到 `PigUser.id`，强制覆盖每条记录防越权 —— pig 实现，QwenPaw 无感知 |
| 认证端点 | `/oauth2/authorize`、`/oauth2/token`（授权码换 token / refresh）—— Spring Authorization Server 自带，QwenPaw 原设计可用 |

---

## 四、改动汇总（工作项排期）

| # | 级别 | 涉及文件 | 改动摘要 | 工作量 |
|---|------|---------|---------|--------|
| 1 | 🔴 | `app/auth.py`、`sync/auth_adapter.py`、`pyproject.toml` | 删 JWKS 验签，改 opaque token 持有 + 401 刷新 | 0.5-1 天 |
| 2 | 🔴 | `sync/memory_sync.py` 等 5 个 sync 文件 | 路径 `/api/sync/` → `/sync/`（约 8 处字符串） | 0.5 天 |
| 3 | 🔴 | `app/routers/sync_auth.py`、`app/auth.py`(_PUBLIC_PATHS) | 回调路由前缀去 `/api`；pig 侧 V23 种子同步改（pig 侧负责） | 0.5 天 |
| 4 | 🟡 | `sync/memory_sync.py`（日志/UI） | seq 按雪花大整数处理，不展示为"第N条" | 0.5 天 |
| 5 | 🟡 | `sync/skill_sync.py` | version 比对保持 `>`，勿加下降短路 | 0 天（确认即可） |
| 6 | 🟡 | 配置文档/部署说明 | 生产 `base_url` 必须 https | 0 天（文档说明） |

**最小可对接集**：完成 1 + 2 + 3 即可端到端跑通全部 8 个端点。

---

## 五、联调验证清单

完成上述调整后，按以下顺序联调：

1. **pig 启动**：`mvn -P boot spring-boot:run`（pig-boot 单体，9999 端口），Flyway 自动应用 V23/V24
2. **OAuth2 登录**：浏览器访问 pig 授权页 → 回调 QwenPaw → 拿到 opaque token
3. **记忆对账**：`GET /admin/sync/memory/meta` → `{max_seq: 0}`
4. **记忆上行**：`POST /admin/sync/memory` 上传 1 条 → `{accepted: 1, deduped: 0}`
5. **记忆幂等**：重复上传同一 `dedup_key` → `{accepted: 0, deduped: 1}`
6. **记忆下行**：`GET /admin/sync/memory?since=0` → 返回刚上传的记录
7. **凭据拉取**：`GET /admin/sync/credentials` → 返回凭据列表（需先在 pig 后台录入企业凭据）
8. **技能 manifest**：`GET /admin/sync/skills/manifest` → `{version: 0, skills: []}`（无技能时）
9. **MCP 模板**：`GET /admin/sync/mcp/templates` → `{templates: []}`（无模板时）

> 端点 7-9 需先在 pig 后台录入测试数据（企业凭据/技能/MCP 模板），录入管理界面属 pig 侧后续工作（不在本次端点开发范围）。

---

*文档创建日期：2026-07-31*
*配套文档：[pig-side-implementation-plan.md](./pig-side-implementation-plan.md) · [qwenpaw-adaptation.md](./qwenpaw-adaptation.md)*
