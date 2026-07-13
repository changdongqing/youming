/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消费者组状态 VO。
 *
 * @author youming
 */
@Data
@Schema(description = "消费者组状态")
public class ConsumerGroupVO {

	@Schema(description = "消费者组名称")
	private String consumerGroup;

	@Schema(description = "待处理消息数（PEL）")
	private long pendingCount;

	@Schema(description = "Stream 中未消费消息数（lag）")
	private long lag;

	@Schema(description = "是否在线（有活跃消费者）")
	private boolean online;

}
