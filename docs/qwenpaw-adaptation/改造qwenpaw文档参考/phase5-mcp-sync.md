# 阶段 5 详细设计：MCP 模板同步

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)
>
> **目标**：登录时拉取企业 MCP 模板到本地缓存，用户可基于模板创建本地实例并个性化修改，模板更新时前端提示。
>
> **预估工期**：2-3 天 · **依赖**：阶段 0 + 阶段 1

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. MCP 模板同步管理器](#2-mcp-模板同步管理器)
- [3. DriverConfigService 扩展](#3-driverconfigservice-扩展)
- [4. 模板列表与实例化路由](#4-模板列表与实例化路由)
- [5. 前端适配](#5-前端适配)
- [6. SyncLifecycle 集成](#6-synclifecycle-集成)
- [7. pig 服务端接口约定](#7-pig-服务端接口约定)
- [8. 验收标准](#8-验收标准)

---

## 1. 整体架构

### 1.1 模板与实例的关系

```
┌──────────────────────────────────────────────────────────┐
│                    QwenPaw 本地                           │
│                                                          │
│  ~/.qwenpaw/                                             │
│    enterprise_mcp_templates.yaml  ← 企业MCP模板缓存(只读)  │
│      template: "jira-mcp"                                 │
│        endpoint: "https://jira.company.com/mcp"           │
│        default_policy: "ask"                             │
│        credential_ref: "jira_token"                       │
│                                                          │
│    workspace/default/drivers/     ← 本地MCP实例(可编辑)    │
│      jira-mcp-instance-1.json     source_template:        │
│        endpoint: "https://jira.company.com/mcp"  ← 可改   │
│        policy: "allow"                           ← 可改   │
│        credential_ref: "personal_jira_token"     ← 可改   │
│                                                          │
│  工作流:                                                  │
│    1. 管理员在 pig 配置企业 MCP 模板                       │
│    2. 用户登录 → 拉取模板缓存到本地                        │
│    3. 用户基于模板创建实例 → 本地个性化                     │
│    4. 模板更新 → 前端提示用户                              │
│    5. 本地实例不被模板覆盖                                 │
└──────────────────────┬───────────────────────────────────┘
                       │ 登录时拉取
                       ▼
                 ┌──────────┐
                 │ pig 服务端│
                 │ MCP模板库 │
                 └──────────┘
```

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **模板只读** | 企业 MCP 模板从服务端拉取，本地不可编辑 |
| **实例独立** | 基于模板创建的本地实例可自由修改，不受模板更新影响 |
| **更新提示** | 模板更新时前端提示用户（不自动覆盖实例） |
| **断网可用** | 使用本地缓存的模板和实例，无网络依赖 |

---

## 2. MCP 模板同步管理器

**新建文件**：`src/qwenpaw/sync/mcp_sync.py`

### 2.1 类定义

```python
# -*- coding: utf-8 -*-
"""企业 MCP 模板同步。

从 pig 拉取企业 MCP 驱动模板，缓存到本地。
用户可基于模板创建本地实例并个性化修改。

核心原则:
- 模板只读，不可本地编辑
- 基于模板创建的实例独立，不受模板更新影响
- 模板更新时前端提示用户（不自动覆盖）
"""
from __future__ import annotations

import logging
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import yaml

from ..config.config import McpSyncConfig
from ..constant import SECRET_DIR
from .client import PigClient
from .errors import SyncError

logger = logging.getLogger(__name__)

_TEMPLATE_CACHE_FILE = SECRET_DIR / "enterprise_mcp_templates.yaml"
_TEMPLATE_CACHE_VERSION = 1


class McpTemplateSync:
    """企业 MCP 模板同步管理器。

    生命周期: 由 SyncLifecycle 持有，登录时调用 pull_templates()。
    """

    def __init__(
        self,
        pig_client: PigClient,
        config: McpSyncConfig,
    ) -> None:
        self._pig = pig_client
        self._config = config
        self._lock = threading.RLock()
        self._templates: dict[str, dict] = {}
        self._load_cache()

    # ================================================================
    # 持久化
    # ================================================================

    def _load_cache(self) -> None:
        """从磁盘加载模板缓存。"""
        if not _TEMPLATE_CACHE_FILE.is_file():
            return
        try:
            data = yaml.safe_load(
                _TEMPLATE_CACHE_FILE.read_text(encoding="utf-8"),
            )
            if data and isinstance(data, dict):
                self._templates = data.get("templates", {})
        except Exception as exc:
            logger.warning(
                "Failed to load MCP template cache: %s",
                exc,
            )

    def _save_cache(self) -> None:
        """持久化模板缓存到磁盘。"""
        _TEMPLATE_CACHE_FILE.parent.mkdir(parents=True, exist_ok=True)
        payload = {
            "version": _TEMPLATE_CACHE_VERSION,
            "templates": self._templates,
            "last_pulled_at": datetime.now(timezone.utc).isoformat(),
        }
        _TEMPLATE_CACHE_FILE.write_text(
            yaml.dump(payload, allow_unicode=True, default_flow_style=False),
            encoding="utf-8",
        )
        try:
            _TEMPLATE_CACHE_FILE.chmod(0o600)
        except OSError:
            pass

    # ================================================================
    # 同步（从 pig 拉取）
    # ================================================================

    async def pull_templates(self) -> int:
        """从 pig 拉取企业 MCP 模板，全量替换本地缓存。

        Returns:
            拉取的模板数量。
        """
        try:
            resp = await self._pig.get("/api/sync/mcp/templates")
            templates = resp.get("templates", [])

            with self._lock:
                self._templates = {}
                for tmpl in templates:
                    name = tmpl.get("name", "")
                    if name:
                        self._templates[name] = tmpl
                self._save_cache()

            count = len(self._templates)
            logger.info(
                "Enterprise MCP templates pulled: %d entries",
                count,
            )
            return count

        except SyncError as exc:
            logger.warning("MCP template pull failed: %s", exc)
            return 0
        except Exception:
            logger.warning(
                "MCP template pull unexpected error",
                exc_info=True,
            )
            return 0

    # ================================================================
    # 读取接口
    # ================================================================

    def get_template(self, name: str) -> dict | None:
        """获取一个模板（只读）。"""
        with self._lock:
            return dict(self._templates.get(name, {}))

    def list_templates(self) -> list[dict]:
        """列出所有企业 MCP 模板。"""
        with self._lock:
            return [
                {
                    "name": name,
                    "endpoint": tmpl.get("endpoint", ""),
                    "description": tmpl.get("description", ""),
                    "version": tmpl.get("version", ""),
                    "protocol": tmpl.get("protocol", "mcp"),
                    "credential_ref": tmpl.get("credential_ref", ""),
                    "default_policy": tmpl.get("default_policy", "ask"),
                }
                for name, tmpl in self._templates.items()
            ]

    def list_template_names(self) -> list[str]:
        """列出模板名称。"""
        with self._lock:
            return sorted(self._templates.keys())

    # ================================================================
    # 模板版本对比（用于提示用户更新）
    # ================================================================

    def check_template_updates(self) -> list[dict]:
        """检查哪些模板有更新。

        对比本地实例的 source_template_version 与缓存模板的 version，
        返回有更新的模板列表。

        Returns:
            [{"template_name": ..., "old_version": ..., "new_version": ...}]
        """
        updates: list[dict] = []
        # 遍历本地 MCP 实例（通过 DriverConfigService）
        # 对比 source_template 和 source_template_version
        # 实施时需要 DriverConfigService 提供遍历所有 driver card 的接口
        return updates
```

### 2.2 模板缓存文件格式

```yaml
# ~/.qwenpaw.secret/enterprise_mcp_templates.yaml (权限 0600)
version: 1
templates:
  jira-mcp:
    name: jira-mcp
    endpoint: "https://jira.company.com/mcp/sse"
    description: "企业 Jira MCP 服务"
    version: "2.0"
    protocol: mcp
    transport: sse
    credential_ref: "jira_token"
    default_policy: "ask"
    capabilities:
      - name: create_issue
        policy: "ask"
      - name: search_issues
        policy: "allow"
  gitlab-mcp:
    name: gitlab-mcp
    endpoint: "https://gitlab.company.com/mcp/sse"
    description: "企业 GitLab MCP 服务"
    version: "1.5"
    protocol: mcp
    transport: sse
    credential_ref: "gitlab_token"
    default_policy: "ask"
last_pulled_at: "2026-07-30T10:00:00Z"
```

---

## 3. DriverConfigService 扩展

### 3.1 source_template 字段

**文件**：`src/qwenpaw/app/driver_config_service.py`

本地 MCP 驱动卡片（DriverCard）增加 `source_template` 字段，标记基于哪个企业模板创建：

```python
class DriverConfigService:
    # ... 现有代码 ...

    async def create_from_template(
        self,
        template_name: str,
        instance_name: str,
        overrides: dict | None = None,
    ) -> DriverCard:
        """基于企业 MCP 模板创建本地实例。

        Args:
            template_name: 企业模板名称
            instance_name: 本地实例名称
            overrides: 个性化覆盖（endpoint, policy, credential_ref 等）

        Returns:
            创建的 DriverCard。
        """
        from ..sync.lifecycle import get_sync_lifecycle

        sync_lc = get_sync_lifecycle()
        if not sync_lc or not sync_lc._mcp_sync:
            raise HTTPException(403, "MCP sync is not enabled")

        template = sync_lc._mcp_sync.get_template(template_name)
        if not template:
            raise HTTPException(404, f"Template '{template_name}' not found")

        # 合并模板与覆盖
        card_data = {
            "name": instance_name,
            "protocol": template.get("protocol", "mcp"),
            "endpoint": overrides.get(
                "endpoint", template.get("endpoint", ""),
            ) if overrides else template.get("endpoint", ""),
            "transport": overrides.get(
                "transport", template.get("transport", ""),
            ) if overrides else template.get("transport", ""),
            "credential_ref": overrides.get(
                "credential_ref", template.get("credential_ref", ""),
            ) if overrides else template.get("credential_ref", ""),
            "source_template": template_name,        # 新增
            "source_template_version": template.get(
                "version", "",
            ),  # 新增
            "policies": overrides.get("policies") if overrides else None,
        }

        # 复用现有的 DriverCard 创建逻辑
        # 具体调用取决于现有 DriverCard 的创建 API
        ...
        return card
```

### 3.2 列出企业模板

```python
    async def list_enterprise_templates(self) -> list[dict]:
        """列出可用的企业 MCP 模板。"""
        from ..sync.lifecycle import get_sync_lifecycle

        sync_lc = get_sync_lifecycle()
        if not sync_lc or not sync_lc._mcp_sync:
            return []

        return sync_lc._mcp_sync.list_templates()
```

### 3.3 检查模板更新

```python
    async def check_template_updates(self) -> list[dict]:
        """检查哪些本地实例对应的模板有更新。

        对比每个本地实例的 source_template_version 与缓存模板的 version。
        """
        from ..sync.lifecycle import get_sync_lifecycle

        sync_lc = get_sync_lifecycle()
        if not sync_lc or not sync_lc._mcp_sync:
            return []

        updates: list[dict] = []
        mcp_sync = sync_lc._mcp_sync

        # 遍历所有本地 MCP driver cards
        for card_name in await self._list_all_mcp_cards():
            card = await self.load_card(card_name, protocol="mcp")
            template_name = getattr(card, "source_template", None)
            if not template_name:
                continue  # 非模板创建的实例，跳过

            template = mcp_sync.get_template(template_name)
            if not template:
                continue  # 模板已被删除

            local_version = getattr(
                card, "source_template_version", "",
            )
            server_version = template.get("version", "")
            if server_version and local_version != server_version:
                updates.append({
                    "instance_name": card_name,
                    "template_name": template_name,
                    "old_version": local_version,
                    "new_version": server_version,
                })

        return updates
```

---

## 4. 模板列表与实例化路由

### 4.1 新增路由

**文件**：`src/qwenpaw/app/routers/mcp.py`（或新建 `sync_mcp.py`）

```python
# 在现有 mcp 路由中追加

@router.get("/mcp/templates")
async def list_enterprise_mcp_templates():
    """列出企业 MCP 模板。"""
    from ...sync.lifecycle import get_sync_lifecycle

    sync_lc = get_sync_lifecycle()
    if not sync_lc or not sync_lc._mcp_sync:
        return {"templates": [], "enabled": False}

    templates = sync_lc._mcp_sync.list_templates()
    return {"templates": templates, "enabled": True}


@router.post("/mcp/from-template")
async def create_mcp_from_template(
    template_name: str = Body(..., embed=True),
    instance_name: str = Body(..., embed=True),
    overrides: dict = Body(default=None, embed=True),
    workspace_id: str = Depends(get_workspace_id),
):
    """基于企业 MCP 模板创建本地实例。"""
    service = get_driver_config_service(workspace_id)
    card = await service.create_from_template(
        template_name, instance_name, overrides,
    )
    return {"created": True, "name": instance_name}


@router.get("/mcp/template-updates")
async def check_mcp_template_updates():
    """检查模板更新。"""
    service = get_driver_config_service()
    updates = await service.check_template_updates()
    return {"updates": updates}
```

### 4.2 公开路径

模板列表路由需要认证（不属于公开路径），无需修改 `_PUBLIC_PATHS`。

---

## 5. 前端适配

### 5.1 MCP 页面增加模板列表

**文件**：`console/src/pages/Agent/MCP/`

在 MCP 管理页面增加"企业模板"区域：

```tsx
// MCP 管理页面
function McpManager() {
  const [templates, setTemplates] = useState([]);
  const [updates, setUpdates] = useState([]);

  useEffect(() => {
    // 拉取企业模板
    fetchMcpTemplates().then(setTemplates);
    // 检查模板更新
    checkMcpTemplateUpdates().then(setUpdates);
  }, []);

  return (
    <div>
      {/* 模板更新提示 */}
      {updates.length > 0 && (
        <Alert
          message={`${updates.length} 个 MCP 模板有更新`}
          type="info"
          showIcon
        />
      )}

      {/* 企业 MCP 模板区域 */}
      <Card title="企业 MCP 模板">
        {templates.map((tmpl) => (
          <div key={tmpl.name}>
            <span>{tmpl.name}</span>
            <span>{tmpl.description}</span>
            <Button
              onClick={() => showCreateFromTemplate(tmpl.name)}
            >
              创建实例
            </Button>
          </div>
        ))}
      </Card>

      {/* 现有 MCP 实例列表（不变） */}
      <Card title="MCP 实例">
        {/* ... 现有代码 ... */}
      </Card>
    </div>
  );
}
```

### 5.2 从模板创建实例对话框

```tsx
function CreateFromTemplateModal({
  templateName,
  template,
  onCreated,
}) {
  const [instanceName, setInstanceName] = useState("");
  const [endpoint, setEndpoint] = useState(template.endpoint);
  const [credentialRef, setCredentialRef] = useState(
    template.credential_ref,
  );

  const handleCreate = async () => {
    await createMcpFromTemplate({
      template_name: templateName,
      instance_name: instanceName,
      overrides: {
        endpoint,
        credential_ref: credentialRef,
      },
    });
    onCreated();
  };

  return (
    <Modal title="从模板创建 MCP 实例" onOk={handleCreate}>
      <Form>
        <Form.Item label="实例名称">
          <Input value={instanceName}
            onChange={(e) => setInstanceName(e.target.value)} />
        </Form.Item>
        <Form.Item label="端点（可修改）">
          <Input value={endpoint}
            onChange={(e) => setEndpoint(e.target.value)} />
        </Form.Item>
        <Form.Item label="凭据引用（可修改）">
          <Input value={credentialRef}
            onChange={(e) => setCredentialRef(e.target.value)} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
```

---

## 6. SyncLifecycle 集成

### 6.1 初始化

**文件**：`src/qwenpaw/sync/lifecycle.py`

在 `on_startup()` 中创建 `McpTemplateSync`：

```python
async def on_startup(self) -> None:
    # ... PigClient 初始化 ...

    # 创建 MCP 模板同步
    if cfg.sync.mcp.auto_pull_templates:
        try:
            from .mcp_sync import McpTemplateSync
            self._mcp_sync = McpTemplateSync(
                self._pig_client,
                cfg.sync.mcp,
            )
            self.register_mcp_sync(self._mcp_sync)
        except Exception:
            logger.warning("MCP template sync init failed", exc_info=True)
```

### 6.2 登录时拉取

在 `on_login_success()` 中：

```python
if cfg.sync.mcp.auto_pull_templates and self._mcp_sync:
    tasks.append((
        "mcp",
        asyncio.create_task(self._mcp_sync.pull_templates()),
    ))
```

---

## 7. pig 服务端接口约定

### 7.1 拉取 MCP 模板

```
GET /api/sync/mcp/templates

Headers:
  Authorization: Bearer <access_token>

Response 200:
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
    },
    {
      "name": "gitlab-mcp",
      "endpoint": "https://gitlab.company.com/mcp/sse",
      "description": "企业 GitLab MCP 服务",
      "version": "1.5",
      "protocol": "mcp",
      "transport": "sse",
      "credential_ref": "gitlab_token",
      "default_policy": "ask",
      "capabilities": []
    }
  ]
}
```

### 7.2 pig 服务端实现要点（Java/Spring）

```java
// pig 侧需新建表
CREATE TABLE qwenpaw_mcp_templates (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    endpoint        VARCHAR(500) NOT NULL,
    description     TEXT,
    version         VARCHAR(50) NOT NULL,
    protocol        VARCHAR(50) DEFAULT 'mcp',
    transport       VARCHAR(50) DEFAULT 'sse',
    credential_ref  VARCHAR(255),
    default_policy  VARCHAR(50) DEFAULT 'ask',
    capabilities    JSONB DEFAULT '[]',
    tenant_id       VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);
```

> `credential_ref` 指向企业凭据（阶段 3 的 `qwenpaw_enterprise_credentials` 表中的 `ref`），用户创建实例时可替换为个人凭据。

### 7.3 模板与凭据的关系

```
pig 服务端:
  MCP 模板 "jira-mcp"
    └─ credential_ref: "jira_token"  → 企业凭据 (qwenpaw_enterprise_credentials)

QwenPaw 本地:
  MCP 实例 "jira-mcp-instance-1" (基于模板创建)
    └─ credential_ref: "jira_token"  → 企业凭据缓存 (enterprise_credentials.yaml)
    或
    └─ credential_ref: "personal_jira_token"  → 个人凭据 (credentials.yaml)
```

> 用户可在创建实例时选择使用企业凭据或个人凭据。

---

## 8. 验收标准

### 功能验收

- [ ] 登录后 `enterprise_mcp_templates.yaml` 生成在 `SECRET_DIR/`，权限 0600
- [ ] 前端 MCP 页面显示"企业 MCP 模板"区域
- [ ] 模板列表包含名称、端点、描述、版本
- [ ] 点击"创建实例"弹出对话框，预填模板默认值
- [ ] 创建实例时可修改端点、凭据引用
- [ ] 创建的实例出现在"MCP 实例"列表中
- [ ] 实例的 `source_template` 字段标记来源模板

### 模板更新验收

- [ ] 服务端模板 version 变更后，下次登录拉取到本地
- [ ] 前端检查到模板更新时显示提示
- [ ] 提示包含实例名、旧版本、新版本
- [ ] 模板更新**不自动覆盖**已创建的本地实例

### 独立性验收

- [ ] 本地实例的 endpoint 修改不被模板更新覆盖
- [ ] 本地实例的 credential_ref 修改不被模板更新覆盖
- [ ] 本地实例的 policy 修改不被模板更新覆盖
- [ ] 删除模板不影响已创建的本地实例

### 断网验收

- [ ] 断网时使用本地缓存的模板正常工作
- [ ] pig 不可达时不影响现有 MCP 实例
- [ ] 模板拉取失败时本地缓存仍然可用

### 开关验收

- [ ] `mcp.auto_pull_templates=false` 时零行为变化
- [ ] `sync.enabled=false` 时不创建 `McpTemplateSync`
- [ ] 模板列表 API 在 `sync.enabled=false` 时返回 `{templates: [], enabled: false}`

### 安全验收

- [ ] 模板缓存文件权限 0600
- [ ] 模板中的 credential_ref 可指向企业凭据或个人凭据
- [ ] 创建实例需要认证（非公开路径）

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)*
