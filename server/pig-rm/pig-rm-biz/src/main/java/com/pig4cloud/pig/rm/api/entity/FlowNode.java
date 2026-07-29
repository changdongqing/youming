/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.api.entity;

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
 * 流程节点配置（审批链路可视化配置）
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "流程节点配置")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_flow_node")
public class FlowNode extends Model<FlowNode> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "流程编码 REQUIREMENT_FLOW/DEV_TASK_FLOW")
	private String flowCode;

	@Schema(description = "节点编码")
	private String nodeCode;

	@Schema(description = "节点名称")
	private String nodeName;

	@Schema(description = "审批人类型 ROLE/DEPT_LEADER/USER/INITIATOR_LEADER")
	private String approverType;

	@Schema(description = "审批人引用（角色编码/部门ID/用户ID）")
	private String approverRef;

	@Schema(description = "通过后下一节点编码")
	private String nextNodePass;

	@Schema(description = "驳回后下一节点编码")
	private String nextNodeReject;

	@Schema(description = "节点顺序")
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
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记")
	private String delFlag;

}
