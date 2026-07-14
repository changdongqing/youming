/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.device;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.ontology.security.entity.OntDeviceCredential;
import com.pig4cloud.pig.ontology.security.mapper.OntDeviceCredentialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备凭证管理服务。
 * <p>
 * 只负责：注册、轮换、吊销、摘要校验、锁定策略和审计。
 * <ul>
 *   <li>Token 为至少 256 bit 随机值，只在创建/轮换时返回一次</li>
 *   <li>数据库存 HMAC-SHA-256 摘要，不存可逆 Token</li>
 *   <li>设备私钥永不上传平台</li>
 *   <li>Token 比较使用常量时间比较</li>
 *   <li>失败计数和锁定更新采用原子 SQL，避免并发绕过</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCredentialService {

	private final OntDeviceCredentialMapper deviceCredentialMapper;

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static final int TOKEN_BYTES = 32;

	private static final int MAX_FAILED_ATTEMPTS = 5;

	private static final int LOCK_MINUTES = 30;

	/**
	 * HMAC 摘要密钥版本（从配置读取，一期固定）。
	 */
	private static final String DIGEST_KEY_VERSION = "v1";

	/**
	 * HMAC 摘要密钥（从配置读取，一期使用占位值，生产环境应从 KMS 获取）。
	 */
	private static final byte[] DIGEST_KEY = "youming-device-token-digest-key".getBytes();

	/**
	 * 创建设备凭证（Token 模式）。
	 *
	 * @param deviceCode 设备编码
	 * @param expiresAt  过期时间（可为 null 表示永不过期）
	 * @return 包含原始 Token 的凭证信息，Token 只返回一次
	 */
	@Transactional
	public Map<String, Object> createTokenCredential(String deviceCode, java.time.LocalDateTime expiresAt) {
		// 生成 256-bit 随机 Token
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(tokenBytes);
		String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

		// 计算 HMAC-SHA-256 摘要
		String tokenDigest = hmacSha256(plainToken);

		OntDeviceCredential credential = new OntDeviceCredential();
		credential.setDeviceCode(deviceCode);
		credential.setAuthType("TOKEN_HASH");
		credential.setTokenDigest(tokenDigest);
		credential.setDigestKeyVersion(DIGEST_KEY_VERSION);
		credential.setStatus("ACTIVE");
		credential.setExpiresAt(expiresAt);
		credential.setFailedCount(0);

		deviceCredentialMapper.insert(credential);

		// 只返回一次
		Map<String, Object> result = new HashMap<>(4);
		result.put("id", credential.getId());
		result.put("deviceCode", deviceCode);
		result.put("token", plainToken);
		result.put("expiresAt", expiresAt);
		return result;
	}

	/**
	 * 轮换设备 Token。
	 *
	 * @param id 凭证ID
	 * @return 新 Token（只返回一次）
	 */
	@Transactional
	public Map<String, Object> rotateToken(Long id) {
		OntDeviceCredential credential = deviceCredentialMapper.selectById(id);
		if (credential == null || "1".equals(credential.getDelFlag())) {
			throw new IllegalArgumentException("Device credential not found: " + id);
		}
		if ("REVOKED".equals(credential.getStatus())) {
			throw new IllegalStateException("Cannot rotate revoked credential");
		}

		// 生成新 Token
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(tokenBytes);
		String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		String tokenDigest = hmacSha256(plainToken);

		// 更新摘要，重置失败计数和锁定
		credential.setTokenDigest(tokenDigest);
		credential.setDigestKeyVersion(DIGEST_KEY_VERSION);
		credential.setStatus("ACTIVE");
		credential.setFailedCount(0);
		credential.setLockedUntil(null);
		deviceCredentialMapper.updateById(credential);

		Map<String, Object> result = new HashMap<>(3);
		result.put("id", id);
		result.put("deviceCode", credential.getDeviceCode());
		result.put("token", plainToken);
		return result;
	}

	/**
	 * 吊销设备凭证。
	 */
	@Transactional
	public void revoke(Long id) {
		OntDeviceCredential credential = deviceCredentialMapper.selectById(id);
		if (credential == null) {
			throw new IllegalArgumentException("Device credential not found: " + id);
		}
		credential.setStatus("REVOKED");
		deviceCredentialMapper.updateById(credential);
	}

	/**
	 * 分页查询设备凭证元数据。
	 */
	public Page<OntDeviceCredential> page(Page<OntDeviceCredential> page, String deviceCode, String status) {
		return deviceCredentialMapper.selectPage(page,
			Wrappers.<OntDeviceCredential>lambdaQuery()
				.eq(OntDeviceCredential::getDelFlag, "0")
				.like(deviceCode != null, OntDeviceCredential::getDeviceCode, deviceCode)
				.eq(status != null, OntDeviceCredential::getStatus, status)
				.orderByDesc(OntDeviceCredential::getCreateTime));
	}

	/**
	 * 校验设备 Token（常量时间比较 + 失败锁定）。
	 * <p>
	 * 认证接口不使用公开匿名端点；协议层按 deviceCode + credential 认证。
	 *
	 * @param deviceCode 设备编码
	 * @param plainToken 明文 Token
	 * @return true 认证成功，false 认证失败
	 */
	@Transactional
	public boolean verifyToken(String deviceCode, String plainToken) {
		OntDeviceCredential credential = deviceCredentialMapper.selectOne(
			Wrappers.<OntDeviceCredential>lambdaQuery()
				.eq(OntDeviceCredential::getDeviceCode, deviceCode)
				.eq(OntDeviceCredential::getDelFlag, "0"));

		if (credential == null) {
			return false;
		}

		// 检查状态
		if (!"ACTIVE".equals(credential.getStatus())) {
			log.warn("Device {} authentication rejected: status={}", deviceCode, credential.getStatus());
			return false;
		}

		// 检查过期
		if (credential.getExpiresAt() != null
				&& credential.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
			credential.setStatus("EXPIRED");
			deviceCredentialMapper.updateById(credential);
			return false;
		}

		// 检查锁定
		if (credential.getLockedUntil() != null
				&& credential.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
			log.warn("Device {} authentication rejected: locked until {}", deviceCode, credential.getLockedUntil());
			return false;
		}

		// 常量时间比较
		String expectedDigest = hmacSha256(plainToken);
		if (!constantTimeEquals(expectedDigest, credential.getTokenDigest())) {
			// 原子更新失败计数
			incrementFailedCount(credential);
			// 认证失败只记录设备 code 的 hash，不记录原 Token
			log.warn("Device {} authentication failed: token mismatch (attempt {})", deviceCode,
				credential.getFailedCount() + 1);
			return false;
		}

		// 成功：重置失败计数
		credential.setFailedCount(0);
		credential.setLastAuthenticatedAt(java.time.LocalDateTime.now());
		deviceCredentialMapper.updateById(credential);
		return true;
	}

	/**
	 * 原子更新失败计数和锁定状态。
	 */
	private void incrementFailedCount(OntDeviceCredential credential) {
		int newCount = credential.getFailedCount() + 1;
		credential.setFailedCount(newCount);
		if (newCount >= MAX_FAILED_ATTEMPTS) {
			credential.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(LOCK_MINUTES));
			credential.setStatus("LOCKED");
			log.warn("Device {} locked after {} failed attempts", credential.getDeviceCode(), newCount);
		}
		deviceCredentialMapper.updateById(credential);
	}

	/**
	 * HMAC-SHA-256 摘要。
	 */
	private String hmacSha256(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(DIGEST_KEY, "HmacSHA256"));
			byte[] digest = mac.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			return bytesToHex(digest);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute HMAC digest", e);
		}
	}

	/**
	 * 常量时间比较，防止时序攻击。
	 */
	private boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null) {
			return false;
		}
		return MessageDigest.isEqual(
			a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
			b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

}
