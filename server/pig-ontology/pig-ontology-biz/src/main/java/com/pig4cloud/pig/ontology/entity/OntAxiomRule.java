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
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公理规则。
 *
 * @author youming
 */
@Data
@TableName("ont_axiom_rule")
@Schema(description = "公理规则")
@EqualsAndHashCode(callSuper = true)
public class OntAxiomRule extends Model<OntAxiomRule> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "公理规则ID")
	private Long id;

	@Schema(description = "规则编码")
	private String ruleCode;

	@Schema(description = "规则名称")
	private String name;

	@Schema(description = "规则大类")
	private String category;

	@Schema(description = "规则子类")
	private String subType;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "模板编码")
	private String templateCode;

	@Schema(description = "模板版本")
	private Integer templateVersion;

	@Schema(description = "形式化模式")
	private String formalizationMode;

	@Schema(description = "校验模式")
	private String validationMode;

	@Schema(description = "执行器编码")
	private String executorCode;

	@Schema(description = "配置JSON")
	private String configJson;

	@Schema(description = "OWL公理")
	private String owlAxiom;

	@Schema(description = "SHACL形状")
	private String shaclShape;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "是否启用，1是0否")
	private String isEnabled;

	@Schema(description = "严重级别")
	private String severity;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "来源引用")
	private String sourceReference;

	@Schema(description = "拦截原因")
	private String blockedReason;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "平台治理备注")
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
