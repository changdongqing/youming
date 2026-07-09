# youming

> 基于 [pig](https://github.com/pig-mesh/pig)（后端）与 [pig-ui](https://github.com/pig-mesh/pig-ui)（前端）二次开发的开源权限管理与微服务脚手架项目。
> 通过 Git Subtree 将两个上游开源项目集成到本仓库，支持统一提交、推送，同时保留从源项目拉取更新的能力。

## 仓库结构

```
youming/
├── aidocs/      # AI 生成的文档（待评审，非正式交付物）
├── docs/        # 已评审归档的文档
│   ├── init/    # 项目初始化相关文档
│   └── start/   # 本机环境配置与调试启动文档
├── server/      # 后端代码（pig，Java 17 + Spring Boot 4.x + Spring Cloud）
├── web/         # 前端代码（pig-ui，Vue 3 + Vite + TypeScript）
├── .gitignore
├── LICENSE
└── README.md
```

## 技术栈

| 层 | 技术 | 版本（参考） |
|----|------|-------------|
| 后端框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 4.0.7 / 2025.1.2 / 2025.1.0.0 |
| 后端语言 | Java | 17 |
| 注册/配置中心 | Nacos（内嵌于 `pig-register`，standalone 模式） | 3.2.2 |
| 数据库 | PostgreSQL | 18.x（库结构由 [Flyway](https://flywaydb.org/) 统一管理，迁移脚本见 `server/pig-common/pig-common-data/src/main/resources/db/migration/`） |
| 缓存 | Redis | 8.x |
| 前端框架 | Vue 3 + Element Plus + Vite + TypeScript | 3.5 / 2.13 / 8.1 / 4.9 |
| 包管理 | Maven（后端）/ npm（前端） | — |

后端支持两种运行形态：

- **微服务版**：`pig-register`（Nacos）+ `pig-gateway`（9999）+ `pig-auth`（3000）+ `pig-upms-biz`（4000）+ `pig-visual`（codegen / monitor / quartz）等独立服务，通过 Nacos 做配置中心与服务发现。
- **单体版**：`pig-boot`（9999，context-path `/admin`），关闭 Nacos/Sentinel/Cloud 发现，直连 PostgreSQL 与 Redis，开箱即用。

> 数据库已从上游的 MySQL 迁移至 PostgreSQL（业务库 + Nacos 元数据库全部去 MySQL），迁移设计与核验记录见 `docs/dbversion/`。**开发与测试一律以 PostgreSQL 为准**。

## 快速开始

本机调试的具体配置要求与 VSCode 启动步骤，参见：

- [微服务版开发服务器本机配置要求](docs/start/微服务版开发服务器本机配置要求.md)
- [单体版开发服务器本机配置要求](docs/start/单体版开发服务器本机配置要求.md)

## 与上游开源项目的同步

本仓库使用 Git Subtree 集成上游代码。日常所有改动（含 `server/`、`web/` 内部）在根目录统一 `git add / commit / push` 即可。

如需从上游拉取最新代码（注意上游默认分支为 `master`，非 `main`）：

```bash
# 更新后端 pig
git subtree pull --prefix=server https://github.com/pig-mesh/pig.git master --squash

# 更新前端 pig-ui
git subtree pull --prefix=web https://github.com/pig-mesh/pig-ui.git master --squash
```

> ⚠️ 拉取更新后如出现冲突，按常规 Git 冲突流程解决。注意：上游更新可能覆盖本仓库对子目录的二次开发改动，合并前请确认本地已提交。

## 文档约定

- `aidocs/`：AI 辅助生成的草稿文档，内容可随时变动。
- `docs/`：经评审的正式文档。新增正式文档请归档到 `docs/` 对应子目录。

## 许可证

详见 [LICENSE](LICENSE)。
