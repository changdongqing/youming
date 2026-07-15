/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.preview;

/**
 * 映射预览服务接口（18-06 §6）。
 * <p>
 * 预览默认只读，不写实例事实表和Outbox。
 * V1样本上限100且超时15秒，可同步返回。
 *
 * @author youming
 */
public interface MappingPreviewService {

	/**
	 * 执行映射预览。
	 * @param request 预览请求
	 * @return 预览结果（含脱敏值，不含原始整行数据）
	 */
	MappingPreviewResult preview(MappingPreviewRequest request);

}
