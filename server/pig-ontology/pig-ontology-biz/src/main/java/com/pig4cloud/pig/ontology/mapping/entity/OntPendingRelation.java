/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.entity;

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
 * 待解析关系表（18-05 §6）。
 * <p>
 * 当关系映射执行时目标实例尚未创建，将关系键延迟保存，待目标绑定创建后重试解析。
 * 不保存源整行，只保存规范化关系键和两端记录键。
 *
 * @author youming
 */
@Data
@TableName("ont_pending_relation")
@Schema(description = "待解析关系")
@EqualsAndHashCode(callSuper = true)
public class OntPendingRelation extends Model<OntPendingRelation> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "待解析关系ID")
	private Long id;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "关系映射编码")
	private String relationMappingCode;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "源关系键")
	private String sourceRelationKey;

	@Schema(description = "源关系键哈希")
	private String sourceRelationKeyHash;

	@Schema(description = "主体实体映射编码")
	private String subjectEntityMappingCode;

	@Schema(description = "主体记录键")
	private String subjectRecordKey;

	@Schema(description = "主体记录键哈希")
	private String subjectRecordKeyHash;

	@Schema(description = "客体实体映射编码")
	private String objectEntityMappingCode;

	@Schema(description = "客体记录键")
	private String objectRecordKey;

	@Schema(description = "客体记录键哈希")
	private String objectRecordKeyHash;

	@Schema(description = "待解析原因: SUBJECT_MISSING / OBJECT_MISSING / BOTH_MISSING")
	private String pendingReason;

	@Schema(description = "待解析状态: PENDING / RESOLVED / FAILED / IGNORED")
	private String pendingStatus;

	@Schema(description = "重试次数")
	private Integer retryCount;

	@Schema(description = "下次重试时间")
	private LocalDateTime nextRetryAt;

	@Schema(description = "最近错误码")
	private String lastErrorCode;

	@Schema(description = "最近错误消息（脱敏）")
	private String lastErrorMessage;

	@Schema(description = "首次作业ID")
	private Long firstJobId;

	@Schema(description = "最近作业ID")
	private Long lastJobId;

	@Schema(description = "已解析关系断言ID")
	private Long resolvedRelationId;

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
