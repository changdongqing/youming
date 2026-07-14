/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import lombok.Builder;
import lombok.Data;

/**
 * 实例失活结果。
 *
 * @author youming
 */
@Data
@Builder
public class DeactivationResult {

	/** 结果状态 */
	private Status status;

	/** 实例ID */
	private Long instanceId;

	/** 绑定ID */
	private Long bindingId;

	/** 错误码 */
	private String errorCode;

	/** 错误消息 */
	private String errorMessage;

	/**
	 * 失活结果状态。
	 */
	public enum Status {

		/** 标记为MISSING */
		MARKED_MISSING,

		/** 实例已失活 */
		DEACTIVATED,

		/** 实例已软删除 */
		SOFT_DELETED,

		/** 因冲突阻止，需人工审查 */
		BLOCKED,

		/** 失活失败 */
		FAILED

	}

	public static DeactivationResult markedMissing(Long instanceId, Long bindingId) {
		return DeactivationResult.builder()
			.status(Status.MARKED_MISSING)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.build();
	}

	public static DeactivationResult deactivated(Long instanceId, Long bindingId) {
		return DeactivationResult.builder()
			.status(Status.DEACTIVATED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.build();
	}

	public static DeactivationResult softDeleted(Long instanceId, Long bindingId) {
		return DeactivationResult.builder()
			.status(Status.SOFT_DELETED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.build();
	}

	public static DeactivationResult blocked(Long instanceId, Long bindingId, String errorMessage) {
		return DeactivationResult.builder()
			.status(Status.BLOCKED)
			.instanceId(instanceId)
			.bindingId(bindingId)
			.errorMessage(errorMessage)
			.build();
	}

	public static DeactivationResult failed(String errorCode, String errorMessage) {
		return DeactivationResult.builder()
			.status(Status.FAILED)
			.errorCode(errorCode)
			.errorMessage(errorMessage)
			.build();
	}

}
