/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 领域事件模型，由业务 Service 构建后传给 {@code OntDomainEventPublisher}。
 * <p>
 * 只包含业务语义字段；eventId、replayNo、source 等传输字段由 Publisher / Relay 补充。
 *
 * @author youming
 */
@Data
@Builder
@Schema(description = "领域事件")
public class OntologyDomainEvent {

	@Schema(description = "事件类型，如 ONTOLOGY_INSTANCE_CHANGED")
	private String eventType;

	@Schema(description = "事件契约版本号，默认1")
	@Builder.Default
	private Integer eventVersion = 1;

	@Schema(description = "所属本体工程ID")
	private Long ontologyId;

	@Schema(description = "聚合类型，如 ENTITY_INSTANCE")
	private String aggregateType;

	@Schema(description = "聚合ID，字符串兼容雪花ID/IRI")
	private String aggregateId;

	@Schema(description = "聚合修订号，有顺序要求的聚合必填")
	private Long aggregateRevision;

	@Schema(description = "操作类型，如 CREATED/UPDATED/DELETED")
	private String operation;

	@Schema(description = "事件发生时间（UTC）")
	@Builder.Default
	private Instant occurredAt = Instant.now();

	@Schema(description = "触发操作的用户ID")
	private Long actorId;

	@Schema(description = "链路追踪ID")
	private String traceId;

	@Schema(description = "事件负载，只放消费所需最小信息")
	private Map<String, Object> payload;

	@Schema(description = "事件元数据")
	private Map<String, Object> metadata;

}
