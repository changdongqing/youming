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

package com.pig4cloud.pig.rm.flow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.rm.api.entity.FlowNode;
import com.pig4cloud.pig.rm.mapper.FlowNodeMapper;
import com.pig4cloud.pig.rm.mapper.RmUserQueryMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 审批流程引擎
 * <p>
 * 1. 查询 rm_flow_node 获取流程节点配置<br>
 * 2. 根据 approver_type 解析审批人<br>
 * 3. 返回当前节点配置供 Service 层驱动状态流转
 *
 * @author youming
 * @date 2026-07-29
 */
@Component
@AllArgsConstructor
public class FlowEngine {

	private final FlowNodeMapper flowNodeMapper;

	private final RmUserQueryMapper rmUserQueryMapper;

	/**
	 * 获取流程的全部节点（按顺序）
	 */
	public List<FlowNode> getFlowNodes(String flowCode) {
		return flowNodeMapper
			.selectList(Wrappers.<FlowNode>lambdaQuery().eq(FlowNode::getFlowCode, flowCode)
				.orderByAsc(FlowNode::getSortOrder));
	}

	/**
	 * 获取指定节点配置
	 */
	public FlowNode getNode(String flowCode, String nodeCode) {
		return flowNodeMapper.selectOne(Wrappers.<FlowNode>lambdaQuery()
			.eq(FlowNode::getFlowCode, flowCode)
			.eq(FlowNode::getNodeCode, nodeCode));
	}

	/**
	 * 获取流程的起始节点
	 * <p>
	 * 对需求流程：交付中心需求(CUSTOMER_DELIVERY)从 DELIVERY_APPROVAL 开始，
	 * 其他来源从 PRODUCT_APPROVAL 开始
	 */
	public FlowNode getStartNode(String flowCode, String source) {
		String startNodeCode = "CUSTOMER_DELIVERY".equals(source) ? "DELIVERY_APPROVAL" : "PRODUCT_APPROVAL";
		return getNode(flowCode, startNodeCode);
	}

	/**
	 * 根据 FlowNode 的 approver_type 解析审批人ID列表
	 * @param node 流程节点
	 * @param initiatorId 发起人ID（INITIATOR_LEADER 类型用）
	 * @return 审批人ID列表
	 */
	public List<Long> resolveApprovers(FlowNode node, Long initiatorId) {
		if (node == null) {
			return List.of();
		}
		String type = node.getApproverType();
		String ref = node.getApproverRef();

		return switch (type) {
			// 按角色编码查用户
			case "ROLE" -> ref != null && !ref.isBlank() ? rmUserQueryMapper.getUserIdsByRoleCode(ref) : List.of();
			// 指定用户
			case "USER" -> ref != null && !ref.isBlank() ? List.of(Long.parseLong(ref)) : List.of();
			// 发起人所在部门负责人
			case "INITIATOR_LEADER" -> {
				if (initiatorId == null) {
					yield List.of();
				}
				// 先查发起人所在部门，再查部门负责人
				// 简化：通过 rm_requirement 的 initiator_dept_id 查 sys_dept.leader
				// 此处由调用方传入 deptId 更合适，但为保持接口简洁，由调用方在 Service 层处理
				yield List.of();
			}
			default -> List.of();
		};
	}

	/**
	 * 按部门ID解析部门负责人
	 */
	public Long getDeptLeaderId(Long deptId) {
		if (deptId == null) {
			return null;
		}
		return rmUserQueryMapper.getDeptLeaderId(deptId);
	}

}
