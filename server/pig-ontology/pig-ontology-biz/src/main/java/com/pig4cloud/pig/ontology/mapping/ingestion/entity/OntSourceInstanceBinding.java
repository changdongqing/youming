/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion.entity;

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
 * 来源实例绑定。
 * <p>
 * 记录外部数据源记录与本体实例之间的稳定唯一绑定关系，是幂等摄入的事实基础。
 *
 * @author youming
 */
@Data
@TableName("ont_source_instance_binding")
@Schema(description = "来源实例绑定")
@EqualsAndHashCode(callSuper = true)
public class OntSourceInstanceBinding extends Model<OntSourceInstanceBinding> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "绑定ID")
	private Long id;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "当前映射版本ID")
	private Long currentMappingVersionId;

	@Schema(description = "实体映射编码")
	private String entityMappingCode;

	@Schema(description = "源对象名")
	private String sourceObject;

	@Schema(description = "规范化复合键")
	private String sourceRecordKey;

	@Schema(description = "复合键SHA-256哈希")
	private String sourceRecordKeyHash;

	@Schema(description = "绑定的实例ID")
	private Long instanceId;

	@Schema(description = "源记录更新时间")
	private LocalDateTime sourceUpdatedAt;

	@Schema(description = "内容SHA-256哈希")
	private String contentHash;

	@Schema(description = "首次发现作业ID")
	private Long firstSeenJobId;

	@Schema(description = "最近发现作业ID")
	private Long lastSeenJobId;

	@Schema(description = "首次发现时间")
	private LocalDateTime firstSeenAt;

	@Schema(description = "最近发现时间")
	private LocalDateTime lastSeenAt;

	@Schema(description = "绑定状态：ACTIVE/INACTIVE/MISSING/CONFLICT")
	private String bindingStatus;

	@Schema(description = "连续缺失次数")
	private Integer missCount;

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
