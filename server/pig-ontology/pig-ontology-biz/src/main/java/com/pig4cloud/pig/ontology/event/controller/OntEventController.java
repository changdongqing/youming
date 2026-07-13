/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.event.dto.DeadLetterQuery;
import com.pig4cloud.pig.ontology.event.dto.OutboxQuery;
import com.pig4cloud.pig.ontology.event.dto.ReplayRequest;
import com.pig4cloud.pig.ontology.event.dto.ResolveRequest;
import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import com.pig4cloud.pig.ontology.event.service.EventQueryService;
import com.pig4cloud.pig.ontology.event.service.EventReplayService;
import com.pig4cloud.pig.ontology.event.vo.ConsumerGroupVO;
import com.pig4cloud.pig.ontology.event.vo.DeadLetterVO;
import com.pig4cloud.pig.ontology.event.vo.EventOverviewVO;
import com.pig4cloud.pig.ontology.event.vo.OutboxVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 事件中心管理 API。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/events")
@Tag(description = "ontology-event", name = "事件中心")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntEventController {

	private final EventQueryService queryService;

	private final EventReplayService replayService;

	@Operation(summary = "事件概览", description = "Stream/Outbox/PEL/DLQ 概览")
	@GetMapping("/overview")
	@HasPermission("ontology_event_view")
	public R<EventOverviewVO> overview() {
		return R.ok(queryService.getOverview());
	}

	@Operation(summary = "Outbox分页", description = "分页查询 Outbox 事件")
	@GetMapping("/outbox/page")
	@HasPermission("ontology_event_view")
	public R<Page<OutboxVO>> outboxPage(@ParameterObject Page<OntEventOutbox> page,
			@ParameterObject OutboxQuery query) {
		return R.ok(queryService.outboxPage(page, query.getEventType(), query.getStatus(),
				query.getOntologyId(), query.getEventId(), query.getAggregateType(),
				query.getAggregateId()));
	}

	@Operation(summary = "重试失败投递", description = "将 FAILED 的 Outbox 事件重置为 PENDING")
	@PostMapping("/outbox/{id}/retry")
	@SysLog("事件-重试投递")
	@HasPermission("ontology_event_admin")
	public R<Boolean> retryOutbox(@PathVariable Long id) {
		return replayService.retryOutbox(id);
	}

	@Operation(summary = "消费者组状态", description = "查询所有消费者组的 lag/pending 状态")
	@GetMapping("/consumers")
	@HasPermission("ontology_event_view")
	public R<List<ConsumerGroupVO>> consumers() {
		return R.ok(queryService.getConsumerGroups());
	}

	@Operation(summary = "死信分页", description = "分页查询死信记录")
	@GetMapping("/dead-letters/page")
	@HasPermission("ontology_event_view")
	public R<Page<DeadLetterVO>> deadLetterPage(@ParameterObject Page<OntEventDeadLetter> page,
			@ParameterObject DeadLetterQuery query) {
		return R.ok(queryService.deadLetterPage(page, query.getConsumerGroup(), query.getEventType(),
				query.getStatus(), query.getEventId()));
	}

	@Operation(summary = "死信回放", description = "将指定死信回放到目标消费者组")
	@PostMapping("/dead-letters/{id}/replay")
	@SysLog("事件-死信回放")
	@HasPermission("ontology_event_admin")
	public R<Integer> replayDeadLetter(@PathVariable Long id, @RequestBody(required = false) ResolveRequest body) {
		String reason = body != null && body.getRemark() != null ? body.getRemark() : "人工触发回放";
		return replayService.replayDeadLetter(id, reason);
	}

	@Operation(summary = "受控回放", description = "从 Outbox 回放指定事件到目标消费者组")
	@PostMapping("/replays")
	@SysLog("事件-受控回放")
	@HasPermission("ontology_event_admin")
	public R<Boolean> replay(@Valid @RequestBody ReplayRequest request) {
		return replayService.replayFromOutbox(request);
	}

	@Operation(summary = "死信处理", description = "人工标记死信为已处理/忽略")
	@PostMapping("/dead-letters/{id}/resolve")
	@SysLog("事件-死信处理")
	@HasPermission("ontology_event_admin")
	public R<Boolean> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
		return replayService.resolveDeadLetter(id, request.getAction());
	}

}
