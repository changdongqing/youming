package com.pig4cloud.pig.ontology.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 分类模板新增/编辑请求 DTO（FR-2）
 * <p>
 * 承载分类模板基础字段 + 结构骨架（propertyRefs）。classificationCode 可选：
 * 不填则按编码规则自动生成（AC-8.2），手填则校验前缀一致 + 符合规则（AC-8.5）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板新增/编辑请求")
public class ClassTemplateSaveDTO {

	@Schema(description = "主键（编辑时必填）")
	private Long id;

	@Schema(description = "唯一标识，人类可读，如 pump/spray-pump")
	@NotBlank(message = "模板标识不能为空")
	private String templateCode;

	@Schema(description = "规范分类编码，不填则自动生成（AC-8.2），手填则校验规则（AC-8.5）")
	private String classificationCode;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "父分类模板 id，NULL=根节点")
	private Long parentId;

	@Schema(description = "所属分类树标识，如 equipment")
	@NotBlank(message = "分类树标识不能为空")
	private String treeRoot;

	@Schema(description = "外观：emoji 或图标类名")
	private String icon;

	@Schema(description = "外观：hex 色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观（默认 1）")
	private String inheritAppearance;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@Schema(description = "同级排序")
	private Integer sortOrder;

	@Schema(description = "结构骨架（属性/关系引用），可空")
	@Valid
	private List<RefItem> propertyRefs;

	/**
	 * 结构骨架单条引用。
	 */
	@Data
	@Schema(description = "结构骨架引用项")
	public static class RefItem {

		@Schema(description = "-> ont_property_template.template_code")
		@NotBlank(message = "属性模板标识不能为空")
		private String propertyTemplateCode;

		@Schema(description = "property / relationship")
		@NotBlank(message = "引用类型不能为空")
		private String refType;

		@Schema(description = "注入顺序")
		private Integer sortOrder;

		@Schema(description = "0/1 继承自父 vs 本节点新增")
		private String inheritFlag;
	}

}
