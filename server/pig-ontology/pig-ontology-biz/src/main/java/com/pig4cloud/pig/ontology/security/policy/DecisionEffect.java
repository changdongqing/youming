/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

/**
 * 策略决策效果。
 *
 * @author youming
 */
public enum DecisionEffect {

	/**
	 * 允许返回授权明文。
	 */
	ALLOW,

	/**
	 * 返回结构和脱敏值。
	 */
	MASK,

	/**
	 * 拒绝访问，不返回数据。
	 */
	DENY;

	/**
	 * 多个效果冲突时取更严格结果：DENY > MASK > ALLOW。
	 */
	public static DecisionEffect stricter(DecisionEffect a, DecisionEffect b) {
		if (a == DENY || b == DENY) {
			return DENY;
		}
		if (a == MASK || b == MASK) {
			return MASK;
		}
		return ALLOW;
	}

}
