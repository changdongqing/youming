package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 注释属性供给视图（FR-5 / AC-5.5/5.7）
 * <p>
 * 供给接口的稳定化返回结构：供建模侧序列化器/解析器拉取全量注册表驱动读写注释属性，
 * 而非硬编码 ont: 字面量。仅暴露建模侧需要的业务字段，屏蔽 createBy/updateBy/delFlag
 * 等内部审计字段，避免表结构变更破坏建模侧契约。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "注释属性供给视图")
public class AnnotationPropertySupplyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 icon/unitRef（建模侧据此拼 ont:xxx 序列化）")
	private String localName;

	@Schema(description = "显示名")
	private String label;

	@Schema(description = "值域 XSD，如 xsd:string/xsd:boolean")
	private String rangeXsd;

	@Schema(description = "作用对象：class/datatypeProperty/objectProperty/individual/all")
	private String appliesTo;

	@Schema(description = "说明")
	private String description;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "builtin / custom")
	private String source;

}
