package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 反向关系建议请求 DTO（AC-13.5）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "反向关系建议请求")
public class InverseSuggestDTO {

	@Schema(description = "原属性名")
	@NotBlank(message = "属性名不能为空")
	private String localName;

	@Schema(description = "原 domain 类 ID")
	@NotNull(message = "domain 不能为空")
	private Long domainClassId;

	@Schema(description = "原 range 类 ID")
	@NotNull(message = "range 不能为空")
	private Long rangeClassId;

}
