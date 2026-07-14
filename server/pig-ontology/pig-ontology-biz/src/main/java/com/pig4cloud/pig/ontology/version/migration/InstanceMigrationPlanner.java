/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.migration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.version.vo.MigrationPreviewVO;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 迁移计划预览和校验。
 * <p>
 * 支持设计文档 §5.4 的 6 种声明式规则：
 * SET_DEFAULT_LITERAL、COPY_LITERAL、MAP_LITERAL、DROP_ASSERTION、RETYPE_INSTANCE、COPY_RELATION。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstanceMigrationPlanner {

	/**
	 * 支持的迁移规则类型。
	 */
	public static final String SET_DEFAULT_LITERAL = "SET_DEFAULT_LITERAL";

	public static final String COPY_LITERAL = "COPY_LITERAL";

	public static final String MAP_LITERAL = "MAP_LITERAL";

	public static final String DROP_ASSERTION = "DROP_ASSERTION";

	public static final String RETYPE_INSTANCE = "RETYPE_INSTANCE";

	public static final String COPY_RELATION = "COPY_RELATION";

	private static final Set<String> SUPPORTED_RULES = Set.of(SET_DEFAULT_LITERAL, COPY_LITERAL, MAP_LITERAL,
			DROP_ASSERTION, RETYPE_INSTANCE, COPY_RELATION);

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntEntityInstanceMapper entityInstanceMapper;

	private final ObjectMapper objectMapper;

	/**
	 * 预览迁移影响。
	 * @param ontologyId 本体工程ID
	 * @param diff 版本 diff 结果
	 * @param migrationPlanJson 迁移计划 JSON
	 * @return 迁移预览
	 */
	public MigrationPreviewVO preview(Long ontologyId, VersionDiffVO diff, String migrationPlanJson) {
		MigrationPreviewVO result = new MigrationPreviewVO();
		result.setRules(new ArrayList<>());

		// 统计受影响实例总数
		long totalInstances = entityInstanceMapper.selectCount(
			Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId));
		result.setAffectedInstanceCount(totalInstances);

		if (migrationPlanJson != null && !migrationPlanJson.isBlank()) {
			try {
				JsonNode plan = objectMapper.readTree(migrationPlanJson);
				JsonNode rules = plan.get("rules");
				if (rules != null && rules.isArray()) {
					for (JsonNode rule : rules) {
						MigrationPreviewVO.MigrationRulePreview preview = new MigrationPreviewVO.MigrationRulePreview();
						preview.setRuleType(textOrNull(rule, "ruleType"));
						preview.setDescription(textOrNull(rule, "description"));
						preview.setAffectedCount(estimateAffectedCount(ontologyId, rule));
						result.getRules().add(preview);
					}
				}
			}
			catch (Exception e) {
				log.warn("解析迁移计划失败: {}", e.getMessage());
			}
		}

		// 如果有 BREAKING 变更但没有迁移计划，根据 diff 自动推断
		if (diff != null && diff.getRemoved() != null && !diff.getRemoved().isEmpty()) {
			for (VersionDiffVO.DiffResource res : diff.getRemoved()) {
				if ("DATA_PROPERTY".equals(res.getResourceType())) {
					long count = countInstancesUsingDataProperty(ontologyId, res.getIri());
					if (count > 0) {
						MigrationPreviewVO.MigrationRulePreview preview = new MigrationPreviewVO.MigrationRulePreview();
						preview.setRuleType(DROP_ASSERTION);
						preview.setDescription("删除数据属性 " + res.getIri() + "，需清理 " + count + " 条实例断言");
						preview.setAffectedCount(count);
						result.getRules().add(preview);
					}
				}
				if ("ENTITY_TYPE".equals(res.getResourceType())) {
					long count = countInstancesOfType(ontologyId, res.getIri());
					if (count > 0) {
						MigrationPreviewVO.MigrationRulePreview preview = new MigrationPreviewVO.MigrationRulePreview();
						preview.setRuleType(RETYPE_INSTANCE);
						preview.setDescription("删除实体类型 " + res.getIri() + "，需迁移 " + count + " 个实例");
						preview.setAffectedCount(count);
						result.getRules().add(preview);
					}
				}
			}
		}

		return result;
	}

	/**
	 * 校验迁移计划是否合法。
	 * @param migrationPlanJson 迁移计划 JSON
	 * @return 错误消息列表，空列表表示合法
	 */
	public List<String> validate(String migrationPlanJson) {
		List<String> errors = new ArrayList<>();
		if (migrationPlanJson == null || migrationPlanJson.isBlank()) {
			return errors;
		}
		try {
			JsonNode plan = objectMapper.readTree(migrationPlanJson);
			JsonNode rules = plan.get("rules");
			if (rules == null || !rules.isArray()) {
				errors.add("迁移计划缺少 rules 数组");
				return errors;
			}
			for (JsonNode rule : rules) {
				String ruleType = textOrNull(rule, "ruleType");
				if (ruleType == null || !SUPPORTED_RULES.contains(ruleType)) {
					errors.add("不支持的迁移规则类型: " + ruleType);
				}
			}
		}
		catch (Exception e) {
			errors.add("迁移计划JSON解析失败: " + e.getMessage());
		}
		return errors;
	}

	private long estimateAffectedCount(Long ontologyId, JsonNode rule) {
		String ruleType = textOrNull(rule, "ruleType");
		if (ruleType == null) {
			return 0L;
		}
		return switch (ruleType) {
			case DROP_ASSERTION -> {
				String propIri = textOrNull(rule, "propertyIri");
				yield propIri != null ? countInstancesUsingDataProperty(ontologyId, propIri) : 0L;
			}
			case RETYPE_INSTANCE -> {
				String typeIri = textOrNull(rule, "sourceTypeIri");
				yield typeIri != null ? countInstancesOfType(ontologyId, typeIri) : 0L;
			}
			default -> 0L;
		};
	}

	private long countInstancesUsingDataProperty(Long ontologyId, String propertyIri) {
		OntDataProperty prop = dataPropertyMapper.selectOne(
			Wrappers.<OntDataProperty>lambdaQuery().eq(OntDataProperty::getOntologyId, ontologyId)
				.eq(OntDataProperty::getIri, propertyIri));
		if (prop == null) {
			return 0L;
		}
		// 通过 OntInstanceDataValue 间接统计（简化首期实现）
		return 0L;
	}

	private long countInstancesOfType(Long ontologyId, String typeIri) {
		OntEntityType type = entityTypeMapper.selectOne(
			Wrappers.<OntEntityType>lambdaQuery().eq(OntEntityType::getOntologyId, ontologyId)
				.eq(OntEntityType::getIri, typeIri));
		if (type == null) {
			return 0L;
		}
		return entityInstanceMapper.selectCount(
			Wrappers.<OntEntityInstance>lambdaQuery().eq(OntEntityInstance::getOntologyId, ontologyId)
				.eq(OntEntityInstance::getRdfTypeId, type.getId()));
	}

	private String textOrNull(JsonNode node, String field) {
		if (node == null) {
			return null;
		}
		JsonNode val = node.get(field);
		return val != null && !val.isNull() ? val.asText() : null;
	}

}
