/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 死信人工处理请求。
 *
 * @author youming
 */
@Data
@Schema(description = "死信处理请求")
public class ResolveRequest {

	@Schema(description = "处理方式：RESOLVED/IGNORED")
	private String action;

	@Schema(description = "处理备注")
	private String remark;

}
