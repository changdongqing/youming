/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.security.entity.OntOperationApprovalAction;
import com.pig4cloud.pig.ontology.security.entity.OntOperationApprovalRequest;
import com.pig4cloud.pig.ontology.security.mapper.OntOperationApprovalActionMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntOperationApprovalRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 高风险操作审批服务。
 * <p>
 * 适用场景：版本恢复执行、批量删除、死信强制重放、下行控制、设备凭证吊销等。
 * <p>
 * 流程：
 * <ol>
 *   <li>发起人提交操作预览 → 服务计算 targetDigest + payloadDigest → 创建审批请求</li>
 *   <li>审批人审批；双人场景发起人不得作为审批人</li>
 *   <li>达到审批数后状态 APPROVED</li>
 *   <li>执行接口提交 approvalRequestNo + 原始操作参数</li>
 *   <li>服务重新计算两个 digest 并比对</li>
 *   <li>数据库行锁下将 APPROVED 原子更新为 CONSUMED</li>
 *   <li>执行业务操作并记录审计</li>
 * </ol>
 * 审批请求只能消费一次，不能作为通用 Token，也不能通过 URL query 参数传递。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationApprovalService {

	private final OntOperationApprovalRequestMapper approvalRequestMapper;

	private final OntOperationApprovalActionMapper approvalActionMapper;

	/**
	 * 审批默认过期时间（分钟）。
	 */
	private static final int DEFAULT_EXPIRY_MINUTES = 5;

	/**
	 * 创建审批请求。
	 *
	 * @param operationType     操作类型
	 * @param targetRef         目标引用
	 * @param payload           原始参数（用于计算 digest）
	 * @param requestedBy       发起人ID
	 * @param requiredApprovals 需要审批人数（1 或 2）
	 * @return 审批请求
	 */
	@Transactional
	public OntOperationApprovalRequest createRequest(String operationType, String targetRef,
			String payload, Long requestedBy, int requiredApprovals) {
		if (requiredApprovals != 1 && requiredApprovals != 2) {
			throw new IllegalArgumentException("requiredApprovals must be 1 or 2");
		}

		String targetDigest = sha256(targetRef);
		String payloadDigest = sha256(payload);
		String requestNo = UUID.randomUUID().toString();

		OntOperationApprovalRequest request = new OntOperationApprovalRequest();
		request.setRequestNo(requestNo);
		request.setOperationType(operationType);
		request.setTargetRef(targetRef);
		request.setTargetDigest(targetDigest);
		request.setPayloadDigest(payloadDigest);
		request.setRequestedBy(requestedBy);
		request.setRequiredApprovals(requiredApprovals);
		request.setStatus("PENDING");
		request.setExpiresAt(LocalDateTime.now().plusMinutes(DEFAULT_EXPIRY_MINUTES));

		approvalRequestMapper.insert(request);
		log.info("Approval request created: no={}, type={}, requestedBy={}", requestNo, operationType, requestedBy);
		return request;
	}

	/**
	 * 审批/拒绝。
	 *
	 * @param requestNo  审批编号
	 * @param approverId 审批人ID
	 * @param decision   APPROVE / REJECT
	 * @param comment    审批意见
	 */
	@Transactional
	public OntOperationApprovalRequest approve(String requestNo, Long approverId, String decision,
			String comment) {
		OntOperationApprovalRequest request = findByNo(requestNo);
		validateForApproval(request, approverId);

		// 记录审批明细
		OntOperationApprovalAction action = new OntOperationApprovalAction();
		action.setRequestId(request.getId());
		action.setApproverId(approverId);
		action.setDecision(decision);
		action.setComment(comment);
		approvalActionMapper.insert(action);

		if ("REJECT".equals(decision)) {
			request.setStatus("REJECTED");
		}
		else if ("APPROVE".equals(decision)) {
			// 统计已通过数
			long approvedCount = approvalActionMapper.selectCount(
				Wrappers.<OntOperationApprovalAction>lambdaQuery()
					.eq(OntOperationApprovalAction::getRequestId, request.getId())
					.eq(OntOperationApprovalAction::getDecision, "APPROVE"));
			if (approvedCount >= request.getRequiredApprovals()) {
				request.setStatus("APPROVED");
			}
		}

		approvalRequestMapper.updateById(request);
		log.info("Approval action: no={}, approverId={}, decision={}", requestNo, approverId, decision);
		return request;
	}

	/**
	 * 消费审批请求（执行接口调用）。
	 * <p>
	 * 重新计算两个 digest 并比对，数据库行锁下将 APPROVED 原子更新为 CONSUMED。
	 * 只能消费一次。
	 *
	 * @param requestNo   审批编号
	 * @param targetRef   原始目标引用
	 * @param payload     原始参数
	 * @return 已消费的审批请求
	 */
	@Transactional
	public OntOperationApprovalRequest consume(String requestNo, String targetRef, String payload) {
		// 行锁查询
		OntOperationApprovalRequest request = approvalRequestMapper.selectByNoForUpdate(requestNo);

		if (request == null) {
			throw new IllegalArgumentException("Approval request not found: " + requestNo);
		}

		// 验证状态
		if (!"APPROVED".equals(request.getStatus())) {
			throw new IllegalStateException("Approval request is not APPROVED: " + request.getStatus());
		}

		// 验证过期
		if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
			request.setStatus("EXPIRED");
			approvalRequestMapper.updateById(request);
			throw new IllegalStateException("Approval request has expired");
		}

		// 重新计算 digest 并比对
		String targetDigest = sha256(targetRef);
		String payloadDigest = sha256(payload);
		if (!targetDigest.equals(request.getTargetDigest())
				|| !payloadDigest.equals(request.getPayloadDigest())) {
			throw new IllegalStateException("Approval digest mismatch: target or payload has changed");
		}

		// 原子更新为 CONSUMED
		request.setStatus("CONSUMED");
		request.setExecutedAt(LocalDateTime.now());
		approvalRequestMapper.updateById(request);

		log.info("Approval consumed: no={}", requestNo);
		return request;
	}

	/**
	 * 查看审批状态。
	 */
	public OntOperationApprovalRequest findByNo(String requestNo) {
		return approvalRequestMapper.selectOne(
			Wrappers.<OntOperationApprovalRequest>lambdaQuery()
				.eq(OntOperationApprovalRequest::getRequestNo, requestNo));
	}

	/**
	 * 验证审批请求是否可被当前用户审批。
	 */
	private void validateForApproval(OntOperationApprovalRequest request, Long approverId) {
		if (!"PENDING".equals(request.getStatus()) && !"APPROVED".equals(request.getStatus())) {
			throw new IllegalStateException("Approval request is not in PENDING/APPROVED: " + request.getStatus());
		}

		// 过期检查
		if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
			request.setStatus("EXPIRED");
			approvalRequestMapper.updateById(request);
			throw new IllegalStateException("Approval request has expired");
		}

		// 双人审批场景：发起人不得作为审批人
		if (request.getRequiredApprovals() == 2 && request.getRequestedBy().equals(approverId)) {
			throw new IllegalArgumentException("Initiator cannot approve their own request in dual-approval mode");
		}

		// 重复审批检查
		long existingCount = approvalActionMapper.selectCount(
			Wrappers.<OntOperationApprovalAction>lambdaQuery()
				.eq(OntOperationApprovalAction::getRequestId, request.getId())
				.eq(OntOperationApprovalAction::getApproverId, approverId));
		if (existingCount > 0) {
			throw new IllegalStateException("Approver has already acted on this request");
		}
	}

	private String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute SHA-256 digest", e);
		}
	}

}
