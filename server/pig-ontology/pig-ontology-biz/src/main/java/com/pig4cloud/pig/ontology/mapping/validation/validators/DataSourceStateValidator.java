/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation.validators;

import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.validation.IssueCollector;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidationContext;
import com.pig4cloud.pig.ontology.mapping.validation.MappingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 数据源状态校验器（L2 数据源元数据，18-06 §3）。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>所有被引用的数据源状态为 ACTIVE</li>
 *   <li>数据源最近测试成功且 revision 与 lastTestRevision 一致</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
public class DataSourceStateValidator implements MappingValidator {

	@Override
	public String code() {
		return "DataSourceStateValidator";
	}

	@Override
	public int order() {
		return 20;
	}

	@Override
	public void validate(MappingValidationContext context, IssueCollector issues) {
		if (context.isPrerequisiteFailed()) {
			issues.addSkipped(code());
			return;
		}

		Set<Long> checkedSourceIds = new HashSet<>();

		// 检查实体映射引用的数据源
		for (var em : context.getEntityMappings()) {
			if (em.getSourceId() == null) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "ENTITY",
						em.getMappingCode(), "实体映射未配置数据源", "设置有效的数据源ID");
				continue;
			}
			validateDataSource(em.getSourceId(), em.getMappingCode(), context, issues, checkedSourceIds);
		}

		// 检查关系映射引用的数据源
		for (var rm : context.getRelationMappings()) {
			if (rm.getSourceId() == null) {
				issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "RELATION",
						rm.getMappingCode(), "关系映射未配置数据源", "设置有效的数据源ID");
				continue;
			}
			validateDataSource(rm.getSourceId(), rm.getMappingCode(), context, issues, checkedSourceIds);
		}

		log.debug("DataSourceStateValidator completed: {} issues", issues.getTotalCount());
	}

	private void validateDataSource(Long sourceId, String mappingCode, MappingValidationContext context,
			IssueCollector issues, Set<Long> checkedSourceIds) {
		if (!checkedSourceIds.add(sourceId)) {
			return; // 已检查过
		}

		var dataSource = context.getDataSourceMap().get(sourceId);
		if (dataSource == null) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_202.getCode(), "SOURCE",
					sourceId.toString(), "数据源不存在或已删除: " + sourceId, "注册并启用数据源");
			return;
		}

		// 数据源必须 ACTIVE
		if (!"ACTIVE".equals(dataSource.getStatus())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_201.getCode(), "SOURCE",
					dataSource.getSourceCode(), "数据源非 ACTIVE 状态: " + dataSource.getStatus(),
					"启用数据源并通过连接测试");
		}

		// 最近测试必须成功
		if (!"SUCCESS".equals(dataSource.getLastTestStatus())) {
			issues.addViolation(ValidationErrorCode.ONT_MAP_201.getCode(), "SOURCE",
					dataSource.getSourceCode(), "数据源最近连接测试未成功",
					"重新测试数据源连接");
		}

		// 测试时的 revision 必须与当前 revision 一致
		if (dataSource.getRevision() != null && dataSource.getLastTestRevision() != null
				&& !dataSource.getRevision().equals(dataSource.getLastTestRevision())) {
			issues.addWarning(ValidationErrorCode.ONT_MAP_204.getCode(), "SOURCE",
					dataSource.getSourceCode(),
					"数据源自上次测试后配置已变更（revision=" + dataSource.getRevision()
							+ ", lastTestRevision=" + dataSource.getLastTestRevision() + "）",
					"重新测试数据源连接以刷新元数据");
		}
	}

}
