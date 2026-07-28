package com.pig4cloud.pig.ontology.modeling.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模板实例化请求 DTO
 * <p>
 * 用于已有空白类追加模板属性的场景（画布右键"从模板实例化"，PRD 场景三 / 10.2 POST /{id}/instantiate）。
 * 新建类时直接在 ModelClass.templateCode 中传模板编码，走 saveClass 内部实例化。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "模板实例化请求")
public class ClassInstantiateDTO {

	@Schema(description = "分类模板 template_code（必填）")
	@NotBlank(message = "分类模板编码不能为空")
	private String templateCode;

}
