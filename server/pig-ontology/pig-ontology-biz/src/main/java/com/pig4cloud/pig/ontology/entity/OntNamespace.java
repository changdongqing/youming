/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

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
 * 命名空间。
 *
 * @author youming
 */
@Data
@TableName("ont_namespace")
@Schema(description = "命名空间")
@EqualsAndHashCode(callSuper = true)
public class OntNamespace extends Model<OntNamespace> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "命名空间ID")
	private Long id;

	@NotBlank(message = "前缀不能为空")
	@Schema(description = "前缀标识")
	private String prefix;

	@NotBlank(message = "命名空间URI不能为空")
	@Schema(description = "命名空间URI")
	private String uri;

	@Schema(description = "是否默认，1是0否")
	private String isDefault;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "描述")
	private String description;

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
