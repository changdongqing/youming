/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 审计哈希链服务。
 * <p>
 * 1. chain_scope 为 yyyyMMdd:ontologyId
 * 2. 插入前用 PostgreSQL transaction advisory lock 锁定该 scope
 * 3. 读取上一条 chain_seq/current_hash
 * 4. 规范化字段并计算 SHA-256
 * 5. 插入下一序号
 * 6. 每日根哈希由模块 37 导出到独立只写存储
 * <p>
 * 链式哈希只能检测普通篡改，不得宣传为绝对不可篡改。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditChainService {

	private final JdbcTemplate jdbcTemplate;

	private static final DateTimeFormatter SCOPE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	/**
	 * 在审计日志插入前，获取哈希链的 prev_hash 和下一个序号。
	 * <p>
	 * 使用 PostgreSQL advisory lock 确保并发安全。
	 *
	 * @param ontologyId 本体工程ID
	 * @return 哈希链上下文（prev_hash + chain_seq）
	 */
	public ChainContext acquireChainContext(Long ontologyId) {
		String chainScope = buildChainScope(ontologyId);

		// PostgreSQL advisory lock ( bigint key: hash of scope )
		long lockKey = chainScope.hashCode();
		jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(?)", Object.class, lockKey);

		// 查询当前 scope 的最后一条记录
		Optional<ChainContext> lastOpt = jdbcTemplate.query(
			"SELECT chain_seq, current_hash FROM ont_data_access_log "
					+ "WHERE chain_scope = ? ORDER BY chain_seq DESC LIMIT 1",
			(rs, rowNum) -> new ChainContext(rs.getString("current_hash"), rs.getLong("chain_seq") + 1),
			chainScope).stream().findFirst();

		return lastOpt.orElse(new ChainContext(null, 1L));
	}

	/**
	 * 计算当前记录的哈希。
	 * <p>
	 * 规范化字段顺序：chain_scope|chain_seq|username|access_type|resource_type|resource_ref|decision|outcome|prev_hash
	 */
	public String computeHash(String chainScope, Long chainSeq, String username, String accessType,
			String resourceType, String resourceRef, String decision, String outcome, String prevHash) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String normalized = String.join("|",
				nullSafe(chainScope),
				chainSeq.toString(),
				nullSafe(username),
				nullSafe(accessType),
				nullSafe(resourceType),
				nullSafe(resourceRef),
				nullSafe(decision),
				nullSafe(outcome),
				nullSafe(prevHash));
			byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute audit hash", e);
		}
	}

	/**
	 * 构建哈希链作用域：yyyyMMdd:ontologyId。
	 */
	public String buildChainScope(Long ontologyId) {
		return LocalDate.now().format(SCOPE_DATE_FORMAT) + ":" + (ontologyId != null ? ontologyId : "unknown");
	}

	/**
	 * 校验指定链范围的完整性。
	 * <p>
	 * 逐条验证 prev_hash 是否连续，返回第一个断链位置。
	 *
	 * @param chainScope 链作用域
	 * @return 断链位置，null 表示完整
	 */
	public String verifyChain(String chainScope) {
		// TODO: 阶段5 Controller 调用时实现完整校验逻辑
		return null;
	}

	private String nullSafe(String value) {
		return value != null ? value : "";
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * 哈希链上下文。
	 */
	public record ChainContext(String prevHash, Long chainSeq) {
	}

}
