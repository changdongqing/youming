package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 建立 subClassOf 请求 DTO
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "建立 subClassOf 请求")
public class SubclassOfSaveDTO {

	@Schema(description = "所属项目 ID")
	@NotNull(message = "项目 ID 不能为空")
	private Long projectId;

	@Schema(description = "子类 ID")
	@NotNull(message = "子类 ID 不能为空")
	private Long childClassId;

	@Schema(description = "父类 ID")
	@NotNull(message = "父类 ID 不能为空")
	private Long parentClassId;

	@Schema(description = "溯源：建议来源的分类模板 template_code（可空）")
	private String sourceTemplateRef;

}
