/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.transform;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 转换器注册表（18-04 §7）。
 * <p>
 * V1 内置转换器仅注册元信息，不含 {@code transform()} 执行逻辑。
 * 运行时转换由 18-07 执行器调用。
 * <p>
 * 内置转换器：
 * <table>
 *   <tr><th>code</th><th>用途</th></tr>
 *   <tr><td>IDENTITY</td><td>类型兼容时直传</td></tr>
 *   <tr><td>TRIM</td><td>去首尾空格</td></tr>
 *   <tr><td>LOWER / UPPER</td><td>Locale.ROOT大小写</td></tr>
 *   <tr><td>TO_STRING</td><td>数字/布尔转规范字符串</td></tr>
 *   <tr><td>TO_INTEGER</td><td>精确整数，禁止静默舍入</td></tr>
 *   <tr><td>TO_DECIMAL</td><td>BigDecimal规范化</td></tr>
 *   <tr><td>TO_BOOLEAN</td><td>配置true/false值集合</td></tr>
 *   <tr><td>DATE_FORMAT</td><td>日期时间转目标DATE词法值</td></tr>
 *   <tr><td>ENUM_MAP</td><td>显式字典映射</td></tr>
 *   <tr><td>PREFIX / SUFFIX</td><td>固定前后缀</td></tr>
 *   <tr><td>TRUNCATE</td><td>显式最大长度</td></tr>
 * </table>
 *
 * @author youming
 */
@Slf4j
@Component
public class TransformerRegistry {

	private final Map<String, TransformerInfo> registry = new LinkedHashMap<>();

	@PostConstruct
	void init() {
		register("IDENTITY", "类型兼容时直传",
				List.of("BOOLEAN", "DATE", "NUMERIC", "TEXT", "TIMESTAMP", "VARCHAR"),
				List.of("STRING", "URI", "DATE", "INTEGER", "DECIMAL", "BOOLEAN"),
				null);

		register("TRIM", "去首尾空格",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				null);

		register("LOWER", "Locale.ROOT 小写",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				null);

		register("UPPER", "Locale.ROOT 大写",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				null);

		register("TO_STRING", "数字/布尔转规范字符串",
				List.of("NUMERIC", "INTEGER", "BIGINT", "DECIMAL", "BOOLEAN"),
				List.of("STRING"),
				null);

		register("TO_INTEGER", "精确整数，禁止静默舍入",
				List.of("NUMERIC", "INTEGER", "BIGINT", "DECIMAL", "TEXT", "VARCHAR"),
				List.of("INTEGER"),
				null);

		register("TO_DECIMAL", "BigDecimal 规范化",
				List.of("NUMERIC", "INTEGER", "BIGINT", "DECIMAL", "TEXT", "VARCHAR"),
				List.of("DECIMAL"),
				null);

		register("TO_BOOLEAN", "配置 true/false 值集合",
				List.of("TEXT", "VARCHAR", "CHAR", "BOOLEAN", "INTEGER"),
				List.of("BOOLEAN"),
				"""
				{"trueValues":["true","1","yes"],"falseValues":["false","0","no"]}""");

		register("DATE_FORMAT", "日期时间转目标 DATE/DATETIME 词法值",
				List.of("DATE", "TIMESTAMP", "TEXT", "VARCHAR"),
				List.of("DATE", "DATETIME", "STRING"),
				"""
				{"outputFormat":"yyyyMMdd"}""");

		register("ENUM_MAP", "显式字典映射",
				List.of("TEXT", "VARCHAR", "CHAR", "INTEGER", "BIGINT"),
				List.of("STRING"),
				"""
				{"entries":{"0":"正常","9":"锁定"},"unknownPolicy":"REJECT","unknownValue":null}""");

		register("PREFIX", "固定前缀",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				"""
				{"prefix":"prefix_"}""");

		register("SUFFIX", "固定后缀",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				"""
				{"suffix":"_suffix"}""");

		register("TRUNCATE", "显式最大长度",
				List.of("TEXT", "VARCHAR", "CHAR"),
				List.of("STRING"),
				"""
				{"maxLength":255}""");

		log.info("TransformerRegistry initialized with {} transformers", registry.size());
	}

	/**
	 * 注册转换器元信息。
	 * @param code 转换器编码
	 * @param description 用途描述
	 * @param supportedSourceTypes 支持的源 JDBC 类型族
	 * @param supportedTargetTypes 支持的目标字面量类型
	 * @param configSchema 配置 JSON Schema 描述
	 */
	private void register(String code, String description, List<String> supportedSourceTypes,
			List<String> supportedTargetTypes, String configSchema) {
		registry.put(code, new TransformerInfo(code, description, supportedSourceTypes,
				supportedTargetTypes, configSchema));
	}

	/**
	 * 列出全部可用转换器。
	 * @return 转换器元信息列表
	 */
	public List<TransformerInfo> listAvailable() {
		return List.copyOf(registry.values());
	}

	/**
	 * 校验转换器编码是否已注册。
	 * @param code 转换器编码
	 * @return true 已注册
	 */
	public boolean isRegistered(String code) {
		return registry.containsKey(code);
	}

	/**
	 * 获取转换器元信息。
	 * @param code 转换器编码
	 * @return 转换器元信息，不存在返回 null
	 */
	public TransformerInfo getInfo(String code) {
		return registry.get(code);
	}

}
