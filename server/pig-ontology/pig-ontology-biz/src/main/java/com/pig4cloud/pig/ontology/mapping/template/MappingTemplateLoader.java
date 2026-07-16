/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 映射模板加载器，从 classpath:mapping-templates/ 目录读取 JSON 模板文件。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappingTemplateLoader {

	private static final String TEMPLATE_DIR = "classpath:mapping-templates/*.json";

	private final ObjectMapper objectMapper;

	/**
	 * 列出所有可用模板的摘要信息。
	 * @return 模板摘要列表
	 */
	public List<MappingTemplateSummary> listTemplates() {
		List<MappingTemplateSummary> summaries = new ArrayList<>();
		for (Resource resource : resolveResources()) {
			try {
				MappingTemplateDefinition def = readResource(resource);
				MappingTemplateSummary summary = new MappingTemplateSummary();
				summary.setTemplateCode(def.getTemplateCode());
				summary.setTemplateName(def.getTemplateName());
				summary.setDescription(def.getDescription());
				summary.setVersion(def.getVersion());
				summary.setNamespacePrefix(def.getNamespacePrefix());
				summary.setEntityMappingCount(
						def.getEntityMappings() != null ? def.getEntityMappings().size() : 0);
				summary.setRelationMappingCount(
						def.getRelationMappings() != null ? def.getRelationMappings().size() : 0);
				summaries.add(summary);
			}
			catch (IOException ex) {
				log.warn("加载映射模板失败: {}", resource.getFilename(), ex);
			}
		}
		return summaries;
	}

	/**
	 * 加载指定编码的模板完整定义。
	 * @param templateCode 模板编码
	 * @return 模板定义
	 * @throws IllegalArgumentException 模板不存在或加载失败
	 */
	public MappingTemplateDefinition loadTemplate(String templateCode) {
		for (Resource resource : resolveResources()) {
			try {
				MappingTemplateDefinition def = readResource(resource);
				if (templateCode.equals(def.getTemplateCode())) {
					return def;
				}
			}
			catch (IOException ex) {
				log.warn("加载映射模板失败: {}", resource.getFilename(), ex);
			}
		}
		throw new IllegalArgumentException("映射模板不存在: " + templateCode);
	}

	private Resource[] resolveResources() {
		try {
			return new PathMatchingResourcePatternResolver().getResources(TEMPLATE_DIR);
		}
		catch (IOException ex) {
			log.warn("扫描映射模板目录失败", ex);
			return new Resource[0];
		}
	}

	private MappingTemplateDefinition readResource(Resource resource) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			return objectMapper.readValue(is, MappingTemplateDefinition.class);
		}
	}

}
