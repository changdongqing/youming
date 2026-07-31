# pig 服务端对接 QwenPaw —— 实施计划

> **文档定位**：本仓库（youming / pig 二次开发）服务端侧的对接实施计划。配套 QwenPaw 客户端改造文档见 `改造qwenpaw文档参考/`。
>
> **职责边界**：本计划只覆盖 pig 服务端（端点 + 建表 + 客户端注册 + 联调说明），不涉及 QwenPaw 客户端代码改动。
>
> **制定日期**：2026-07-31 · **制定人**：架构师视角

---

## 一、关键架构决策（基于代码库事实）

### 1.1 认证模式：opaque token（非 JWT）

**事实**：pig-auth 基于 Spring Authorization Server，但 token 格式为 `OAuth2TokenFormat.REFERENCE`（opaque 不透明令牌），不是 JWT。资源服务器用 `PigCustomOpaqueTokenIntrospector` 通过 Redis 内省令牌，无 `JwtDecoder`。token 格式 `client:username:uuid`，无法本地解码。

**决策**：pig 侧保持现状，注册 `qwenpaw` OAuth2 客户端即可，零侵入。

> ⚠️ **QwenPaw 侧需相应调整**：phase1 文档设计的「JWKS 本地验签 + PyJWT」在 pig 当前架构下不可用。QwenPaw 应改为「持有 opaque token + 401 触发 refresh_token 刷新」模式。此项属 QwenPaw 改造范围，不在本仓库。P0 末尾输出联调说明。

### 1.2 路径前缀：`/sync` 模块前缀（boot 下达 `/admin/sync/**`）

**事实**：pig-boot context-path 为 `/admin`；现有模块 controller 用模块 slug 前缀（pig-rm 用 `/rm/...`，pig-upms 用 `/user/...`），外部路径形如 `/admin/rm/bug/page`。无 `/api/**` 命名空间。

**决策**：sync 模块 controller 用 `@RequestMapping("/sync")`，方法路径 `/sync/memory`、`/sync/credentials` 等。boot 模式下外部可达 `/admin/sync/memory`。

> ⚠️ QwenPaw 的 `pig.base_url` 应指向 `http://<pig-host>:9999/admin`，调用 `/sync/memory` 等（非 `/api/sync/`）。

### 1.3 数据表风格：贴合 youming Flyway 约定

**事实**：本仓库 Flyway 迁移（V19/V4 为准）约定：`bigint` 雪花 ID（非 BIGSERIAL）、`text` 存 JSON（非 JSONB）、`timestamp` 无时区（非 timestamptz）、5 个标准审计列、命名约束。

**决策**：表名 `qwenpaw_xxx`，ID 用 `bigint`（MyBatis-Plus `IdType.ASSIGN_ID` 雪花），JSON 列用 `text`，时间用 `timestamp`，附 5 审计列 + `COMMENT`。下一个迁移版本为 **V23**。

### 1.4 对象存储：复用 `pig-common-oss`

**事实**：`pig-common-oss` 已有 `FileTemplate` 接口（`LocalFileTemplate` + `OssTemplate` S3/MinIO 双实现），pig-upms 已依赖。`SysFileController` 有现成的 `@RequestPart MultipartFile` 上传 + 流式下载模式。

**决策**：技能包（P3）的 zip 上传/下载直接注入 `FileTemplate`，存储路径记入 `qwenpaw_enterprise_skills.storage_path`。开发态用 `file.type: local`，生产可切 MinIO，零代码改动。

---

## 二、模块结构

新建 `server/pig-sync/`（两层 Maven 结构，镜像 `pig-rm`）：

