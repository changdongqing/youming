/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.ontology.security.entity.OntDataAccessLog;
import com.pig4cloud.pig.ontology.security.entity.OntDataPolicyRule;
import com.pig4cloud.pig.ontology.security.entity.OntOntologyProjectAcl;
import com.pig4cloud.pig.ontology.security.entity.OntOperationApprovalRequest;
import com.pig4cloud.pig.ontology.security.entity.OntSecurityLevel;
import com.pig4cloud.pig.ontology.security.entity.OntSecuritySubjectClearance;
import com.pig4cloud.pig.ontology.security.entity.OntDeviceCredential;
import com.pig4cloud.pig.ontology.security.mapper.OntDataAccessLogMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntDataPolicyRuleMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntOntologyProjectAclMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntSecurityLevelMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntSecuritySubjectClearanceMapper;
import com.pig4cloud.pig.ontology.security.approval.OperationApprovalService;
import com.pig4cloud.pig.ontology.security.device.DeviceCredentialService;
import com.pig4cloud.pig.ontology.security.audit.AuditChainService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 安全与合规管理 API。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/security")
@Tag(name = "安全与合规管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntSecurityController {

	private final OntSecurityLevelMapper securityLevelMapper;

	private final OntOntologyProjectAclMapper projectAclMapper;

	private final OntSecuritySubjectClearanceMapper clearanceMapper;

	private final OntDataPolicyRuleMapper policyRuleMapper;

	private final OntDataAccessLogMapper auditLogMapper;

	private final DeviceCredentialService deviceCredentialService;

	private final OperationApprovalService approvalService;

	private final AuditChainService auditChainService;

	// ==================== 安全级别 ====================

	@GetMapping("/levels")
	@HasPermission("ontology_security_view")
	public R<List<OntSecurityLevel>> levels() {
		return R.ok(securityLevelMapper.selectList(null));
	}

	// ==================== 工程 ACL ====================

	@GetMapping("/project-acl/page")
	@HasPermission("ontology_security_view")
	public R<Page<OntOntologyProjectAcl>> projectAclPage(Page<OntOntologyProjectAcl> page,
			@RequestParam(required = false) Long ontologyId) {
		return R.ok(projectAclMapper.selectPage(page,
			com.baomidou.mybatisplus.core.toolkit.Wrappers.<OntOntologyProjectAcl>lambdaQuery()
				.eq(ontologyId != null, OntOntologyProjectAcl::getOntologyId, ontologyId)
				.eq(OntOntologyProjectAcl::getDelFlag, "0")
				.orderByDesc(OntOntologyProjectAcl::getCreateTime)));
	}

	@PostMapping("/project-acl")
	@SysLog("新增工程ACL")
	@HasPermission("ontology_security_admin")
	public R<Boolean> saveProjectAcl(@RequestBody OntOntologyProjectAcl acl) {
		return R.ok(projectAclMapper.insert(acl) > 0);
	}

	@PutMapping("/project-acl")
	@SysLog("修改工程ACL")
	@HasPermission("ontology_security_admin")
	public R<Boolean> updateProjectAcl(@RequestBody OntOntologyProjectAcl acl) {
		return R.ok(projectAclMapper.updateById(acl) > 0);
	}

	@DeleteMapping("/project-acl/{id}")
	@SysLog("删除工程ACL")
	@HasPermission("ontology_security_admin")
	public R<Boolean> deleteProjectAcl(@PathVariable Long id) {
		return R.ok(projectAclMapper.deleteById(id) > 0);
	}

	// ==================== 主体安全许可 ====================

	@GetMapping("/clearances/page")
	@HasPermission("ontology_security_view")
	public R<Page<OntSecuritySubjectClearance>> clearancesPage(Page<OntSecuritySubjectClearance> page,
			@RequestParam(required = false) Long ontologyId) {
		return R.ok(clearanceMapper.selectPage(page,
			com.baomidou.mybatisplus.core.toolkit.Wrappers.<OntSecuritySubjectClearance>lambdaQuery()
				.eq(ontologyId != null, OntSecuritySubjectClearance::getOntologyId, ontologyId)
				.eq(OntSecuritySubjectClearance::getDelFlag, "0")
				.orderByDesc(OntSecuritySubjectClearance::getCreateTime)));
	}

	@PostMapping("/clearances")
	@SysLog("配置主体安全许可")
	@HasPermission("ontology_security_admin")
	public R<Boolean> saveClearance(@RequestBody OntSecuritySubjectClearance clearance) {
		return R.ok(clearanceMapper.insert(clearance) > 0);
	}

	// ==================== 数据策略规则 ====================

	@GetMapping("/policies/page")
	@HasPermission("ontology_security_view")
	public R<Page<OntDataPolicyRule>> policiesPage(Page<OntDataPolicyRule> page,
			@RequestParam(required = false) Long ontologyId) {
		return R.ok(policyRuleMapper.selectPage(page,
			com.baomidou.mybatisplus.core.toolkit.Wrappers.<OntDataPolicyRule>lambdaQuery()
				.eq(ontologyId != null, OntDataPolicyRule::getOntologyId, ontologyId)
				.eq(OntDataPolicyRule::getDelFlag, "0")
				.orderByDesc(OntDataPolicyRule::getCreateTime)));
	}

	@PostMapping("/policies")
	@SysLog("新增数据策略规则")
	@HasPermission("ontology_security_admin")
	public R<Boolean> savePolicy(@RequestBody OntDataPolicyRule rule) {
		return R.ok(policyRuleMapper.insert(rule) > 0);
	}

	@PutMapping("/policies")
	@SysLog("修改数据策略规则")
	@HasPermission("ontology_security_admin")
	public R<Boolean> updatePolicy(@RequestBody OntDataPolicyRule rule) {
		return R.ok(policyRuleMapper.updateById(rule) > 0);
	}

	// ==================== 审计日志 ====================

	@GetMapping("/audit/page")
	@HasPermission("ontology_audit_view")
	public R<Page<OntDataAccessLog>> auditPage(Page<OntDataAccessLog> page,
			@RequestParam(required = false) Long ontologyId,
			@RequestParam(required = false) Long userId) {
		return R.ok(auditLogMapper.selectPage(page,
			com.baomidou.mybatisplus.core.toolkit.Wrappers.<OntDataAccessLog>lambdaQuery()
				.eq(ontologyId != null, OntDataAccessLog::getOntologyId, ontologyId)
				.eq(userId != null, OntDataAccessLog::getUserId, userId)
				.orderByDesc(OntDataAccessLog::getOccurredAt)));
	}

	@PostMapping("/audit/verify")
	@HasPermission("ontology_audit_verify")
	public R<String> verifyChain(@RequestParam String chainScope) {
		String result = auditChainService.verifyChain(chainScope);
		return R.ok(result != null ? result : "Chain integrity verified");
	}

	// ==================== 设备凭证 ====================

	@GetMapping("/device-credentials/page")
	@HasPermission("ontology_device_credential_manage")
	public R<Page<OntDeviceCredential>> deviceCredentialsPage(Page<OntDeviceCredential> page,
			@RequestParam(required = false) String deviceCode,
			@RequestParam(required = false) String status) {
		return R.ok(deviceCredentialService.page(page, deviceCode, status));
	}

	@PostMapping("/device-credentials")
	@SysLog("创建设备凭证")
	@HasPermission("ontology_device_credential_manage")
	public R<Map<String, Object>> createDeviceCredential(@RequestParam String deviceCode,
			@RequestParam(required = false) java.time.LocalDateTime expiresAt) {
		// Token 只返回一次
		return R.ok(deviceCredentialService.createTokenCredential(deviceCode, expiresAt));
	}

	@PostMapping("/device-credentials/{id}/rotate")
	@SysLog("轮换设备Token")
	@HasPermission("ontology_device_credential_manage")
	public R<Map<String, Object>> rotateDeviceToken(@PathVariable Long id) {
		return R.ok(deviceCredentialService.rotateToken(id));
	}

	@PostMapping("/device-credentials/{id}/revoke")
	@SysLog("吊销设备凭证")
	@HasPermission("ontology_device_credential_manage")
	public R<Boolean> revokeDeviceCredential(@PathVariable Long id) {
		deviceCredentialService.revoke(id);
		return R.ok(true);
	}

	// ==================== 高风险审批 ====================

	@PostMapping("/approvals")
	public R<OntOperationApprovalRequest> createApproval(@RequestParam String operationType,
			@RequestParam String targetRef, @RequestParam String payload,
			@RequestParam(defaultValue = "1") int requiredApprovals) {
		PigUser user = SecurityUtils.getUser();
		return R.ok(approvalService.createRequest(operationType, targetRef, payload,
			user.getId(), requiredApprovals));
	}

	@PostMapping("/approvals/{requestNo}/actions")
	@HasPermission("ontology_security_approve")
	@SysLog("审批/拒绝高风险操作")
	public R<OntOperationApprovalRequest> approveAction(@PathVariable String requestNo,
			@RequestParam String decision, @RequestParam(required = false) String comment) {
		PigUser user = SecurityUtils.getUser();
		return R.ok(approvalService.approve(requestNo, user.getId(), decision, comment));
	}

	@GetMapping("/approvals/{requestNo}")
	public R<OntOperationApprovalRequest> getApproval(@PathVariable String requestNo) {
		return R.ok(approvalService.findByNo(requestNo));
	}

}
