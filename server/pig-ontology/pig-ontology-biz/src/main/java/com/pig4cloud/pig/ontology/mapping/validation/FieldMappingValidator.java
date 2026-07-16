/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.mapping.EntityMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.transform.TransformerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字段映射校验器（18-04 §6~§10）。
 * <p>
 * 静态校验规则：
 * <ul>
 *   <li>source_kind=COLUMN 时 source_column 非空；CONSTANT 时 constant_value 非空</li>
 *   <li>目标数据属性的 domainEntityTypeId 与实体映射的 targetEntityTypeId 一致</li>
 *   <li>转换器 code 在注册表中存在</li>
 *   <li>单位分类兼容（unit 的 categoryId = 属性的 unitCategoryId）</li>
 * </ul>
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldMappingValidator {

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntEntityTypeHierarchyMapper entityTypeHierarchyMapper;

	private final OntUnitMapper unitMapper;

	private final TransformerRegistry transformerRegistry;

	/**
	 * 校验字段映射配置。
	 * @param sourceKind 来源类型 COLUMN / CONSTANT
	 * @param sourceColumn 源列名
	 * @param constantValue 常量值
	 * @param targetDataPropertyId 目标数据属性ID
	 * @param transformer 转换器编码
	 * @param unitId 固定单位ID
	 * @param entityMapping 父实体映射
	 * @throws IllegalArgumentException 如果校验失败
	 */
	public void validate(String sourceKind, String sourceColumn, String constantValue,
			Long targetDataPropertyId, String transformer, Long unitId,
			OntEntityMapping entityMapping) {
		// 1. 校验 source_kind 与 source_column / constant_value 一致性
		if ("COLUMN".equals(sourceKind)) {
			if (sourceColumn == null || sourceColumn.isBlank()) {
				throw new IllegalArgumentException("source_kind=COLUMN 时 source_column 不能为空");
			}
			if (constantValue != null) {
				throw new IllegalArgumentException("source_kind=COLUMN 时 constant_value 必须为空");
			}
		}
		else if ("CONSTANT".equals(sourceKind)) {
			if (constantValue == null || constantValue.isBlank()) {
				throw new IllegalArgumentException("source_kind=CONSTANT 时 constant_value 不能为空");
			}
			if (sourceColumn != null) {
				throw new IllegalArgumentException("source_kind=CONSTANT 时 source_column 必须为空");
			}
		}
		else {
			throw new IllegalArgumentException("非法 source_kind: " + sourceKind);
		}

		// 2. 校验目标数据属性存在
		OntDataProperty dataProperty = dataPropertyMapper.selectById(targetDataPropertyId);
		if (dataProperty == null || "1".equals(dataProperty.getDelFlag())) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_105.getMessage());
		}

		// 3. 校验数据属性的 domainEntityTypeId 与实体映射的 targetEntityTypeId 一致或存在继承关系
		Long domainId = dataProperty.getDomainEntityTypeId();
		Long targetId = entityMapping.getTargetEntityTypeId();
		if (domainId == null || (!domainId.equals(targetId) && !isSubtypeOf(targetId, domainId))) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_105.getMessage()
					+ ": 数据属性定义域与目标实体类型不一致");
		}

		// 4. 校验转换器 code 在注册表中存在
		String transformerCode = transformer != null && !transformer.isBlank() ? transformer : "IDENTITY";
		if (!transformerRegistry.isRegistered(transformerCode)) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_107.getMessage()
					+ ": 未知转换器 " + transformerCode);
		}

		// 5. 校验单位分类兼容
		if (unitId != null) {
			OntUnit unit = unitMapper.selectById(unitId);
			if (unit == null || "1".equals(unit.getDelFlag())) {
				throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_108.getMessage()
						+ ": 单位不存在");
			}
			if (dataProperty.getUnitCategoryId() != null
					&& !dataProperty.getUnitCategoryId().equals(unit.getCategoryId())) {
				throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_108.getMessage()
						+ ": 单位分类与属性要求不一致");
			}
		}
		else if (dataProperty.getUnitRefMode() != null
				&& ("REQUIRED".equals(dataProperty.getUnitRefMode())
						|| "FIXED".equals(dataProperty.getUnitRefMode()))) {
			throw new IllegalArgumentException(EntityMappingErrorCode.ONT_MAP_108.getMessage()
					+ ": 属性要求单位但未配置 unit_id");
		}
	}

	/**
	 * 判断 childId 是否是 ancestorId 的后代（直接或间接继承），通过 ont_entity_type_hierarchy BFS 向上查找。
	 * @param childId 待判定的实体类型 ID
	 * @param ancestorId 祖先实体类型 ID
	 * @return true 如果 childId 是 ancestorId 的子类型
	 */
	private boolean isSubtypeOf(Long childId, Long ancestorId) {
		Set<Long> visited = new HashSet<>();
		List<Long> queue = new java.util.ArrayList<>();
		queue.add(childId);
		visited.add(childId);

		while (!queue.isEmpty()) {
			Long currentId = queue.remove(0);
			List<OntEntityTypeHierarchy> parents = entityTypeHierarchyMapper.selectList(
					com.baomidou.mybatisplus.core.toolkit.Wrappers.<OntEntityTypeHierarchy>lambdaQuery()
							.eq(OntEntityTypeHierarchy::getChildId, currentId));
			for (OntEntityTypeHierarchy hierarchy : parents) {
				Long parentId = hierarchy.getParentId();
				if (ancestorId.equals(parentId)) {
					return true;
				}
				if (!visited.contains(parentId)) {
					visited.add(parentId);
					queue.add(parentId);
				}
			}
		}
		return false;
	}

}
