/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.support;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 映射版本本体版本约束校验器（18-03 §5）。
 * <p>
 * 支持 {@code ontology_version_constraint} 表达式：
 * <ul>
 *   <li>{@code =1.2.0} — 精确匹配</li>
 *   <li>{@code >=1.2.0 <2.0.0} — 范围组合（AND）</li>
 *   <li>{@code >=2.0.0 <3.0.0} — 范围组合（AND）</li>
 * </ul>
 * 仅支持比较符 {@code =, >, >=, <, <=} 和空格 AND 组合，不支持 OR、通配函数和任意表达式。
 *
 * @author youming
 */
@Component
public class MappingSemVerValidator {

	private static final Pattern COMPARATOR_PATTERN = Pattern.compile("^([><=]+)\\s*(.+)$");

	/**
	 * 校验约束表达式是否合法。
	 * @param constraint 约束表达式，如 ">=1.2.0 <2.0.0"
	 * @return true 合法
	 */
	public boolean isValidConstraint(String constraint) {
		if (constraint == null || constraint.isBlank()) {
			return false;
		}
		try {
			parseComparators(constraint);
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * 检查版本号是否满足约束表达式。
	 * @param versionNumber 版本号，如 "1.5.0"
	 * @param constraint 约束表达式，如 ">=1.2.0 <2.0.0"
	 * @return true 满足
	 * @throws IllegalArgumentException 如果版本号或约束表达式非法
	 */
	public boolean isSatisfied(String versionNumber, String constraint) {
		Version semver = parseVersion(versionNumber);
		List<Comparator> comparators = parseComparators(constraint);
		return comparators.stream().allMatch(c -> c.matches(semver));
	}

	/**
	 * 解析版本号。
	 */
	private Version parseVersion(String versionNumber) {
		if (versionNumber == null || versionNumber.isBlank()) {
			throw new IllegalArgumentException("版本号不能为空");
		}
		try {
			return Version.valueOf(versionNumber);
		}
		catch (ParseException e) {
			throw new IllegalArgumentException("版本号格式非法: " + versionNumber);
		}
	}

	/**
	 * 解析约束表达式为比较器列表。
	 * <p>
	 * 以空格切分表达式为多个 token，每个 token 形如 ">=1.2.0"。
	 * 每个独立 token 必须可拆分为运算符和版本号。
	 */
	private List<Comparator> parseComparators(String constraint) {
		String trimmed = constraint.trim();
		// 需要处理如 ">=1.2.0 <2.0.0" 的表达式
		// 用正则匹配所有 运算符+版本号 组合
		List<Comparator> result = new ArrayList<>();
		// 匹配模式：运算符（>=, <=, >, <, =）后跟版本号
		Pattern tokenPattern = Pattern.compile("([><=]+)\\s*([0-9]+\\.[0-9]+\\.[0-9]+)");
		Matcher matcher = tokenPattern.matcher(trimmed);

		int lastEnd = 0;
		while (matcher.find()) {
			// 检查 token 之间是否有非法字符
			if (matcher.start() > lastEnd) {
				String gap = trimmed.substring(lastEnd, matcher.start()).trim();
				if (!gap.isEmpty()) {
					throw new IllegalArgumentException("约束表达式包含不支持的字符: " + gap);
				}
			}
			String operator = matcher.group(1);
			String version = matcher.group(2);
			result.add(new Comparator(operator, version));
			lastEnd = matcher.end();
		}

		// 检查尾部是否有非法字符
		if (lastEnd < trimmed.length()) {
			String tail = trimmed.substring(lastEnd).trim();
			if (!tail.isEmpty()) {
				throw new IllegalArgumentException("约束表达式包含不支持的字符: " + tail);
			}
		}

		if (result.isEmpty()) {
			throw new IllegalArgumentException("约束表达式不包含有效的比较器: " + constraint);
		}
		return result;
	}

	/**
	 * 单个比较器。
	 */
	private record Comparator(String operator, String versionStr) {

		boolean matches(Version semver) {
			Version constraintVersion = Version.valueOf(versionStr);
			int cmp = semver.compareTo(constraintVersion);
			return switch (operator) {
				case "=" -> cmp == 0;
				case ">" -> cmp > 0;
				case ">=" -> cmp >= 0;
				case "<" -> cmp < 0;
				case "<=" -> cmp <= 0;
				default -> throw new IllegalArgumentException("不支持的比较运算符: " + operator);
			};
		}

	}

}
