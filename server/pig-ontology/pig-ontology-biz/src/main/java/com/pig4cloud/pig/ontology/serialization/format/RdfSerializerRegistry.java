/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.format;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * RDF 序列化器注册表。
 * <p>
 * 启动时扫描所有 {@link RdfSerializer} Bean，按格式注册。
 * </p>
 *
 * @author youming
 */
@Component
public class RdfSerializerRegistry {

	private final Map<RdfFormat, RdfSerializer> serializers = new EnumMap<>(RdfFormat.class);

	public RdfSerializerRegistry(List<RdfSerializer> serializerList) {
		for (RdfSerializer serializer : serializerList) {
			RdfFormat format = serializer.getFormat();
			Assert.state(!serializers.containsKey(format),
				"重复的 RDF 序列化器: " + format);
			serializers.put(format, serializer);
		}
	}

	/**
	 * 按格式查找序列化器。
	 * @param format RDF 格式
	 * @return 序列化器实例
	 * @throws IllegalArgumentException 不支持的格式
	 */
	public RdfSerializer getSerializer(RdfFormat format) {
		RdfSerializer serializer = serializers.get(format);
		if (serializer == null) {
			throw new IllegalArgumentException("不支持的 RDF 格式: " + format);
		}
		return serializer;
	}

}
