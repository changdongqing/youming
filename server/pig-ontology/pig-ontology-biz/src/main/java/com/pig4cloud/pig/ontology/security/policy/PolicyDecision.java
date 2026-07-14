/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 策略决策结果。
 *
 * @author youming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDecision {

	/**
	 * 决策效果：ALLOW / MASK / DENY。
	 */
	private DecisionEffect effect;

	/**
	 * 有效安全级别编码。
	 */
	private String effectiveLevelCode;

	/**
	 * 脱敏类型（effect=MASK 时有效）。
	 */
	private String maskType;

	/**
	 * 脱敏参数（effect=MASK 时有效）。
	 */
	private Map<String, Object> maskParameter;

	/**
	 * 匹配的规则ID列表。
	 */
	private List<Long> matchedRuleIds;

	/**
	 * 决策原因码。
	 */
	private String reasonCode;

	public static PolicyDecision deny(String reasonCode) {
		return PolicyDecision.builder()
			.effect(DecisionEffect.DENY)
			.reasonCode(reasonCode)
			.build();
	}

	public static PolicyDecision allow(String levelCode, String reasonCode) {
		return PolicyDecision.builder()
			.effect(DecisionEffect.ALLOW)
			.effectiveLevelCode(levelCode)
			.reasonCode(reasonCode)
			.build();
	}

}
