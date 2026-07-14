/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 实体摄入结果。
 *
 * @author youming
 */
@Data
@Builder
public class IngestionResult {

	/** 结果状态 */
	private Status status;

	/** 实例ID */
	private Long instanceId;

	/** 来源绑定ID */
	private Long bindingId;

	/** 错误码 */
	private String errorCode;

	/** 错误消息 */
	private String errorMessage;

	/** 变更的字段映射编码集合 */
	private Set<String> changedFieldCodes;

	/**
	 * 摄入结果状态。
	 */
	public enum Status {

		/** 新建实例 */
		CREATED,

		/** 更新已有实例 */
		UPDATED,

		/** 内容未变化，跳过 */
		SKIPPED_UNCHANGED,

		/** 实例已失活 */
		DEACTIVATED,

		/** 摄入失败 */
		FAILED

	}

	public static IngestionResult created(Long instanceId, Long bindingId, Set<String> changedFieldCodes) {
		return IngestionResult.builder()
			.status(Status.CREATED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.changedFieldCodes(changedFieldCodes)
			.build();
	}

	public static IngestionResult updated(Long instanceId, Long bindingId, Set<String> changedFieldCodes) {
		return IngestionResult.builder()
			.status(Status.UPDATED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.changedFieldCodes(changedFieldCodes)
			.build();
	}

	public static IngestionResult skipped(Long instanceId, Long bindingId) {
		return IngestionResult.builder()
			.status(Status.SKIPPED_UNCHANGED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.build();
	}

	public static IngestionResult failed(String errorCode, String errorMessage) {
		return IngestionResult.builder()
			.status(Status.FAILED)
			.errorCode(errorCode)
			.errorMessage(errorMessage)
			.build();
	}

}
