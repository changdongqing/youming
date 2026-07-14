/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.crypto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * 密钥提供者 SPI。
 * <p>
 * 默认实现 {@link EnvironmentKeyProvider} 从 Spring 配置读取版本化密钥，
 * 生产环境应替换为 KMS/HSM 实现（如 Vault Transit、AWS KMS）。
 *
 * @author youming
 */
public interface KeyProvider {

	/**
	 * 获取当前活跃数据密钥。
	 *
	 * @return 活跃密钥
	 * @throws CryptoException 密钥不可用时抛出，禁止回退明文
	 */
	DataKey getActiveKey();

	/**
	 * 按 key version 获取历史密钥（轮换后旧密钥仍在保留期内可解密）。
	 *
	 * @param keyId 密钥版本标识
	 * @return 密钥，不存在时返回 empty
	 */
	Optional<DataKey> getKey(String keyId);

	/**
	 * 当前活跃 keyId，用于判断是否需要重加密。
	 *
	 * @return 活跃密钥版本标识
	 */
	String getActiveKeyId();

	/**
	 * 数据密钥。
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	class DataKey {

		/**
		 * 密钥版本标识。
		 */
		private String keyId;

		/**
		 * AES-256 密钥字节（32 字节）。
		 */
		private byte[] keyBytes;

		/**
		 * 固定 "AES/GCM/NoPadding"。
		 */
		private String algorithm;

	}

}
