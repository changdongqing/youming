package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 序列化预览结果（AC-15.8）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "序列化预览结果")
public class SerializePreviewVO {

	@Schema(description = "序列化文本（Turtle 或 OWL XML）")
	private String content;

	@Schema(description = "格式：TTL / OWL_XML")
	private String format;

	@Schema(description = "类数量")
	private Integer classCount;

	@Schema(description = "数据属性数量")
	private Integer datatypePropertyCount;

	@Schema(description = "对象属性数量")
	private Integer objectPropertyCount;

	@Schema(description = "subClassOf 关系数量")
	private Integer subclassOfCount;

}
