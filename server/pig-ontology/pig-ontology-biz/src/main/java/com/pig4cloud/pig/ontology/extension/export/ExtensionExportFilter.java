/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.export;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 扩展模块导出资源过滤器。
 *
 * <p>各 ID 列表为空时表示不过滤该类型。
 *
 * @author youming
 */
@Data
@Builder
public class ExtensionExportFilter {

	private List<Long> entityTypeIds;

	private List<Long> dataPropertyIds;

	private List<Long> objectPropertyIds;

	private List<Long> axiomRuleIds;

	private List<Long> unitIds;

}
