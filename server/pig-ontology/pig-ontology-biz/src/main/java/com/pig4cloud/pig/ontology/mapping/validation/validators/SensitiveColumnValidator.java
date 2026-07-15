/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 敏感列与禁止字段校验器（L7 安全合规，18-06 §8）。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>禁止字段（password/salt/secret/token/private_key/access_key/refresh_token/credential）
 *       → VIOLATION，禁止映射，发布时不可通过 WARNING 确认放行</li>
 *   <li>敏感字段（phone/email/openid/id_card）
 *       → WARNING，需确认或根据数据属性安全级别要求加密</li>
 * </ul>
 * 匹配规则：忽略大小写，按下划线/驼峰分词后精确词匹配，避免 password_expire_flag 被误判。
 *
 * @author youming
 */
@Slf4j
@Component
public class SensitiveColumnValidator implements MappingValidator {

	/**
	 * 禁止字段词表（VIOLATION）。
	 */
	private static final Set<String> FORBIDDEN_WORDS = Set.of(
			"password", "passwd", "salt", "secret", "token", "private_key",
			"access_key", "refresh_token", "credential");

	/**
	 * 敏感字段词表（WARNING）。
	 */
	private static final Set<String> SENSITIVE_WORDS = Set.of(
			"phone", "email", "openid", "id_card");

	/**
	 * 驼峰/下划线分词正则。
	 */
	private static final Pattern WORD_SPLIT = Pattern.compile(
			"([a-z0-9]+|[A-Z][a-z0-9]*)");

	@Override
	public String code() {
		return "SensitiveColumnValidator";
	}

	@Override
	public int order() {
		return 70;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		for (var em : context.getEntityMappings()) {
			if (!"1".equals(em.getEnabled())) {
				continue;
			}

			List<OntFieldMapping> fieldMappings = context.getFieldMappings(em.getId());
			for (OntFieldMapping fm : fieldMappings) {
				if (!"COLUMN".equals(fm.getSourceKind())) {
					continue;
				}
				if (fm.getSourceColumn() == null || fm.getSourceColumn().isBlank()) {
					continue;
				}

				String scopeRef = em.getMappingCode() + "." + fm.getFieldMappingCode();
				List<String> words = tokenizeColumn(fm.getSourceColumn());

				// 禁止字段检查
				for (String word : words) {
					if (FORBIDDEN_WORDS.contains(word)) {
						issues.addViolation(ValidationErrorCode.ONT_MAP_206.getCode(), "FIELD",
								scopeRef, "禁止字段映射: 列 '" + fm.getSourceColumn()
										+ "' 包含禁止词 '" + word + "'",
								"移除该字段映射或使用脱敏后的派生列");
						break; // 一个禁止词足够
					}
				}

				// 敏感字段检查
				for (String word : words) {
					if (SENSITIVE_WORDS.contains(word)) {
						issues.addWarning("SENSITIVE_COLUMN", "FIELD",
								scopeRef, "敏感字段映射: 列 '" + fm.getSourceColumn()
										+ "' 包含敏感词 '" + word + "'",
								"确认目标属性已标记安全级别或加密策略");
						break;
					}
				}
			}
		}

		log.debug("SensitiveColumnValidator completed: {} issues", issues.getTotalCount());
	}

	/**
	 * 将列名按驼峰/下划线分词，全部转小写。
	 * <p>
	 * 例如：userPassword → [user, password]
	 * password_expire_flag → [password, expire, flag]
	 */
	private List<String> tokenizeColumn(String columnName) {
		// 先按下划线拆分
		String[] underscoreParts = columnName.split("_+");
		List<String> words = new java.util.ArrayList<>();
		for (String part : underscoreParts) {
			// 再按驼峰拆分
			java.util.regex.Matcher matcher = WORD_SPLIT.matcher(part);
			while (matcher.find()) {
				words.add(matcher.group(1).toLowerCase(Locale.ROOT));
			}
		}
		return words;
	}

}
