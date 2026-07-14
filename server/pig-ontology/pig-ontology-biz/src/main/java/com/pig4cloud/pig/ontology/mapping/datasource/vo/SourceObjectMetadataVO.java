/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据源对象元数据 VO。
 * <p>
 * 描述一个表或视图的列、主键、唯一键和外键结构。
 *
 * @author youming
 */
@Data
@Schema(description = "数据源对象元数据")
public class SourceObjectMetadataVO {

	@Schema(description = "Schema名")
	private String schemaName;

	@Schema(description = "对象名")
	private String objectName;

	@Schema(description = "对象类型: TABLE / VIEW")
	private String objectType;

	@Schema(description = "列列表")
	private List<ColumnMetadata> columns;

	@Schema(description = "主键列名列表")
	private List<String> primaryKey;

	@Schema(description = "唯一键列表")
	private List<UniqueKey> uniqueKeys;

	@Schema(description = "外键列表")
	private List<ForeignKey> foreignKeys;

	@Schema(description = "元数据指纹哈希")
	private String metadataHash;

	/**
	 * 源对象摘要（表或视图名 + 类型）。
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Schema(description = "源对象摘要")
	public static class SourceObjectSummary {

		@Schema(description = "Schema名")
		private String schemaName;

		@Schema(description = "对象名")
		private String objectName;

		@Schema(description = "对象类型: TABLE / VIEW")
		private String objectType;

	}

	/**
	 * 列元数据。
	 */
	@Data
	@Schema(description = "列元数据")
	public static class ColumnMetadata {

		@Schema(description = "列名")
		private String name;

		@Schema(description = "JDBC类型名")
		private String jdbcType;

		@Schema(description = "是否可空")
		private Boolean nullable;

		@Schema(description = "序号")
		private Integer ordinal;

		@Schema(description = "列大小")
		private Integer size;

	}

	/**
	 * 唯一键。
	 */
	@Data
	@Schema(description = "唯一键")
	public static class UniqueKey {

		@Schema(description = "唯一键名")
		private String name;

		@Schema(description = "列名列表")
		private List<String> columns;

	}

	/**
	 * 外键。
	 */
	@Data
	@Schema(description = "外键")
	public static class ForeignKey {

		@Schema(description = "外键名")
		private String name;

		@Schema(description = "外键列名列表")
		private List<String> columns;

		@Schema(description = "目标Schema")
		private String targetSchema;

		@Schema(description = "目标对象名")
		private String targetObject;

		@Schema(description = "目标列名列表")
		private List<String> targetColumns;

	}

}
