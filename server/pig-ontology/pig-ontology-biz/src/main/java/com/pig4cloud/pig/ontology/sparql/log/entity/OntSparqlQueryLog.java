/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.log.entity;

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

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SPARQL 查询日志实体。
 * <p>
 * 兼具"用户查询历史"和"执行审计摘要"功能，不是不可删除的合规审计账本。
 * 合规审计由模块 36 的 ont_data_access_log 承担。
 * </p>
 *
 * @author youming
 */
@Data
@TableName("ont_sparql_query_log")
@Schema(description = "SPARQL查询日志")
@EqualsAndHashCode(callSuper = true)
public class OntSparqlQueryLog extends Model<OntSparqlQueryLog> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键ID")
	private Long id;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "查询类型：SELECT/ASK")
	private String queryType;

	@Schema(description = "完整查询文本（默认不落库）")
	private String queryText;

	@Schema(description = "查询预览（去除字符串字面量后的受控摘要）")
	private String queryPreview;

	@Schema(description = "规范化查询文本的SHA-256")
	private String queryHash;

	@Schema(description = "结果格式：JSON/CSV")
	private String resultFormat;

	@Schema(description = "结果行数")
	private Integer rowCount;

	@Schema(description = "执行耗时（毫秒）")
	private Long durationMs;

	@Schema(description = "结果是否截断：0/1")
	private String truncated;

	@Schema(description = "执行状态：SUCCESS/REJECTED/TIMEOUT/FAILED")
	private String status;

	@Schema(description = "稳定错误码")
	private String errorCode;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记：0/1")
	private String delFlag;

}
