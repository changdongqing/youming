/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.handler;

import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 安全审计消费者。
 * <p>
 * 监听安全/版本/序列化事件，补充异步安全事件审计。
 * <p>
 * 注意：敏感访问审计不能只依赖异步事件，否则 Redis 不可用时会丢失合规记录。主审计仍需同步落库。
 *
 * @author youming
 */
@Slf4j
@Component
public class SecurityAuditEventHandler implements OntologyEventHandler {

	@Override
	public String consumerGroup() {
		return "ontology-security-audit-v1";
	}

	@Override
	public Set<String> supportedEventTypes() {
		return Set.of(OntologyEventTypes.ONTOLOGY_SECURITY_EVENT,
				OntologyEventTypes.ONTOLOGY_VERSION_PUBLISHED,
				OntologyEventTypes.ONTOLOGY_SERIALIZATION_COMPLETED);
	}

	@Override
	public Set<Integer> supportedVersions(String eventType) {
		return Set.of(1);
	}

	@Override
	public void handle(OntologyEventEnvelope event) {
		log.info("安全审计事件: eventId={}, type={}, actorId={}",
				event.getEventId(), event.getEventType(), event.getActorId());
	}

}
