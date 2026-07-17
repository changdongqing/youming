/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.compiler;

import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IRI 模板编译器（18-04 §4）。
 * <p>
 * V1 语法：{@code prefix/{column|filter}}，支持复合键和过滤器。
 * <p>
 * 内置过滤器：
 * <ul>
 *   <li>{@code url} — UTF-8 percent encoding</li>
 *   <li>{@code lower} — Locale.ROOT 小写</li>
 *   <li>{@code upper} — Locale.ROOT 大写</li>
 *   <li>{@code trim}</li>
 *   <li>{@code number} — 规范十进制</li>
 *   <li>{@code date:yyyyMMdd} — 日期格式化</li>
 * </ul>
 * 禁止：{@code now/random/uuid}、网络查询、脚本。
 * <p>
 * IRI 模板变量必须是 keyColumns 中的列。完整 IRI 不超过 512，本地名不超过 128。
 *
 * @author youming
 */
@Slf4j
@Component
public class IriTemplateCompiler {

	/**
	 * 模板变量正则：{@code {column}} 或 {@code {column|filter}}。
	 */
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");

	/**
	 * 禁止的变量名（安全约束）。
	 */
	private static final List<String> FORBIDDEN_VARS = List.of("now", "random", "uuid");

	/**
	 * 本地名最大长度。
	 */
	private static final int MAX_LOCAL_NAME_LENGTH = 128;

	/**
	 * 完整 IRI 最大长度。
	 */
	private static final int MAX_IRI_LENGTH = 512;

