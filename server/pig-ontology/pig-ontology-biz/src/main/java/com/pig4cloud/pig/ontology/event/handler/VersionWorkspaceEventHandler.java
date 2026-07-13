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
 * 版本工作区标脏消费者。
 * <p>
 * Schema 变更时标记当前工作区相对已发布版本已变化。首期只记录指标。
 *
 * @author youming
 */
@Slf4j
@Component
public class VersionWorkspaceEventHandler implements OntologyEventHandler {

	@Override
	public String consumerGroup() {
		return "ontology-version-workspace-v1";
	}

	@Override
	public Set<String> supportedEventTypes() {
		return Set.of(OntologyEventTypes.ONTOLOGY_SCHEMA_CHANGED);
	}

	@Override
	public Set<Integer> supportedVersions(String eventType) {
		return Set.of(1);
	}

	@Override
	public void handle(OntologyEventEnvelope event) {
		log.info("版本工作区标脏事件（首期仅记录）: eventId={}, type={}, ontologyId={}",
				event.getEventId(), event.getEventType(), event.getOntologyId());
	}

}
