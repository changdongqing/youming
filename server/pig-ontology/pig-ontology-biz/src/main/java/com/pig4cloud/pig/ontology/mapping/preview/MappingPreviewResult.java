/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.preview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 映射预览结果（18-06 §6）。
 * <p>
 * 预览默认只读，不写实例事实表和Outbox。
 * 敏感目标属性仅返回脱敏值；普通列默认最大展示128字符。
 *
 * @author youming
 */
@Data
@Schema(description = "映射预览结果")
public class MappingPreviewResult {

	@Schema(description = "校验报告ID（预览也会产生报告）")
	private Long reportId;

	@Schema(description = "实体映射预览结果")
	private List<EntityPreviewResult> entityResults;

	@Schema(description = "关系映射预览结果")
	private List<RelationPreviewResult> relationResults;

	@Schema(description = "是否截断（达到样本上限）")
	private Boolean truncated;

	/**
	 * 实体映射预览结果。
	 */
	@Data
	@Schema(description = "实体预览结果")
	public static class EntityPreviewResult {

		@Schema(description = "映射编码")
		private String mappingCode;

		@Schema(description = "源记录键哈希")
		private String sourceRecordKeyHash;

		@Schema(description = "生成的IRI")
		private String generatedIri;

		@Schema(description = "标签")
		private String label;

		@Schema(description = "操作: WOULD_CREATE / WOULD_UPDATE / UNCHANGED")
		private String action;

		@Schema(description = "值预览列表")
		private List<ValuePreview> values;

		@Schema(description = "此记录的校验问题")
		private List<String> issues;

	}

	/**
	 * 值预览。
	 */
	@Data
	@Schema(description = "值预览")
	public static class ValuePreview {

		@Schema(description = "目标属性ID")
		private Long propertyId;

		@Schema(description = "结果类型")
		private String resultType;

		@Schema(description = "脱敏预览值")
		private String maskedPreview;

	}

	/**
	 * 关系映射预览结果。
	 */
	@Data
	@Schema(description = "关系预览结果")
	public static class RelationPreviewResult {

		@Schema(description = "关系映射编码")
		private String mappingCode;

		@Schema(description = "主体记录键哈希")
		private String subjectRecordKeyHash;

		@Schema(description = "客体记录键哈希")
		private String objectRecordKeyHash;

		@Schema(description = "操作: WOULD_CREATE / UNCHANGED / PENDING")
		private String action;

		@Schema(description = "此关系的校验问题")
		private List<String> issues;

	}

}
