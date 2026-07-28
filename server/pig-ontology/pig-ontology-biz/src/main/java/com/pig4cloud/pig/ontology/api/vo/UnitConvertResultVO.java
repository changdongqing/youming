package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 换算结果视图（FR-3 / AC-3.3）
 * <p>
 * 同量纲返回换算值；跨量纲或单位不存在返回 result=null + reason（不抛异常，跨量纲是正常分支）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "换算结果")
public class UnitConvertResultVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "换算结果；跨量纲或单位不存在时为 null")
	private BigDecimal result;

	@Schema(description = "结果为 null 时的原因（quantity kind mismatch / unit not found）")
	private String reason;

	@Schema(description = "输入值")
	private BigDecimal value;

	@Schema(description = "源单位 IRI")
	private String fromIri;

	@Schema(description = "目标单位 IRI")
	private String toIri;

}
