/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 批量摄入结果。
 * <p>
 * 仅适用于小批次（如重试作业的≤1000条）。大批量同步作业应逐条或小批次调用 upsertEntity。
 *
 * @author youming
 */
@Data
@Builder
public class BatchIngestionResult {

	/** 成功创建数 */
	private int totalCreated;

	/** 成功更新数 */
	private int totalUpdated;

	/** 跳过未变化数 */
	private int totalSkipped;

	/** 失败数 */
	private int totalFailed;

	/** 失败结果列表 */
	private List<IngestionResult> failures;

	/** 全部结果列表 */
	private List<IngestionResult> results;

	/**
	 * 是否全部成功。
	 * @return 无失败返回true
	 */
	public boolean allSucceeded() {
		return totalFailed == 0;
	}

}
