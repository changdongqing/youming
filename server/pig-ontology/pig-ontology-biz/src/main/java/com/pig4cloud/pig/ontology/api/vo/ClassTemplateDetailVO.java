package com.pig4cloud.pig.ontology.api.vo;

import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplateRef;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类模板详情（FR-2，PRD 12.3）
 * <p>
 * 含外观合并（inherit_appearance=1 且自身为空时取父链最近非空 icon/color）+ 本节点 refs
 * （不含继承的，继承视图用独立接口 {@code /inherited}）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板详情")
public class ClassTemplateDetailVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "分类模板 id")
	private Long id;

	@Schema(description = "模板标识")
	private String templateCode;

	@Schema(description = "规范分类编码")
	private String classificationCode;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "父分类模板 id，NULL=根节点")
	private Long parentId;

	@Schema(description = "父模板标识（溯源）")
	private String parentTemplateCode;

	@Schema(description = "父分类编码（溯源）")
	private String parentClassificationCode;

	@Schema(description = "所属分类树标识")
	private String treeRoot;

	@Schema(description = "合并后的外观：图标")
	private String icon;

	@Schema(description = "合并后的外观：色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观")
	private String inheritAppearance;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@Schema(description = "同级排序")
	private Integer sortOrder;

	@Schema(description = "本节点结构骨架（不含继承的，继承视图用 /inherited 接口）")
	private List<ClassTemplateRef> refs;

	/**
	 * 从 Entity 构造（外观与父信息由 Service 层填充）。
	 * @param tpl 分类模板实体
	 * @return 详情 VO
	 */
	public static ClassTemplateDetailVO from(ClassTemplate tpl) {
		ClassTemplateDetailVO vo = new ClassTemplateDetailVO();
		vo.setId(tpl.getId());
		vo.setTemplateCode(tpl.getTemplateCode());
		vo.setClassificationCode(tpl.getClassificationCode());
		vo.setLabel(tpl.getLabel());
		vo.setLabelCn(tpl.getLabelCn());
		vo.setDescription(tpl.getDescription());
		vo.setParentId(tpl.getParentId());
		vo.setTreeRoot(tpl.getTreeRoot());
		vo.setIcon(tpl.getIcon());
		vo.setColor(tpl.getColor());
		vo.setInheritAppearance(tpl.getInheritAppearance());
		vo.setSource(tpl.getSource());
		vo.setSourceRef(tpl.getSourceRef());
		vo.setDeprecated(tpl.getDeprecated());
		vo.setSortOrder(tpl.getSortOrder());
		return vo;
	}

}
