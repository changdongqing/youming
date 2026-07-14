/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import lombok.Builder;
import lombok.Data;

/**
 * 关系摄入结果。
 *
 * @author youming
 */
@Data
@Builder
public class RelationIngestionResult {

	/** 结果状态 */
	private Status status;

	/** 关系断言ID */
	private Long relationId;

	/** 关系来源ID */
	private Long provenanceId;

	/** 错误码 */
	private String errorCode;

	/** 错误消息 */
	private String errorMessage;

	/**
	 * 关系摄入结果状态。
	 */
	public enum Status {

		/** 新建关系断言 */
		CREATED,

		/** 关系已存在，无变化 */
		UNCHANGED,

		/** 客体缺失，进入待解析 */
		PENDING,

		/** 摄入失败 */
		FAILED

	}

	public static RelationIngestionResult created(Long relationId, Long provenanceId) {
		return RelationIngestionResult.builder()
			.status(Status.CREATED)
			.relationId(relationId)
			.provenanceId(provenanceId)
			.build();
	}

	public static RelationIngestionResult unchanged(Long relationId) {
		return RelationIngestionResult.builder()
			.status(Status.UNCHANGED)
			.relationId(relationId)
			.build();
	}

	public static RelationIngestionResult pending() {
		return RelationIngestionResult.builder()
			.status(Status.PENDING)
			.build();
	}

	public static RelationIngestionResult failed(String errorCode, String errorMessage) {
		return RelationIngestionResult.builder()
			.status(Status.FAILED)
			.errorCode(errorCode)
			.errorMessage(errorMessage)
			.build();
	}

}
