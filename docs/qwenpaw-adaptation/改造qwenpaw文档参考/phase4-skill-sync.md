# 阶段 4 详细设计：企业技能分发

> **配套文档**：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)
>
> **目标**：登录时增量拉取企业技能到 `skill_pool/`（只读），个人技能独立于 `workspace/skills/`，同名时个人优先。
>
> **预估工期**：3-4 天 · **依赖**：阶段 0 + 阶段 1

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. manifest source 字段扩展](#2-manifest-source-字段扩展)
- [3. 技能同步管理器](#3-技能同步管理器)
- [4. 企业技能只读保护](#4-企业技能只读保护)
- [5. 个人技能上传（可选）](#5-个人技能上传可选)
- [6. SyncLifecycle 集成](#6-synclifecycle-集成)
- [7. pig 服务端接口约定](#7-pig-服务端接口约定)
- [8. 验收标准](#8-验收标准)

---

## 1. 整体架构

### 1.1 技能分层模型

```
┌──────────────────────────────────────────────────────────┐
│                    QwenPaw 本地                           │
│                                                          │
│  ~/.qwenpaw/                                             │
│    skill_pool/               ← 企业技能 (服务端拉取, 只读) │
│      skill.json (manifest)      source: "enterprise"     │
│      pdf/                                                  │
│      browser/                                              │
│      make-skill/                                           │
│                                                          │
│    workspace/skills/         ← 个人技能 (本地可编辑)       │
│      skill.json (manifest)      source: "personal"        │
│      my-custom-skill/                                      │
│                                                          │
│  技能加载 = enterprise(只读) ∪ personal(可编辑)            │
│  同名时: personal 优先                                     │
└──────────────────────┬───────────────────────────────────┘
                       │ 登录时增量拉取
                       ▼
                 ┌──────────┐
                 │ pig 服务端│
                 │ 企业技能库│
                 │ (zip 包)  │
                 └──────────┘
```

### 1.2 同步策略

| 事件 | 动作 |
|------|------|
| **登录时** | 对比本地 manifest `version` 与服务端 `version`，增量拉取变更技能 |
| **个人技能** | 纯本地，用户主动点"上传"才推送服务端（云备份，可选） |
| **离线** | 完全使用本地已拉取的技能，无任何网络依赖 |
| **企业技能更新** | 下次登录或定时同步时增量拉取新版本 |

### 1.3 与现有技能体系的兼容

现有 `classify_pool_skill_source()` 返回 `"builtin"` 或 `"customized"`。新增 `"enterprise"` 来源，三者的优先级：

```
技能加载优先级（同名时）:
  1. workspace/skills/ (personal)     ← 最高优先级
  2. skill_pool/ (enterprise)          ← 企业分发
  3. skill_pool/ (builtin)             ← 内置默认
  4. skill_pool/ (customized)          ← 本地自定义覆盖
```

> 实际加载顺序由 `get_skill_pool_dirs()`（第 131 行）控制，`skill_pool/` 在前，`workspace/skills/` 通过 workspace 机制独立加载。enterprise 和 builtin/customized 都在 `skill_pool/` 内，通过 manifest 的 `source` 字段区分。

---

## 2. manifest source 字段扩展

### 2.1 source 值定义

**文件**：`src/qwenpaw/agents/skill_system/store.py`

| source 值 | 含义 | 可编辑 | 来源 |
|-----------|------|--------|------|
| `"builtin"` | 内置技能 | 否 | 打包随程序 |
| `"customized"` | 本地自定义覆盖 | 是 | 用户创建/导入 |
| `"enterprise"` | 企业技能（**新增**） | **否** | pig 服务端拉取 |
| `"personal"` | 个人技能 | 是 | workspace/skills/ |

### 2.2 辅助函数

**文件**：`src/qwenpaw/agents/skill_system/store.py`

新增企业技能判断函数：

```python
def is_enterprise_skill_entry(entry: dict[str, Any] | None) -> bool:
    """Return whether one pool manifest entry represents an enterprise skill."""
    normalized = normalize_skill_manifest_entry(entry)
    return (
        bool(normalized)
        and str(normalized.get("source", "") or "") == "enterprise"
    )


def is_enterprise_skill(skill_name: str) -> bool:
    """Check if a skill in the pool is an enterprise skill.

    读取 pool manifest 检查指定 skill 的 source 字段。
    """
    manifest = read_skill_pool_manifest()
    skills = manifest.get("skills", {})
    entry = skills.get(skill_name)
    return is_enterprise_skill_entry(entry)
```

### 2.3 classify_pool_skill_source 扩展

**文件**：`src/qwenpaw/agents/skill_system/store.py`（第 438 行）

```python
def classify_pool_skill_source(
    skill_name: str,
    skill_dir: Path,
    existing: dict[str, Any],
    builtin_names: list[str],
) -> tuple[str, bool]:
    """Classify one pool skill against packaged builtins.

    Preserve the manifest's source intent when the entry already exists.
    新增: enterprise 来源优先保留（不被 builtin/customized 覆盖）。
    """
    # 新增: 企业技能保持 enterprise 标记
    if existing and is_enterprise_skill_entry(existing):
        return "enterprise", False

    # 现有逻辑不变
    if existing and is_pool_builtin_entry(existing):
        return "builtin", False

    if not _is_builtin_skill(skill_name, builtin_names):
        return "customized", False

    if existing:
        return "customized", False

    pool_version = extract_version(
        _read_frontmatter_safe(skill_dir, skill_name),
    )
    if pool_version:
        return "builtin", False
    return "customized", False
```

### 2.4 pool manifest 结构

企业技能拉取后，pool manifest (`skill.json`) 结构：

```json
{
  "schema_version": "skill-pool-manifest.v1",
  "version": 15,
  "skills": {
    "pdf": {
      "source": "builtin",
      "version": "1.0.0",
      "md_hash": "abc123..."
    },
    "browser": {
      "source": "enterprise",
      "version": "2.1.0",
      "md_hash": "def456...",
      "enterprise_version": 3,
      "last_pulled_at": "2026-07-30T10:00:00Z"
    },
    "my-custom-tool": {
      "source": "customized",
      "version": "0.1.0"
    }
  },
  "builtin_skill_names": ["pdf"]
}
```

> `enterprise_version` 和 `last_pulled_at` 是企业技能专有的增量同步字段。

---

## 3. 技能同步管理器

**新建文件**：`src/qwenpaw/sync/skill_sync.py`

### 3.1 类定义

```python
# -*- coding: utf-8 -*-
"""企业技能增量同步。

登录时对比本地 manifest version 与服务端 version，
增量下载变更技能（zip 包），解压到 skill_pool/。

核心原则:
- 企业技能只读，不可本地编辑/删除
- 个人技能独立于 workspace/skills/，不受影响
- 增量同步：仅下载 version 变更的技能
- 断网时使用本地已拉取的技能
"""
from __future__ import annotations

import asyncio
import logging
import shutil
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from ..agents.skill_system.store import (
    get_skill_pool_dir,
    read_skill_pool_manifest,
    write_json_atomic,
    _extract_and_validate_zip,
    compute_skill_md_hash,
    safe_skill_dir,
)
from ..config.config import SkillSyncConfig
from .client import PigClient
from .errors import SyncError, SyncNetworkError, SyncServerError

logger = logging.getLogger(__name__)

_MAX_CONCURRENT_DOWNLOADS = 3


class SkillSyncManager:
    """企业技能增量同步管理器。

    生命周期: 由 SyncLifecycle 持有，登录时调用 pull_enterprise_skills()。
    """

    def __init__(
        self,
        pig_client: PigClient,
        config: SkillSyncConfig,
    ) -> None:
        self._pig = pig_client
        self._config = config
        self._pool_dir = get_skill_pool_dir()

    async def pull_enterprise_skills(self) -> int:
        """登录时增量拉取企业技能。

        1. GET /api/sync/skills/manifest → 服务端 manifest
        2. 对比本地 pool manifest 的 enterprise_version
        3. 对变更技能: 下载 zip → 解压到 skill_pool/
        4. 更新本地 pool manifest

        Returns:
            本次更新的技能数量。
        """
        if not self._config.auto_pull_enterprise:
            return 0

        try:
            # 1. 拉取服务端 manifest
            server_manifest = await self._pig.get(
                "/api/sync/skills/manifest",
            )
            server_skills = server_manifest.get("skills", [])
            server_version = server_manifest.get("version", 0)

            # 2. 读取本地 manifest
            local_manifest = read_skill_pool_manifest()
            local_skills = local_manifest.get("skills", {})

            # 3. 找出需要更新的技能
            to_update: list[dict] = []
            for skill in server_skills:
                name = skill.get("name", "")
                if not name:
                    continue
                local_entry = local_skills.get(name, {})
                local_ev = local_entry.get("enterprise_version", 0)
                server_ev = skill.get("enterprise_version", 0)
                if server_ev > local_ev:
                    to_update.append(skill)

            if not to_update:
                logger.debug(
                    "Enterprise skills up to date "
                    "(server version: %d)",
                    server_version,
                )
                return 0

            logger.info(
                "Enterprise skills to update: %d",
                len(to_update),
            )

            # 4. 并发下载并安装（限制并发数）
            semaphore = asyncio.Semaphore(_MAX_CONCURRENT_DOWNLOADS)
            tasks = [
                asyncio.create_task(
                    self._download_and_install(semaphore, skill),
                )
                for skill in to_update
            ]
            results = await asyncio.gather(*tasks, return_exceptions=True)

            # 5. 更新 manifest
            updated = 0
            now = datetime.now(timezone.utc).isoformat()
            for skill, result in zip(to_update, results):
                if isinstance(result, Exception):
                    logger.warning(
                        "Skill '%s' download failed: %s",
                        skill.get("name"),
                        result,
                    )
                    continue
                if result:
                    name = skill["name"]
                    local_skills[name] = {
                        "source": "enterprise",
                        "version": skill.get("version", ""),
                        "md_hash": skill.get("md_hash", ""),
                        "enterprise_version": skill.get(
                            "enterprise_version", 0,
                        ),
                        "last_pulled_at": now,
                    }
                    updated += 1

            # 6. 写回 manifest
            local_manifest["skills"] = local_skills
            manifest_path = self._pool_dir / "skill.json"
            write_json_atomic(manifest_path, local_manifest)

            logger.info(
                "Enterprise skills updated: %d/%d",
                updated,
                len(to_update),
            )
            return updated

        except SyncError as exc:
            logger.warning("Enterprise skill pull failed: %s", exc)
            return 0
        except Exception:
            logger.warning(
                "Enterprise skill pull unexpected error",
                exc_info=True,
            )
            return 0

    async def _download_and_install(
        self,
        semaphore: asyncio.Semaphore,
        skill: dict,
    ) -> bool:
        """下载并安装一个企业技能。

        Returns:
            True 安装成功，False 失败。
        """
        name = skill["name"]
        async with semaphore:
            try:
                # 下载 zip 到临时文件
                with tempfile.NamedTemporaryFile(
                    suffix=".zip",
                    delete=False,
                ) as tmp:
                    tmp_path = Path(tmp.name)

                try:
                    await self._pig.download(
                        f"/api/sync/skills/{name}/download",
                        tmp_path,
                    )

                    # 安装到 skill_pool/
                    await asyncio.to_thread(
                        self._install_skill_zip,
                        tmp_path,
                        name,
                    )

                    logger.debug(
                        "Enterprise skill '%s' installed",
                        name,
                    )
                    return True
                finally:
                    tmp_path.unlink(missing_ok=True)

            except (SyncNetworkError, SyncServerError) as exc:
                logger.warning(
                    "Skill '%s' download failed: %s",
                    name,
                    exc,
                )
                return False

    def _install_skill_zip(self, zip_path: Path, skill_name: str) -> None:
        """解压 zip 到 skill_pool/{skill_name}/。

        复用现有 _extract_and_validate_zip 做安全校验。
        """
        target_dir = safe_skill_dir(self._pool_dir, skill_name)

        # 如果已存在（旧版本），先删除
        if target_dir.exists():
            shutil.rmtree(target_dir)

        target_dir.mkdir(parents=True, exist_ok=True)

        # 读取 zip 数据并解压
        zip_data = zip_path.read_bytes()
        _extract_and_validate_zip(zip_data, target_dir)

        # 验证 SKILL.md 存在
        skill_md = target_dir / "SKILL.md"
        if not skill_md.exists():
            shutil.rmtree(target_dir)
            raise ValueError(
                f"Downloaded skill '{skill_name}' has no SKILL.md",
            )

    # ================================================================
    # 个人技能上传（可选）
    # ================================================================

    async def upload_personal_skill(
        self,
        skill_name: str,
        skill_dir: Path,
    ) -> bool:
        """上传个人技能到 pig 服务端（云备份）。

        1. 将 skill_dir 打包为 zip
        2. POST /api/sync/skills/upload
        3. 服务端存储为个人备份

        Returns:
            True 上传成功，False 失败。
        """
        try:
            import zipfile

            with tempfile.NamedTemporaryFile(
                suffix=".zip",
                delete=False,
            ) as tmp:
                tmp_path = Path(tmp.name)

            try:
                # 打包技能目录为 zip
                with zipfile.ZipFile(
                    tmp_path, "w", zipfile.ZIP_DEFLATED,
                ) as zf:
                    for file_path in skill_dir.rglob("*"):
                        if file_path.is_file():
                            arcname = file_path.relative_to(skill_dir)
                            zf.write(file_path, arcname)

                # 上传
                await self._pig.upload(
                    "/api/sync/skills/upload",
                    tmp_path,
                )

                logger.info(
                    "Personal skill '%s' uploaded to pig",
                    skill_name,
                )
                return True
            finally:
                tmp_path.unlink(missing_ok=True)

        except SyncError as exc:
            logger.warning(
                "Personal skill '%s' upload failed: %s",
                skill_name,
                exc,
            )
            return False

    # ================================================================
    # 删除企业技能（可选，用于离开企业时清理）
    # ================================================================

    def remove_enterprise_skills(self) -> int:
        """删除本地所有企业技能。

        用于用户退出企业/切换账号时清理。
        Returns: 删除的技能数。
        """
        manifest = read_skill_pool_manifest()
        skills = manifest.get("skills", {})
        removed = 0

        for name, entry in list(skills.items()):
            if is_enterprise_skill_entry(entry):
                skill_dir = self._pool_dir / name
                if skill_dir.exists():
                    shutil.rmtree(skill_dir)
                skills.pop(name)
                removed += 1

        if removed > 0:
            manifest["skills"] = skills
            write_json_atomic(
                self._pool_dir / "skill.json",
                manifest,
            )
            logger.info(
                "Removed %d enterprise skills",
                removed,
            )
        return removed
```

### 3.2 设计决策

| 决策 | 理由 |
|------|------|
| 增量同步用 `enterprise_version` | 独立于技能自身的 `version`，服务端每次更新企业技能时递增 |
| 并发下载限制 3 个 | 避免大量技能同时下载时带宽打满 |
| 下载到临时文件再解压 | 避免下载中断导致技能目录损坏 |
| 安装前删除旧版本目录 | 确保企业技能与服务端完全一致 |
| 复用 `_extract_and_validate_zip` | 路径安全校验（防 zip traversal、symlink） |

---

## 4. 企业技能只读保护

### 4.1 写入路由守卫

**文件**：`src/qwenpaw/app/routers/skills.py`

在企业技能相关的写入操作（create/update/delete/import 覆盖）中增加守卫。

实施时需找到 skills 路由中所有写入操作，在操作前检查：

```python
from ...agents.skill_system.store import is_enterprise_skill


def _guard_enterprise_skill_readonly(skill_name: str):
    """拒绝对企业技能的写入操作。"""
    if is_enterprise_skill(skill_name):
        from fastapi import HTTPException
        raise HTTPException(
            status_code=403,
            detail=(
                f"Skill '{skill_name}' is an enterprise skill "
                f"and is read-only. It is managed by the server."
            ),
        )
```

在以下路由中调用守卫（具体函数名实施时根据 `skills.py` 实际路由确定）：

| 路由 | 守卫位置 |
|------|---------|
| `POST /api/skills/{name}` (创建) | 检查同名企业技能是否已存在 |
| `PUT /api/skills/{name}` (更新) | 检查目标是否为企业技能 |
| `DELETE /api/skills/{name}` (删除) | 检查目标是否为企业技能 |
| `POST /api/skills/import` (导入覆盖) | 检查同名企业技能 |

```python
# 示例：delete 路由守卫
@router.delete("/skills/{skill_name}")
async def delete_skill(skill_name: str):
    _guard_enterprise_skill_readonly(skill_name)  # 新增
    # ... 现有删除逻辑 ...
```

### 4.2 前端只读标识

**文件**：`console/src/pages/Agent/Skills/`

前端技能列表中，企业技能应显示只读标识（如"企业"标签），并禁用编辑/删除按钮。

实施时在技能卡片组件中根据 `source === "enterprise"` 显示：

```tsx
// 技能卡片
{skill.source === "enterprise" && (
  <Tag color="blue">企业</Tag>
)}
{skill.source === "enterprise" && (
  // 禁用编辑/删除按钮
  <Button disabled>编辑</Button>
  <Button disabled>删除</Button>
)}
```

> 前端具体实施需根据 Skills 页面的实际组件结构调整。

---

## 5. 个人技能上传（可选）

### 5.1 上传路由

**文件**：`src/qwenpaw/app/routers/skills.py`

新增上传端点：

```python
@router.post("/skills/{skill_name}/upload-to-server")
async def upload_skill_to_server(
    skill_name: str,
    request: Request,
):
    """上传个人技能到 pig 服务端（云备份）。"""
    from ...sync.lifecycle import get_sync_lifecycle
    from ...agents.skill_system.store import (
        get_workspace_skills_dir,
        resolve_pool_skill_dir,
    )

    sync_lc = get_sync_lifecycle()
    if not sync_lc or not sync_lc._skill_sync:
        raise HTTPException(403, "Skill sync is not enabled")

    # 拒绝上传企业技能
    from ...agents.skill_system.store import is_enterprise_skill
    if is_enterprise_skill(skill_name):
        raise HTTPException(403, "Cannot upload enterprise skill")

    # 查找技能目录
    skill_dir = resolve_pool_skill_dir(skill_name)
    if not skill_dir:
        raise HTTPException(404, f"Skill '{skill_name}' not found")

    success = await sync_lc._skill_sync.upload_personal_skill(
        skill_name, skill_dir,
    )
    if not success:
        raise HTTPException(500, "Upload failed")

    return {"message": f"Skill '{skill_name}' uploaded", "uploaded": True}
```

### 5.2 前端上传按钮

个人技能卡片增加"上传到服务端"按钮：

```tsx
{skill.source !== "enterprise" && (
  <Button onClick={() => uploadToServer(skill.name)}>
    上传到服务端
  </Button>
)}
```

---

## 6. SyncLifecycle 集成

### 6.1 初始化

**文件**：`src/qwenpaw/sync/lifecycle.py`

在 `on_startup()` 中创建 `SkillSyncManager`：

```python
async def on_startup(self) -> None:
    # ... PigClient 初始化 ...

    # 创建技能同步管理器
    if cfg.sync.skill.auto_pull_enterprise:
        try:
            from .skill_sync import SkillSyncManager
            self._skill_sync = SkillSyncManager(
                self._pig_client,
                cfg.sync.skill,
            )
            self.register_skill_sync(self._skill_sync)
        except Exception:
            logger.warning("Skill sync manager init failed", exc_info=True)
```

### 6.2 登录时拉取

在 `on_login_success()` 中已预留（阶段 0 设计）：

```python
if cfg.sync.skill.auto_pull_enterprise and self._skill_sync:
    tasks.append((
        "skills",
        asyncio.create_task(
            self._skill_sync.pull_enterprise_skills(),
        ),
    ))
```

---

## 7. pig 服务端接口约定

### 7.1 技能 manifest

```
GET /api/sync/skills/manifest

Headers:
  Authorization: Bearer <access_token>

Response 200:
{
  "version": 15,
  "skills": [
    {
      "name": "browser",
      "version": "2.1.0",
      "md_hash": "sha256:abcdef...",
      "enterprise_version": 3,
      "description": "Browser automation skill"
    },
    {
      "name": "pdf",
      "version": "1.0.0",
      "md_hash": "sha256:123456...",
      "enterprise_version": 1,
      "description": "PDF processing skill"
    }
  ]
}
```

### 7.2 技能包下载

```
GET /api/sync/skills/{name}/download

Headers:
  Authorization: Bearer <access_token>

Response 200:
  binary (application/zip)
```

zip 包结构：

```
browser.zip
  ├── SKILL.md          (frontmatter + 内容)
  ├── scripts/
  │   └── browser.py
  └── config.yaml
```

### 7.3 个人技能上传（可选）

```
POST /api/sync/skills/upload

Headers:
  Authorization: Bearer <access_token>
  Content-Type: multipart/form-data

Body:
  file: <zip binary>

Response 200:
{
  "name": "my-custom-skill",
  "stored": true,
  "version": "0.1.0"
}
```

### 7.4 pig 服务端实现要点（Java/Spring）

```java
// pig 侧需新建表
CREATE TABLE qwenpaw_enterprise_skills (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    version           VARCHAR(50),
    md_hash           VARCHAR(255),
    enterprise_version INTEGER NOT NULL DEFAULT 1,
    description       TEXT,
    storage_path      VARCHAR(500) NOT NULL,  -- 对象存储路径
    tenant_id         VARCHAR(255),
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, tenant_id)
);

CREATE TABLE qwenpaw_personal_skills (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    version     VARCHAR(50),
    md_hash     VARCHAR(255),
    storage_path VARCHAR(500) NOT NULL,
    user_id     VARCHAR(255) NOT NULL,   -- 上传者
    tenant_id   VARCHAR(255),
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE(name, user_id, tenant_id)
);
```

> 技能包文件存储建议使用对象存储（MinIO/OSS），数据库只存元数据和路径。`enterprise_version` 每次更新技能时递增。

---

## 8. 验收标准

### 功能验收

- [ ] 登录后 `skill_pool/` 包含服务端企业技能
- [ ] 企业技能目录包含 `SKILL.md` 文件
- [ ] pool manifest (`skill.json`) 中企业技能 `source: "enterprise"`
- [ ] 企业技能在前端展示，标记为"企业"来源
- [ ] 个人技能（`workspace/skills/`）不受影响
- [ ] 同名时个人技能优先加载

### 增量同步验收

- [ ] 首次登录拉取全部企业技能
- [ ] 第二次登录（无变更）不重复下载，日志 `up to date`
- [ ] 服务端技能 `enterprise_version` 递增后，下次登录增量下载
- [ ] 下载中断时不损坏现有技能目录（临时文件机制）
- [ ] 并发下载数不超过 3 个

### 只读保护验收

- [ ] 企业技能不可通过 API 编辑（403）
- [ ] 企业技能不可通过 API 删除（403）
- [ ] 企业技能不可被同名导入覆盖（403）
- [ ] 前端企业技能的编辑/删除按钮禁用

### 安全验收

- [ ] 下载的 zip 经过路径安全校验（无 zip traversal）
- [ ] 下载的 zip 不含 symlink
- [ ] 下载的 zip 不超过 200MB（`_MAX_ZIP_BYTES`）

### 断网验收

- [ ] 断网时使用本地已拉取的技能正常工作
- [ ] pig 不可达时不影响现有技能加载
- [ ] 部分技能下载失败时，其他技能不受影响

### 开关验收

- [ ] `skill.auto_pull_enterprise=false` 时零行为变化
- [ ] `sync.enabled=false` 时不创建 `SkillSyncManager`
- [ ] 个人技能上传功能在 `sync.enabled=false` 时不可用（403）

---

*文档创建日期：2026-07-30*
*配套文档：[architecture.md](./architecture.md) · [implementation-plan.md](./implementation-plan.md) · [phase0-infrastructure.md](./phase0-infrastructure.md) · [phase1-auth-oauth2.md](./phase1-auth-oauth2.md)*
