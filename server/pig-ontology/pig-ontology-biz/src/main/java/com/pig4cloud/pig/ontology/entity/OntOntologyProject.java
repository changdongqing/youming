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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 最小本体工程作用域。
 *
 * @author youming
 */
@Data
@TableName("ont_ontology_project")
public class OntOntologyProject {

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "工程ID")
	private Long id;

	@Schema(description = "本体工程编码")
	private String projectCode;

	@Schema(description = "本体工程名称")
	private String projectName;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "本体标识IRI")
	private String ontologyIri;

	@Schema(description = "版本IRI基础路径")
	private String versionIriBase;

	@Schema(description = "当前已发布版本ID")
	private Long currentVersionId;

	@Schema(description = "工作区状态：EDITABLE/PREPARED/MIGRATING")
	private String workspaceStatus;

	@Schema(description = "工作区修订号，用于乐观锁校验")
	private Long workspaceRevision;

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
