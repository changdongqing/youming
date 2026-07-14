/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 数据属性值摄入项。
 *
 * @param fieldMappingCode 字段映射编码
 * @param dataPropertyId 目标数据属性ID
 * @param literalValue 规范化词法值
 * @param literalType 字面量类型
 * @param unitId 单位ID
 * @param literalSymbol 单位词法快照
 * @param sortOrder 同属性多值排序
 * @param ownershipPolicy 所有权策略
 * @param sourceKind 来源类型：COLUMN/CONSTANT
 * @param sourceReference 来源引用（列名或constant标记）
 * @param valueHash 值SHA-256哈希
 *
 * @author youming
 */
public record ValueIngestionItem(
	String fieldMappingCode,
	Long dataPropertyId,
	String literalValue,
	String literalType,
	Long unitId,
	String literalSymbol,
	Integer sortOrder,
	OwnershipPolicy ownershipPolicy,
	String sourceKind,
	String sourceReference,
	String valueHash
) {

}
