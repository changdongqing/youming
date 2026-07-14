/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.service.OntNamespaceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 命名空间服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntNamespaceServiceImpl extends ServiceImpl<OntNamespaceMapper, OntNamespace>
		implements OntNamespaceService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*$");

	private static final Pattern LOCAL_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

	private final OntUnitMapper unitMapper;

	private final OntEntityTypeMapper entityTypeMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	private final OntObjectPropertyMapper objectPropertyMapper;

	private final OntEntityInstanceMapper entityInstanceMapper;

	private final OntDomainEventPublisher eventPublisher;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntNamespace> saveNamespace(OntNamespace namespace) {
		R<OntNamespace> validation = validateNamespace(namespace, false);
		if (validation.getCode() != 0) {
			return validation;
		}
		namespace.setId(null);
		namespace.setIsBuiltin(EXTENSION);
		namespace.setIsDefault(EXTENSION);
		if (namespace.getSortOrder() == null) {
			namespace.setSortOrder(0);
		}
		this.save(namespace);
		publishSchemaChanged(namespace.getId(), null, "CREATED");
		return R.ok(namespace);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntNamespace> updateNamespace(OntNamespace namespace) {
		if (namespace.getId() == null) {
			return R.failed("命名空间ID不能为空");
		}
		OntNamespace old = this.getById(namespace.getId());
		if (old == null) {
			return R.failed("命名空间不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			OntNamespace update = new OntNamespace();
			update.setId(old.getId());
			update.setDescription(namespace.getDescription());
			update.setSortOrder(namespace.getSortOrder());
			this.updateById(update);
			publishSchemaChanged(old.getId(), null, "UPDATED");
			return R.ok(this.getById(old.getId()));
		}

		boolean uriChanged = !Objects.equals(old.getUri(), namespace.getUri());
		if (uriChanged) {
			long entityTypeCount = entityTypeMapper.selectCount(Wrappers.<OntEntityType>lambdaQuery()
				.eq(OntEntityType::getNamespaceId, old.getId()));
			if (entityTypeCount > 0) {
				return R.failed("该命名空间已被" + entityTypeCount + "个实体类型引用，命名空间URI不可修改（请使用relocate流程重算IRI）");
			}
			long dataPropertyCount = dataPropertyMapper.selectCount(Wrappers.<OntDataProperty>lambdaQuery()
				.eq(OntDataProperty::getNamespaceId, old.getId()));
			if (dataPropertyCount > 0) {
				return R.failed("该命名空间已被" + dataPropertyCount + "个数据属性引用，命名空间URI不可修改（请使用relocate流程重算IRI）");
			}
			long objectPropertyCount = objectPropertyMapper.selectCount(Wrappers.<OntObjectProperty>lambdaQuery()
				.eq(OntObjectProperty::getNamespaceId, old.getId()));
			if (objectPropertyCount > 0) {
				return R.failed("该命名空间已被" + objectPropertyCount + "个对象属性引用，命名空间URI不可修改（请使用relocate流程重算IRI）");
			}
			long instanceCount = entityInstanceMapper.selectCount(Wrappers.<OntEntityInstance>lambdaQuery()
				.eq(OntEntityInstance::getNamespaceId, old.getId()));
			if (instanceCount > 0) {
				return R.failed("该命名空间已被" + instanceCount + "个实例引用，命名空间URI不可修改（请使用relocate流程重算实例IRI）");
			}
		}
		R<OntNamespace> validation = validateNamespace(namespace, true);
		if (validation.getCode() != 0) {
			return validation;
		}
		namespace.setIsBuiltin(EXTENSION);
		this.updateById(namespace);
		publishSchemaChanged(namespace.getId(), null, "UPDATED");
		return R.ok(this.getById(namespace.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeNamespace(Long id) {
		OntNamespace namespace = this.getById(id);
		if (namespace == null) {
			return R.failed("命名空间不存在");
		}
		if (BUILTIN.equals(namespace.getIsBuiltin())) {
			return R.failed("内置命名空间不可删除");
		}
		long unitCount = unitMapper.selectCount(Wrappers.<OntUnit>lambdaQuery().eq(OntUnit::getNamespaceId, id));
		if (unitCount > 0) {
			return R.failed("该命名空间被单位条目引用，不能删除");
		}
		long entityTypeCount = entityTypeMapper.selectCount(Wrappers.<OntEntityType>lambdaQuery()
			.eq(OntEntityType::getNamespaceId, id));
		if (entityTypeCount > 0) {
			return R.failed("该命名空间被实体类型引用，不能删除");
		}
		long dataPropertyCount = dataPropertyMapper.selectCount(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getNamespaceId, id));
		if (dataPropertyCount > 0) {
			return R.failed("该命名空间被数据属性引用，不能删除");
		}
		long objectPropertyCount = objectPropertyMapper.selectCount(Wrappers.<OntObjectProperty>lambdaQuery()
			.eq(OntObjectProperty::getNamespaceId, id));
		if (objectPropertyCount > 0) {
			return R.failed("该命名空间被对象属性引用，不能删除");
		}
		long instanceCount = entityInstanceMapper.selectCount(Wrappers.<OntEntityInstance>lambdaQuery()
			.eq(OntEntityInstance::getNamespaceId, id));
		if (instanceCount > 0) {
			return R.failed("该命名空间被" + instanceCount + "个实例引用，不能删除");
		}
		this.removeById(id);
		publishSchemaChanged(id, null, "DELETED");
		return R.ok(true);
	}

	/**
	 * 发布 Schema 变更事件到 Outbox。
	 */
	private void publishSchemaChanged(Long aggregateId, Long ontologyId, String operation) {
		eventPublisher.append(OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.ONTOLOGY_SCHEMA_CHANGED)
			.ontologyId(ontologyId)
			.aggregateType("NAMESPACE")
			.aggregateId(aggregateId != null ? aggregateId.toString() : null)
			.operation(operation)
			.build());
	}

	private R<OntNamespace> validateNamespace(OntNamespace namespace, boolean edit) {
		if (!StringUtils.hasText(namespace.getPrefix())) {
			return R.failed("前缀不能为空");
		}
		if (!PREFIX_PATTERN.matcher(namespace.getPrefix()).matches()) {
			return R.failed("前缀仅支持小写英文、数字、下划线和中划线，且必须以小写英文开头");
		}
		if (!StringUtils.hasText(namespace.getUri())) {
			return R.failed("命名空间URI不能为空");
		}
		String uri = namespace.getUri();
		if (!uri.endsWith("#") && !uri.endsWith("/")) {
			return R.failed("命名空间URI必须以#或/结尾");
		}

		long prefixCount = this.count(Wrappers.<OntNamespace>lambdaQuery()
			.eq(OntNamespace::getPrefix, namespace.getPrefix())
			.ne(edit && namespace.getId() != null, OntNamespace::getId, namespace.getId()));
		if (prefixCount > 0) {
			return R.failed("前缀已存在");
		}

		long uriCount = this.count(Wrappers.<OntNamespace>lambdaQuery()
			.eq(OntNamespace::getUri, namespace.getUri())
			.ne(edit && namespace.getId() != null, OntNamespace::getId, namespace.getId()));
		if (uriCount > 0) {
			return R.failed("命名空间URI已存在");
		}
		return R.ok(namespace);
	}

}
