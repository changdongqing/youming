package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 继承视图单属性（FR-2，NFR-14）
 * <p>
 * 继承视图合并父链 propertyRefs 后的每条属性，带 source 三态标记供建模侧/前端区分展示：
 * <ul>
 * <li>{@code node} - 本节点新增</li>
 * <li>{@code inherited} - 继承自父</li>
 * <li>{@code overridden} - 覆盖父同名（AC-2.5）</li>
 * </ul>
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "继承视图单属性")
public class InheritedPropertyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "-> ont_property_template.template_code")
	private String propertyTemplateCode;

	@Schema(description = "property / relationship")
	private String refType;

	@Schema(description = "注入顺序（合并后重排）")
	private Integer sortOrder;

	@Schema(description = "来源三态：node=本节点新增 / inherited=继承自父 / overridden=覆盖父同名")
	private String source;

	@Schema(description = "贡献该属性的分类模板 id（溯源用）")
	private Long sourceClassTemplateId;

	@Schema(description = "贡献该属性的分类模板 template_code（溯源用）")
	private String sourceClassTemplateCode;

}
