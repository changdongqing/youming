package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 量纲节点视图（FR-3）
 * <p>
 * 左侧量纲树节点：含该量纲下单位数，供前端分组浏览。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "量纲节点")
public class QuantityKindNodeVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "量纲 id")
	private Long id;

	@Schema(description = "QUDT IRI")
	private String qudtIri;

	@Schema(description = "英文 label")
	private String label;

	@Schema(description = "中文 label")
	private String labelCn;

	@Schema(description = "量纲向量")
	private String dimensionVector;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "该量纲下单位数（排除逻辑删除）")
	private Long unitCount;

}
