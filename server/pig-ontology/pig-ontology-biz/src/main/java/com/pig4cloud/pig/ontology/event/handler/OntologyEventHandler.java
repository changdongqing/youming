/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.handler;

import com.pig4cloud.pig.ontology.event.model.OntologyEventEnvelope;

import java.util.Set;

/**
 * 领域事件处理器接口。
 * <p>
 * 每个逻辑订阅者实现此接口，声明自己的 Consumer Group、支持的事件类型和版本，
 * 并实现处理逻辑。Dispatcher 按 Consumer Group 路由到 Handler。
 *
 * @author youming
 */
public interface OntologyEventHandler {

	/**
	 * 该 Handler 所属的 Consumer Group 名称。
	 * <p>
	 * 一个逻辑订阅者一个 Group，不同业务 Handler 不得共用同一 Group。
	 * @return Consumer Group 名称
	 */
	String consumerGroup();

	/**
	 * 支持的事件类型集合。
	 * @return 事件类型集合
	 */
	Set<String> supportedEventTypes();

	/**
	 * 指定事件类型支持的版本集合。
	 * @param eventType 事件类型
	 * @return 支持的版本号集合
	 */
	Set<Integer> supportedVersions(String eventType);

	/**
	 * 处理事件。
	 * @param event 事件 Envelope
	 * @throws Exception 处理失败时抛出，Dispatcher 将保留在 PEL 等待 reclaim 重试
	 */
	void handle(OntologyEventEnvelope event) throws Exception;

}
