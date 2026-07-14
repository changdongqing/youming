/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 高风险操作审批请求（不做逻辑删除）。
 *
 * @author youming
 */
@Data
@TableName("ont_operation_approval_request")
@Schema(description = "高风险操作审批请求")
@EqualsAndHashCode(callSuper = true)
public class OntOperationApprovalRequest extends Model<OntOperationApprovalRequest> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "审批请求ID")
	private Long id;

	@Schema(description = "审批编号")
	private String requestNo;

	@Schema(description = "操作类型")
	private String operationType;

	@Schema(description = "目标引用")
	private String targetRef;

	@Schema(description = "目标摘要（SHA-256）")
	private String targetDigest;

	@Schema(description = "参数摘要（SHA-256）")
	private String payloadDigest;

	@Schema(description = "发起人ID")
	private Long requestedBy;

	@Schema(description = "需要审批人数：1或2")
	private Integer requiredApprovals;

	@Schema(description = "状态：PENDING/APPROVED/REJECTED/EXPIRED/CONSUMED/CANCELLED")
	private String status;

	@Schema(description = "过期时间")
	private LocalDateTime expiresAt;

	@Schema(description = "执行时间")
	private LocalDateTime executedAt;

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

}
