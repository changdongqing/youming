# youming 仓库记忆 (AGENTS.md)

> 本文件记录 youming 项目专属的环境、约定与关键事实，供 AI 会话延续。`server/AGENTS.md` 是 pig 上游的构建/测试规范，两者并存。

## 项目定位

- 基于 [pig](https://github.com/pig-mesh/pig)（后端 `server/`）+ [pig-ui](https://github.com/pig-mesh/pig-ui)（前端 `web/`）二次开发。
- 通过 Git Subtree 集成上游，所有改动在根目录统一提交；上游默认分支为 `master`，本仓库主分支为 `main`。
- 当前开发分支：`youming`（基于 `main`）。
- 两种运行形态：微服务版（Nacos + gateway + auth + upms-biz + visual 等）；单体版 `pig-boot`（9999，context-path `/admin`，直连 DB/Redis，关闭 Nacos）。

## 本机开发环境（Docker 部署的基础设施）

本机通过 1Panel 管理 Docker 容器，开发所需基础设施均已运行：

| 服务 | 容器名 | 端口 | 凭证 / 说明 |
|------|--------|------|------------|
| **PostgreSQL** | `1Panel-postgresql-ANBv` | 5432 | PG 18.4-alpine；**用户名 `user_PAmcy2`，密码 `password_bkQ4JT`**；数据卷 `/opt/1panel/apps/postgresql/postgresql/data`。业务库结构由 Flyway 管理（见下） |
| MySQL（已弃用） | `1Panel-mysql-AFGI` | 3306 | MySQL 8.4.10；账号 `root/root`。**youming 已迁移至 PostgreSQL，开发测试不再使用 MySQL**（仅保留作迁移源回溯） |
| Nacos | `1Panel-nacos-4GLs-standalone` | 8848/9848/8080 | nacos-server v3.2.2 standalone（可复用，也可用内嵌 pig-register） |
| Redis | `1Panel-redis-2G4M` | 6379 | redis 8.8.0 |

### youming 业务库（PostgreSQL）

- **数据库名：`youmingdb`**（容器 `1Panel-postgresql-ANBv` 内，已创建）
- 连接串：`jdbc:postgresql://127.0.0.1:5432/youmingdb`（容器内或宿主机均可用 5432）
- 用户名 / 密码：`user_PAmcy2` / `password_bkQ4JT`
- **库结构由 [Flyway](https://flywaydb.org/) 统一管理**：迁移脚本位于 `server/pig-common/pig-common-data/src/main/resources/db/migration/`（V1 业务表结构 / V2 种子数据 / V3 Quartz 表），应用启动自动迁移。**变更库结构须新增版本脚本，禁止直接改已应用的脚本**（checksum 校验）。

> 操作 PG 的便捷方式（本机无 psql CLI，通过 docker exec）：
> ```bash
> # 执行 SQL
> docker exec -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv psql -U user_PAmcy2 -d youmingdb -c "<SQL>"
> # 导入 SQL 文件
> docker exec -i -e PGPASSWORD=password_bkQ4JT 1Panel-postgresql-ANBv psql -U user_PAmcy2 -d youmingdb < some.sql
> # Nacos 元数据库同理（库名按实际）
> ```

## 数据库迁移背景（MySQL → PostgreSQL）

- 原始设计由 pig 上游产出，评审文档见 `docs/dbversion/00~05`（已复核）。
- youming 专属落地设计见 `docs/dbversion/06-youming迁移执行设计.md`、`07-youming-Flyway落地设计.md`、`08-DDL转换结果核验清单.md`。
- 迁移范围：业务库 + Nacos 元数据库，全部去 MySQL。
- 迁移方式：空库迁移，直接 Flyway 基线脚本建表 + 种子数据（不用 pgloader）。
- 关键源码事实（已核实）：见 `docs/dbversion/00-总评审报告.md` 第二节证据链。

## 开发与提交约定

- 提交格式：`type(scope): summary`（中文 summary 可接受），如 `feat: ...`、`docs: ...`、`fix: ...`。
- 每完成一个独立任务提交一次（用户偏好细粒度提交）。
- 不提交 target/、.env 等生成物与密钥。

## 分支与上游同步

- 三分支模型：`main`（主干）/ `upstream-sync`（上游同步专用）/ `youming`（开发）。详见 `docs/init/分支管理与上游同步规范.md`。
- 拉取上游更新：
  ```bash
  git subtree pull --prefix=server https://github.com/pig-mesh/pig.git master --squash
  git subtree pull --prefix=web https://github.com/pig-mesh/pig-ui.git master --squash
  ```
