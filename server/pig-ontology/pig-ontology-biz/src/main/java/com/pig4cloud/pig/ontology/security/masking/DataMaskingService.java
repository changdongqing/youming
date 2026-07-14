/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.masking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * 受控脱敏服务。
 * <p>
 * 内置脱敏类型：
 * <ul>
 *   <li>PHONE: 保留前三后四</li>
 *   <li>ID_CARD: 按参数配置保留位</li>
 *   <li>EMAIL: 保留首字符和域名</li>
 *   <li>IP: IPv4/IPv6 分别处理</li>
 *   <li>NAME: 按 Unicode code point 处理</li>
 *   <li>FIXED: 固定返回 ***</li>
 *   <li>PARTIAL: 由受控 JSON 参数指定前后保留长度</li>
 * </ul>
 * 不允许用户提交任意正则替换或脚本作为脱敏规则。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataMaskingService {

	private final ObjectMapper objectMapper;

	/**
	 * 脱敏类型常量。
	 */
	public static final String MASK_PHONE = "PHONE";

	public static final String MASK_ID_CARD = "ID_CARD";

	public static final String MASK_EMAIL = "EMAIL";

	public static final String MASK_IP = "IP";

	public static final String MASK_NAME = "NAME";

	public static final String MASK_FIXED = "FIXED";

	public static final String MASK_PARTIAL = "PARTIAL";

	/**
	 * 执行脱敏。
	 *
	 * @param value        原始值
	 * @param maskType     脱敏类型
	 * @param maskParameter 脱敏参数 JSON 字符串
	 * @return 脱敏后的值
	 */
	public String mask(String value, String maskType, String maskParameter) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		if (maskType == null) {
			return MASK_FIXED_RESULT;
		}
		return switch (maskType) {
			case MASK_PHONE -> maskPhone(value);
			case MASK_ID_CARD -> maskIdCard(value, maskParameter);
			case MASK_EMAIL -> maskEmail(value);
			case MASK_IP -> maskIp(value);
			case MASK_NAME -> maskName(value);
			case MASK_FIXED -> MASK_FIXED_RESULT;
			case MASK_PARTIAL -> maskPartial(value, maskParameter);
			default -> MASK_FIXED_RESULT;
		};
	}

	private static final String MASK_FIXED_RESULT = "***";

	/**
	 * 手机号脱敏：保留前三后四。
	 */
	private String maskPhone(String value) {
		if (value.length() <= 7) {
			return MASK_FIXED_RESULT;
		}
		return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
	}

	/**
	 * 证件号脱敏：按参数配置保留位，不假定所有证件均为 18 位身份证。
	 * <p>
	 * 参数 JSON: {"prefixKeep": 3, "suffixKeep": 4}
	 */
	private String maskIdCard(String value, String maskParameter) {
		int prefixKeep = 3;
		int suffixKeep = 4;
		if (maskParameter != null && !maskParameter.isEmpty()) {
			try {
				Map<String, Object> params = objectMapper.readValue(maskParameter, Map.class);
				prefixKeep = params.get("prefixKeep") != null ? ((Number) params.get("prefixKeep")).intValue() : 3;
				suffixKeep = params.get("suffixKeep") != null ? ((Number) params.get("suffixKeep")).intValue() : 4;
			}
			catch (JsonProcessingException e) {
				log.warn("Failed to parse maskParameter for ID_CARD: {}", e.getMessage());
			}
		}
		if (value.length() <= prefixKeep + suffixKeep) {
			return MASK_FIXED_RESULT;
		}
		String prefix = value.substring(0, prefixKeep);
		String suffix = value.substring(value.length() - suffixKeep);
		int maskLen = value.length() - prefixKeep - suffixKeep;
		return prefix + "*".repeat(Math.min(maskLen, 10)) + suffix;
	}

	/**
	 * 邮箱脱敏：保留首字符和域名。
	 */
	private String maskEmail(String value) {
		int atIndex = value.indexOf('@');
		if (atIndex <= 1) {
			return MASK_FIXED_RESULT;
		}
		return value.charAt(0) + "***" + value.substring(atIndex);
	}

	/**
	 * IP 地址脱敏：IPv4/IPv6 分别处理。
	 */
	private String maskIp(String value) {
		try {
			InetAddress addr = InetAddress.getByName(value);
			byte[] bytes = addr.getAddress();
			if (bytes.length == 4) {
				// IPv4: 保留前两段
				return (bytes[0] & 0xFF) + "." + (bytes[1] & 0xFF) + ".***.***";
			}
			else {
				// IPv6: 保留前两组
				return String.format("%02x%02x:****:****:****:****:****:****:****", bytes[0] & 0xFF, bytes[1] & 0xFF);
			}
		}
		catch (UnknownHostException e) {
			return MASK_FIXED_RESULT;
		}
	}

	/**
	 * 姓名脱敏：按 Unicode code point 处理，避免截断代理对。
	 * <p>
	 * 单字保留，两字保留首字，三字及以上保留首尾。
	 */
	private String maskName(String value) {
		int[] codePoints = value.codePoints().toArray();
		if (codePoints.length == 1) {
			return "*";
		}
		if (codePoints.length == 2) {
			return new String(codePoints, 0, 1) + "*";
		}
		StringBuilder sb = new StringBuilder();
		sb.appendCodePoint(codePoints[0]);
		sb.append("*".repeat(codePoints.length - 2));
		sb.appendCodePoint(codePoints[codePoints.length - 1]);
		return sb.toString();
	}

	/**
	 * 部分脱敏：由受控 JSON 参数指定前后保留长度。
	 * <p>
	 * 参数 JSON: {"prefixKeep": 2, "suffixKeep": 3}
	 */
	private String maskPartial(String value, String maskParameter) {
		int prefixKeep = 0;
		int suffixKeep = 0;
		if (maskParameter != null && !maskParameter.isEmpty()) {
			try {
				Map<String, Object> params = objectMapper.readValue(maskParameter, Map.class);
				prefixKeep = params.get("prefixKeep") != null ? ((Number) params.get("prefixKeep")).intValue() : 0;
				suffixKeep = params.get("suffixKeep") != null ? ((Number) params.get("suffixKeep")).intValue() : 0;
			}
			catch (JsonProcessingException e) {
				log.warn("Failed to parse maskParameter for PARTIAL: {}", e.getMessage());
			}
		}
		if (value.length() <= prefixKeep + suffixKeep) {
			return MASK_FIXED_RESULT;
		}
		String prefix = prefixKeep > 0 ? value.substring(0, prefixKeep) : "";
		String suffix = suffixKeep > 0 ? value.substring(value.length() - suffixKeep) : "";
		return prefix + "***" + suffix;
	}

}
