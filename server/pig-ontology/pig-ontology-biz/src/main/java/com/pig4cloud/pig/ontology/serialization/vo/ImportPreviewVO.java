/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 导入预检结果VO。
 *
 * @author youming
 */
@Data
@Schema(description = "导入预检结果")
public class ImportPreviewVO {

	@Schema(description = "预检ID")
	private String previewId;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "摘要统计")
	private Summary summary;

	@Schema(description = "实例列表")
	private List<InstanceInfo> instances;

	@Schema(description = "冲突列表")
	private List<ConflictInfo> conflicts;

	@Schema(description = "错误列表")
	private List<IssueInfo> errors;

	@Schema(description = "警告列表")
	private List<IssueInfo> warnings;

	@Data
	@Schema(description = "摘要统计")
	public static class Summary {

		@Schema(description = "解析实例数")
		private int totalInstances;

		@Schema(description = "数据值数")
		private int totalDataValues;

		@Schema(description = "对象关系数")
		private int totalObjectRelations;

		@Schema(description = "Schema三元组跳过数")
		private int schemaSkipped;

		@Schema(description = "冲突数")
		private int conflictCount;

		@Schema(description = "错误数")
		private int errorCount;

		@Schema(description = "警告数")
		private int warningCount;

	}

	@Data
	@Schema(description = "实例信息")
	public static class InstanceInfo {

		@Schema(description = "实例IRI")
		private String iri;

		@Schema(description = "标签")
		private String label;

		@Schema(description = "实体类型IRI")
		private String rdfTypeIri;

		@Schema(description = "实体类型ID")
		private Long rdfTypeId;

		@Schema(description = "冲突类型")
		private String conflictType;

		@Schema(description = "数据值数")
		private int dataValueCount;

		@Schema(description = "对象关系数")
		private int objectRelationCount;

	}

	@Data
	@Schema(description = "冲突信息")
	public static class ConflictInfo {

		@Schema(description = "实例IRI")
		private String iri;

		@Schema(description = "冲突类型")
		private String conflictType;

	}

	@Data
	@Schema(description = "问题信息")
	public static class IssueInfo {

		@Schema(description = "实例IRI")
		private String instanceIri;

		@Schema(description = "消息")
		private String message;

	}

}
