/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 扩展变更影响摘要。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展变更影响摘要")
public class ExtensionImpactSummaryVO {

	@Schema(description = "扩展模块ID")
	private Long moduleId;

	@Schema(description = "模块代码")
	private String moduleCode;

	@Schema(description = "资源摘要")
	private ResourceSummaryVO resourceSummary;

	@Schema(description = "实例影响")
	private InstanceImpactVO instanceImpact;

	@Schema(description = "导出影响")
	private ExportImpactVO exportImpact;

	/**
	 * 资源数量摘要。
	 */
	@Data
	@Schema(description = "资源数量摘要")
	public static class ResourceSummaryVO {

		@Schema(description = "实体类型数")
		private int entityType;

		@Schema(description = "数据属性数")
		private int dataProperty;

		@Schema(description = "对象属性数")
		private int objectProperty;

		@Schema(description = "公理规则数")
		private int axiomRule;

		@Schema(description = "单位条目数")
		private int unit;

	}

	/**
	 * 实例影响摘要。
	 */
	@Data
	@Schema(description = "实例影响摘要")
	public static class InstanceImpactVO {

		@Schema(description = "关联实例总数")
		private int totalInstances;

		@Schema(description = "按实体类型分布的实例数")
		private java.util.List<InstanceCountVO> instancesByType;

	}

	/**
	 * 实例按类型计数。
	 */
	@Data
	@Schema(description = "实例按类型计数")
	public static class InstanceCountVO {

		@Schema(description = "实体类型IRI")
		private String entityTypeIri;

		@Schema(description = "实例数")
		private int count;

	}

	/**
	 * 导出影响摘要。
	 */
	@Data
	@Schema(description = "导出影响摘要")
	public static class ExportImpactVO {

		@Schema(description = "活跃导出任务数")
		private int activeExportTasks;

		@Schema(description = "最近导出日志ID")
		private Long lastExportLogId;

	}

}
