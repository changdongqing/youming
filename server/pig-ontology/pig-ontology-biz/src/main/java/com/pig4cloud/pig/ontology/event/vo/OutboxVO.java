/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.vo;

import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Outbox VO，隐藏完整 payload 摘要。
 *
 * @author youming
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Outbox事件")
public class OutboxVO extends OntEventOutbox {

	@Schema(description = "payload 摘要（前 200 字符）")
	private String payloadSummary;

}
