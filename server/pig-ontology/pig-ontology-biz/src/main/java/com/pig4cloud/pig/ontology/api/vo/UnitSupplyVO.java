package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 单位供给视图（FR-5 / AC-5.4/5.7）
 * <p>
 * 供给接口的稳定化返回结构：供建模侧拉取，仅暴露建模侧需要的业务字段（含所属量纲 IRI/label，
 * 供建模侧校验同量纲），屏蔽 createBy/updateBy/delFlag 等内部审计与状态字段，避免表结构变更破坏建模侧。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "单位供给视图")
public class UnitSupplyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "QUDT IRI，建模侧据此引用")
	private String qudtIri;

	@Schema(description = "符号")
	private String symbol;

	@Schema(description = "英文 label")
	private String label;

	@Schema(description = "中文 label")
	private String labelCn;

	@Schema(description = "所属量纲 id")
	private Long quantityKindId;

	@Schema(description = "所属量纲 IRI（供建模侧校验同量纲）")
	private String quantityKindIri;

	@Schema(description = "所属量纲 label")
	private String quantityKindLabel;

	@Schema(description = "换算系数")
	private BigDecimal conversionMultiplier;

	@Schema(description = "换算偏移")
	private BigDecimal conversionOffset;

	@Schema(description = "基准单位 IRI")
	private String scalingOf;

	@Schema(description = "UCUM 编码")
	private String ucumCode;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "0/1 弃用")
	private String deprecated;

}
