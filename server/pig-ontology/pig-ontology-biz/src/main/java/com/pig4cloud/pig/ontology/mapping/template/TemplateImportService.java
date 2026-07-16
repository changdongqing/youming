/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import com.pig4cloud.pig.ontology.mapping.project.vo.MappingProjectVO;

import java.util.List;

/**
 * 映射模板导入服务。
 * <p>
 * 从 classpath:mapping-templates/ 加载 JSON 模板，批量创建映射工程 + 初始 DRAFT 版本 +
 * 实体映射 + 字段映射 + 关系映射，实现一键导入。
 *
 * @author youming
 */
public interface TemplateImportService {

	/**
	 * 列出所有可用模板。
	 * @return 模板摘要列表
	 */
	List<MappingTemplateSummary> listTemplates();

	/**
	 * 从模板创建映射工程（含全部映射配置）。
	 * @param request 导入请求（模板编码 + 数据源ID + 可选编码覆盖）
	 * @return 创建的映射工程详情
	 */
	MappingProjectVO importTemplate(TemplateImportRequest request);

}
