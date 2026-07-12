/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.log.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 序列化与交换审计日志。
 *
 * @author youming
 */
@Data
@TableName("ont_serialization_log")
@Schema(description = "序列化审计日志")
@EqualsAndHashCode(callSuper = true)
public class OntSerializationLog extends Model<OntSerializationLog> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "日志ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "操作类型: EXPORT/IMPORT")
	private String operationType;

	@Schema(description = "RDF格式: TURTLE/JSON-LD/RDF-XML/N-TRIPLES")
	private String rdfFormat;

	@Schema(description = "导出范围（仅EXPORT）")
	private String exportScope;

	@Schema(description = "谓词IRI策略（仅EXPORT）")
	private String predicateStrategy;

	@Schema(description = "子树过滤目标类型ID（仅EXPORT INSTANCE_SUBTREE）")
	private Long targetTypeId;

	@Schema(description = "导出/导入的三元组数")
	private Integer tripleCount;

	@Schema(description = "文件大小（字节）")
	private Long contentSize;

	@Schema(description = "涉及的实例数")
	private Integer instanceCount;

	@Schema(description = "涉及的数据属性值数")
	private Integer dataValueCount;

	@Schema(description = "涉及的对象关系数")
	private Integer objectRelationCount;

	@Schema(description = "跳过数（导入SKIP模式）")
	private Integer skippedCount;

	@Schema(description = "失败数")
	private Integer failedCount;

	@Schema(description = "导出前置校验是否通过（仅EXPORT）")
	private String precheckPassed;

	@Schema(description = "关联的校验报告ID")
	private Long validationReportId;

	@Schema(description = "是否强制导出，1是0否")
	private String forceFlag;

	@Schema(description = "导入IRI合并模式（仅IMPORT）")
	private String importIriMergeMode;

	@Schema(description = "耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "错误信息")
	private String errorMessage;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
