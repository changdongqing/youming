/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 扩展模块详情。
 *
 * @author youming
 */
@Data
@Schema(description = "扩展模块详情")
public class ExtensionModuleDetailVO {

	@Schema(description = "扩展模块ID")
	private Long id;

	@Schema(description = "模块代码")
	private String moduleCode;

	@Schema(description = "模块名称")
	private String moduleName;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "命名空间前缀")
	private String namespacePrefix;

	@Schema(description = "命名空间URI")
	private String namespaceUri;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "模块描述")
	private String description;

	@Schema(description = "模块版本号")
	private String version;

	@Schema(description = "是否内置模块")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remarks;

	@Schema(description = "关联资源数")
	private Long resourceCount;

	@Schema(description = "创建人")
	private String createBy;

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

}
