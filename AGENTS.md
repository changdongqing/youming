# youming 仓库指南

youming 是基于 [pig](https://github.com/pig-mesh/pig)（后端）与 [pig-ui](https://github.com/pig-mesh/pig-ui)（前端）二次开发的开源权限管理与微服务脚手架。通过 **Git Subtree** 将两个上游开源项目集成到本仓库，支持统一提交、推送，同时保留从源项目拉取更新的能力。

- 远程：github `changdongqing/youming`
- 许可证：Apache 2.0

## 仓库结构

```
youming/
├── aidocs/      # AI 生成的文档（待评审，非正式交付物）
├── docs/        # 已评审归档的文档（init/、start/）
├── server/      # 后端代码（pig，Java 17 + Spring Boot 4.x + Spring Cloud）
├── web/         # 前端代码（pig-ui，Vue 3 + Vite + TypeScript）
├── .gitignore
├── LICENSE
└── README.md
```

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 后端框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 4.0.7 / 2025.1.2 / 2025.1.0.0 |
| 后端语言 | Java | 17 |
| 注册/配置中心 | Nacos（内嵌于 `pig-register`，standalone） | 3.2.2 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis | 6.x / 7.x |
| 前端框架 | Vue 3 + Element Plus + Vite + TypeScript | 3.5 / 2.13 / 8.1 / 4.9 |
| 包管理 | Maven（后端）/ npm（前端） | — |

## 双形态运行

后端支持两种运行形态：

- **微服务版**：`pig-register`（Nacos）+ `pig-gateway`（9999）+ `pig-auth`（3000）+ `pig-upms-biz`（4000）+ `pig-visual`（codegen / monitor / quartz）等独立服务，通过 Nacos 做配置中心与服务发现。
- **单体版**：`pig-boot`（9999，context-path `/admin`），关闭 Nacos/Sentinel/Cloud 发现，直连 MySQL 与 Redis，开箱即用。

### 端口速查

| 服务 | 端口 |
|---|---|
| pig-register（Nacos 控制台） | 8848 |
| pig-register（Nacos gRPC） | 9848 |
| pig-gateway | 9999 |
| pig-monitor | 5001 |

## Git Subtree 工作流（重要）

本仓库用 Git Subtree 集成上游代码。**日常所有改动（含 `server/`、`web/` 内部）在根目录统一 `git add / commit / push` 即可。**

### 从上游拉取更新

> ⚠️ 上游默认分支为 `master`，非 `main`。

```bash
# 更新后端 pig
git subtree pull --prefix=server https://github.com/pig-mesh/pig.git master --squash

# 更新前端 pig-ui
git subtree pull --prefix=web https://github.com/pig-mesh/pig-ui.git master --squash
```

拉取后如出现冲突，按常规 Git 冲突流程解决。**注意：上游更新可能覆盖本仓库对子目录的二次开发改动，合并前请确认本地已提交。**

## 子项目指南

- **后端（pig）** → `server/AGENTS.md`：模块地图、构建命令（cloud/boot）、测试约定。
- **前端（pig-ui）** → `web/AGENTS.md`、`web/CLAUDE.md`：项目结构、API 集成模式、路由架构、状态管理、代码风格。

## 文档约定

- `aidocs/`：AI 辅助生成的草稿文档，内容可随时变动，不作为正式交付物。
- `docs/`：经评审的正式文档。新增正式文档归档到 `docs/` 对应子目录。

## codegraph 索引

`server/` 与 `web/` 各自有 `.codegraph/README.md` 索引导引。查询时 `projectPath` 指向对应子目录。

## 行为准则

以下四条规则适用于所有任务，优先级高于其他默认行为。

### 1. Think Before Coding

- 需求模糊时先提问，绝不擅自假设。
- 若存在更简单的方案，明确指出，而非默默选一个方向。
- 有不清楚的地方就暴露困惑，不要凭猜测推进。

### 2. Prefer Simplicity

- 用解决问题所需的最少代码；不添加未要求的东西。
- 不为一次性代码引入抽象，不为假想的未来需求做设计。
- 自问：资深工程师会觉得这过度设计了吗？

### 3. Surgical Changes

- 只动任务要求的范围；不把相邻重构当副作用。
- 不改与当前任务无关的风格或结构。
- 每一行改动都能追溯到明确的需求。

### 4. Goal-Driven Execution

- 把模糊指令转化为可验证的目标——优先用测试作为成功标准。
- "修 bug" → 写一个能复现的失败测试，再让它通过。
- "加校验" → 写覆盖非法输入的测试，再让它们通过。
