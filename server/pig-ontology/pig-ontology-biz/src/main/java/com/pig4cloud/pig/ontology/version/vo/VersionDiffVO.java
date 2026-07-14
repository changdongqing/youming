/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 两版本 diff 结果 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "版本差异")
public class VersionDiffVO {

	@Schema(description = "新增资源列表")
	private List<DiffResource> added;

	@Schema(description = "删除资源列表")
	private List<DiffResource> removed;

	@Schema(description = "修改资源列表")
	private List<DiffModified> modified;

	@Schema(description = "兼容性")
	private String compatibility;

	@Schema(description = "BREAKING原因列表")
	private List<String> breakingReasons;

	@Data
	@Schema(description = "差异资源项")
	public static class DiffResource {

		@Schema(description = "资源类型")
		private String resourceType;

		@Schema(description = "IRI")
		private String iri;

	}

	@Data
	@Schema(description = "修改资源项")
	public static class DiffModified {

		@Schema(description = "资源类型")
		private String resourceType;

		@Schema(description = "IRI")
		private String iri;

		@Schema(description = "字段变更列表")
		private List<FieldChange> changes;

	}

	@Data
	@Schema(description = "字段变更")
	public static class FieldChange {

		@Schema(description = "字段名")
		private String field;

		@Schema(description = "旧值")
		private String oldValue;

		@Schema(description = "新值")
		private String newValue;

	}

}