```
server/pig-sync/
├── pom.xml                              # packaging=pom，parent=com.pig4cloud:pig:4.0.0
└── pig-sync-biz/
    ├── pom.xml                          # jar，依赖 pig-common-{data,core,security,log,swagger,oss}
    └── src/main/
        ├── resources/
        │   └── application.yml          # port 4005, name @artifactId@
        └── java/com/pig4cloud/pig/sync/
            ├── PigSyncApplication.java  # @EnableOpenApi("sync") @EnablePigResourceServer @EnableDiscoveryClient @SpringBootApplication
            ├── api/
            │   ├── dto/                 # MemoryPullDTO, MemoryUploadDTO, MemoryEntryDTO...
            │   ├── entity/              # ConversationHistory, EnterpriseCredential, EnterpriseSkill, PersonalSkill, McpTemplate
            │   └── vo/                  # MemoryMetaVO, SkillManifestVO, CredentialVO...
            ├── controller/
            │   ├── MemorySyncController.java     # /sync/memory, /sync/memory/meta
            │   ├── CredentialSyncController.java # /sync/credentials
            │   ├── SkillSyncController.java      # /sync/skills/manifest, /sync/skills/{name}/download, /sync/skills/upload
            │   └── McpSyncController.java        # /sync/mcp/templates
            ├── mapper/                  # 5 个 @Mapper extends MPJBaseMapper
            ├── service/                  # 4 个 interface extends IService
            └── service/impl/            # 4 个 @Service extends ServiceImpl
```

**注册**：
- `server/pom.xml` `<modules>` 追加 `<module>pig-sync</module>`
- `server/pig-boot/pom.xml` 追加 `<dependency>pig-sync-biz</dependency>`

---

## 三、分阶段实施

### P0：OAuth2 客户端注册 + 认证通路（0.5 天）

**改动文件**：`server/pig-common/pig-common-data/src/main/resources/db/migration/V23__qwenpaw_client_seed.sql`（新建）

**内容**：向 `sys_oauth_client_details` 插入 `qwenpaw` 客户端：

| 列 | 值 |
|----|----|
| id | 9（V2 已用到 8=mini） |
| client_id | `qwenpaw` |
| client_secret | `qwenpaw`（明文，运行时加 `{noop}`） |
| scope | `server` |
| authorized_grant_types | `authorization_code,refresh_token` |
| web_server_redirect_uri | `http://localhost:8000/api/sync/oauth/callback`（QwenPaw 回调，按部署调整） |
| access_token_validity | `3600`（1 小时） |
| refresh_token_validity | `604800`（7 天） |
| additional_information | `{"enc_flag":"0","captcha_flag":"0"}` |
| autoapprove | `true` |

**验收**：应用启动后 Flyway 自动应用 V23；用 `client_id=qwenpaw` 走授权码流程能拿到 opaque token，带 `Bearer` 调任意 sync 端点能被内省通过（返回 401 而非 500 即表示安全链路通）。

**联调说明（输出给 QwenPaw 侧）**：
- pig 不支持 JWT 本地验签，token 为 opaque（格式 `client:username:uuid`）
- QwenPaw 应：① 持有 access_token；② 请求带 `Authorization: Bearer <token>`；③ 收到 401 时用 refresh_token 调 `/oauth2/token`（grant_type=refresh_token）刷新；④ 刷新失败则重走授权码登录

---

### P1：记忆同步（最高优先级，2-3 天）

**建表**：`V24__qwenpaw_sync_schema.sql`（新建，含全部 4 张业务表，一次性建好避免多次迁移）

`qwenpaw_conversation_history` 关键设计：
```sql
CREATE TABLE qwenpaw_conversation_history (
    seq           bigint       NOT NULL,
    session_id    varchar(255) NOT NULL,
    agent_id      varchar(255),
    kind          varchar(50)  NOT NULL,
    role          varchar(50),
    name          varchar(255),
    content       text,
    tool_call_id  varchar(255),
    tool_input    text,
    tool_state    varchar(50),
    headline      varchar(500),
    blocks        text,
    metadata      text,
    created_at    timestamp,
    dedup_key     varchar(255),
    user_id       varchar(255) NOT NULL,
    client_seq    bigint,
    create_by     varchar(64)  DEFAULT ' ',
    create_time   timestamp    DEFAULT now(),
    update_by     varchar(64)  DEFAULT ' ',
    update_time   timestamp    DEFAULT now(),
    del_flag      char(1)      DEFAULT '0',
    CONSTRAINT pk_qwenpaw_ch PRIMARY KEY (seq),
    CONSTRAINT uk_qwenpaw_ch_dedup UNIQUE (session_id, dedup_key, user_id)
);
CREATE INDEX idx_qwenpaw_ch_user_seq ON qwenpaw_conversation_history(user_id, seq);
CREATE INDEX idx_qwenpaw_ch_session ON qwenpaw_conversation_history(session_id);
```

