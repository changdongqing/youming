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
 * SPARQL 缓存失效消费者。
 * <p>
 * 首期模块 13 无共享 Model 缓存，此 Handler 只记录指标，未来增加缓存时再启用实际失效逻辑。
 *
 * @author youming
 */
@Slf4j
@Component
public class SparqlCacheEventHandler implements OntologyEventHandler {

	@Override
	public String consumerGroup() {
		return "ontology-sparql-cache-v1";
	}

	@Override
	public Set<String> supportedEventTypes() {
		return Set.of(OntologyEventTypes.ONTOLOGY_SCHEMA_CHANGED,
				OntologyEventTypes.ONTOLOGY_INSTANCE_CHANGED,
				OntologyEventTypes.ONTOLOGY_VERSION_PUBLISHED);
	}

	@Override
	public Set<Integer> supportedVersions(String eventType) {
		return Set.of(1);
	}

	@Override
	public void handle(OntologyEventEnvelope event) {
		log.info("SPARQL 缓存失效事件（首期仅记录）: eventId={}, type={}, aggregate={}/{}",
				event.getEventId(), event.getEventType(),
				event.getAggregateType(), event.getAggregateId());
	}

}
