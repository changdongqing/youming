# codegraph 索引导引 — youming/server（pig 后端）

## 项目信息

- **语言**：Java 17
- **构建**：Maven 多模块聚合（根 `pom.xml`）
- **项目根**：`youming/server/`（即本目录的父目录）

## 如何查询

使用 `codegraph_explore` 工具时，将 `projectPath` 指向 server 目录：

```
projectPath: "D:/src/hebing/dqwork/youming/server"
```

### 常用查询示例

| 查询意图 | query 参数 |
|---|---|
| 各服务启动类 | `PigGatewayApplication PigAuthApplication PigAdminApplication PigBootApplication` |
| 网关过滤器 | `GatewayFilter GlobalFilter pig-gateway` |
| 认证逻辑 | `PigAuthApplication OAuth2 security token` |
| 用户权限 | `pig-upms User Role Menu permission` |
| 公共核心 | `pig-common-core R Result RetOps` |
| 代码生成 | `pig-codegen GenTable GenColumn template` |

## 重建索引

codegraph 索引存储在 `.codegraph/` 目录。重建时删除索引文件（保留本 README.md）后重新运行索引命令。注意 Maven 多模块结构，索引需覆盖所有 `pig-*` 子模块的 `src/main/java`。
