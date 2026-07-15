/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待解析关系详情 VO（18-05 §13）。
 * <p>
 * 脱敏返回：不返回 sourceRelationKey/subjectRecordKey/objectRecordKey 明文，
 * 只返回映射码、键摘要（hash后缀）和脱敏错误消息。
 *
 * @author youming
 */
@Data
@Schema(description = "待解析关系详情（脱敏）")
public class PendingRelationVO {

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

	@Schema(description = "源关系键摘要（hash后缀）")
	private String sourceRelationKeyDigest;

	@Schema(description = "主体实体映射编码")
	private String subjectEntityMappingCode;

	@Schema(description = "主体记录键摘要（hash后缀）")
	private String subjectRecordKeyDigest;

	@Schema(description = "客体实体映射编码")
	private String objectEntityMappingCode;

	@Schema(description = "客体记录键摘要（hash后缀）")
	private String objectRecordKeyDigest;

	@Schema(description = "待解析原因")
	private String pendingReason;

	@Schema(description = "待解析状态")
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

	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

}
