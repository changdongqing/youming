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
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;

/**
 * 校验报告主表。
 *
 * @author youming
 */
@Data
@TableName("ont_validation_report")
@Schema(description = "校验报告")
@EqualsAndHashCode(callSuper = true)
public class OntValidationReport extends Model<OntValidationReport> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "报告ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "是否全部通过（0 VIOLATION）")
	private Boolean conforms;

	@Schema(description = "总结果数")
	private Integer totalCount;

	@Schema(description = "违规数")
	private Integer violationCount;

	@Schema(description = "警告数")
	private Integer warningCount;

	@Schema(description = "信息数")
	private Integer infoCount;

	@Schema(description = "执行规则数")
	private Integer ruleCount;

	@Schema(description = "校验实例数")
	private Integer instanceCount;

	@Schema(description = "校验耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "校验范围：FULL/INSTANCE")
	private String scope;

	@Schema(description = "增量校验目标实例ID")
	private Long targetInstanceId;

	@Schema(description = "状态：RUNNING/COMPLETED/FAILED")
	private String status;

	@Schema(description = "触发人")
	private String triggeredBy;

	@Schema(description = "触发时间")
	private LocalDateTime triggeredAt;

	@Schema(description = "完成时间")
	private LocalDateTime completedAt;

	@Schema(description = "FAILED时的错误信息")
	private String errorMessage;

	@Schema(description = "完整JSON快照")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String resultJson;

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
