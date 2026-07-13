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
 * 可视化缓存/通知刷新消费者。
 * <p>
 * 首期只记录指标，未来增加可视化缓存时再启用实际刷新逻辑。
 *
 * @author youming
 */
@Slf4j
@Component
public class VisualizationEventHandler implements OntologyEventHandler {

	@Override
	public String consumerGroup() {
		return "ontology-visualization-v1";
	}

	@Override
	public Set<String> supportedEventTypes() {
		return Set.of(OntologyEventTypes.ONTOLOGY_SCHEMA_CHANGED,
				OntologyEventTypes.ONTOLOGY_INSTANCE_CHANGED);
	}

	@Override
	public Set<Integer> supportedVersions(String eventType) {
		return Set.of(1);
	}

	@Override
	public void handle(OntologyEventEnvelope event) {
		log.info("可视化刷新事件（首期仅记录）: eventId={}, type={}, aggregate={}/{}",
				event.getEventId(), event.getEventType(),
				event.getAggregateType(), event.getAggregateId());
	}

}
