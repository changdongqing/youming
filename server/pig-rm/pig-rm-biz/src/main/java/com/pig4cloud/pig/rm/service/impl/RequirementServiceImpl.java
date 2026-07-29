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

package com.pig4cloud.pig.rm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.rm.api.dto.RequirementApproveDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementDesignDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementQueryDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementScheduleDTO;
import com.pig4cloud.pig.rm.api.entity.ApprovalRecord;
import com.pig4cloud.pig.rm.api.entity.FlowNode;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.api.vo.DevTaskSummaryVO;
import com.pig4cloud.pig.rm.api.vo.RequirementDetailVO;
import com.pig4cloud.pig.rm.api.vo.RequirementStatisticsVO;
import com.pig4cloud.pig.rm.flow.FlowEngine;
import com.pig4cloud.pig.rm.mapper.ApprovalRecordMapper;
import com.pig4cloud.pig.rm.mapper.RequirementMapper;
import com.pig4cloud.pig.rm.mapper.SysUserMapper;
import com.pig4cloud.pig.rm.service.CodeGeneratorService;
import com.pig4cloud.pig.rm.service.NotifyService;
import com.pig4cloud.pig.rm.service.RequirementService;
import com.pig4cloud.pig.rm.service.TodoService;
import com.pig4cloud.pig.rm.statemachine.RequirementEventEnum;
import com.pig4cloud.pig.rm.statemachine.RequirementStateMachine;
import com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 软件需求申请单服务实现
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class RequirementServiceImpl extends ServiceImpl<RequirementMapper, Requirement>
		implements RequirementService {

	private final CodeGeneratorService codeGenerator;

	private final RequirementStateMachine stateMachine;

	private final FlowEngine flowEngine;

	private final ApprovalRecordMapper approvalRecordMapper;

	private final SysUserMapper sysUserMapper;

	private final TodoService todoService;

	private final NotifyService notifyService;

	// ==================== CRUD ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveRequirement(Requirement req) {
		PigUser user = SecurityUtils.getUser();
		req.setReqCode(codeGenerator.next("REQ"));
		req.setStatus(RequirementStatusEnum.DRAFT.name());
		req.setInitiatorId(user.getId());
		req.setInitiatorDeptId(user.getDeptId());
		if (StrUtil.isBlank(req.getNeedReview())) {
			req.setNeedReview("0");
		}
		if (StrUtil.isBlank(req.getPriority())) {
			req.setPriority("MEDIUM");
		}
		save(req);
		return R.ok(req);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateRequirement(Requirement req) {
		Requirement existing = getById(req.getId());
		if (existing == null) {
			return R.failed("需求不存在");
		}
		String status = existing.getStatus();
		if (!RequirementStatusEnum.DRAFT.name().equals(status)
				&& !RequirementStatusEnum.REJECTED.name().equals(status)) {
			return R.failed("当前状态不可编辑");
		}
		updateById(req);
		return R.ok();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeRequirement(Long id) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		if (!RequirementStatusEnum.DRAFT.name().equals(req.getStatus())) {
			return R.failed("仅草稿状态可删除");
		}
		removeById(id);
		return R.ok();
	}

	@Override
	public IPage<Requirement> page(Page page, RequirementQueryDTO query) {
		return baseMapper.selectPage(page,
				Wrappers.<Requirement>lambdaQuery()
					.eq(StrUtil.isNotBlank(query.getSource()), Requirement::getSource, query.getSource())
					.eq(StrUtil.isNotBlank(query.getStatus()), Requirement::getStatus, query.getStatus())
					.eq(query.getInitiatorId() != null, Requirement::getInitiatorId, query.getInitiatorId())
					.ge(query.getStartDate() != null, Requirement::getCreateTime, query.getStartDate())
					.le(query.getEndDate() != null, Requirement::getCreateTime, query.getEndDate())
					.like(StrUtil.isNotBlank(query.getKeyword()), Requirement::getTitle, query.getKeyword())
					.orderByDesc(Requirement::getCreateTime));
	}

	@Override
	public R detail(Long id) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		RequirementDetailVO vo = new RequirementDetailVO();
		vo.setRequirement(req);

		// 审批记录
		List<ApprovalRecord> records = approvalRecordMapper.selectList(Wrappers.<ApprovalRecord>lambdaQuery()
			.eq(ApprovalRecord::getBillType, "REQUIREMENT")
			.eq(ApprovalRecord::getBillId, id)
			.orderByAsc(ApprovalRecord::getApproveTime));
		vo.setApprovalRecords(records);

		// 发起人/部门名称
		if (req.getInitiatorId() != null) {
			vo.setInitiatorName(sysUserMapper.getUserName(req.getInitiatorId()));
		}
		if (req.getInitiatorDeptId() != null) {
			vo.setInitiatorDeptName(sysUserMapper.getDeptName(req.getInitiatorDeptId()));
		}
		// devTasks 在阶段 1.4 补充（当前为空列表）
		vo.setDevTasks(new ArrayList<>());
		return R.ok(vo);
	}

	// ==================== 流程操作 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R submit(Long id) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		// 状态流转：DRAFT → PENDING_APPROVAL
		RequirementStatusEnum next = stateMachine.transit(RequirementStatusEnum.valueOf(req.getStatus()),
				RequirementEventEnum.SUBMIT);
		req.setStatus(next.name());
		updateById(req);

		// 启动审批流：根据来源确定起始节点
		FlowNode startNode = flowEngine.getStartNode("REQUIREMENT_FLOW", req.getSource());
		if (startNode != null) {
			createTodoForNode(startNode, "REQUIREMENT", req.getId(),
					"待审批：" + req.getReqCode() + " " + req.getTitle(), req.getInitiatorId(), req.getInitiatorDeptId());
		}
		return R.ok("提交成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R approve(RequirementApproveDTO dto) {
		Requirement req = getById(dto.getBillId());
		if (req == null) {
			return R.failed("需求不存在");
		}
		if (!RequirementStatusEnum.PENDING_APPROVAL.name().equals(req.getStatus())) {
			return R.failed("当前状态不可审批");
		}

		// 记录审批记录（只增不删）
		saveApprovalRecord(req.getId(), dto, dto.getNodeCode());
		// 关闭当前节点的审批待办
		todoService.closeTodo("REQUIREMENT", req.getId(), "APPROVE");

		RequirementStatusEnum currentStatus = RequirementStatusEnum.valueOf(req.getStatus());

		if ("PASS".equals(dto.getConclusion())) {
			// 审批通过
			stateMachine.transit(currentStatus, RequirementEventEnum.APPROVE_PASS);

			// 判断是否需要讨论会评审
			if ("1".equals(req.getNeedReview())) {
				// 需要讨论会 → PENDING_REVIEW
				req.setStatus(RequirementStatusEnum.PENDING_REVIEW.name());
			}
			else {
				// 不需要讨论会 → 直接进入设计
				req.setStatus(RequirementStatusEnum.DESIGNING.name());
				// 生成设计待办给产品经理
				FlowNode designNode = flowEngine.getNode("REQUIREMENT_FLOW", "DESIGN");
				if (designNode != null) {
					createTodoForNode(designNode, "REQUIREMENT", req.getId(),
							"待设计：" + req.getReqCode(), req.getInitiatorId(), req.getInitiatorDeptId());
				}
			}
			updateById(req);
		}
		else {
			// 审批驳回（必填原因）
			if (StrUtil.isBlank(dto.getOpinion())) {
				return R.failed("驳回必须填写原因");
			}
			stateMachine.transit(currentStatus, RequirementEventEnum.APPROVE_REJECT);
			req.setStatus(RequirementStatusEnum.REJECTED.name());
			updateById(req);
			// 通知发起人被驳回
			notifyService.notifyUser(req.getInitiatorId(), "REJECT",
					"需求被驳回：" + req.getReqCode(), dto.getOpinion());
		}
		return R.ok("审批完成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R review(RequirementApproveDTO dto) {
		Requirement req = getById(dto.getBillId());
		if (req == null) {
			return R.failed("需求不存在");
		}
		if (!RequirementStatusEnum.PENDING_REVIEW.name().equals(req.getStatus())) {
			return R.failed("当前状态不可评审");
		}

		saveApprovalRecord(req.getId(), dto, "REVIEW_GATE");
		todoService.closeTodo("REQUIREMENT", req.getId(), "REVIEW");

		RequirementStatusEnum currentStatus = RequirementStatusEnum.valueOf(req.getStatus());

		if ("PASS".equals(dto.getConclusion())) {
			stateMachine.transit(currentStatus, RequirementEventEnum.REVIEW_PASS);
			req.setStatus(RequirementStatusEnum.DESIGNING.name());
			req.setReviewConclusion("PASS");
			req.setReviewRemark(dto.getOpinion());
			updateById(req);
			// 生成设计待办
			FlowNode designNode = flowEngine.getNode("REQUIREMENT_FLOW", "DESIGN");
			if (designNode != null) {
				createTodoForNode(designNode, "REQUIREMENT", req.getId(),
						"待设计：" + req.getReqCode(), req.getInitiatorId(), req.getInitiatorDeptId());
			}
		}
		else {
			stateMachine.transit(currentStatus, RequirementEventEnum.REVIEW_FAIL);
			req.setStatus(RequirementStatusEnum.REJECTED.name());
			req.setReviewConclusion("FAIL");
			req.setReviewRemark(dto.getOpinion());
			updateById(req);
			notifyService.notifyUser(req.getInitiatorId(), "REJECT",
					"讨论会评审未通过：" + req.getReqCode(), dto.getOpinion());
		}
		return R.ok("评审完成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveDesign(RequirementDesignDTO dto) {
		Requirement req = getById(dto.getRequirementId());
		if (req == null) {
			return R.failed("需求不存在");
		}
		if (!RequirementStatusEnum.DESIGNING.name().equals(req.getStatus())) {
			return R.failed("当前状态不可保存设计");
		}
		req.setDesignContent(dto.getDesignContent());
		req.setDesignWorkload(dto.getDesignWorkload());
		req.setDesignPlanDate(dto.getDesignPlanDate());
		updateById(req);
		return R.ok("设计已保存");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R submitDesignReview(Long id) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		stateMachine.transit(RequirementStatusEnum.valueOf(req.getStatus()),
				RequirementEventEnum.DESIGN_SUBMIT);
		// 记录实际设计完成时间（考核点3）
		req.setDesignActualDate(LocalDate.now());
		req.setStatus(RequirementStatusEnum.DESIGN_REVIEW.name());
		updateById(req);

		// 生成设计评审待办给领导
		FlowNode reviewNode = flowEngine.getNode("REQUIREMENT_FLOW", "DESIGN_REVIEW");
		if (reviewNode != null) {
			createTodoForNode(reviewNode, "REQUIREMENT", id,
					"待设计评审：" + req.getReqCode(), req.getInitiatorId(), req.getInitiatorDeptId());
		}
		return R.ok("已提交设计评审");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R designReview(RequirementApproveDTO dto) {
		Requirement req = getById(dto.getBillId());
		if (req == null) {
			return R.failed("需求不存在");
		}
		RequirementStatusEnum current = RequirementStatusEnum.valueOf(req.getStatus());

		saveApprovalRecord(req.getId(), dto, "DESIGN_REVIEW");
		todoService.closeTodo("REQUIREMENT", req.getId(), "REVIEW");

		RequirementEventEnum event = "PASS".equals(dto.getConclusion()) ? RequirementEventEnum.DESIGN_REVIEW_PASS
				: RequirementEventEnum.DESIGN_REVIEW_FAIL;
		RequirementStatusEnum next = stateMachine.transit(current, event);
		req.setStatus(next.name());
		updateById(req);

		if (next == RequirementStatusEnum.SCHEDULING) {
			// 评审通过 → 生成排期待办给开发经理
			FlowNode scheduleNode = flowEngine.getNode("REQUIREMENT_FLOW", "SCHEDULE");
			if (scheduleNode != null) {
				createTodoForNode(scheduleNode, "REQUIREMENT", req.getId(),
						"待排期：" + req.getReqCode(), req.getInitiatorId(), req.getInitiatorDeptId());
			}
		}
		else if (next == RequirementStatusEnum.DESIGNING) {
			// 评审不通过 → 退回重新设计，通知发起人
			notifyService.notifyUser(req.getInitiatorId(), "REJECT",
					"设计评审未通过：" + req.getReqCode(), dto.getOpinion());
		}
		return R.ok("评审完成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R schedule(RequirementScheduleDTO dto) {
		Requirement req = getById(dto.getRequirementId());
		if (req == null) {
			return R.failed("需求不存在");
		}
		stateMachine.transit(RequirementStatusEnum.valueOf(req.getStatus()),
				RequirementEventEnum.SCHEDULE_CONFIRM);
		req.setScheduleRemark(dto.getScheduleRemark());
		req.setScheduleRisk(StrUtil.isBlank(dto.getScheduleRisk()) ? "NORMAL" : dto.getScheduleRisk());
		req.setStatus(RequirementStatusEnum.DEVELOPING.name());
		updateById(req);
		todoService.closeTodo("REQUIREMENT", req.getId(), "DEV");

		// 通知发起人排期已完成
		notifyService.notifyUser(req.getInitiatorId(), "APPROVAL",
				"需求已排期：" + req.getReqCode(), req.getScheduleRemark());
		return R.ok("排期完成");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R qualityConfirm(Long id) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		if (!RequirementStatusEnum.TESTING.name().equals(req.getStatus())) {
			return R.failed("当前状态不可进行质量确认");
		}
		// 质量确认 + 测试通过 → 自动流转到待验收
		stateMachine.transit(RequirementStatusEnum.TESTING, RequirementEventEnum.TEST_PASS);
		req.setStatus(RequirementStatusEnum.PENDING_ACCEPTANCE.name());
		req.setCompleteTime(LocalDateTime.now());
		updateById(req);

		// 通知发起人验收
		notifyService.notifyUser(req.getInitiatorId(), "APPROVAL",
				"需求已完成，待验收：" + req.getReqCode(), "请及时验收");
		return R.ok("质量确认完成，需求状态已更新为待验收");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R accept(Long id, String conclusion, String remark) {
		Requirement req = getById(id);
		if (req == null) {
			return R.failed("需求不存在");
		}
		stateMachine.transit(RequirementStatusEnum.valueOf(req.getStatus()), RequirementEventEnum.ACCEPT);
		req.setStatus(RequirementStatusEnum.COMPLETED.name());
		req.setAcceptConclusion(conclusion);
		req.setAcceptRemark(remark);
		updateById(req);
		todoService.closeTodo("REQUIREMENT", req.getId(), "ACCEPT");
		return R.ok("验收完成");
	}

	// ==================== 统计 ====================

	@Override
	public R statistics() {
		RequirementStatisticsVO vo = new RequirementStatisticsVO();
		vo.setTotalCount(count());

		// 按来源统计
		List<Requirement> all = list();
		Map<String, Long> bySource = all.stream()
			.filter(r -> r.getSource() != null)
			.collect(Collectors.groupingBy(Requirement::getSource, Collectors.counting()));
		vo.setBySource(bySource.entrySet().stream()
			.map(e -> {
				Map<String, Object> m = new java.util.HashMap<>();
				m.put("key", e.getKey());
				m.put("count", e.getValue());
				return m;
			})
			.collect(Collectors.toList()));

		// 按状态统计
		Map<String, Long> byStatus = all.stream()
			.filter(r -> r.getStatus() != null)
			.collect(Collectors.groupingBy(Requirement::getStatus, Collectors.counting()));
		vo.setByStatus(byStatus.entrySet().stream()
			.map(e -> {
				Map<String, Object> m = new java.util.HashMap<>();
				m.put("key", e.getKey());
				m.put("count", e.getValue());
				return m;
			})
			.collect(Collectors.toList()));

		// 按月统计
		Map<String, Long> byMonth = all.stream()
			.filter(r -> r.getCreateTime() != null)
			.collect(Collectors.groupingBy(
					r -> r.getCreateTime().getYear() + "-" + String.format("%02d", r.getCreateTime().getMonthValue()),
					Collectors.counting()));
		vo.setByMonth(byMonth.entrySet().stream()
			.map(e -> {
				Map<String, Object> m = new java.util.HashMap<>();
				m.put("key", e.getKey());
				m.put("count", e.getValue());
				return m;
			})
			.collect(Collectors.toList()));

		// 设计工作量合计
		BigDecimal totalWorkload = all.stream()
			.map(Requirement::getDesignWorkload)
			.filter(w -> w != null)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		vo.setTotalDesignWorkload(totalWorkload);
		return R.ok(vo);
	}

	// ==================== 私有方法 ====================

	/**
	 * 保存审批记录（只增不删）
	 */
	private void saveApprovalRecord(Long billId, RequirementApproveDTO dto, String nodeCode) {
		ApprovalRecord record = new ApprovalRecord();
		record.setBillType("REQUIREMENT");
		record.setBillId(billId);
		record.setNodeCode(nodeCode);
		record.setApproverId(SecurityUtils.getUser().getId());
		record.setConclusion(dto.getConclusion());
		record.setOpinion(dto.getOpinion());
		record.setApproveTime(LocalDateTime.now());
		approvalRecordMapper.insert(record);
	}

	/**
	 * 为流程节点创建待办（解析审批人）
	 */
	private void createTodoForNode(FlowNode node, String billType, Long billId, String title, Long initiatorId,
			Long initiatorDeptId) {
		List<Long> approverIds = resolveApproverIds(node, initiatorId, initiatorDeptId);
		if (approverIds.isEmpty()) {
			log.warn("节点 {} 未解析到审批人，单据 {}/{}", node.getNodeCode(), billType, billId);
			return;
		}
		String url = buildUrl(billType, billId);
		todoService.createTodoForUsers(approverIds, node.getNodeCode(), billType, billId, title, url);
	}

	/**
	 * 解析审批人ID列表
	 */
	private List<Long> resolveApproverIds(FlowNode node, Long initiatorId, Long initiatorDeptId) {
		if (node == null) {
			return List.of();
		}
		String type = node.getApproverType();
		String ref = node.getApproverRef();
		return switch (type) {
			case "ROLE" -> (ref != null && !ref.isBlank()) ? sysUserMapper.getUserIdsByRoleCode(ref) : List.of();
			case "USER" -> (ref != null && !ref.isBlank()) ? List.of(Long.parseLong(ref)) : List.of();
			case "DEPT_LEADER", "INITIATOR_LEADER" -> {
				Long deptId = "INITIATOR_LEADER".equals(type) ? initiatorDeptId : Long.parseLong(ref);
				Long leaderId = flowEngine.getDeptLeaderId(deptId);
				yield leaderId != null ? List.of(leaderId) : List.of();
			}
			default -> List.of();
		};
	}

	/**
	 * 构建跳转 URL
	 */
	private String buildUrl(String billType, Long billId) {
		return switch (billType) {
			case "REQUIREMENT" -> "/admin/rm/requirement/detail?id=" + billId;
			case "DEV_TASK" -> "/admin/rm/dev-task/detail?id=" + billId;
			case "TEST_TASK" -> "/admin/rm/test/task/detail?id=" + billId;
			default -> null;
		};
	}

}
