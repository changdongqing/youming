/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.template;

import com.pig4cloud.pig.ontology.sparql.vo.SparqlTemplateVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPARQL 预置查询模板提供器。
 * <p>
 * 模板以只读资源文件维护，不建表。占位符格式为 ${varName}，
 * 前端填充时必须作为完整 IRI 放入 <...>，不得拼接到前缀本地名后。
 * </p>
 *
 * @author youming
 */
@Slf4j
@Component
public class SparqlTemplateProvider {

	private static final String TEMPLATE_LOCATION = "classpath:sparql-templates/*.rq";

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

	/** 模板元数据（ID -> 名称/说明/查询类型） */
	private static final Map<String, String[]> TEMPLATE_META = Map.of(
			"subclass-tree", new String[] { "子类层次", "SELECT", "查询指定类的传递子类" },
			"instance-properties", new String[] { "实例属性", "SELECT", "查询实例全部断言" },
			"one-hop-neighbors", new String[] { "一跳邻居", "SELECT", "查询实例入边和出边" },
			"type-statistics", new String[] { "类型统计", "SELECT", "按 rdf:type 统计实例数" },
			"iri-label-search", new String[] { "IRI/标签搜索", "SELECT", "按 IRI 或 rdfs:label 检索" },
			"appendix-d-summary", new String[] { "附录D摘要", "SELECT", "按附录D实例IRI前缀统计" });

	private final List<SparqlTemplateVO> templates = new ArrayList<>();

	@PostConstruct
	public void init() {
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resolver.getResources(TEMPLATE_LOCATION);

			for (Resource resource : resources) {
				String filename = resource.getFilename();
				if (filename == null) {
					continue;
				}
				String templateId = filename.substring(0, filename.length() - 3); // 去掉 .rq
				String[] meta = TEMPLATE_META.get(templateId);
				if (meta == null) {
					log.warn("SPARQL模板无元数据定义: {}", templateId);
					continue;
				}

				String content;
				try (InputStream is = resource.getInputStream()) {
					content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				}

				SparqlTemplateVO vo = new SparqlTemplateVO();
				vo.setId(templateId);
				vo.setName(meta[0]);
				vo.setQueryType(meta[1]);
				vo.setDescription(meta[2]);
				vo.setQuery(content);
				vo.setPlaceholders(extractPlaceholders(content));
				templates.add(vo);
			}

			templates.sort(Comparator.comparing(SparqlTemplateVO::getId));
			log.info("SPARQL预置模板加载完成: {} 个", templates.size());
		}
		catch (Exception e) {
			log.error("SPARQL预置模板加载失败", e);
		}
	}

	/**
	 * 获取所有预置模板。
	 * @return 模板列表
	 */
	public List<SparqlTemplateVO> getTemplates() {
		return templates;
	}

	/**
	 * 从查询文本中提取占位符变量名。
	 */
	private List<String> extractPlaceholders(String content) {
		List<String> placeholders = new ArrayList<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
		while (matcher.find()) {
			String name = matcher.group(1);
			if (!placeholders.contains(name)) {
				placeholders.add(name);
			}
		}
		return placeholders;
	}

}
