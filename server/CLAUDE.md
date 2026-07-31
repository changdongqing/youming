# CLAUDE.md — youming/server（pig 后端）

本文件为 Claude Code 提供后端子项目指南。完整的仓库级指南见上级 `../AGENTS.md`；本目录的通用指南见 `AGENTS.md`。

## 项目概述

pig 后端——基于 Spring Cloud 的开源权限管理与微服务脚手架。youming 通过 Git Subtree 集成上游 [pig](https://github.com/pig-mesh/pig)，在 `server/` 下二次开发。

## 版本（关键）

- **Java**：17
- **Spring Boot**：4.0.7（pom.xml 中有两处 `spring-boot.version` 声明，Maven 取最后一条即 4.0.7 生效）
- **Spring Cloud**：2025.1.2
- **Spring Cloud Alibaba**：2025.1.0.0
- **Nacos**：3.2.2（内嵌于 pig-register）
- **MySQL**：8.0.32（db/Dockerfile 基于 mysql-server:8.0.32）
- **Redis**：经 docker-compose 提供，pig-boot 配置 `spring.data.redis.host: pig-redis`

## 构建命令

```bash
# 微服务版（cloud，默认 profile，聚合 register/gateway/auth/upms/common/visual）
mvn clean install -T 4 -Pcloud

# 单体版（boot，加入 pig-boot 模块）
mvn clean install -Pboot -DskipTests

# 测试（JUnit 5 + AssertJ + Mockito，spring-javaformat 检查）
mvn verify

# Docker 启动（微服务版）
docker compose build && docker compose up

# Docker 启动（单体版）
docker compose -f docker-compose-boot.yml up -d
```

## 模块地图

| 模块 | 职责 | 主类 |
|---|---|---|
| `pig-register` | Nacos 注册配置中心（内嵌 standalone） | `com.alibaba.nacos.bootstrap.PigNacosApplication` |
| `pig-gateway` | 服务网关（spring-cloud-gateway-server-webflux） | `com.pig4cloud.pig.gateway.PigGatewayApplication` |
| `pig-auth` | 认证授权中心（Spring Security OAuth2） | `com.pig4cloud.pig.auth.PigAuthApplication` |
| `pig-upms` | 用户权限管理聚合（api + biz） | `com.pig4cloud.pig.admin.PigAdminApplication` |
| `pig-common` | 公共聚合（12 子模块：core/data/datasource/excel/feign/log/oss/security/sentinel/swagger/xss/bom） | — |
| `pig-visual` | 图形化功能（codegen/quartz/monitor） | 各有独立主类 |
| `pig-boot` | 单体版启动器（仅 `-Pboot`） | `com.pig4cloud.pig.PigBootApplication` |

## 端口

- pig-register：8848（控制台）/ 9848（gRPC）/ 18080
- pig-gateway：9999
- pig-monitor：5001

## 数据库

- SQL 文件：`db/pig.sql`、`db/pig_config.sql`，随 MySQL 容器自动初始化。
- Redis：docker-compose 内 `pig-redis`，未发布端口（内部网络）。

## 测试约定

- 框架：`spring-boot-starter-test`（JUnit 5、AssertJ、Mockito）
- 命名：`*Tests.java`，fixtures 放 `src/test/resources`
- 覆盖重点：认证、网关过滤器、用户/权限逻辑、定时任务、代码生成
- 代码风格：`io.spring.javaformat:spring-javaformat-maven-plugin`（0.0.47）

## 提交规范

`type(scope): summary`，如 `fix(upms): clear login failure cache`、`feat(codegen): add template option`。PR 需描述影响范围、列出受影响模块、引用 issue；UI/OpenAPI 变更附 curl/截图。

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
