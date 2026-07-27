package com.pig4cloud.pig.ontology.api.vo;

import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类模板树节点（FR-2，PRD 12.3）
 * <p>
 * 左树右表的左侧分类树节点。显示「编码 + label」+ 合并父外观后的 icon/color。
 * children 由前端 handleTree 或后端组装（parentId 自引用 → 树）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板树节点")
public class ClassTemplateNodeVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "分类模板 id")
	private Long id;

	@Schema(description = "模板标识")
	private String templateCode;

	@Schema(description = "规范分类编码，如 30-01-01")
	private String classificationCode;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "父分类模板 id，NULL=根节点")
	private Long parentId;

	@Schema(description = "所属分类树标识")
	private String treeRoot;

	@Schema(description = "合并父外观后的图标（inherit_appearance=1 且自身空时取父）")
	private String icon;

	@Schema(description = "合并父外观后的色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观")
	private String inheritAppearance;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识，如 brick")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@Schema(description = "同级排序")
	private Integer sortOrder;

	@Schema(description = "子节点")
	private List<ClassTemplateNodeVO> children;

	/**
	 * 从 Entity 转换（不合并外观，外观合并由 Service 层处理）。
	 * @param tpl 分类模板实体
	 * @return 节点 VO
	 */
	public static ClassTemplateNodeVO from(ClassTemplate tpl) {
		ClassTemplateNodeVO vo = new ClassTemplateNodeVO();
		vo.setId(tpl.getId());
		vo.setTemplateCode(tpl.getTemplateCode());
		vo.setClassificationCode(tpl.getClassificationCode());
		vo.setLabel(tpl.getLabel());
		vo.setLabelCn(tpl.getLabelCn());
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
