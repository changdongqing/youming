/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import java.time.Instant;
import java.util.List;

/**
 * 实体摄入命令。
 * <p>
 * 由映射作业或未来其他来源（IoT/规则/API导入）构建，传入摄入服务执行幂等写入。
 *
 * @param ontologyId 本体工程ID
 * @param namespaceId 命名空间ID
 * @param entityTypeId 目标实体类型ID
 * @param sourceIdentity 来源身份标识
 * @param iriLocalName IRI本地名
 * @param expectedIri 预期完整IRI
 * @param label UI显示标签
 * @param values 数据属性值列表
 * @param conflictPolicy 冲突策略
 * @param contentHash 内容哈希
 * @param sourceUpdatedAt 源记录更新时间
 * @param context 摄入上下文
 *
 * @author youming
 */
public record EntityIngestionCommand(
	Long ontologyId,
	Long namespaceId,
	Long entityTypeId,
	SourceIdentity sourceIdentity,
	String iriLocalName,
	String expectedIri,
	String label,
	List<ValueIngestionItem> values,
	OwnershipPolicy conflictPolicy,
	String contentHash,
	Instant sourceUpdatedAt,
	IngestionContext context
) {

}
