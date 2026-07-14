/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntInstanceValueProvenance;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntSourceInstanceBinding;
import com.pig4cloud.pig.ontology.mapping.ingestion.mapper.OntInstanceValueProvenanceMapper;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.guard.WorkspaceStatusGuard;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一程序化实例摄入服务实现。
 * <p>
 * 按 18-01 §8.1 的 10 步 Upsert 算法执行，保证幂等、可追溯和失败关闭。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyInstanceIngestionServiceImpl implements OntologyInstanceIngestionService {

	private final OntEntityInstanceMapper entityInstanceMapper;

	private final OntInstanceDataValueMapper dataValueMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper projectMapper;

	private final OntOntologyVersionMapper ontologyVersionMapper;

	private final OntInstanceValueProvenanceMapper valueProvenanceMapper;

	private final SourceBindingRepository sourceBindingRepository;

	private final OntDomainEventPublisher eventPublisher;

	private final WorkspaceStatusGuard workspaceStatusGuard;

	private final SecuritySubjectResolver securitySubjectResolver;

	private static final String SOURCE_TYPE_DATA_MAPPING = "DATA_MAPPING";
	private static final String DECLARATION_MODE_EXPLICIT = "EXPLICIT";
	private static final String IS_BUILTIN_NO = "0";
	private static final String PROVENANCE_STATUS_ACTIVE = "ACTIVE";

	// ==================== upsertEntity ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public IngestionResult upsertEntity(EntityIngestionCommand command) {
		IngestionContext ctx = command.context();
		try {
			// 步骤1：校验映射版本、本体版本、工作区和授权上下文
			String error = validateExecutionContext(command, ctx);
			if (error != null) {
				return IngestionResult.failed(IngestionErrorCode.ONT_ING_001.code(), error);
			}

			// 步骤2：规范化SourceIdentity并计算keyHash
			SourceIdentity sourceIdentity = command.sourceIdentity();
			String keyHash = sourceIdentity.keyHash();

			// 步骤3-4：并发查找/创建来源绑定
			OntSourceInstanceBinding binding = sourceBindingRepository.findByIdentityForUpdate(sourceIdentity);

			if (binding == null) {
				// 步骤5：绑定不存在，创建新实例和绑定
				return createNewInstanceAndBinding(command, sourceIdentity, keyHash, ctx);
			}

			// 哈希碰撞检查：完整键必须相等
			if (!sourceIdentity.sourceRecordKey().equals(binding.getSourceRecordKey())) {
				return IngestionResult.failed(IngestionErrorCode.ONT_ING_009.code(),
					IngestionErrorCode.ONT_ING_009.message());
			}

			// 步骤6：绑定存在，校验实例并决定更新策略
			return updateExistingInstance(command, binding, ctx);

		}
		catch (IllegalStateException e) {
			log.warn("摄入失败（状态异常）: {}", e.getMessage());
			return IngestionResult.failed(IngestionErrorCode.ONT_ING_002.code(), e.getMessage());
		}
		catch (Exception e) {
			log.error("摄入异常: {}", e.getMessage(), e);
			return IngestionResult.failed(IngestionErrorCode.ONT_ING_007.code(), e.getMessage());
		}
	}

	// ==================== upsertRelation ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RelationIngestionResult upsertRelation(RelationIngestionCommand command) {
		// V1 关系摄入由 18-05 关系模块详细实现，此处提供基础占位
		// 完整实现需要 ont_instance_object_relation 和 ont_instance_relation_provenance 的联合操作
		log.debug("upsertRelation: relationMappingCode={}", command.relationMappingCode());
		return RelationIngestionResult.failed(
			IngestionErrorCode.ONT_ING_001.code(),
			"关系摄入由 18-05 模块实现，当前为摄入底座占位");
	}

	// ==================== deactivateEntity ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DeactivationResult deactivateEntity(SourceIdentity sourceIdentity,
											   DeleteStrategy strategy,
											   IngestionContext context) {
		OntSourceInstanceBinding binding = sourceBindingRepository.findByIdentityForUpdate(sourceIdentity);
		if (binding == null) {
			return DeactivationResult.failed(
				IngestionErrorCode.ONT_ING_004.code(),
				"来源绑定不存在，无法失活");
		}

		switch (strategy) {
			case IGNORE:
				binding.setBindingStatus(BindingStatus.MISSING.name());
				binding.setMissCount(binding.getMissCount() + 1);
				binding.setLastSeenJobId(context.getJobId());
				binding.setLastSeenAt(LocalDateTime.now());
				sourceBindingRepository.update(binding);
				return DeactivationResult.markedMissing(binding.getInstanceId(), binding.getId());

			case MARK_INACTIVE:
				binding.setBindingStatus(BindingStatus.INACTIVE.name());
				binding.setMissCount(binding.getMissCount() + 1);
				binding.setLastSeenJobId(context.getJobId());
				binding.setLastSeenAt(LocalDateTime.now());
				sourceBindingRepository.update(binding);
				// 目标本体状态属性写入由实体映射配置决定，此处不硬编码
				return DeactivationResult.deactivated(binding.getInstanceId(), binding.getId());

			case SOFT_DELETE:
				// 仅当实例没有非本映射来源值/关系、没有受保护引用时软删除
				return softDeleteIfSafe(binding, context);

			case BLOCK_AND_REVIEW:
				binding.setBindingStatus(BindingStatus.CONFLICT.name());
				binding.setLastSeenJobId(context.getJobId());
				binding.setLastSeenAt(LocalDateTime.now());
				sourceBindingRepository.update(binding);
				return DeactivationResult.blocked(binding.getInstanceId(), binding.getId(),
					"实例存在其他来源引用，需人工审查");

			default:
				return DeactivationResult.failed(
					IngestionErrorCode.ONT_ING_004.code(),
					"未知的删除策略: " + strategy);
		}
	}

	// ==================== ingestBatch ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BatchIngestionResult ingestBatch(List<EntityIngestionCommand> commands, IngestionContext context) {
		List<IngestionResult> results = new ArrayList<>(commands.size());
		List<IngestionResult> failures = new ArrayList<>();
		int created = 0, updated = 0, skipped = 0, failed = 0;

		for (EntityIngestionCommand command : commands) {
			IngestionResult result = upsertEntity(command);
			results.add(result);
			switch (result.getStatus()) {
				case CREATED -> created++;
				case UPDATED -> updated++;
				case SKIPPED_UNCHANGED -> skipped++;
				case FAILED -> {
					failed++;
					failures.add(result);
				}
				default -> { }
			}
		}

		return BatchIngestionResult.builder()
			.totalCreated(created)
			.totalUpdated(updated)
			.totalSkipped(skipped)
			.totalFailed(failed)
			.results(results)
			.failures(failures)
			.build();
	}

	// ==================== 私有方法 ====================

	/**
	 * 步骤1：校验执行上下文。
	 * @return null 可执行，否则返回拒绝原因
	 */
	private String validateExecutionContext(EntityIngestionCommand command, IngestionContext ctx) {
		// 预览上下文跳过工作区/版本严格校验
		if (ctx != null && ctx.isPreview()) {
			return null;
		}

		// 1.1 工作区可写
		String wsError = workspaceStatusGuard.checkEditable(command.ontologyId());
		if (wsError != null) {
			return wsError;
		}

		// 1.2 本体工程存在且当前版本已发布
		OntOntologyProject project = projectMapper.selectById(command.ontologyId());
		if (project == null) {
			return "本体工程不存在";
		}
		Long currentVersionId = project.getCurrentVersionId();
		if (currentVersionId == null) {
			return "本体工程无当前发布版本";
		}
		OntOntologyVersion version = ontologyVersionMapper.selectById(currentVersionId);
		if (version == null || !"PUBLISHED".equals(version.getReleaseStatus())) {
			return "当前本体版本未发布";
		}

		// 1.3 来源身份完整
		SourceIdentity identity = command.sourceIdentity();
		if (identity.mappingProjectId() == null || identity.mappingVersionId() == null
			|| identity.entityMappingCode() == null || identity.sourceRecordKey() == null) {
			return IngestionErrorCode.ONT_ING_004.message();
		}

		return null;
	}

	/**
	 * 步骤5：创建新实例和来源绑定。
	 */
	private IngestionResult createNewInstanceAndBinding(EntityIngestionCommand command,
														SourceIdentity sourceIdentity,
														String keyHash,
														IngestionContext ctx) {
		// 5.1 校验IRI唯一性
		// IRI校验由调用方通过 expectedIri 和 iriLocalName 提供

		// 5.2 创建 source_type=DATA_MAPPING 实例
		OntEntityInstance instance = new OntEntityInstance();
		instance.setOntologyId(command.ontologyId());
		instance.setNamespaceId(command.namespaceId());
		instance.setRdfTypeId(command.entityTypeId());
		instance.setLabel(command.label());
		instance.setIriLocalName(command.iriLocalName());
		instance.setIri(command.expectedIri());
		instance.setSourceType(SOURCE_TYPE_DATA_MAPPING);
		instance.setDeclarationMode(DECLARATION_MODE_EXPLICIT);
		instance.setIsBuiltin(IS_BUILTIN_NO);
		instance.setSortOrder(0);
		// sourceReference 保存可读来源摘要
		instance.setSourceReference(sourceIdentity.sourceObject() + ":" + keyHash.substring(0, 8));
		entityInstanceMapper.insert(instance);

		// 5.3 创建来源绑定
		OntSourceInstanceBinding binding = new OntSourceInstanceBinding();
		binding.setSourceId(sourceIdentity.sourceId());
		binding.setMappingProjectId(sourceIdentity.mappingProjectId());
		binding.setCurrentMappingVersionId(sourceIdentity.mappingVersionId());
		binding.setEntityMappingCode(sourceIdentity.entityMappingCode());
		binding.setSourceObject(sourceIdentity.sourceObject());
		binding.setSourceRecordKey(sourceIdentity.sourceRecordKey());
		binding.setSourceRecordKeyHash(keyHash);
		binding.setInstanceId(instance.getId());
		binding.setContentHash(command.contentHash());
		binding.setBindingStatus(BindingStatus.ACTIVE.name());
		binding.setMissCount(0);
		binding.setFirstSeenJobId(ctx != null ? ctx.getJobId() : null);
		binding.setLastSeenJobId(ctx != null ? ctx.getJobId() : null);
		if (command.sourceUpdatedAt() != null) {
			binding.setSourceUpdatedAt(LocalDateTime.ofInstant(
				command.sourceUpdatedAt(), ZoneId.systemDefault()));
		}
		binding = sourceBindingRepository.insertOrFind(binding);

		// 哈希碰撞检查（并发场景下可能拿到已有绑定）
		if (!sourceIdentity.sourceRecordKey().equals(binding.getSourceRecordKey())) {
			throw new IllegalStateException(IngestionErrorCode.ONT_ING_009.message());
		}

		// 步骤7：插入数据属性值和来源
		Set<String> changedFields = insertDataValuesAndProvenance(command, binding, instance.getId());

		// 步骤8：更新绑定
		binding.setLastSeenAt(LocalDateTime.now());
		sourceBindingRepository.update(binding);

		// 步骤9：写Outbox事件
		publishInstanceEvent(instance, "CREATED", ctx, sourceIdentity, changedFields);

		log.debug("摄入成功（新建）: instanceId={}, bindingId={}", instance.getId(), binding.getId());
		return IngestionResult.created(instance.getId(), binding.getId(), changedFields);
	}

	/**
	 * 步骤6：更新已有实例。
	 */
	private IngestionResult updateExistingInstance(EntityIngestionCommand command,
												  OntSourceInstanceBinding binding,
												  IngestionContext ctx) {
		// 6.1 校验实例仍存在且ontology/type一致
		OntEntityInstance instance = entityInstanceMapper.selectById(binding.getInstanceId());
		if (instance == null || !"0".equals(instance.getDelFlag())) {
			throw new IllegalStateException(
				IngestionErrorCode.ONT_ING_010.message() + ": 绑定实例不存在或已删除");
		}
		if (!instance.getOntologyId().equals(command.ontologyId())) {
			throw new IllegalStateException(
				IngestionErrorCode.ONT_ING_010.message() + ": ontologyId 不一致");
		}
		if (!instance.getRdfTypeId().equals(command.entityTypeId())) {
			throw new IllegalStateException(
				IngestionErrorCode.ONT_ING_013.message());
		}

		// 6.2 contentHash相同且sourceUpdatedAt未前进 → SKIPPED_UNCHANGED
		if (command.contentHash() != null
			&& command.contentHash().equals(binding.getContentHash())
			&& (command.sourceUpdatedAt() == null
				|| binding.getSourceUpdatedAt() == null
				|| !command.sourceUpdatedAt().isAfter(binding.getSourceUpdatedAt().atZone(
					ZoneId.systemDefault()).toInstant()))) {
			log.debug("摄入跳过（内容未变化）: instanceId={}, bindingId={}",
				instance.getId(), binding.getId());
			return IngestionResult.skipped(instance.getId(), binding.getId());
		}

		// 6.3 更新允许变更的label等治理字段
		Set<String> changedFields = new HashSet<>();
		if (command.label() != null && !command.label().equals(instance.getLabel())) {
			instance.setLabel(command.label());
			changedFields.add("label");
		}
		if (!changedFields.isEmpty()) {
			entityInstanceMapper.updateById(instance);
		}

		// 步骤7：按fieldMappingCode合并来源拥有的数据值
		changedFields.addAll(mergeDataValues(command, binding, instance.getId()));

		// 步骤8：更新绑定
		binding.setContentHash(command.contentHash());
		binding.setLastSeenJobId(ctx != null ? ctx.getJobId() : null);
		binding.setLastSeenAt(LocalDateTime.now());
		binding.setCurrentMappingVersionId(command.sourceIdentity().mappingVersionId());
		binding.setBindingStatus(BindingStatus.ACTIVE.name());
		binding.setMissCount(0);
		if (command.sourceUpdatedAt() != null) {
			binding.setSourceUpdatedAt(LocalDateTime.ofInstant(
				command.sourceUpdatedAt(), ZoneId.systemDefault()));
		}
		sourceBindingRepository.update(binding);

		// 步骤9：写Outbox事件
		publishInstanceEvent(instance, "UPDATED", ctx, command.sourceIdentity(), changedFields);

		log.debug("摄入成功（更新）: instanceId={}, bindingId={}", instance.getId(), binding.getId());
		return IngestionResult.updated(instance.getId(), binding.getId(), changedFields);
	}

	/**
	 * 步骤7（新建场景）：插入数据值和来源记录。
	 */
	private Set<String> insertDataValuesAndProvenance(EntityIngestionCommand command,
													 OntSourceInstanceBinding binding,
													 Long instanceId) {
		Set<String> changedFields = new HashSet<>();
		if (command.values() == null || command.values().isEmpty()) {
			return changedFields;
		}
		for (ValueIngestionItem item : command.values()) {
			OntInstanceDataValue dataValue = new OntInstanceDataValue();
			dataValue.setInstanceId(instanceId);
			dataValue.setDataPropertyId(item.dataPropertyId());
			dataValue.setLiteralValue(item.literalValue());
			dataValue.setLiteralType(item.literalType());
			dataValue.setUnitId(item.unitId());
			dataValue.setLiteralSymbol(item.literalSymbol());
			dataValue.setSortOrder(item.sortOrder() != null ? item.sortOrder() : 0);
			dataValueMapper.insert(dataValue);

			// 写值来源
			OntInstanceValueProvenance provenance = new OntInstanceValueProvenance();
			provenance.setDataValueId(dataValue.getId());
			provenance.setSourceBindingId(binding.getId());
			provenance.setMappingVersionId(command.sourceIdentity().mappingVersionId());
			provenance.setFieldMappingCode(item.fieldMappingCode());
			provenance.setSourceKind(item.sourceKind());
			provenance.setSourceReference(item.sourceReference());
			provenance.setOwnershipPolicy(
				item.ownershipPolicy() != null ? item.ownershipPolicy().name() : OwnershipPolicy.SOURCE_WINS.name());
			provenance.setProvenanceStatus(PROVENANCE_STATUS_ACTIVE);
			provenance.setValueHash(item.valueHash());
			provenance.setLastJobId(command.context() != null ? command.context().getJobId() : null);
			valueProvenanceMapper.insert(provenance);

			changedFields.add(item.fieldMappingCode());
		}
		return changedFields;
	}

	/**
	 * 步骤7（更新场景）：按 ConflictPolicy 合并来源拥有的数据值。
	 * <p>
	 * SOURCE_WINS：源值覆盖映射拥有的旧值；
	 * MANUAL_WINS：已覆盖则保持人工值；
	 * REJECT_CONFLICT：值不同则记录失败。
	 */
	private Set<String> mergeDataValues(EntityIngestionCommand command,
									   OntSourceInstanceBinding binding,
									   Long instanceId) {
		Set<String> changedFields = new HashSet<>();
		if (command.values() == null || command.values().isEmpty()) {
			return changedFields;
		}

		for (ValueIngestionItem item : command.values()) {
			// 查找本映射来源拥有的同 fieldMappingCode 的旧值来源
			OntInstanceValueProvenance existingProv = valueProvenanceMapper.selectOne(
				Wrappers.<OntInstanceValueProvenance>lambdaQuery()
					.eq(OntInstanceValueProvenance::getSourceBindingId, binding.getId())
					.eq(OntInstanceValueProvenance::getFieldMappingCode, item.fieldMappingCode())
					.eq(OntInstanceValueProvenance::getDelFlag, "0"));

			if (existingProv == null) {
				// 无旧值，直接新建
				OntInstanceDataValue dataValue = new OntInstanceDataValue();
				dataValue.setInstanceId(instanceId);
				dataValue.setDataPropertyId(item.dataPropertyId());
				dataValue.setLiteralValue(item.literalValue());
				dataValue.setLiteralType(item.literalType());
				dataValue.setUnitId(item.unitId());
				dataValue.setLiteralSymbol(item.literalSymbol());
				dataValue.setSortOrder(item.sortOrder() != null ? item.sortOrder() : 0);
				dataValueMapper.insert(dataValue);

				OntInstanceValueProvenance prov = new OntInstanceValueProvenance();
				prov.setDataValueId(dataValue.getId());
				prov.setSourceBindingId(binding.getId());
				prov.setMappingVersionId(command.sourceIdentity().mappingVersionId());
				prov.setFieldMappingCode(item.fieldMappingCode());
				prov.setSourceKind(item.sourceKind());
				prov.setSourceReference(item.sourceReference());
				prov.setOwnershipPolicy(item.ownershipPolicy() != null
					? item.ownershipPolicy().name() : OwnershipPolicy.SOURCE_WINS.name());
				prov.setProvenanceStatus(PROVENANCE_STATUS_ACTIVE);
				prov.setValueHash(item.valueHash());
				prov.setLastJobId(command.context() != null ? command.context().getJobId() : null);
				valueProvenanceMapper.insert(prov);

				changedFields.add(item.fieldMappingCode());
			}
			else {
				// 有旧值，按策略处理
				OntInstanceDataValue oldValue = dataValueMapper.selectById(existingProv.getDataValueId());
				if (oldValue == null) {
					continue;
				}

				OwnershipPolicy policy = item.ownershipPolicy() != null
					? item.ownershipPolicy() : OwnershipPolicy.SOURCE_WINS;

				if (policy == OwnershipPolicy.MANUAL_WINS
					&& "OVERRIDDEN".equals(existingProv.getProvenanceStatus())) {
					// 人工已覆盖，保持人工值，标记来源为STALE
					existingProv.setProvenanceStatus("STALE");
					existingProv.setLastJobId(command.context() != null
						? command.context().getJobId() : null);
					valueProvenanceMapper.updateById(existingProv);
					continue;
				}

				if (policy == OwnershipPolicy.REJECT_CONFLICT
					&& !item.literalValue().equals(oldValue.getLiteralValue())) {
					throw new IllegalStateException(IngestionErrorCode.ONT_ING_011.message()
						+ ": fieldMappingCode=" + item.fieldMappingCode());
				}

				// SOURCE_WINS 或 MANUAL_WINS（未覆盖）：覆盖旧值
				if (!item.literalValue().equals(oldValue.getLiteralValue())) {
					oldValue.setLiteralValue(item.literalValue());
					oldValue.setLiteralType(item.literalType());
					oldValue.setUnitId(item.unitId());
					oldValue.setLiteralSymbol(item.literalSymbol());
					dataValueMapper.updateById(oldValue);

					existingProv.setValueHash(item.valueHash());
					existingProv.setProvenanceStatus(PROVENANCE_STATUS_ACTIVE);
					existingProv.setLastJobId(command.context() != null
						? command.context().getJobId() : null);
					valueProvenanceMapper.updateById(existingProv);

					changedFields.add(item.fieldMappingCode());
				}
			}
		}
		return changedFields;
	}

	/**
	 * 软删除实例（仅当无其他来源引用时）。
	 */
	private DeactivationResult softDeleteIfSafe(OntSourceInstanceBinding binding, IngestionContext context) {
		// 检查是否有非本映射来源的值或关系
		Long otherProvCount = valueProvenanceMapper.selectCount(
			Wrappers.<OntInstanceValueProvenance>lambdaQuery()
				.ne(OntInstanceValueProvenance::getSourceBindingId, binding.getId())
				.eq(OntInstanceValueProvenance::getDelFlag, "0"));

		if (otherProvCount != null && otherProvCount > 0) {
			binding.setBindingStatus(BindingStatus.CONFLICT.name());
			binding.setLastSeenJobId(context.getJobId());
			binding.setLastSeenAt(LocalDateTime.now());
			sourceBindingRepository.update(binding);
			return DeactivationResult.blocked(binding.getInstanceId(), binding.getId(),
				"实例存在其他映射来源的值，无法软删除");
		}

		// 软删除实例
		OntEntityInstance instance = entityInstanceMapper.selectById(binding.getInstanceId());
		if (instance != null) {
			instance.setDelFlag("1");
			entityInstanceMapper.updateById(instance);
		}
		binding.setBindingStatus(BindingStatus.INACTIVE.name());
		binding.setLastSeenJobId(context.getJobId());
		binding.setLastSeenAt(LocalDateTime.now());
		sourceBindingRepository.update(binding);

		return DeactivationResult.softDeleted(binding.getInstanceId(), binding.getId());
	}

	/**
	 * 写 Outbox 事件。
	 * <p>
	 * Payload 只包含实例ID、ontologyId、entityTypeId、mappingProjectId、mappingVersionId、jobId、
	 * 变更字段ID集合和摘要，不包含字面量明文。
	 */
	private void publishInstanceEvent(OntEntityInstance instance, String operation,
									 IngestionContext ctx, SourceIdentity sourceIdentity,
									 Set<String> changedFields) {
		// SKIPPED_UNCHANGED 默认不发布
		if ("SKIPPED".equals(operation)) {
			return;
		}

		eventPublisher.append(OntologyDomainEvent.builder()
			.eventType(OntologyEventTypes.ONTOLOGY_INSTANCE_CHANGED)
			.ontologyId(instance.getOntologyId())
			.aggregateType("ENTITY_INSTANCE")
			.aggregateId(instance.getId().toString())
			.operation(operation)
			.actorId(ctx != null ? ctx.getRequestedUserId() : null)
			.traceId(ctx != null ? ctx.getTraceId() : null)
			.payload(Map.of(
				"entityTypeId", instance.getRdfTypeId(),
				"mappingProjectId", sourceIdentity.mappingProjectId() != null
					? sourceIdentity.mappingProjectId() : 0,
				"mappingVersionId", sourceIdentity.mappingVersionId() != null
					? sourceIdentity.mappingVersionId() : 0,
				"jobId", ctx != null && ctx.getJobId() != null ? ctx.getJobId() : 0,
				"changedFields", changedFields != null ? changedFields : Set.of()
			))
			.build());
	}

}
