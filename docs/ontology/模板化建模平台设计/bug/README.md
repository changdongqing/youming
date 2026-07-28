# 本体建模平台 Bug 清单

> 在本体建模过程中发现的本程序缺陷记录，便于后续评审和修复。
> 每个 Bug 独立文件，文件名格式：`BUG-{编号}-{简述}.md`

## Bug 列表

| 编号 | 严重级别 | 标题 | 状态 | 发现日期 |
|------|---------|------|------|---------|
| [BUG-001](./BUG-001-属性property_iri序列化缺失namespace_base拼接.md) | 🔴 高 | 属性 property_iri 序列化时缺失 namespace_base 拼接 | 待修复 | 2026-07-29 |
| [BUG-002](./BUG-002-extractPrefixName硬编码返回onto与前缀冲突.md) | 🟡 中 | extractPrefixName 硬编码返回 "onto"，与注释属性命名空间前缀冲突 | 待修复 | 2026-07-29 |

## 发现背景

以上两个 Bug 均在「用户/部门/岗位本体建模」（V17 种子数据）过程中发现：

- **BUG-001**：在编写 V17 种子数据时，需确定 `property_iri` 字段的存储格式（完整 IRI 还是本地名），深入追踪代码后发现写入路径与序列化读取路径的 IRI 格式预期不一致。
- **BUG-002**：在注册 V17 的 `ont_model_prefix` 前缀数据（`ex`/`ont`/`xsd`）后，发现序列化时 `ont_model_prefix` 表完全未被读取，前缀名由 `extractPrefixName` 硬编码返回 `"onto"`。

## 修复优先级建议

1. **BUG-001（高）**：直接影响 RDF 序列化输出的正确性，产出的属性 IRI 不符合 OWL/RDF 规范，应优先修复。
2. **BUG-002（中）**：影响前缀声明的可配置性和输出可读性，不影响数据正确性（类 IRI 本身是正确的绝对 URI），可后续修复。

两个 Bug 存在关联性：BUG-002 的修复（从 `ont_model_prefix` 读取前缀）与 BUG-001 的修复（拼接 namespace_base）都在 `SerializationService.buildModel` 方法中，建议一并修复。
