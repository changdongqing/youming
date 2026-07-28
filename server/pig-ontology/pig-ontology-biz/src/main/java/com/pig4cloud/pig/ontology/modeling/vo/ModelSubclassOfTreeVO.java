package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 类树视图节点（AC-14.1 类树展示）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "类树视图节点")
public class ModelSubclassOfTreeVO {

	@Schema(description = "类 ID")
	private Long classId;

	@Schema(description = "类 IRI")
	private String classIri;

	@Schema(description = "本地名")
	private String localName;

	@Schema(description = "标签")
	private String label;

	@Schema(description = "分类编码")
	private String classificationCode;

	@Schema(description = "子节点")
	private List<ModelSubclassOfTreeVO> children;

}
