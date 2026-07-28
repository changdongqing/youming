package com.pig4cloud.pig.ontology.modeling.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * RDF 导入结果报告（AC-16.1~16.6）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "RDF 导入结果报告")
public class ImportResultVO {

	@Schema(description = "导入的类数量")
	private Integer classCount = 0;

	@Schema(description = "导入的数据属性数量")
	private Integer datatypePropertyCount = 0;

	@Schema(description = "导入的对象属性数量")
	private Integer objectPropertyCount = 0;

	@Schema(description = "导入的 subClassOf 关系数量")
	private Integer subclassOfCount = 0;

	@Schema(description = "识别到溯源的属性数（templateRef 匹配）")
	private Integer recognizedTemplateCount = 0;

	@Schema(description = "孤儿溯源数（templateRef 不存在于治理域）")
	private Integer orphanTemplateCount = 0;

	@Schema(description = "识别到单位的属性数（unitRef 匹配）")
	private Integer recognizedUnitCount = 0;

	@Schema(description = "冲突跳过数")
	private Integer skippedConflictCount = 0;

	@Schema(description = "注册表外 ont:xxx 忽略告警列表")
	private List<String> warnings = new ArrayList<>();

	@Schema(description = "错误信息（解析失败时）")
	private String error;

}