> **关键调整**：文档原表用 `BIGSERIAL` 自增，但本仓库约定雪花 ID。`seq` 改为 `bigint` + MyBatis-Plus `IdType.ASSIGN_ID`。`ON CONFLICT (session_id, dedup_key, user_id) DO NOTHING` 幂等上行逻辑不变。

**端点**（MemorySyncController）：

| 方法 | 路径 | 契约 |
|------|------|------|
| GET | `/sync/memory?since={seq}&limit=500` | `WHERE user_id=? AND seq>? ORDER BY seq LIMIT ?` → `{entries:[...], next_seq, has_more}` |
| POST | `/sync/memory` | body `{entries:[...]}` → 逐条 `ON CONFLICT DO NOTHING` → `{accepted, deduped}` |
| GET | `/sync/memory/meta` | `SELECT MAX(seq) WHERE user_id=?` → `{max_seq}` |

**用户隔离**：service 层 `SecurityUtils.getUser().getId()` 取 userId（Long → String 存库）。

**幂等上行**：MyBatis-Plus 无原生 `ON CONFLICT`，用 mapper 自定义 `@Insert` + `ON CONFLICT(session_id,dedup_key,user_id) DO NOTHING` 文本块 SQL（参照 `RmUserQueryMapper` 的 `@Select` 内联风格）。

**验收**（对照 qwenpaw-adaptation §六 3-5 项）：
- [ ] `GET /sync/memory?since=0` 返回该用户全部记忆
- [ ] `POST /sync/memory` 重复上传同一 `dedup_key` 不重复插入
- [ ] `GET /sync/memory/meta` 返回 `max_seq`
- [ ] 不同 user_id 数据隔离

---

### P2：企业凭据（1-2 天）

**表**：`qwenpaw_enterprise_credentials`（V24 内）—— `public_data`/`secret_data`/`meta_data` 用 `text` 存 JSON（非 JSONB）。

**端点**：`GET /sync/credentials` → `{credentials:[{ref, kind, public, secrets, meta}]}`

**加密**：`secret_data` 在库中加密存储。复用 Jasypt `StringEncryptor` bean（`PBEWithMD5AndDES`，参照 `GenDatasourceConfServiceImpl` 用法），或新增 AES `StringEncryptor` bean。返回时解密（传输走 TLS）。

**验收**：凭据列表按用户/可空 user_id（全员可用）过滤；secrets 字段库中非明文。

---

### P3：企业技能（2-3 天）

**表**：`qwenpaw_enterprise_skills` + `qwenpaw_personal_skills`（V24 内）。

