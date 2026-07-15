/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 关系键预览结果 VO（18-05 §13）。
 *
 * @author youming
 */
@Data
@Schema(description = "关系键预览结果")
public class RelationKeyPreviewResultVO {

	@Schema(description = "主体记录键")
	private String subjectRecordKey;

	@Schema(description = "主体记录键哈希")
	private String subjectRecordKeyHash;

	@Schema(description = "客体记录键")
	private String objectRecordKey;

	@Schema(description = "客体记录键哈希")
	private String objectRecordKeyHash;

	@Schema(description = "关系键")
	private String relationKey;

	@Schema(description = "关系键哈希")
	private String relationKeyHash;

	@Schema(description = "是否成功")
	private boolean success;

	@Schema(description = "错误消息（失败时）")
	private String errorMessage;

}
