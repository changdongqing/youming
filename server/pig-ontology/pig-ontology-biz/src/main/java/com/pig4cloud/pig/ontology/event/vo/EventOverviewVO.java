/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 事件中心概览 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "事件概览")
public class EventOverviewVO {

	@Schema(description = "Outbox PENDING 数量")
	private long outboxPending;

	@Schema(description = "Outbox PROCESSING 数量")
	private long outboxProcessing;

	@Schema(description = "Outbox PUBLISHED 数量")
	private long outboxPublished;

	@Schema(description = "Outbox FAILED 数量")
	private long outboxFailed;

	@Schema(description = "Stream 记录总长度")
	private long streamLength;

	@Schema(description = "死信 OPEN 数量")
	private long deadLetterOpen;

	@Schema(description = "Redis 是否可用")
	private boolean redisAvailable;

}
