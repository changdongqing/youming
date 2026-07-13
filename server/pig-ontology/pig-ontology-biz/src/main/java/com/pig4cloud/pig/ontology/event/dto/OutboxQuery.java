/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Outbox 分页查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "Outbox查询")
public class OutboxQuery {

	@Schema(description = "事件类型")
	private String eventType;

	@Schema(description = "状态：PENDING/PROCESSING/PUBLISHED/FAILED")
	private String status;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "事件ID")
	private String eventId;

	@Schema(description = "聚合类型")
	private String aggregateType;

	@Schema(description = "聚合ID")
	private String aggregateId;

}
