package com.pig4cloud.pig.ontology.modeling.vo;

import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 类详情视图（含属性列表+父类子类+溯源，AC-11.8）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "类详情视图（含属性列表+父类子类+溯源）")
public class ModelClassDetailVO {

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "类 IRI")
	private String classIri;

	@Schema(description = "本地名")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "中文标签")
	private String labelCn;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "溯源：分类模板 template_code")
	private String templateCode;

	@Schema(description = "溯源：分类编码")
	private String classificationCode;

	@Schema(description = "图标")
	private String icon;

	@Schema(description = "颜色")
	private String color;

	@Schema(description = "数据属性列表")
	private List<ModelDatatypeProperty> datatypeProperties;

	@Schema(description = "对象属性列表（作为 domain）")
	private List<ModelObjectProperty> objectProperties;

	@Schema(description = "父类 IRI 列表（DD9 建表后填充）")
	private List<String> parentClassIris;

	@Schema(description = "子类 IRI 列表（DD9 建表后填充）")
	private List<String> childClassIris;

}
