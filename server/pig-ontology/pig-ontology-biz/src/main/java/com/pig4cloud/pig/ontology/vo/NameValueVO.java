/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 名称值对 VO（用于分布统计图表）。
 *
 * @author youming
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "名称值对")
public class NameValueVO {

	@Schema(description = "名称")
	private String name;

	@Schema(description = "值")
	private Integer value;

}
