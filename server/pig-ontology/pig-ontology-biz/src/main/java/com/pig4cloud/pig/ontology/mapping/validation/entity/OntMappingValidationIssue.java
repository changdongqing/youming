/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.entity;

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
 * 映射校验问题表（18-06 §5）。
 * <p>
 * 记录校验过程中发现的各类问题，包括严重级别、范围、建议和样本上下文。
 * sample_context 只允许脱敏、截断后的字段：列名、类型、值长度、值哈希、是否为空、转换结果类型。
 *
 * @author youming
 */
@Data
@TableName("ont_mapping_validation_issue")
@Schema(description = "映射校验问题")
@EqualsAndHashCode(callSuper = true)
public class OntMappingValidationIssue extends Model<OntMappingValidationIssue> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "问题ID")
	private Long id;

	@Schema(description = "报告ID")
	private Long reportId;

	@Schema(description = "严重级别: VIOLATION / WARNING / INFO")
	private String severity;

	@Schema(description = "问题编码")
	private String issueCode;

	@Schema(description = "范围类型: PROJECT / VERSION / SOURCE / ENTITY / FIELD / RELATION / RECORD")
	private String scopeType;

	@Schema(description = "范围引用")
	private String scopeRef;

	@Schema(description = "问题描述")
	private String message;

	@Schema(description = "修复建议")
	private String suggestion;

	@Schema(description = "源记录键哈希")
	private String sourceRecordKeyHash;

	@Schema(description = "样本上下文JSON")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String sampleContext;

	@Schema(description = "是否已确认: 0否 1是")
	private String acknowledged;

	@Schema(description = "确认人")
	private String acknowledgedBy;

	@Schema(description = "确认时间")
	private LocalDateTime acknowledgedAt;

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
