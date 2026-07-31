---
description: 运行前端 ESLint + Prettier 检查并修复
allowed-tools: Bash, Read
---

在 `youming/web/` 目录运行代码质量检查与修复：

$ARGUMENTS

步骤：
1. `pnpm lint:eslint` — ESLint 检查并自动修复 `./src` 下的 `.js/.ts/.vue`
2. `pnpm prettier` — Prettier 格式化全部文件

约定（来自 AGENTS.md）：
- Prettier：tabs 启用，宽度 150，单引号，分号
- ESLint：Vue 3 + TypeScript；`no-console` 为 error
- 优先用 `/@/` 别名而非长相对路径
- 组件用 Composition API `<script setup>`，PascalCase.vue，feature 目录用 `FeatureName/index.vue`

检查后报告发现的问题数量与位置，对无法自动修复的项给出手动修复建议。
