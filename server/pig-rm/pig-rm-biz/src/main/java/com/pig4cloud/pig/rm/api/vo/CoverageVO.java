/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用例覆盖率 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "用例覆盖率VO")
public class CoverageVO {

	@Schema(description = "活跃需求总数")
	private Long totalReq;

	@Schema(description = "已关联用例的需求数")
	private Long coveredReq;

	@Schema(description = "覆盖率(%)")
	private BigDecimal coverageRate;

}
