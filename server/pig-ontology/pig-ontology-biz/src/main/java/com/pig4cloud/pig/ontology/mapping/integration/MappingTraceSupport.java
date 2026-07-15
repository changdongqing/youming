/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.integration;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 映射 Trace 辅助（18-08 §14 日志与 Trace）。
 * <p>
 * 日志统一字段：
 * <pre>
 * traceId, jobId, mappingProjectId, mappingVersionId,
 * sourceId, mappingCode, phase, errorCode, durationMs
 * </pre>
 * 源记录只记录 sourceRecordKeyHash。异常栈仅内部日志可见，先经过 JDBC URL/credential 过滤器。
 * <p>
 * Trace 跨度（§14）：
 * <pre>
 * mapping.job
 *   mapping.entity.scan
 *     datasource.read.page
 *     mapping.transform.batch
 *     ontology.ingest.record
 *     outbox.append
 *   mapping.relation.scan
 *   mapping.delete.detect
 * </pre>
 * 不在 span attribute 写字面量值和 SQL 参数。
 *
 * @author youming
 */
@Slf4j
@Component
public class MappingTraceSupport {

	/**
	 * MDC key 常量。
	 */
	public static final String MDC_TRACE_ID = "traceId";

	public static final String MDC_JOB_ID = "jobId";

	public static final String MDC_PROJECT_ID = "mappingProjectId";

	public static final String MDC_VERSION_ID = "mappingVersionId";

	public static final String MDC_SOURCE_ID = "sourceId";

	public static final String MDC_MAPPING_CODE = "mappingCode";

	public static final String MDC_PHASE = "phase";

	public static final String MDC_ERROR_CODE = "errorCode";

	/**
	 * JDBC URL 过滤正则：匹配 jdbc:xxx://user:password@host 格式中的凭证。
	 */
	private static final Pattern JDBC_CREDENTIAL_PATTERN = Pattern.compile(
			"(jdbc:[^\\s]+://)[^:@/]+:[^@/]+@");

	/**
	 * password=xxx 过滤正则。
	 */
	private static final Pattern PASSWORD_PARAM_PATTERN = Pattern.compile(
			"(?i)(password|passwd|pwd)=[^&\\s]+");

	/**
	 * 开始一个映射作业 span，设置 MDC 上下文。
	 */
	public void startJobSpan(String traceId, Long jobId, Long projectId, Long versionId) {
		putMdc(MDC_TRACE_ID, traceId);
		putMdc(MDC_JOB_ID, jobId != null ? String.valueOf(jobId) : null);
		putMdc(MDC_PROJECT_ID, projectId != null ? String.valueOf(projectId) : null);
		putMdc(MDC_VERSION_ID, versionId != null ? String.valueOf(versionId) : null);
		putMdc(MDC_PHASE, "job");
	}

	/**
	 * 设置当前阶段。
	 */
	public void setPhase(String phase) {
		putMdc(MDC_PHASE, phase);
	}

	/**
	 * 设置映射编码和数据源。
	 */
	public void setMappingContext(String mappingCode, Long sourceId) {
		putMdc(MDC_MAPPING_CODE, mappingCode);
		putMdc(MDC_SOURCE_ID, sourceId != null ? String.valueOf(sourceId) : null);
	}

	/**
	 * 设置错误码。
	 */
	public void setError(String errorCode) {
		putMdc(MDC_ERROR_CODE, errorCode);
	}

	/**
	 * 清除 MDC 上下文（作业结束时调用）。
	 */
	public void clearSpan() {
		MDC.remove(MDC_TRACE_ID);
		MDC.remove(MDC_JOB_ID);
		MDC.remove(MDC_PROJECT_ID);
		MDC.remove(MDC_VERSION_ID);
		MDC.remove(MDC_SOURCE_ID);
		MDC.remove(MDC_MAPPING_CODE);
		MDC.remove(MDC_PHASE);
		MDC.remove(MDC_ERROR_CODE);
	}

	/**
	 * 清除阶段上下文（阶段切换时调用）。
	 */
	public void clearPhase() {
		MDC.remove(MDC_PHASE);
		MDC.remove(MDC_MAPPING_CODE);
		MDC.remove(MDC_SOURCE_ID);
		MDC.remove(MDC_ERROR_CODE);
	}

	/**
	 * 过滤日志消息中的 JDBC URL 凭证和密码参数（§14）。
	 * <p>
	 * 异常栈仅内部日志可见，先经过 JDBC URL/credential 过滤器。
	 *
	 * @param message 原始日志消息
	 * @return 过滤后的安全日志消息
	 */
	public String sanitizeLogMessage(String message) {
		if (message == null || message.isEmpty()) {
			return message;
		}
		// 过滤 JDBC URL 中的 user:password@
		String result = JDBC_CREDENTIAL_PATTERN.matcher(message).replaceAll("$1***:***@");
		// 过滤 password=xxx 参数
		result = PASSWORD_PARAM_PATTERN.matcher(result).replaceAll("$1=***");
		return result;
	}

	/**
	 * 构建 sourceRecordKeyHash 的日志安全表示。
	 * <p>
	 * sourceRecordKey 仅在管理员详情中脱敏展示（§8）。
	 */
	public String safeRecordKeyHash(String keyHash) {
		if (keyHash == null || keyHash.length() <= 8) {
			return "***";
		}
		return keyHash.substring(0, 8) + "***";
	}

	private void putMdc(String key, String value) {
		if (value != null && !value.isEmpty()) {
			MDC.put(key, value);
		}
	}

}
