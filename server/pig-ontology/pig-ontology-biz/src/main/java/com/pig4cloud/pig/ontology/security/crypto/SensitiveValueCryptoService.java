/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 敏感值加解密服务。
 * <p>
 * 使用 AES-256-GCM 认证加密：
 * <ul>
 *   <li>写入：策略判断需加密 → 获取 active key → 随机 96-bit IV → AES-GCM → 清空 literal_value → 写密文与 key id</li>
 *   <li>读取：先做策略决策；DENY 不解密，MASK 可解密后在内存脱敏，ALLOW 解密后返回</li>
 * </ul>
 * AAD 至少绑定 ontology_id + instance_id + data_property_id，防止密文换位。
 * 密钥不可用时拒绝读写，禁止回退明文。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveValueCryptoService {

	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

	private static final int GCM_TAG_LENGTH_BITS = 128;

	private static final int IV_LENGTH_BYTES = 12;

	private final KeyProvider keyProvider;

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 加密敏感值。
	 *
	 * @param plaintext       明文值
	 * @param ontologyId      本体工程ID（AAD 绑定）
	 * @param instanceId      实例ID（AAD 绑定）
	 * @param dataPropertyId  数据属性ID（AAD 绑定）
	 * @return 加密结果（密文 + keyId + IV）
	 */
	public EncryptedValue encrypt(String plaintext, Long ontologyId, Long instanceId, Long dataPropertyId) {
		if (plaintext == null) {
			throw new CryptoException("Cannot encrypt null value");
		}

		KeyProvider.DataKey activeKey = keyProvider.getActiveKey();

		try {
			// 随机 96-bit IV
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			// 构建 AAD: ontology_id + instance_id + data_property_id
			byte[] aad = buildAad(ontologyId, instanceId, dataPropertyId);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(activeKey.getKeyBytes(), "AES");
			GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
			cipher.updateAAD(aad);

			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			return EncryptedValue.builder()
				.encryptedValue(ciphertext)
				.keyId(activeKey.getKeyId())
				.iv(iv)
				.build();
		}
		catch (CryptoException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Encryption failed, refusing to proceed (fail-closed): {}", e.getMessage());
			throw new CryptoException("Encryption failed", e);
		}
	}

	/**
	 * 解密敏感值。
	 *
	 * @param encryptedValue  密文值包（含密文、keyId、IV）
	 * @param ontologyId      本体工程ID（AAD 绑定）
	 * @param instanceId      实例ID（AAD 绑定）
	 * @param dataPropertyId  数据属性ID（AAD 绑定）
	 * @return 明文值
	 * @throws CryptoException 解密失败不返回密文或空字符串冒充有效值
	 */
	public String decrypt(EncryptedValue encryptedValue, Long ontologyId, Long instanceId, Long dataPropertyId) {
		if (encryptedValue == null || encryptedValue.getEncryptedValue() == null
				|| encryptedValue.getKeyId() == null || encryptedValue.getIv() == null) {
			throw new CryptoException("Invalid encrypted value structure");
		}

		KeyProvider.DataKey dataKey = keyProvider.getKey(encryptedValue.getKeyId())
			.orElseThrow(() -> new CryptoException(
				"Decryption key not available for keyId: " + encryptedValue.getKeyId() + " (fail-closed)"));

		try {
			byte[] aad = buildAad(ontologyId, instanceId, dataPropertyId);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(dataKey.getKeyBytes(), "AES");
			GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedValue.getIv());
			cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
			cipher.updateAAD(aad);

			byte[] plaintext = cipher.doFinal(encryptedValue.getEncryptedValue());
			return new String(plaintext, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			log.error("Decryption failed, refusing to return ciphertext or placeholder (fail-closed): {}",
				e.getMessage());
			throw new CryptoException("Decryption failed, AAD mismatch or key error", e);
		}
	}

	/**
	 * 判断密文是否需要重加密（keyId 与当前活跃 keyId 不一致）。
	 */
	public boolean needsReEncryption(String keyId) {
		return !keyProvider.getActiveKeyId().equals(keyId);
	}

	/**
	 * 构建 AAD（Additional Authenticated Data）。
	 * <p>
	 * 绑定 ontology_id + instance_id + data_property_id，防止密文换位。
	 */
	private byte[] buildAad(Long ontologyId, Long instanceId, Long dataPropertyId) {
		ByteBuffer buffer = ByteBuffer.allocate(24);
		buffer.putLong(ontologyId != null ? ontologyId : 0L);
		buffer.putLong(instanceId != null ? instanceId : 0L);
		buffer.putLong(dataPropertyId != null ? dataPropertyId : 0L);
		return buffer.array();
	}

	/**
	 * 加密值数据包。
	 */
	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class EncryptedValue {

		/**
		 * AES-GCM 密文（含认证标签）。
		 */
		private byte[] encryptedValue;

		/**
		 * 密钥版本ID。
		 */
		private String keyId;

		/**
		 * 加密随机 IV。
		 */
		private byte[] iv;

	}

}
