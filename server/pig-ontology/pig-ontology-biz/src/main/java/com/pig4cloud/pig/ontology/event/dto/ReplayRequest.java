/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 受控回放请求。
 *
 * @author youming
 */
@Data
@Schema(description = "事件回放请求")
public class ReplayRequest {

	@NotBlank(message = "事件ID不能为空")
	@Schema(description = "被回放的事件ID")
	private String eventId;

	@NotBlank(message = "目标消费者组不能为空")
	@Schema(description = "目标消费者组")
	private String targetConsumerGroup;

	@NotBlank(message = "回放原因不能为空")
	@Schema(description = "回放原因")
	private String reason;

	@Schema(description = "模块36审批号（批量或高风险回放）")
	private String approvalRequestNo;

	@NotNull(message = "源回放序号不能为空")
	@Schema(description = "源回放序号（0为原始投递）")
	private Integer sourceReplayNo;

}
