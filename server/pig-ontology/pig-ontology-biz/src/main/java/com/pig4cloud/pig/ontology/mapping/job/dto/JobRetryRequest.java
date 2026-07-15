/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 作业重试请求（18-07 §9.2）。
 * <p>
 * 用户可指定错误码/映射码/记录ID集合，最多1000条。
 *
 * @author youming
 */
@Data
@Schema(description = "作业重试请求")
public class JobRetryRequest {

	@Schema(description = "待重试的失败记录ID列表（最多1000条）")
	private List<Long> recordIds;

	@Schema(description = "按错误码过滤")
	private List<String> errorCodes;

	@Schema(description = "按映射编码过滤")
	private String mappingCode;

	@Schema(description = "最大错误率阈值(0-1)")
	private Double maxErrorRate;

}
