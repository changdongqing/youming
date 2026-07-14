/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import lombok.Builder;
import lombok.Data;

/**
 * 摄入上下文。
 * <p>
 * 携带作业、授权、追踪和执行元数据。预览上下文不得进入持久化写分支。
 *
 * @author youming
 */
@Data
@Builder
public class IngestionContext {

	/** 作业ID */
	private Long jobId;

	/** 记录ID（作业记录级） */
	private Long jobRecordId;

	/** 请求人ID */
	private Long requestedUserId;

	/** 请求人用户名 */
	private String requestedUsername;

	/** 授权上下文摘要 */
	private String authorizationSummary;

	/** 映射版本配置哈希 */
	private String configHash;

	/** 追踪ID */
	private String traceId;

	/** 执行时间 */
	private java.time.Instant executedAt;

	/** 是否预览 */
	private boolean preview;

	/** 是否允许受控人工覆盖 */
	private boolean allowManualOverride;

}
