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
 * 校验结果明细。
 *
 * @author youming
 */
@Data
@TableName("ont_validation_result")
@Schema(description = "校验结果明细")
@EqualsAndHashCode(callSuper = true)
public class OntValidationResult extends Model<OntValidationResult> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "结果ID")
	private Long id;

	@Schema(description = "校验报告ID")
	private Long reportId;

	@Schema(description = "严重程度：VIOLATION/WARNING/INFO")
	private String severity;

	@Schema(description = "违规实例IRI")
	private String focusNode;

	@Schema(description = "违规属性IRI")
	private String resultPath;

	@Schema(description = "违反规则名称")
	private String ruleName;

	@Schema(description = "违反规则编码")
	private String ruleCode;

	@Schema(description = "说明")
	private String message;

	@Schema(description = "期望值")
	private String expectedValue;

	@Schema(description = "实际值")
	private String actualValue;

	@Schema(description = "修复建议")
	private String suggestion;

	@Schema(description = "排序")
	private Integer sortOrder;

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
