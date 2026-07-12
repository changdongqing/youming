/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 扩展模块。
 *
 * @author youming
 */
@Data
@TableName("ont_extension_module")
@Schema(description = "扩展模块")
@EqualsAndHashCode(callSuper = true)
public class OntExtensionModule extends Model<OntExtensionModule> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "扩展模块ID")
	private Long id;

	@NotBlank(message = "模块代码不能为空")
	@Schema(description = "模块代码（全局唯一）")
	private String moduleCode;

	@NotBlank(message = "模块名称不能为空")
	@Schema(description = "模块名称")
	private String moduleName;

	@Schema(description = "绑定的扩展命名空间ID")
	private Long namespaceId;

	@Schema(description = "归属本体工程ID")
	private Long ontologyId;

	@Schema(description = "模块描述")
	private String description;

	@Schema(description = "模块版本号")
	private String version;

	@Schema(description = "是否内置模块（首期恒为0）")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remarks;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
