/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 死信分页查询参数。
 *
 * @author youming
 */
@Data
@Schema(description = "死信查询")
public class DeadLetterQuery {

	@Schema(description = "消费者组")
	private String consumerGroup;

	@Schema(description = "事件类型")
	private String eventType;

	@Schema(description = "状态：OPEN/REPLAYED/RESOLVED/IGNORED")
	private String status;

	@Schema(description = "事件ID")
	private String eventId;

}
