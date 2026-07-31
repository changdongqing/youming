# codegraph 索引导引 — youming/web（pig-ui 前端）

## 项目信息

- **语言**：TypeScript + Vue 3（`<script setup>` Composition API）
- **框架**：Vue 3.5 + Element Plus 2.13 + Vite 8.1 + Pinia
- **样式**：Tailwind CSS + DaisyUI + SCSS
- **项目根**：`youming/web/`（即本目录的父目录）
- **路径别名**：`/@/` → `src/`（在 `vite.config.mts` 配置）

## 如何查询

使用 `codegraph_explore` 工具时，将 `projectPath` 指向 web 目录：

```
projectPath: "D:/src/hebing/dqwork/youming/web"
```

### 常用查询示例

| 查询意图 | query 参数 |
|---|---|
| 请求封装 | `request request.ts axios baseURL` |
| 后端路由控制 | `backEnd initBackEndControlRoutes import.meta.glob` |
| 路由守卫 | `router/index.ts beforeEach guard` |
| 状态管理 | `userInfo routesList tagsViewRoutes keepAliveNames themeConfig` |
| API 层 | `api fetchList request admin` |
| 布局 | `layout components` |

## 重建索引

codegraph 索引存储在 `.codegraph/` 目录。重建时删除索引文件（保留本 README.md）后重新运行索引命令。注意 Vue SFC（`.vue`）与 TS 文件均需被索引覆盖。
