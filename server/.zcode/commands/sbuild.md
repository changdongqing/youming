---
description: 构建 pig 后端 cloud 版或 boot 版，含 docker 启动提示
argument-hint: "[cloud|boot]"
allowed-tools: Bash, Read
---

在 `youming/server/` 目录构建后端。形态由参数指定：

$ARGUMENTS

映射规则（默认 `cloud`）：
- `cloud` → `mvn clean install -T 4 -Pcloud`（微服务版，聚合 register/gateway/auth/upms/common/visual）
- `boot` → `mvn clean install -Pboot -DskipTests`（单体版，加入 pig-boot）
- 无参数 → 询问选择

构建成功后提示对应的启动方式：
- cloud 版：`docker compose build && docker compose up`，或分别启动各服务（Nacos 8848、gateway 9999）
- boot 版：`docker compose -f docker-compose-boot.yml up -d`（单体，端口 9999，context-path /admin）

构建失败时分析 Maven 错误输出，定位模块与编译/依赖问题，给出修复建议。