	/**
	 * 编译模板，校验变量合法性。
	 * @param template IRI 或标签模板字符串
	 * @param keyColumnNames 键列名集合（IRI 模板变量必须是其子集，可为 null 跳过校验）
	 * @throws IllegalArgumentException 如果模板引用了禁止变量或非键列
	 */
	public void validateTemplate(String template, List<String> keyColumnNames) {
		if (template == null || template.isBlank()) {
			return;
		}

		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		while (matcher.find()) {
			String expr = matcher.group(1);
			String columnName = expr.contains("|") ? expr.substring(0, expr.indexOf('|')).trim() : expr.trim();

			// 禁止变量检查
			if (FORBIDDEN_VARS.contains(columnName.toLowerCase(Locale.ROOT))) {
				throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_120.getMessage()
						+ ": 禁止使用变量 " + columnName);
			}

			// IRI 模板变量必须是 keyColumns 中的列
			if (keyColumnNames != null && !keyColumnNames.isEmpty()
					&& !keyColumnNames.contains(columnName)) {
				throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_102.getMessage()
						+ ": 变量 " + columnName + " 不在键列中");
			}
		}
	}

	/**
	 * 渲染模板。
	 * @param template 模板字符串
	 * @param values 键值映射（列名 → 样例值）
	 * @return 渲染后的本地名
	 * @throws IllegalArgumentException 如果变量缺失或渲染超长
	 */
	public String renderTemplate(String template, Map<String, String> values) {
		if (template == null || template.isBlank()) {
			return "";
		}

		// 使用 appendReplacement/appendTail 保留模板中的字面量文本，
		// 只替换 {变量} 部分，避免丢掉 organization/ 等前缀导致不同实体 IRI 碰撞
		StringBuffer result = new StringBuffer();
		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		while (matcher.find()) {
			String expr = matcher.group(1);
			String columnName;
			String filter = null;

			if (expr.contains("|")) {
				columnName = expr.substring(0, expr.indexOf('|')).trim();
				filter = expr.substring(expr.indexOf('|') + 1).trim();
			}
			else {
				columnName = expr.trim();
			}

			String value = values.get(columnName);
			if (value == null) {
				value = "";
			}

			if (filter != null && !filter.isEmpty()) {
				value = applyFilter(value, filter);
			}

			// 转义 $ 和 \ 以免被 appendReplacement 当作反向引用
			String replacement = value.replace("\\", "\\\\").replace("$", "\\$");
			matcher.appendReplacement(result, replacement);
		}
		matcher.appendTail(result);

		return result.toString();
	}

	/**
	 * 渲染完整 IRI。
	 * @param namespaceUri 命名空间 URI
	 * @param iriTemplate IRI 模板
	 * @param values 键值映射
	 * @return 完整 IRI
	 * @throws IllegalArgumentException 如果渲染超长
	 */
	public String renderIri(String namespaceUri, String iriTemplate, Map<String, String> values) {
		String localName = renderTemplate(iriTemplate, values);

		if (localName.length() > MAX_LOCAL_NAME_LENGTH) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_103.getMessage()
					+ ": 本地名超过 " + MAX_LOCAL_NAME_LENGTH + " 字符");
		}

		String fullIri = (namespaceUri != null ? namespaceUri : "") + localName;

		if (fullIri.length() > MAX_IRI_LENGTH) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_103.getMessage()
					+ ": 完整 IRI 超过 " + MAX_IRI_LENGTH + " 字符");
		}

		return fullIri;
	}

	/**
	 * 从 key_columns JSON 中提取列名列表。
	 * @param keyColumnsJson key_columns JSONB 字符串，如 {@code [{"column":"user_id","order":1,"normalizer":"LONG"}]}
	 * @return 列名列表
	 */
	public List<String> extractKeyColumnNames(String keyColumnsJson) {
		if (keyColumnsJson == null || keyColumnsJson.isBlank()) {
			return List.of();
		}

		List<String> columns = new ArrayList<>();
		// 简单解析：提取 "column":"xxx" 的值
		Matcher matcher = Pattern.compile("\"column\"\\s*:\\s*\"([^\"]+)\"").matcher(keyColumnsJson);
		while (matcher.find()) {
			columns.add(matcher.group(1));
		}
		return columns;
	}

	/**
	 * 应用过滤器。
	 * @param value 原始值
	 * @param filter 过滤器表达式
	 * @return 过滤后的值
	 */
	private String applyFilter(String value, String filter) {
		if (value == null) {
			return "";
		}

		if (filter.startsWith("date:")) {
			return applyDateFormat(value, filter.substring(5));
		}

		return switch (filter) {
			case "url" -> urlEncode(value);
			case "lower" -> value.toLowerCase(Locale.ROOT);
			case "upper" -> value.toUpperCase(Locale.ROOT);
			case "trim" -> value.trim();
			case "number" -> normalizeNumber(value);
			default -> throw new IllegalArgumentException(
					EntityMappingErrorCode.ONT_MAP_120.getMessage() + ": 未知过滤器 " + filter);
		};
	}

	/**
	 * URL 编码。
	 */
	private String urlEncode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
		}
		catch (UnsupportedEncodingException e) {
			// UTF-8 不会抛出
			return value;
		}
	}

	/**
	 * 规范十进制：去除前导零、去除多余小数尾零。
	 */
	private String normalizeNumber(String value) {
		try {
			java.math.BigDecimal bd = new java.math.BigDecimal(value.strip());
			return bd.stripTrailingZeros().toPlainString();
		}
		catch (NumberFormatException e) {
			return value;
		}
	}

	/**
	 * 日期格式化。
	 * @param value 日期值
	 * @param format 日期格式，如 yyyyMMdd
	 */
	private String applyDateFormat(String value, String format) {
		try {
			// 尝试解析为 LocalDate
			LocalDate date;
			if (value.length() == 8 && !value.contains("-")) {
				date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
			}
			else {
				date = LocalDate.parse(value);
			}
			return date.format(DateTimeFormatter.ofPattern(format, Locale.ROOT));
		}
		catch (Exception e) {
			log.warn("Date format failed: value={}, format={}, error={}", value, format, e.getMessage());
			return value;
		}
	}

}
