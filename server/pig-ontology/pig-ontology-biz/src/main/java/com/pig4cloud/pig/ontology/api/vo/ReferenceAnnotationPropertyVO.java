package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * CCO 注释属性浏览项（FR-7，AC-7.1）
 * <p>
 * 参考本体库 CCO 浏览器的注释属性条目，展示 IRI/label/定义，供治理员参考（仅浏览不导入）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "CCO 注释属性浏览项")
public class ReferenceAnnotationPropertyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "注释属性 IRI")
	private String iri;

	@Schema(description = "显示名（rdfs:label）")
	private String label;

	@Schema(description = "定义（skos:definition）")
	private String definition;

}
