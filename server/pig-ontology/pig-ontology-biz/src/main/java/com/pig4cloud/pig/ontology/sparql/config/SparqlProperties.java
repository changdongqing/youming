/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SPARQL 查询端点资源治理参数。
 * <p>
 * 默认值与最大可配置值参见设计文档 §4.1。
 * </p>
 *
 * @author youming
 */
@Data
@Component
@ConfigurationProperties(prefix = "sparql")
public class SparqlProperties {

	/** 查询文本最大长度，默认 65536，最大 262144 */
	@Min(1024)
	@Max(262144)
	private int queryMaxLength = 65536;

	/** 查询超时（毫秒），默认 10000，最大 30000 */
	@Min(1000)
	@Max(30000)
	private int timeoutMs = 10000;

	/** 默认行上限（用户未写 LIMIT 时使用），默认 1000，最大 10000 */
	@Min(1)
	@Max(10000)
	private int defaultRowLimit = 1000;

	/** 最大行上限（用户 LIMIT 更大时收紧到此值），默认 10000，最大 50000 */
	@Min(1)
	@Max(50000)
	private int maxRowLimit = 10000;

	/** 最大 OFFSET，防止超大 OFFSET 扫描，默认 100000，最大 1000000 */
	@Min(0)
	@Max(1000000)
	private long maxOffset = 100000;

	/** 最大并发查询数（单 JVM 信号量），默认 4，最大 16 */
	@Min(1)
	@Max(16)
	private int maxConcurrentQueries = 4;

	/** CSV 导出最大行数（仅管理员），默认 100000，最大 500000 */
	@Min(1)
	@Max(500000)
	private int exportMaxRows = 100000;

}
