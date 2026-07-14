/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

/**
 * 关系摄入命令。
 *
 * @param ontologyId 本体工程ID
 * @param objectPropertyId 对象属性ID（谓词）
 * @param subjectIdentity 主体来源身份
 * @param objectIdentity 客体来源身份
 * @param relationMappingCode 关系映射编码
 * @param sourceRelationKey 来源关系键
 * @param ownershipPolicy 所有权策略
 * @param context 摄入上下文
 *
 * @author youming
 */
public record RelationIngestionCommand(
	Long ontologyId,
	Long objectPropertyId,
	SourceIdentity subjectIdentity,
	SourceIdentity objectIdentity,
	String relationMappingCode,
	String sourceRelationKey,
	OwnershipPolicy ownershipPolicy,
	IngestionContext context
) {

}
