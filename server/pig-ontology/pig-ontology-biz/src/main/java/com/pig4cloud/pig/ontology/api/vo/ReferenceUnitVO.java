package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * QUDT 单位浏览项（FR-7，AC-7.1）
 * <p>
 * 参考本体库 QUDT 浏览器的单位条目，展示 IRI/label/symbol/量纲/换算系数，供治理员浏览后导入到 ont_unit。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "QUDT 单位浏览项")
public class ReferenceUnitVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "QUDT IRI")
	private String iri;

	@Schema(description = "英文 label")
	private String label;

	@Schema(description = "符号")
	private String symbol;

	@Schema(description = "量纲 IRI")
	private String quantityKindIri;

	@Schema(description = "换算系数")
	private String conversionMultiplier;

	@Schema(description = "科学计数系数")
	private String conversionMultiplierSn;

	@Schema(description = "UCUM 编码")
	private String ucumCode;

	@Schema(description = "描述")
	private String description;

}