**端点**：
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/sync/skills/manifest` | `{version, skills:[{name, version, md_hash, enterprise_version, description}]}` |
| GET | `/sync/skills/{name}/download` | 流式返回 zip（`application/zip`） |
| POST | `/sync/skills/upload` | `@RequestPart("file") MultipartFile` → 存 `FileTemplate` → `{name, stored, version}` |

**存储**：注入 `FileTemplate` + `FileProperties`，`putObject(bucket, "qwenpaw-skills", fileName, zipStream, "application/zip")`。下载时 `getObject` + 流式写 `HttpServletResponse`（参照 `SysFileController` / `ModelSerializeController`）。

**验收**：manifest 版本比对字段 `enterprise_version`；下载返回合法 zip；上传落对象存储并记 `storage_path`。

---

### P4：MCP 模板（1 天）

**表**：`qwenpaw_mcp_templates`（V24 内）—— `capabilities` 用 `text` 存 JSON。

**端点**：`GET /sync/mcp/templates` → `{templates:[{name, endpoint, description, version, protocol, transport, credential_ref, default_policy, capabilities}]}`

**验收**：模板列表按租户/全员过滤；`credential_ref` 指向 P2 的 `enterprise_credentials.ref`。

---

### P5：个人技能上传（可选，0.5 天）

已并入 P3 的 `/sync/skills/upload`，表 `qwenpaw_personal_skills` 已在 V24 建。按 `user_id` 隔离。

---

## 四、交付物清单

| 文件 | 阶段 | 说明 |
|------|------|------|
| `server/pig-sync/pom.xml` + `pig-sync-biz/pom.xml` | 骨架 | 两层 Maven |
| `PigSyncApplication.java` | 骨架 | 启动类 |
| `application.yml` | 骨架 | port 4005 |
| `V23__qwenpaw_client_seed.sql` | P0 | OAuth2 客户端种子 |
| `V24__qwenpaw_sync_schema.sql` | P1 | 4 张业务表 DDL |
| `MemorySyncController/Service/Impl/Mapper/Entity` | P1 | 记忆同步 |
| `CredentialSyncController/Service/Impl/Mapper/Entity` | P2 | 企业凭据 |
| `SkillSyncController/Service/Impl/Mapper/Entity` | P3 | 企业技能 |
| `McpSyncController/Service/Impl/Mapper/Entity` | P4 | MCP 模板 |
| `server/pom.xml`（改） | 骨架 | 注册模块 |
| `server/pig-boot/pom.xml`（改） | 骨架 | 单体依赖 |

---

## 五、约定遵守

- 细粒度提交（每任务一次），格式 `feat(sync): ...`，遵循 AGENTS.md
- 实体 `extends Model<T>`、`@TableId(ASSIGN_ID)`、5 审计列 `@TableField(fill)`、`@TableLogic`
- Mapper `extends MPJBaseMapper`（非 BaseMapper）、`@Mapper`、内联 `@Select/@Insert` 无 XML
- Controller `@RestController @AllArgsConstructor @Tag @SecurityRequirement(AUTHORIZATION)`，无类级 `@RequestMapping`（每方法全路径），返回 `R<>`
- 不动 ignore-urls、不用 `@Inner`（认证端点靠默认 opaque 内省链）
- 提交不含 target/、.env

---

## 六、实施顺序

1. **骨架**：建 `pig-sync` 模块 + 注册 + 启动验证（半天）
2. **P0**：V23 客户端种子 + 授权码流程联调（半天）
3. **P1**：V24 建表 + MemorySync 全套 + 联调（2-3 天，最高价值）
4. **P2**：CredentialSync + Jasypt 加密（1-2 天）
5. **P3**：SkillSync + FileTemplate 集成（2-3 天）
6. **P4**：McpSync（1 天）
7. P5 已并入 P3

每完成一个独立任务提交一次。

---

## 七、验收对照表（对照 qwenpaw-adaptation §六）

| # | 场景 | pig 服务端 | 状态 |
|---|------|-----------|------|
| 1 | OAuth2 登录 | 客户端注册 + 授权页（Spring Auth Server 自带） | ⬜ P0 |
| 2 | token 验证 | opaque token Redis 内省（非 JWT，QwenPaw 侧需适配） | ⬜ P0 |
| 3 | 记忆登录拉取 | `GET /sync/memory?since=0` | ⬜ P1 |
| 4 | 记忆异步上行 | `POST /sync/memory` 幂等 | ⬜ P1 |
| 5 | 记忆对账 | `GET /sync/memory/meta` | ⬜ P1 |
| 6 | 企业凭据拉取 | `GET /sync/credentials` + 解密 | ⬜ P2 |
| 7 | 企业技能拉取 | manifest + zip 下载 | ⬜ P3 |
| 8 | MCP 模板拉取 | `GET /sync/mcp/templates` | ⬜ P4 |
| 9 | 断网容错 | 不涉及（QwenPaw 侧） | — |
| 10 | 联网补传 | 幂等接收（同 4） | ⬜ P1 |

---

*文档创建日期：2026-07-31*
*配套文档：[qwenpaw-adaptation.md](./qwenpaw-adaptation.md) · [改造qwenpaw文档参考/](./改造qwenpaw文档参考/)*
