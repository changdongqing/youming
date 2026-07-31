# GEMINI.md — youming/server（pig 后端）

## 项目概述

pig 后端，基于 Spring Cloud 的开源权限管理与微服务脚手架。youming 通过 Git Subtree 集成上游 pig，在 `server/` 下二次开发。支持微服务版（Nacos + 多独立服务）与单体版（pig-boot）两种运行形态。

## 关键技术

- **框架**：Spring Boot 4.0.7 / Spring Cloud 2025.1.2 / Spring Cloud Alibaba 2025.1.0.0
- **语言**：Java 17
- **注册/配置中心**：Nacos 3.2.2（内嵌 pig-register，standalone）
- **数据库**：MySQL 8.x
- **缓存**：Redis
- **构建**：Maven（多模块聚合）

## 架构

模块化微服务架构，通过根 `pom.xml` 聚合：

- `pig-register` — Nacos 注册配置中心
- `pig-gateway` — 服务网关（端口 9999）
- `pig-auth` — 认证授权中心（Spring Security OAuth2）
- `pig-upms` — 用户权限管理（api + biz 两个子模块）
- `pig-common` — 公共库聚合（core/data/datasource/excel/feign/log/oss/security/sentinel/swagger/xss/bom）
- `pig-visual` — 代码生成 / 定时任务 / 监控
- `pig-boot` — 单体版启动器（仅 `-Pboot` profile，端口 9999，context-path `/admin`）

服务发现与配置经 Nacos（8848 控制台 / 9848 gRPC）。数据库初始化 SQL 在 `db/`（pig.sql、pig_config.sql）。

## 构建

```bash
mvn clean install -T 4 -Pcloud      # 微服务版
mvn clean install -Pboot -DskipTests # 单体版
mvn verify                           # 测试 + spring-javaformat 检查
docker compose up                    # 微服务版容器栈
```
