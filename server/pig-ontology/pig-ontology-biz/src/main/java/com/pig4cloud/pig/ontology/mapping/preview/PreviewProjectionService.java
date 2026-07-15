/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.preview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 预览脱敏投影服务（18-06 §6）。
 * <p>
 * 对于敏感目标属性，预览仅返回脱敏值；普通列默认最大展示128字符。
 * 不保存原始整行数据。
 *
 * @author youming
 */
@Slf4j
@Component
public class PreviewProjectionService {

	/**
	 * 普通列预览最大展示长度。
	 */
	private static final int MAX_PREVIEW_LENGTH = 128;

	/**
	 * 脱敏预览值。
	 * @param value 原始值
	 * @param sensitive 是否为敏感值
	 * @return 脱敏后的预览值
	 */
	public String projectValue(String value, boolean sensitive) {
		if (value == null || value.isEmpty()) {
			return value;
		}

		if (sensitive) {
			return maskSensitive(value);
		}

		return truncate(value);
	}

	/**
	 * 截断普通值。
	 */
	public String truncate(String value) {
		if (value == null) {
			return null;
		}
		if (value.length() <= MAX_PREVIEW_LENGTH) {
			return value;
		}
		return value.substring(0, MAX_PREVIEW_LENGTH) + "...(truncated)";
	}

	/**
	 * 脱敏敏感值：保留前2位和后1位，中间用***代替。
	 */
	public String maskSensitive(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		if (value.length() <= 3) {
			return "***";
		}
		return value.substring(0, 2) + "***" + value.substring(value.length() - 1);
	}

	/**
	 * 计算源记录键哈希（SHA-256），不保存原始键。
	 */
	public String hashRecordKey(String keyContent) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(keyContent.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (Exception e) {
			log.warn("Failed to hash record key: {}", e.getMessage());
			return "hash-error";
		}
	}

	/**
	 * 判断列名是否为敏感列（复用 SensitiveColumnValidator 的词表逻辑）。
	 */
	public boolean isSensitiveColumn(String columnName) {
		if (columnName == null || columnName.isBlank()) {
			return false;
		}
		String lower = columnName.toLowerCase(Locale.ROOT);
		return lower.contains("phone") || lower.contains("email")
				|| lower.contains("openid") || lower.contains("id_card")
				|| lower.contains("password") || lower.contains("salt")
				|| lower.contains("secret") || lower.contains("token");
	}

}
