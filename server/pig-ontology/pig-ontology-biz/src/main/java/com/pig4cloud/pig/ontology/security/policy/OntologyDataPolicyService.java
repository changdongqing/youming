/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.security.entity.OntDataPolicyRule;
import com.pig4cloud.pig.ontology.security.entity.OntOntologyProjectAcl;
import com.pig4cloud.pig.ontology.security.entity.OntSecuritySubjectClearance;
import com.pig4cloud.pig.ontology.security.mapper.OntDataPolicyRuleMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntOntologyProjectAclMapper;
import com.pig4cloud.pig.ontology.security.mapper.OntSecuritySubjectClearanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一数据访问策略决策服务。
 * <p>
 * 唯一策略入口，所有数据访问渠道（实例 API、SPARQL、导出、可视化）必须通过此服务决策。
 * <p>
 * 策略优先级：
 * <ol>
 *   <li>工程 ACL 不满足 → DENY</li>
 *   <li>匹配到任何显式 DENY → DENY</li>
 *   <li>资源级 USER 规则</li>
 *   <li>资源级 ROLE/DEPT 规则</li>
 *   <li>主体 clearance 与数据 rank 比较</li>
 *   <li>等级默认策略</li>
 * </ol>
 * 多个 ALLOW/MASK 冲突时取更严格结果：DENY > MASK > ALLOW。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyDataPolicyService {

	private final OntOntologyProjectAclMapper aclMapper;

	private final OntSecuritySubjectClearanceMapper clearanceMapper;

	private final OntDataPolicyRuleMapper policyRuleMapper;

	private final SecurityLevelResolver securityLevelResolver;

	private final StringRedisTemplate redisTemplate;

	/**
	 * 单条决策。
	 */
	public PolicyDecision decide(SecuritySubject subject, Long ontologyId, SecuredResource resource,
			DataAction action) {
		// 1. 工程 ACL 校验
		if (!checkProjectAcl(subject, ontologyId, action)) {
			return PolicyDecision.deny("PROJECT_ACL_DENIED");
		}

		// 2. 查询匹配的策略规则
		List<OntDataPolicyRule> rules = findMatchingRules(subject, ontologyId, resource, action);

		// 3. 检查显式 DENY
		boolean hasDeny = rules.stream().anyMatch(r -> "DENY".equals(r.getEffect()));
		if (hasDeny) {
			List<Long> ruleIds = rules.stream().filter(r -> "DENY".equals(r.getEffect()))
				.map(OntDataPolicyRule::getId).collect(Collectors.toList());
			return PolicyDecision.builder()
				.effect(DecisionEffect.DENY)
				.matchedRuleIds(ruleIds)
				.reasonCode("EXPLICIT_DENY")
				.build();
		}

		// 4. 显式规则合并（USER 优先，再 ROLE/DEPT）
		DecisionEffect ruleEffect = mergeRuleEffects(rules);
		String maskType = rules.stream()
			.filter(r -> "MASK".equals(r.getEffect()))
			.map(OntDataPolicyRule::getMaskType)
			.findFirst()
			.orElse(null);

		// 5. 如果有显式规则，以规则结果为准
		if (ruleEffect != null) {
			List<Long> ruleIds = rules.stream().map(OntDataPolicyRule::getId).collect(Collectors.toList());
			return PolicyDecision.builder()
				.effect(ruleEffect)
				.effectiveLevelCode(resource.getSecurityLevelCode())
				.maskType(maskType)
				.matchedRuleIds(ruleIds)
				.reasonCode("EXPLICIT_RULE")
				.build();
		}

		// 6. 主体 clearance 与数据 rank 比较
		DecisionEffect clearanceEffect = checkClearance(subject, ontologyId, resource.getSecurityLevelCode());
		if (clearanceEffect == DecisionEffect.DENY) {
			return PolicyDecision.deny("CLEARANCE_INSUFFICIENT");
		}

		// 7. 等级默认策略
		String defaultEffect = securityLevelResolver.getDefaultViewEffect(resource.getSecurityLevelCode());
		DecisionEffect finalEffect = DecisionEffect.stricter(clearanceEffect, toEffect(defaultEffect));

		return PolicyDecision.builder()
			.effect(finalEffect)
			.effectiveLevelCode(resource.getSecurityLevelCode())
			.reasonCode("DEFAULT_LEVEL_POLICY")
			.build();
	}

	/**
	 * 批量决策，避免 N+1。
	 */
	public List<PolicyDecision> batchDecide(SecuritySubject subject, Long ontologyId,
			List<SecuredResource> resources, DataAction action) {
		// 工程 ACL 校验一次
		if (!checkProjectAcl(subject, ontologyId, action)) {
			return resources.stream()
				.map(r -> PolicyDecision.deny("PROJECT_ACL_DENIED"))
				.collect(Collectors.toList());
		}

		// 批量查询所有匹配规则
		List<OntDataPolicyRule> allRules = findAllRulesForSubject(subject, ontologyId, action);

		// 逐条匹配
		return resources.stream()
			.map(resource -> decideForResource(subject, ontologyId, resource, action, allRules))
			.collect(Collectors.toList());
	}

	/**
	 * 检查工程 ACL。
	 */
	private boolean checkProjectAcl(SecuritySubject subject, Long ontologyId, DataAction action) {
		if (subject.isSystem()) {
			// 系统主体按失败关闭处理
			log.warn("SYSTEM subject denied access to ontology {}", ontologyId);
			return false;
		}

		// 查询该主体在该工程的所有 ACL
		List<OntOntologyProjectAcl> acls = aclMapper.selectList(
			Wrappers.<OntOntologyProjectAcl>lambdaQuery()
				.eq(OntOntologyProjectAcl::getOntologyId, ontologyId)
				.eq(OntOntologyProjectAcl::getDelFlag, "0"));

		// 匹配主体的 ACL
		boolean hasView = false;
		boolean hasEdit = false;
		boolean hasPublish = false;
		boolean hasAdmin = false;

		for (OntOntologyProjectAcl acl : acls) {
			if (matchesSubject(subject, acl.getSubjectType(), acl.getSubjectId())) {
				String level = acl.getAccessLevel();
				if ("ADMIN".equals(level)) {
					hasAdmin = hasPublish = hasEdit = hasView = true;
				}
				else if ("PUBLISH".equals(level)) {
					hasPublish = hasEdit = hasView = true;
				}
				else if ("EDIT".equals(level)) {
					hasEdit = hasView = true;
				}
				else if ("VIEW".equals(level)) {
					hasView = true;
				}
			}
		}

		return switch (action) {
			case VIEW -> hasView;
			case EDIT -> hasEdit;
			case EXPORT, SPARQL -> hasView;
		};
	}

	/**
	 * 判断主体是否匹配 ACL/规则的 subject。
	 */
	private boolean matchesSubject(SecuritySubject subject, String subjectType, Long subjectId) {
		return switch (subjectType) {
			case SecurityConstants.SUBJECT_USER -> subject.getUserId() != null && subject.getUserId().equals(subjectId);
			case SecurityConstants.SUBJECT_ROLE -> subject.getRoleIds() != null && subject.getRoleIds().contains(subjectId);
			case SecurityConstants.SUBJECT_DEPT -> subject.getDeptIds() != null && subject.getDeptIds().contains(subjectId);
			default -> false;
		};
	}

	/**
	 * 查询匹配的策略规则。
	 */
	private List<OntDataPolicyRule> findMatchingRules(SecuritySubject subject, Long ontologyId,
			SecuredResource resource, DataAction action) {
		return findAllRulesForSubject(subject, ontologyId, action).stream()
			.filter(r -> resource.getResourceType().equals(r.getResourceType()))
			.filter(r -> resource.getResourceId().equals(r.getResourceId()))
			.collect(Collectors.toList());
	}

	/**
	 * 批量查询主体在该工程的所有策略规则。
	 */
	private List<OntDataPolicyRule> findAllRulesForSubject(SecuritySubject subject, Long ontologyId,
			DataAction action) {
		List<OntDataPolicyRule> rules = new ArrayList<>();

		// USER 规则
		if (subject.getUserId() != null) {
			rules.addAll(policyRuleMapper.selectList(
				Wrappers.<OntDataPolicyRule>lambdaQuery()
					.eq(OntDataPolicyRule::getOntologyId, ontologyId)
					.eq(OntDataPolicyRule::getSubjectType, SecurityConstants.SUBJECT_USER)
					.eq(OntDataPolicyRule::getSubjectId, subject.getUserId())
					.eq(OntDataPolicyRule::getAction, action.name())
					.eq(OntDataPolicyRule::getDelFlag, "0")));
		}

		// ROLE 规则
		if (subject.getRoleIds() != null && !subject.getRoleIds().isEmpty()) {
			rules.addAll(policyRuleMapper.selectList(
				Wrappers.<OntDataPolicyRule>lambdaQuery()
					.eq(OntDataPolicyRule::getOntologyId, ontologyId)
					.eq(OntDataPolicyRule::getSubjectType, SecurityConstants.SUBJECT_ROLE)
					.in(OntDataPolicyRule::getSubjectId, subject.getRoleIds())
					.eq(OntDataPolicyRule::getAction, action.name())
					.eq(OntDataPolicyRule::getDelFlag, "0")));
		}

		// DEPT 规则
		if (subject.getDeptIds() != null && !subject.getDeptIds().isEmpty()) {
			rules.addAll(policyRuleMapper.selectList(
				Wrappers.<OntDataPolicyRule>lambdaQuery()
					.eq(OntDataPolicyRule::getOntologyId, ontologyId)
					.eq(OntDataPolicyRule::getSubjectType, SecurityConstants.SUBJECT_DEPT)
					.in(OntDataPolicyRule::getSubjectId, subject.getDeptIds())
					.eq(OntDataPolicyRule::getAction, action.name())
					.eq(OntDataPolicyRule::getDelFlag, "0")));
		}

		return rules;
	}

	/**
	 * 逐条资源的决策（用于批量）。
	 */
	private PolicyDecision decideForResource(SecuritySubject subject, Long ontologyId,
			SecuredResource resource, DataAction action, List<OntDataPolicyRule> allRules) {
		List<OntDataPolicyRule> matched = allRules.stream()
			.filter(r -> resource.getResourceType().equals(r.getResourceType()))
			.filter(r -> resource.getResourceId().equals(r.getResourceId()))
			.collect(Collectors.toList());

		// 显式 DENY
		if (matched.stream().anyMatch(r -> "DENY".equals(r.getEffect()))) {
			return PolicyDecision.deny("EXPLICIT_DENY");
		}

		// 合并规则效果
		DecisionEffect ruleEffect = mergeRuleEffects(matched);
		if (ruleEffect != null) {
			return PolicyDecision.builder()
				.effect(ruleEffect)
				.effectiveLevelCode(resource.getSecurityLevelCode())
				.maskType(matched.stream().filter(r -> "MASK".equals(r.getEffect()))
					.map(OntDataPolicyRule::getMaskType).findFirst().orElse(null))
				.reasonCode("EXPLICIT_RULE")
				.build();
		}

		// clearance 检查
		DecisionEffect clearanceEffect = checkClearance(subject, ontologyId, resource.getSecurityLevelCode());
		if (clearanceEffect == DecisionEffect.DENY) {
			return PolicyDecision.deny("CLEARANCE_INSUFFICIENT");
		}

		// 默认策略
		String defaultEffect = securityLevelResolver.getDefaultViewEffect(resource.getSecurityLevelCode());
		DecisionEffect finalEffect = DecisionEffect.stricter(clearanceEffect, toEffect(defaultEffect));
		return PolicyDecision.builder()
			.effect(finalEffect)
			.effectiveLevelCode(resource.getSecurityLevelCode())
			.reasonCode("DEFAULT_LEVEL_POLICY")
			.build();
	}

	/**
	 * 合并规则效果：DENY > MASK > ALLOW，返回 null 表示无匹配规则。
	 */
	private DecisionEffect mergeRuleEffects(List<OntDataPolicyRule> rules) {
		if (rules == null || rules.isEmpty()) {
			return null;
		}
		// USER 规则优先
		DecisionEffect userEffect = rules.stream()
			.filter(r -> SecurityConstants.SUBJECT_USER.equals(r.getSubjectType()))
			.map(r -> toEffect(r.getEffect()))
			.reduce(null, DecisionEffect::stricter);

		// ROLE/DEPT 规则
		DecisionEffect groupEffect = rules.stream()
			.filter(r -> SecurityConstants.SUBJECT_ROLE.equals(r.getSubjectType())
					|| SecurityConstants.SUBJECT_DEPT.equals(r.getSubjectType()))
			.map(r -> toEffect(r.getEffect()))
			.reduce(null, DecisionEffect::stricter);

		// USER 优先，但 GROUP 更严格时取更严格
		return DecisionEffect.stricter(userEffect, groupEffect);
	}

	/**
	 * 主体 clearance 检查。
	 */
	private DecisionEffect checkClearance(SecuritySubject subject, Long ontologyId, String levelCode) {
		if (levelCode == null) {
			levelCode = SecurityConstants.LEVEL_INTERNAL;
		}

		int dataRank = securityLevelResolver.getRank(levelCode);
		List<OntSecuritySubjectClearance> clearances = new ArrayList<>();

		// USER clearance
		if (subject.getUserId() != null) {
			clearances.addAll(clearanceMapper.selectList(
				Wrappers.<OntSecuritySubjectClearance>lambdaQuery()
					.eq(OntSecuritySubjectClearance::getOntologyId, ontologyId)
					.eq(OntSecuritySubjectClearance::getSubjectType, SecurityConstants.SUBJECT_USER)
					.eq(OntSecuritySubjectClearance::getSubjectId, subject.getUserId())
					.eq(OntSecuritySubjectClearance::getDelFlag, "0")));
		}

		// ROLE clearance
		if (subject.getRoleIds() != null && !subject.getRoleIds().isEmpty()) {
			clearances.addAll(clearanceMapper.selectList(
				Wrappers.<OntSecuritySubjectClearance>lambdaQuery()
					.eq(OntSecuritySubjectClearance::getOntologyId, ontologyId)
					.eq(OntSecuritySubjectClearance::getSubjectType, SecurityConstants.SUBJECT_ROLE)
					.in(OntSecuritySubjectClearance::getSubjectId, subject.getRoleIds())
					.eq(OntSecuritySubjectClearance::getDelFlag, "0")));
		}

		// DEPT clearance
		if (subject.getDeptIds() != null && !subject.getDeptIds().isEmpty()) {
			clearances.addAll(clearanceMapper.selectList(
				Wrappers.<OntSecuritySubjectClearance>lambdaQuery()
					.eq(OntSecuritySubjectClearance::getOntologyId, ontologyId)
					.eq(OntSecuritySubjectClearance::getSubjectType, SecurityConstants.SUBJECT_DEPT)
					.in(OntSecuritySubjectClearance::getSubjectId, subject.getDeptIds())
					.eq(OntSecuritySubjectClearance::getDelFlag, "0")));
		}

		if (clearances.isEmpty()) {
			// 无 clearance 配置，使用默认策略判断
			return null;
		}

		// 取主体最高 clearance rank
		int maxClearanceRank = clearances.stream()
			.mapToInt(c -> securityLevelResolver.getRank(c.getMaxLevelCode()))
			.max()
			.orElse(0);

		if (maxClearanceRank >= dataRank) {
			return null; // clearance 足够，交给默认策略
		}
		return DecisionEffect.DENY;
	}

	private DecisionEffect toEffect(String effect) {
		if (effect == null) {
			return null;
		}
		return switch (effect) {
			case "ALLOW" -> DecisionEffect.ALLOW;
			case "MASK" -> DecisionEffect.MASK;
			case "DENY" -> DecisionEffect.DENY;
			default -> null;
		};
	}

	/**
	 * 策略变更后递增修订号，使缓存自然过期。
	 */
	public void invalidatePolicyCache(Long ontologyId) {
		redisTemplate.delete(SecurityConstants.CACHE_POLICY_PREFIX + ontologyId + ":*");
		redisTemplate.opsForValue().increment(SecurityConstants.CACHE_POLICY_REVISION_PREFIX + ontologyId);
	}

}
