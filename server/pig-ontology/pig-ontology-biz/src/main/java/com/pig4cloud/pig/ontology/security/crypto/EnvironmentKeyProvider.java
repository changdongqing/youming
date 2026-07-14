/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link KeyProvider} 默认实现：从 Spring 配置读取版本化密钥。
 * <p>
 * 配置示例：
 * <pre>
 * ontology:
 *   security:
 *     crypto:
 *       active-key-id: v1
 *       keys:
 *         v1: "Base64编码的32字节AES-256密钥"
 *         v2: "Base64编码的32字节AES-256密钥"
 * </pre>
 * <p>
 * 生产环境应替换为 KMS/HSM 实现，不得把根密钥写入源码、数据库或前端。
 *
 * @author youming
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "ontology.security.crypto")
public class EnvironmentKeyProvider implements KeyProvider {

	/**
	 * 当前活跃密钥版本。
	 */
	@Value("${ontology.security.crypto.active-key-id:v1}")
	private String activeKeyId;

	/**
	 * 版本化密钥映射：keyId → Base64 编码密钥。
	 */
	private Map<String, String> keys = new HashMap<>();

	@Override
	public DataKey getActiveKey() {
		String base64Key = keys.get(activeKeyId);
		if (base64Key == null || base64Key.isEmpty()) {
			log.error("Active crypto key not found for keyId: {}", activeKeyId);
			throw new CryptoException("Active crypto key not available, refusing to proceed (fail-closed)");
		}
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != 32) {
			log.error("Crypto key length invalid: expected 32 bytes, got {}", keyBytes.length);
			throw new CryptoException("Invalid crypto key length, expected 32 bytes");
		}
		return DataKey.builder()
			.keyId(activeKeyId)
			.keyBytes(keyBytes)
			.algorithm("AES/GCM/NoPadding")
			.build();
	}

	@Override
	public Optional<DataKey> getKey(String keyId) {
		String base64Key = keys.get(keyId);
		if (base64Key == null || base64Key.isEmpty()) {
			return Optional.empty();
		}
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		if (keyBytes.length != 32) {
			log.error("Crypto key length invalid for keyId {}: expected 32 bytes, got {}", keyId, keyBytes.length);
			return Optional.empty();
		}
		return Optional.of(DataKey.builder()
			.keyId(keyId)
			.keyBytes(keyBytes)
			.algorithm("AES/GCM/NoPadding")
			.build());
	}

	@Override
	public String getActiveKeyId() {
		return activeKeyId;
	}

	public Map<String, String> getKeys() {
		return keys;
	}

	public void setKeys(Map<String, String> keys) {
		this.keys = keys;
	}

}
