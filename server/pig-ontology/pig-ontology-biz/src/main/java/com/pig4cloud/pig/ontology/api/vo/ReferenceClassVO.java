package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Brick 类浏览项（FR-7，AC-7.1）
 * <p>
 * 参考本体库 Brick 浏览器的类条目，展示 IRI/label/定义/父类（subClassOf），供治理员浏览后导入为分类模板。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "Brick 类浏览项")
public class ReferenceClassVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "类 IRI")
	private String iri;

	@Schema(description = "显示名（rdfs:label 或本地名）")
	private String label;

	@Schema(description = "定义（skos:definition）")
	private String definition;

	@Schema(description = "父类 IRI（主父类）")
	private String parentIri;

	@Schema(description = "是否弃用")
	private Boolean deprecated;

}
