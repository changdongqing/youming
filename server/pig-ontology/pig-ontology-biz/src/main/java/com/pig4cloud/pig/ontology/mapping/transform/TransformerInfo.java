/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.transform;

import java.util.List;

/**
 * 转换器元信息（18-04 §7）。
 * <p>
 * 仅描述转换器的 code、用途和配置 Schema，不含执行逻辑。
 *
 * @author youming
 *
 * @param code 转换器编码
 * @param description 用途描述
 * @param supportedSourceTypes 支持的源 JDBC 类型族
 * @param supportedTargetTypes 支持的目标字面量类型
 * @param configSchema 配置 JSON Schema 描述
 */
public record TransformerInfo(
		String code,
		String description,
		List<String> supportedSourceTypes,
		List<String> supportedTargetTypes,
		String configSchema) {
}
