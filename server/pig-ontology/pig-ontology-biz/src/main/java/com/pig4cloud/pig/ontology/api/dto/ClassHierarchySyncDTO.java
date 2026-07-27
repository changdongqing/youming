package com.pig4cloud.pig.ontology.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 建模侧同步类层级请求 DTO（FR-9，10.7）
 * <p>
 * 供建模侧推送 subClassOf 关系到镜像表 ont_class_hierarchy。建模侧是类层级权威源，
 * 治理侧只读镜像。环路校验在建模侧执行，本 DTO 仅做基本格式校验。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "建模侧同步类层级请求")
public class ClassHierarchySyncDTO {

	@Schema(description = "子类 IRI（建模侧 owl:Class，只镜像不创建）")
	@NotBlank(message = "子类 IRI 不能为空")
	private String childClassIri;

	@Schema(description = "父类 IRI")
	@NotBlank(message = "父类 IRI 不能为空")
	private String parentClassIri;

	@Schema(description = "溯源：建议来源的分类模板 template_code")
	private String sourceTemplateRef;

	@Schema(description = "所属类树标识")
	private String treeRoot;

}
