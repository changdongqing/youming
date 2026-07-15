/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.preview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 映射预览请求（18-06 §6）。
 * <p>
 * 样本模式：
 * <ul>
 *   <li>FIRST_N：按主键升序前N条，确定性最强</li>
 *   <li>KEYS：用户提交指定记录键</li>
 *   <li>RANDOM：V1不实现</li>
 * </ul>
 *
 * @author youming
 */
@Data
@Schema(description = "映射预览请求")
public class MappingPreviewRequest {

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "实体映射编码列表（可选，为空则预览全部启用映射）")
	private List<String> entityMappingCodes;

	@Schema(description = "样本数量，上限100")
	private Integer sampleSize;

	@Schema(description = "样本模式: FIRST_N / KEYS（V1不支持RANDOM）")
	private String sampleMode;

	@Schema(description = "起始键（KEYS模式时使用）")
	private Map<String, String> startKeys;

}
