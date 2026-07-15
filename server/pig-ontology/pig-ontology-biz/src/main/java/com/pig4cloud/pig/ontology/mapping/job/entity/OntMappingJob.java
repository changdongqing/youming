/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.entity;

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
 * 映射作业主表（18-07 §3）。
 * <p>
 * 管理 FULL/INCREMENTAL/RETRY/RELATION_RETRY 作业的生命周期、租约、游标和计数。
 *
 * @author youming
 */
@Data
@TableName("ont_mapping_job")
@Schema(description = "映射作业")
@EqualsAndHashCode(callSuper = true)
public class OntMappingJob extends Model<OntMappingJob> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "作业ID")
	private Long id;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "运行类型: PREVIEW/FULL/INCREMENTAL/RETRY/RELATION_RETRY")
	private String runType;

	@Schema(description = "触发类型: MANUAL/SCHEDULE/API/RECOVERY")
	private String triggerType;

	@Schema(description = "作业状态: QUEUED/STARTING/RUNNING/RECOVERING/CANCELLING/CANCELLED/SUCCEEDED/PARTIAL_SUCCESS/FAILED")
	private String jobStatus;

	@Schema(description = "请求人用户名")
	private String requestedBy;

	@Schema(description = "请求人用户ID")
	private Long requestedUserId;

	@Schema(description = "授权上下文快照JSONB")
	private String authorizationSnapshot;

	@Schema(description = "执行时配置哈希")
	private String configHash;

	@Schema(description = "本体版本ID")
	private Long ontologyVersionId;

	@Schema(description = "工作区修订号")
	private Long workspaceRevision;

	@Schema(description = "游标前值JSONB")
	private String cursorBefore;

	@Schema(description = "游标后值JSONB")
	private String cursorAfter;

	@Schema(description = "当前阶段: ENTITY/RELATION/DELETE/PENDING")
	private String currentPhase;

	@Schema(description = "当前映射编码")
	private String currentMappingCode;

	@Schema(description = "当前页号")
	private Long currentPageNo;

	@Schema(description = "页大小")
	private Integer pageSize;

	@Schema(description = "总读取数")
	private Long totalRead;

	@Schema(description = "新建数")
	private Long totalCreated;

	@Schema(description = "更新数")
	private Long totalUpdated;

	@Schema(description = "未变化数")
	private Long totalUnchanged;

	@Schema(description = "跳过数")
	private Long totalSkipped;

	@Schema(description = "失败数")
	private Long totalFailed;

	@Schema(description = "关系数")
	private Long totalRelations;

	@Schema(description = "安全策略拒绝记录数")
	private Long securityDeniedCount;

	@Schema(description = "租约持有者")
	private String leaseOwner;

	@Schema(description = "租约到期时间")
	private LocalDateTime leaseUntil;

	@Schema(description = "心跳时间")
	private LocalDateTime heartbeatAt;

	@Schema(description = "是否请求取消: 0否 1是")
	private String cancelRequested;

	@Schema(description = "开始时间")
	private LocalDateTime startedAt;

	@Schema(description = "完成时间")
	private LocalDateTime finishedAt;

	@Schema(description = "错误码")
	private String errorCode;

	@Schema(description = "错误消息")
	private String errorMessage;

	@Schema(description = "追踪ID")
	private String traceId;

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
