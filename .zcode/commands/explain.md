---
description: 解释 youming 整体架构或某子项目
argument-hint: "[server|web|架构]"
allowed-tools: Read, Bash
---

解释 youming 仓库的架构。目标由参数指定：

$ARGUMENTS

映射规则：
- `server` → 读取 `server/AGENTS.md`，总结后端模块地图（pig-register/gateway/auth/upms/common/visual/boot）、cloud vs boot 构建模式、数据库与缓存。
- `web` → 读取 `web/AGENTS.md` 与 `web/CLAUDE.md`，总结前端结构（api/components/views/router/stores）、路由架构、状态管理、代码风格。
- `架构` 或无参数 → 读取根 `AGENTS.md`，总结 subtree 集成模式、双形态运行、前后端协作关系。
- 其他 → 视为后端模块名（如 gateway/auth/upms），定位并解释该模块。

输出格式：概述 → 关键文件/类 → 构建/运行命令。
