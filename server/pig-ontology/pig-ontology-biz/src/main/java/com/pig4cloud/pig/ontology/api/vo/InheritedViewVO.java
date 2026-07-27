package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类模板继承视图（FR-2，AC-2.3/2.5，NFR-12/14）
 * <p>
 * 合并父链 propertyRefs 后的全部属性（去重，子覆盖父）+ 合并后的外观（inherit_appearance=1
 * 且自身为空时取父链最近非空 icon/color）+ 父链 template_code 列表（溯源）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板继承视图")
public class InheritedViewVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "分类模板 id")
	private Long id;

	@Schema(description = "模板标识")
	private String templateCode;

	@Schema(description = "规范分类编码")
	private String classificationCode;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "合并后的外观：emoji 或图标类名")
	private String icon;

	@Schema(description = "合并后的外观：hex 色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观")
	private String inheritAppearance;

	@Schema(description = "合并后的属性清单（区分 node/inherited/overridden）")
	private List<InheritedPropertyVO> properties;

	@Schema(description = "父链 template_code 列表（根在前，本节点在末尾）")
	private List<String> parentChain;

}
