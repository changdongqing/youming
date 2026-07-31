---
description: 构建 pig-ui 前端（安装 + lint + build）
allowed-tools: Bash, Read
---

在 `youming/web/` 目录执行完整前端构建流程：

$ARGUMENTS

步骤：
1. `pnpm install` — 安装依赖（Node >= 20.19.0）
2. `pnpm lint:eslint` — ESLint 检查并自动修复 `.js/.ts/.vue`（`no-console` 为 error）
3. `pnpm build` — 生产构建（`cross-env NODE_OPTIONS=--max-old-space-size=8192 vite build`，输出 `dist/`）

可选：
- 若参数含 `docker`，改用 `pnpm build:docker`（输出到 `docker/dist/`）
- 构建前确认 `.env` 中 `VITE_API_URL`、`VITE_ADMIN_PROXY_PATH` 配置正确

构建失败时分析 Vite/ESLint 错误输出，定位文件与问题，给出修复建议。注意路径别名 `/@/` → `src/`。
