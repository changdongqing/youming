/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.vo;

import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 死信 VO。
 *
 * @author youming
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "死信事件")
public class DeadLetterVO extends OntEventDeadLetter {

	@Schema(description = "payload 摘要（前 200 字符）")
	private String payloadSummary;

}
