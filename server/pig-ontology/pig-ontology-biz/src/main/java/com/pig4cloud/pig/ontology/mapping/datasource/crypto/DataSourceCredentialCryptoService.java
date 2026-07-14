/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.crypto;

import com.pig4cloud.pig.ontology.security.crypto.CryptoException;
import com.pig4cloud.pig.ontology.security.crypto.KeyProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 数据源凭证加解密服务。
 * <p>
 * 复用模块36 {@link KeyProvider} 和 AES-256-GCM 算法，但 AAD 绑定域不同于
 * {@code SensitiveValueCryptoService}（后者绑定 ontologyId/instanceId/dataPropertyId）。
 * <p>
 * AAD 绑定：{@code ontology-data-source:{sourceId}:{sourceCode}:{revision}}
 * <p>
 * Fail-closed：密钥不可用时拒绝读写，禁止回退明文。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceCredentialCryptoService {

	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

	private static final int GCM_TAG_LENGTH_BITS = 128;

	private static final int IV_LENGTH_BYTES = 12;

	private static final String AAD_PREFIX = "ontology-data-source";

	private final KeyProvider keyProvider;

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * 加密数据源凭证。
	 * @param sourceId    数据源ID
	 * @param sourceCode  数据源编码
	 * @param revision    配置版本号
	 * @param credential  明文凭证（username + password）
	 * @return 加密结果（密文 + keyId + IV）
	 */
	public EncryptedCredential encrypt(Long sourceId, String sourceCode, Long revision, DataSourceCredential credential) {
		if (credential == null || credential.username() == null || credential.password() == null) {
			throw new CryptoException("Cannot encrypt null credential");
		}

		KeyProvider.DataKey activeKey = keyProvider.getActiveKey();

		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			byte[] aad = buildAad(sourceId, sourceCode, revision);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(activeKey.getKeyBytes(), "AES");
			GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
			cipher.updateAAD(aad);

			// 凭证载荷 JSON: {"username":"...","password":"..."}
			String plaintext = serializeCredential(credential);
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			return EncryptedCredential.builder()
				.ciphertext(ciphertext)
				.keyId(activeKey.getKeyId())
				.iv(iv)
				.build();
		}
		catch (CryptoException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Credential encryption failed (fail-closed): {}", e.getMessage());
			throw new CryptoException("Credential encryption failed", e);
		}
	}

	/**
	 * 解密数据源凭证。
	 * @param sourceId    数据源ID
	 * @param sourceCode  数据源编码
	 * @param revision    配置版本号（加密时的版本号）
	 * @param encrypted   加密凭证包
	 * @return 明文凭证
	 * @throws CryptoException 解密失败不返回密文或占位符
	 */
	public DataSourceCredential decrypt(Long sourceId, String sourceCode, Long revision, EncryptedCredential encrypted) {
		if (encrypted == null || encrypted.getCiphertext() == null
				|| encrypted.getKeyId() == null || encrypted.getIv() == null) {
			throw new CryptoException("Invalid encrypted credential structure");
		}

		KeyProvider.DataKey dataKey = keyProvider.getKey(encrypted.getKeyId())
			.orElseThrow(() -> new CryptoException(
				"Decryption key not available for keyId: " + encrypted.getKeyId() + " (fail-closed)"));

		try {
			byte[] aad = buildAad(sourceId, sourceCode, revision);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(dataKey.getKeyBytes(), "AES");
			GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, encrypted.getIv());
			cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
			cipher.updateAAD(aad);

			byte[] plaintext = cipher.doFinal(encrypted.getCiphertext());
			String json = new String(plaintext, StandardCharsets.UTF_8);
			return deserializeCredential(json);
		}
		catch (Exception e) {
			log.error("Credential decryption failed (fail-closed): {}", e.getMessage());
			throw new CryptoException("Credential decryption failed, AAD mismatch or key error", e);
		}
	}

	/**
	 * 构建 AAD：ontology-data-source:{sourceId}:{sourceCode}:{revision}
	 */
	private byte[] buildAad(Long sourceId, String sourceCode, Long revision) {
		String aad = AAD_PREFIX + ":" + sourceId + ":" + sourceCode + ":" + revision;
		return aad.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * 序列化凭证为最小 JSON（不使用 ObjectMapper，避免外部依赖和格式漂移）。
	 */
	private String serializeCredential(DataSourceCredential credential) {
		String username = escapeJson(credential.username());
		String password = escapeJson(credential.password());
		return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
	}

	/**
	 * 反序列化凭证 JSON。
	 */
	private DataSourceCredential deserializeCredential(String json) {
		String username = extractJsonField(json, "username");
		String password = extractJsonField(json, "password");
		return new DataSourceCredential(username, password);
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	private String extractJsonField(String json, String field) {
		String key = "\"" + field + "\":\"";
		int start = json.indexOf(key);
		if (start < 0) {
			return null;
		}
		start += key.length();
		StringBuilder sb = new StringBuilder();
		boolean escaped = false;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (escaped) {
				switch (c) {
					case '"' -> sb.append('"');
					case '\\' -> sb.append('\\');
					case 'n' -> sb.append('\n');
					case 'r' -> sb.append('\r');
					case 't' -> sb.append('\t');
					default -> sb.append(c);
				}
				escaped = false;
			}
			else if (c == '\\') {
				escaped = true;
			}
			else if (c == '"') {
				break;
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 明文凭证。
	 */
	public record DataSourceCredential(String username, String password) {
	}

	/**
	 * 加密凭证数据包。
	 */
	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	public static class EncryptedCredential {

		/**
		 * AES-GCM 密文（含认证标签）。
		 */
		private byte[] ciphertext;

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
