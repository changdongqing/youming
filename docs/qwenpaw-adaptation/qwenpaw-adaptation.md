# pig 服务端适配指南

> **文档定位**：QwenPaw 客户端已完成全部 6 个阶段的开发（阶段 0-5），本文档列出 pig 服务端需要提供的全部 API 端点、数据表和配置，以便两端打通。
>
> **pig 技术栈**：Spring Boot 4.0 + Spring Cloud 2025.1.2 + Spring Authorization Server + PostgreSQL + Redis + Nacos

---

## 目录

- [一、总体架构](#一总体架构)
- [二、OAuth2 认证配置](#二oauth2-认证配置)
- [三、pig 需提供的 API 端点](#三pig-需提供的-api-端点)
- [四、数据表设计](#四数据表设计)
- [五、实现优先级](#五实现优先级)
- [六、验收对照表](#六验收对照表)

---

## 一、总体架构

```
┌──────────────────────────────────────────────────────────┐
│                  QwenPaw 本地（已完成）                    │
│                                                          │
│  sync.enabled=true, auth_mode=pig                       │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐           │
│  │OAuth2  │ │Memory  │ │Creden- │ │Skill   │ MCP      │
│  │登录    │ │Sync    │ │tial    │ │Sync    │ Sync     │
│  │(阶段1) │ │(阶段2) │ │(阶段3) │ │(阶段4) │ (阶段5)  │
│  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘│
│      │          │          │          │          │     │
│      └──────────┴──────────┴──────────┴──────────┘     │
│                         │ Bearer JWT                   │
└─────────────────────────┼──────────────────────────────┘
                          │ HTTPS
                    ┌─────┴──────────────────┐
                    │   pig 服务端（待适配）   │
                    │                        │
                    │  ① OAuth2 认证中心 ✅   │ ← Spring Authorization Server 自带
                    │  ② /api/sync/* 端点 ❌  │ ← 需新建
                    │  ③ 数据表 ❌            │ ← 需新建
                    └────────────────────────┘
```

**结论**：pig 的 OAuth2 认证中心（pig-auth）是 Spring Authorization Server 自带的，**无需开发**。但 `/api/sync/*` 端点和对应的数据表**需要新建**。

---

## 二、OAuth2 认证配置

### 2.1 客户端注册

在 pig 管理后台注册 OAuth2 客户端：

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `client_id` | `qwenpaw` | QwenPaw 使用的 client_id |
| `client_secret` | （生成后填入 QwenPaw config.yaml） | 加密存储 |
| `grant_types` | `authorization_code`, `refresh_token` | 授权码 + 刷新 |
| `redirect_uri` | `http://localhost:8000/api/sync/oauth/callback` | 按部署调整 |
| `scopes` | `openid profile` | 标准 scope |
| `token_format` | JWT (RS256) | Spring Auth Server 默认 |
| `access_token 有效期` | 3600s（1小时） | |
| `refresh_token 有效期` | 604800s（7天） | |

### 2.2 Spring Authorization Server 自带端点

以下端点 **pig 已自带，无需开发**：

| 端点 | 说明 | QwenPaw 使用 |
|------|------|-------------|
| `GET /oauth2/authorize` | 授权页 | ✅ 前端跳转 |
| `POST /oauth2/token` | 令牌端点（code 换 token / refresh） | ✅ 后端调用 |
| `GET /oauth2/jwks` | JWKS 公钥 | ✅ JWT 本地验签 |

### 2.3 JWT Claims 约定

pig 签发的 JWT 需包含：

| claim | 说明 | QwenPaw 使用 |
|-------|------|-------------|
| `sub` | 用户标识 | ✅ 作为 username |
| `exp` | 过期时间戳 | ✅ 验证过期 |
| `iat` | 签发时间戳 | 标准 |
| `iss` | 签发方 | 标准 |
| `kid` | 公钥 ID | ✅ JWKS 匹配 |

> Spring Authorization Server 默认包含以上 claim。QwenPaw 也兼容 `user_name` claim。

---

## 三、pig 需提供的 API 端点

### 3.1 端点总览

| # | 方法 | 路径 | 阶段 | 说明 |
|---|------|------|------|------|
| 1 | GET | `/api/sync/memory?since={seq}&limit={n}` | 2 | 记忆增量下行 |
| 2 | POST | `/api/sync/memory` | 2 | 记忆批量上行 |
| 3 | GET | `/api/sync/memory/meta` | 2 | 服务端最新 seq（对账） |
| 4 | GET | `/api/sync/credentials` | 3 | 企业凭据列表 |
| 5 | GET | `/api/sync/skills/manifest` | 4 | 技能 manifest |
| 6 | GET | `/api/sync/skills/{name}/download` | 4 | 技能包下载（zip） |
| 7 | POST | `/api/sync/skills/upload` | 4 | 个人技能上传（可选） |
| 8 | GET | `/api/sync/mcp/templates` | 5 | MCP 模板列表 |

> 所有端点需要 `Authorization: Bearer <jwt>` 认证，从 JWT 的 `sub` claim 提取 `user_id` 做数据隔离。

### 3.2 记忆同步端点（阶段 2，最高优先级）

#### `GET /api/sync/memory`

**请求**：
```
GET /api/sync/memory?since=80&limit=500
Authorization: Bearer <jwt>
```

**响应**：
```json
{
  "entries": [
    {
      "seq": 81,
      "session_id": "abc123",
      "agent_id": "default",
      "kind": "model_turn",
      "role": "assistant",
      "name": "agent",
      "content": "Hello!",
      "tool_call_id": null,
      "tool_input": null,
      "tool_state": null,
      "headline": "Greeting",
      "blocks": "[{...}]",
      "metadata": null,
      "created_at": "2026-07-30T10:00:00Z",
      "dedup_key": "msg-uuid-123"
    }
  ],
  "next_seq": 81,
  "has_more": false
}
```

**服务端逻辑**：
- 从 JWT 提取 `user_id`
- `WHERE user_id = ? AND seq > ? ORDER BY seq LIMIT ?`
- `next_seq` = 最后一条记录的 seq
- `has_more` = 是否还有更多记录

#### `POST /api/sync/memory`

**请求**：
```
POST /api/sync/memory
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "entries": [
    { ... 同上格式, seq 是客户端本地 seq ... }
  ]
}
```

**响应**：
```json
{
  "accepted": 50,
  "deduped": 3
}
```

**服务端逻辑**：
- 从 JWT 提取 `user_id`
- 对每条记录 `INSERT ... ON CONFLICT(session_id, dedup_key, user_id) DO NOTHING`
- `accepted` = 实际插入数，`deduped` = 冲突跳过数
- 服务端自增 `server_seq`（独立于客户端 seq）

#### `GET /api/sync/memory/meta`

**请求**：
```
GET /api/sync/memory/meta
Authorization: Bearer <jwt>
```

**响应**：
```json
{
  "max_seq": 100
}
```

**服务端逻辑**：`SELECT MAX(server_seq) FROM qwenpaw_conversation_history WHERE user_id = ?`

### 3.3 企业凭据端点（阶段 3）

#### `GET /api/sync/credentials`

**请求**：
```
GET /api/sync/credentials
Authorization: Bearer <jwt>
```

**响应**：
```json
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
    }
  ]
}
```

**服务端逻辑**：
- 从 JWT 提取 `user_id` 和 `tenant_id`
- 查询该用户/租户可访问的企业凭据
- `secrets` 在数据库中加密存储，返回时解密（传输走 TLS）

### 3.4 企业技能端点（阶段 4）

#### `GET /api/sync/skills/manifest`

**请求**：
```
GET /api/sync/skills/manifest
Authorization: Bearer <jwt>
```

**响应**：
```json
{
  "version": 15,
  "skills": [
    {
      "name": "browser",
      "version": "2.1.0",
      "md_hash": "sha256:abcdef...",
      "enterprise_version": 3,
      "description": "Browser automation skill"
    }
  ]
}
```

#### `GET /api/sync/skills/{name}/download`

**请求**：
```
GET /api/sync/skills/browser/download
Authorization: Bearer <jwt>
```

**响应**：`binary (application/zip)`

zip 包结构：
```
browser.zip
  ├── SKILL.md
  ├── scripts/
  │   └── browser.py
  └── config.yaml
```

#### `POST /api/sync/skills/upload`（可选）

**请求**：`multipart/form-data`，字段 `file` = zip

**响应**：
```json
{
  "name": "my-custom-skill",
  "stored": true,
  "version": "0.1.0"
}
```

### 3.5 MCP 模板端点（阶段 5）

#### `GET /api/sync/mcp/templates`

**请求**：
```
GET /api/sync/mcp/templates
Authorization: Bearer <jwt>
```

**响应**：
```json
{
  "templates": [
    {
      "name": "jira-mcp",
      "endpoint": "https://jira.company.com/mcp/sse",
      "description": "企业 Jira MCP 服务",
      "version": "2.0",
      "protocol": "mcp",
      "transport": "sse",
      "credential_ref": "jira_token",
      "default_policy": "ask",
      "capabilities": [
        {"name": "create_issue", "policy": "ask"},
        {"name": "search_issues", "policy": "allow"}
      ]
    }
  ]
}
```

---

## 四、数据表设计

### 4.1 记忆表（阶段 2）

```sql
CREATE TABLE qwenpaw_conversation_history (
    seq          BIGSERIAL PRIMARY KEY,          -- 服务端全局自增
    session_id   VARCHAR(255) NOT NULL,
    agent_id     VARCHAR(255),
    kind         VARCHAR(50) NOT NULL,            -- model_turn | context_msg | tool_result
    role         VARCHAR(50),
    name         VARCHAR(255),
    content      TEXT,
    tool_call_id VARCHAR(255),
    tool_input   TEXT,                            -- JSON
    tool_state   VARCHAR(50),
    headline     VARCHAR(500),
    blocks       TEXT,                            -- JSON
    metadata     TEXT,                            -- JSON
    created_at   TIMESTAMP,
    dedup_key    VARCHAR(255),
    user_id      VARCHAR(255) NOT NULL,           -- 从 JWT sub 提取
    tenant_id    VARCHAR(255),                    -- 租户隔离
    client_seq   BIGINT,                          -- 客户端原始 seq（可选）
    UNIQUE(session_id, dedup_key, user_id)       -- 幂等去重
);

CREATE INDEX idx_qch_user_seq ON qwenpaw_conversation_history(user_id, seq);
CREATE INDEX idx_qch_session  ON qwenpaw_conversation_history(session_id);
```

### 4.2 企业凭据表（阶段 3）

```sql
CREATE TABLE qwenpaw_enterprise_credentials (
    id          BIGSERIAL PRIMARY KEY,
    ref         VARCHAR(255) NOT NULL,             -- 凭据引用名
    kind        VARCHAR(50) NOT NULL DEFAULT 'static',
    public_data JSONB DEFAULT '{}',                 -- 公开信息
    secret_data JSONB DEFAULT '{}',                 -- 加密的 secrets（服务端加密）
    meta_data   JSONB DEFAULT '{}',
    user_id     VARCHAR(255),                      -- 所属用户（NULL = 全员可用）
    tenant_id   VARCHAR(255),                      -- 租户
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(ref, user_id, tenant_id)
);
```

> `secret_data` 在服务端必须加密存储，不能明文。建议使用 pig 的加密能力或 Jasypt。

### 4.3 企业技能表（阶段 4）

```sql
CREATE TABLE qwenpaw_enterprise_skills (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    version           VARCHAR(50),                  -- 技能自身版本
    md_hash           VARCHAR(255),
    enterprise_version INTEGER NOT NULL DEFAULT 1,  -- 每次更新递增
    description       TEXT,
    storage_path      VARCHAR(500) NOT NULL,        -- 对象存储路径
    tenant_id         VARCHAR(255),
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);

-- 个人技能上传备份（可选）
CREATE TABLE qwenpaw_personal_skills (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    version      VARCHAR(50),
    md_hash      VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    user_id      VARCHAR(255) NOT NULL,
    tenant_id    VARCHAR(255),
    created_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, user_id, tenant_id)
);
```

> 技能包文件建议存对象存储（MinIO/OSS），数据库只存元数据和路径。

### 4.4 MCP 模板表（阶段 5）

```sql
CREATE TABLE qwenpaw_mcp_templates (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    endpoint        VARCHAR(500) NOT NULL,
    description     TEXT,
    version         VARCHAR(50) NOT NULL,
    protocol        VARCHAR(50) DEFAULT 'mcp',
    transport      VARCHAR(50) DEFAULT 'sse',
    credential_ref VARCHAR(255),                  -- 引用 enterprise_credentials.ref
    default_policy VARCHAR(50) DEFAULT 'ask',
    capabilities    JSONB DEFAULT '[]',
    tenant_id       VARCHAR(255),
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);
```

---

## 五、实现优先级

按 QwenPaw 客户端的使用顺序，pig 服务端应按以下优先级实现：

| 优先级 | 端点 | 数据表 | 说明 |
|--------|------|--------|------|
| **P0** | OAuth2 客户端注册 | 无 | pig 管理后台操作，无需编码 |
| **P0** | `/oauth2/jwks` | 无 | Spring Auth Server 自带 |
| **P1** | `GET/POST /api/sync/memory` + `GET /api/sync/memory/meta` | `qwenpaw_conversation_history` | 记忆同步，价值最高 |
| **P2** | `GET /api/sync/credentials` | `qwenpaw_enterprise_credentials` | 企业凭据 |
| **P3** | `GET /api/sync/skills/manifest` + `GET /api/sync/skills/{name}/download` | `qwenpaw_enterprise_skills` + 对象存储 | 企业技能 |
| **P4** | `GET /api/sync/mcp/templates` | `qwenpaw_mcp_templates` | MCP 模板 |
| **P5** | `POST /api/sync/skills/upload` | `qwenpaw_personal_skills` | 个人技能备份（可选） |

### pig 侧 Spring Boot 实现建议

```
pig
├── pig-auth              ← OAuth2 认证中心（已有，无需改动）
├── pig-module-sync       ← 新建模块
│   ├── pom.xml
│   ├── src/main/java/com/pig4cloud/pig/sync/
│   │   ├── controller/
│   │   │   ├── MemorySyncController.java      ← /api/sync/memory/**
│   │   │   ├── CredentialSyncController.java   ← /api/sync/credentials
│   │   │   ├── SkillSyncController.java        ← /api/sync/skills/**
│   │   │   └── McpSyncController.java          ← /api/sync/mcp/templates
│   │   ├── service/
│   │   │   ├── MemorySyncService.java
│   │   │   ├── CredentialSyncService.java
│   │   │   ├── SkillSyncService.java
│   │   │   └── McpSyncService.java
│   │   ├── mapper/
│   │   │   ├── MemorySyncMapper.java
│   │   │   ├── CredentialSyncMapper.java
│   │   │   ├── SkillSyncMapper.java
│   │   │   └── McpSyncMapper.java
│   │   ├── entity/
│   │   │   ├── ConversationHistory.java
│   │   │   ├── EnterpriseCredential.java
│   │   │   ├── EnterpriseSkill.java
│   │   │   └── McpTemplate.java
│   │   └── config/
│   │       └── SyncSecurityConfig.java         ← 路径放行配置
│   └── src/main/resources/
│       └── db/
│           └── sync_schema.sql                 ← 建表脚本
```

### pig 侧关键代码片段（参考）

#### 用户 ID 提取（从 JWT）

```java
// 所有 /api/sync/** 端点通过这种方式获取 user_id
@GetMapping("/sync/memory")
public Result memorySync(
        @RequestParam(defaultValue = "0") long since,
        @RequestParam(defaultValue = "500") int limit,
        @AuthenticationPrincipal OAuth2AuthenticationPrincipal principal) {
    String userId = principal.getName(); // JWT sub claim
    return memorySyncService.pullIncremental(userId, since, limit);
}
```

#### 幂等上行

```java
@PostMapping("/sync/memory")
public Result uploadMemory(
        @RequestBody MemoryUploadRequest request,
        @AuthenticationPrincipal OAuth2AuthenticationPrincipal principal) {
    String userId = principal.getName();
    int accepted = 0;
    for (MemoryEntry entry : request.getEntries()) {
        // ON CONFLICT 幂等
        int rows = memorySyncMapper.insertIfAbsent(
            userId, entry.getSessionId(), entry.getDedupKey(),
            entry.getKind(), entry.getRole(), entry.getContent(), ...
        );
        accepted += rows;
    }
    return Result.ok().put("accepted", accepted);
}
```

#### 路径放行（Spring Security）

```java
@Configuration
public class SyncSecurityConfig {
    @Bean
    SecurityFilterChain syncFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(reg -> reg
            // /api/sync/** 需要认证（Bearer JWT）
            .requestMatchers("/api/sync/**").authenticated()
            // /oauth2/** 由 Spring Authorization Server 处理
            .requestMatchers("/oauth2/**").permitAll()
        );
        return http.build();
    }
}
```

---

## 六、验收对照表

### 端到端验收清单

| # | 场景 | QwenPaw 客户端 | pig 服务端 | 状态 |
|---|------|---------------|-----------|------|
| 1 | OAuth2 登录 | 跳转 pig SSO → 回调拿 JWT | 客户端注册 + 授权页 | ✅ 客户端 / ⬜ 服务端配置 |
| 2 | JWT 验签 | JWKS 缓存 + RS256 验签 | `/oauth2/jwks` 端点 | ✅ 客户端 / ✅ 服务端自带 |
| 3 | 记忆登录拉取 | `GET /api/sync/memory?since=0` | 查询 + 返回 entries | ✅ 客户端 / ⬜ 服务端开发 |
| 4 | 记忆异步上行 | `POST /api/sync/memory` | 幂等插入 | ✅ 客户端 / ⬜ 服务端开发 |
| 5 | 记忆对账 | `GET /api/sync/memory/meta` | `SELECT MAX(seq)` | ✅ 客户端 / ⬜ 服务端开发 |
| 6 | 企业凭据拉取 | `GET /api/sync/credentials` | 查询 + 解密返回 | ✅ 客户端 / ⬜ 服务端开发 |
| 7 | 企业技能拉取 | manifest 比对 + 下载 zip | manifest + 对象存储 | ✅ 客户端 / ⬜ 服务端开发 |
| 8 | MCP 模板拉取 | `GET /api/sync/mcp/templates` | 查询返回 | ✅ 客户端 / ⬜ 服务端开发 |
| 9 | 断网容错 | 本地正常工作，队列堆积 | 不涉及 | ✅ 客户端 / — |
| 10 | 联网补传 | 队列 drain 到服务端 | 幂等接收 | ✅ 客户端 / ⬜ 服务端开发 |

### pig 服务端开发量估算

| 模块 | 工作量 | 说明 |
|------|--------|------|
| OAuth2 客户端注册 | 0.5 天 | pig 管理后台操作 |
| 记忆同步（P1） | 2-3 天 | Controller + Service + Mapper + 建表 |
| 企业凭据（P2） | 1-2 天 | Controller + Service + Mapper + 建表 + 加密 |
| 企业技能（P3） | 2-3 天 | Controller + Service + Mapper + 对象存储 + zip 打包 |
| MCP 模板（P4） | 1 天 | Controller + Service + Mapper + 建表 |
| **合计** | **6-9 天** | |

---

*文档创建日期：2026-07-31*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md)*
