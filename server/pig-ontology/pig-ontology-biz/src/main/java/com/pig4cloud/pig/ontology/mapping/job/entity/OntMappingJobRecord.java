/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.entity;

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
 * 映射作业记录结果表（18-07 §4）。
 * <p>
 * 只为 FAILED/PENDING 和可审计变化持久化明细；UNCHANGED 默认只累加计数，不逐条落库。
 *
 * @author youming
 */
@Data
@TableName("ont_mapping_job_record")
@Schema(description = "映射作业记录")
@EqualsAndHashCode(callSuper = true)
public class OntMappingJobRecord extends Model<OntMappingJobRecord> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "记录ID")
	private Long id;

	@Schema(description = "作业ID")
	private Long jobId;

	@Schema(description = "重试原记录ID")
	private Long retryOfRecordId;

	@Schema(description = "阶段: ENTITY/RELATION/DELETE/PENDING")
	private String phase;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "记录键SHA-256哈希")
	private String sourceRecordKeyHash;

	@Schema(description = "脱敏后的记录键摘要")
	private String sourceRecordKeyMasked;

	@Schema(description = "记录动作: CREATE/UPDATE/UNCHANGED/SKIP/DEACTIVATE/DELETE/RELATE/UNRELATE/PEND")
	private String recordAction;

	@Schema(description = "记录状态: SUCCESS/FAILED/SKIPPED/PENDING")
	private String recordStatus;

	@Schema(description = "实例ID")
	private Long instanceId;

	@Schema(description = "关系ID")
	private Long relationId;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "错误消息")
	private String errorMessage;

	@Schema(description = "字段级错误JSONB")
	private String fieldErrors;

	@Schema(description = "内容哈希")
	private String payloadHash;

	@Schema(description = "源记录更新时间")
	private LocalDateTime sourceUpdatedAt;

	@Schema(description = "重试次数")
	private Integer retryCount;

	@Schema(description = "处理耗时(ms)")
	private Long durationMs;

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
