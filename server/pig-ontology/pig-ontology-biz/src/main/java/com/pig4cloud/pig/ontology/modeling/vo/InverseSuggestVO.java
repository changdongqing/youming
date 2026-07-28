package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 反向关系建议结果（AC-13.5）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "反向关系建议结果")
public class InverseSuggestVO {

	@Schema(description = "建议的反向属性名")
	private String suggestedLocalName;

	@Schema(description = "建议的反向属性标签")
	private String suggestedLabel;

	@Schema(description = "建议的 domain（原 range 反转）")
	private Long suggestedDomainClassId;

	@Schema(description = "建议的 range（原 domain 反转）")
	private Long suggestedRangeClassId;

}
