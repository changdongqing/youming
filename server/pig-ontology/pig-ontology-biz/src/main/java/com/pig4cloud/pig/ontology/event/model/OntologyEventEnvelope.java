/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 统一事件 Envelope，序列化为 JSON 写入 Outbox 和 Redis Stream。
 * <p>
 * 正常重复投递保持 eventId 和 replayNo 不变；人工回放递增 replayNo。
 *
 * @author youming
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一事件 Envelope")
public class OntologyEventEnvelope {

	@Schema(description = "领域事件唯一ID（UUID）")
	private String eventId;

	@Schema(description = "回放序号，0为原始投递，人工回放递增")
	@Builder.Default
	private Integer replayNo = 0;

	@Schema(description = "事件类型")
	private String eventType;

	@Schema(description = "事件契约版本号")
	private Integer eventVersion;

	@Schema(description = "事件发生时间（UTC ISO-8601 Z）")
	private Instant occurredAt;

	@Schema(description = "事件来源模块")
	private String source;

	@Schema(description = "所属本体工程ID")
	private Long ontologyId;

	@Schema(description = "聚合类型")
	private String aggregateType;

	@Schema(description = "聚合ID")
	private String aggregateId;

	@Schema(description = "聚合修订号")
	private Long aggregateRevision;

	@Schema(description = "操作类型")
	private String operation;

	@Schema(description = "触发操作的用户ID")
	private Long actorId;

	@Schema(description = "链路追踪ID")
	private String traceId;

	@Schema(description = "事件负载")
	private Map<String, Object> payload;

	@Schema(description = "事件元数据，如 targetConsumerGroup")
	private Map<String, Object> metadata;

}
