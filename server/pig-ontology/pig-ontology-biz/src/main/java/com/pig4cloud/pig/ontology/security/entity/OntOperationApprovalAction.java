/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审批明细（不做逻辑删除）。
 *
 * @author youming
 */
@Data
@TableName("ont_operation_approval_action")
@Schema(description = "审批明细")
@EqualsAndHashCode(callSuper = true)
public class OntOperationApprovalAction extends Model<OntOperationApprovalAction> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "审批明细ID")
	private Long id;

	@Schema(description = "审批请求ID")
	private Long requestId;

	@Schema(description = "审批人ID")
	private Long approverId;

	@Schema(description = "审批决定：APPROVE/REJECT")
	private String decision;

	@Schema(description = "审批意见")
	private String comment;

	@Schema(description = "审批时间")
	private LocalDateTime decidedAt;

}
