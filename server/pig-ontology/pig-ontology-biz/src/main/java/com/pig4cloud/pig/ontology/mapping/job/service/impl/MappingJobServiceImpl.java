/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.security.service.PigUser;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.event.model.OntologyDomainEvent;
import com.pig4cloud.pig.ontology.event.model.OntologyEventTypes;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.DataSourcePoolManager;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.DataSourceConnector;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.ConnectorRegistry;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql.PostgreSqlDialect;
import com.pig4cloud.pig.ontology.mapping.datasource.crypto.DataSourceCredentialCryptoService;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.compiler.IriTemplateCompiler;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntRelationMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntRelationMappingMapper;
import com.pig4cloud.pig.ontology.mapping.ingestion.DeactivationResult;
import com.pig4cloud.pig.ontology.mapping.ingestion.DeleteStrategy;
import com.pig4cloud.pig.ontology.mapping.ingestion.EntityIngestionCommand;
import com.pig4cloud.pig.ontology.mapping.ingestion.IngestionContext;
import com.pig4cloud.pig.ontology.mapping.ingestion.IngestionResult;
import com.pig4cloud.pig.ontology.mapping.ingestion.OntologyInstanceIngestionService;
import com.pig4cloud.pig.ontology.mapping.ingestion.OwnershipPolicy;
import com.pig4cloud.pig.ontology.mapping.ingestion.RelationIngestionResult;
import com.pig4cloud.pig.ontology.mapping.ingestion.SourceIdentity;
import com.pig4cloud.pig.ontology.mapping.ingestion.SourceBindingRepository;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntSourceInstanceBinding;
import com.pig4cloud.pig.ontology.mapping.ingestion.mapper.OntSourceInstanceBindingMapper;
import com.pig4cloud.pig.ontology.mapping.job.MappingJobErrorCode;
import com.pig4cloud.pig.ontology.mapping.job.cursor.CursorCodec;
import com.pig4cloud.pig.ontology.mapping.job.cursor.MappingCursor;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobCreateRequest;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobQuery;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobRetryRequest;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJob;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJobRecord;
import com.pig4cloud.pig.ontology.mapping.job.mapper.OntMappingJobMapper;
import com.pig4cloud.pig.ontology.mapping.job.mapper.OntMappingJobRecordMapper;
import com.pig4cloud.pig.ontology.mapping.job.service.MappingJobService;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobRecordVO;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobVO;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 映射作业服务实现（18-07 §8~§12）。
 * <p>
 * V1 采用同步执行模式：创建作业后在同一请求线程内执行扫描。
 * 作业状态通过 CAS 状态机流转，租约/心跳保留但简化为同线程内的心跳更新。
 * <p>
 * 执行流程：创建→领取租约→CAS RUNNING→校验→ENTITY扫描→RELATION→PENDING重试→
 * FULL删除检测→汇总终态→更新工程游标→发事件。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingJobServiceImpl extends ServiceImpl<OntMappingJobMapper, OntMappingJob>
		implements MappingJobService {

	// ==================== 常量 ====================

	private static final int DEFAULT_PAGE_SIZE = 500;

	private static final int CONNECT_TIMEOUT_SECONDS = 10;

	private static final int QUERY_TIMEOUT_SECONDS = 60;

	private static final int HEARTBEAT_INTERVAL_RECORDS = 50;

	private static final double DEFAULT_MAX_ERROR_RATE = 0.1;

	private static final int ERROR_RATE_MIN_READS = 100;

	private static final int ERROR_RATE_ABSOLUTE_LIMIT = 20;

	private static final int MAX_RETRY_RECORDS = 1000;

	private static final int FULL_MISS_THRESHOLD = 2;

	private static final long LEASE_DURATION_SECONDS = 300;

	// ==================== 依赖 ====================

	private final OntMappingVersionMapper versionMapper;

	private final OntMappingProjectMapper projectMapper;

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private final OntRelationMappingMapper relationMappingMapper;

	private final OntDataSourceMapper dataSourceMapper;

	private final OntSourceInstanceBindingMapper bindingMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final OntOntologyProjectMapper ontologyProjectMapper;

	private final OntMappingJobRecordMapper jobRecordMapper;

	private final OntologyInstanceIngestionService ingestionService;

	private final SourceBindingRepository bindingRepository;

	private final DataSourcePoolManager poolManager;

	private final ConnectorRegistry connectorRegistry;

	private final DataSourceCredentialCryptoService credentialCryptoService;

	private final IriTemplateCompiler iriTemplateCompiler;

	private final CursorCodec cursorCodec;

	private final OntDomainEventPublisher eventPublisher;

	private final ObjectMapper objectMapper;

	// ==================== 创建作业 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingJobVO createJob(Long versionId, JobCreateRequest request) {
		// 1. 加载映射版本
		OntMappingVersion version = versionMapper.selectById(versionId);
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}

		// 2. 校验版本状态：FULL/INCREMENTAL 要求 PUBLISHED，RETRY 允许 RETIRED
		String runType = request.getRunType();
		if (!"PUBLISHED".equals(version.getVersionStatus())
				&& !("RETRY".equals(runType) && "RETIRED".equals(version.getVersionStatus()))) {
			throw new IllegalStateException(MappingJobErrorCode.ONT_MAP_021.getMessage()
					+ ": 当前版本状态=" + version.getVersionStatus());
		}

		// 3. 锁工程行并校验无运行中作业
		OntMappingProject project = projectMapper.selectForUpdate(version.getMappingProjectId());
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}

		// 4. 固化作业上下文
		OntOntologyProject ontologyProject = ontologyProjectMapper.selectById(project.getOntologyId());
		Long ontologyVersionId = ontologyProject != null ? ontologyProject.getCurrentVersionId() : null;
		Long workspaceRevision = ontologyProject != null ? ontologyProject.getWorkspaceRevision() : 0L;

		// 请求人信息
		String requestedBy = getCurrentUsername();
		Long requestedUserId = getCurrentUserId();
		String authSnapshot = buildAuthorizationSnapshot();

		// 5. 创建作业记录
		OntMappingJob job = new OntMappingJob();
		job.setMappingProjectId(project.getId());
		job.setMappingVersionId(versionId);
		job.setRunType(runType);
		job.setTriggerType("MANUAL");
		job.setJobStatus("QUEUED");
		job.setRequestedBy(requestedBy);
		job.setRequestedUserId(requestedUserId);
		job.setAuthorizationSnapshot(authSnapshot);
		job.setConfigHash(version.getConfigHash());
		job.setOntologyVersionId(ontologyVersionId);
		job.setWorkspaceRevision(workspaceRevision);
		job.setCursorBefore(project.getLastJobId() != null ? getLastJobCursor(project.getLastJobId()) : null);
		job.setPageSize(DEFAULT_PAGE_SIZE);
		job.setCurrentPageNo(0L);
		job.setTotalRead(0L);
		job.setTotalCreated(0L);
		job.setTotalUpdated(0L);
		job.setTotalUnchanged(0L);
		job.setTotalSkipped(0L);
		job.setTotalFailed(0L);
		job.setTotalRelations(0L);
		job.setCancelRequested("0");
		job.setTraceId(java.util.UUID.randomUUID().toString());

		baseMapper.insert(job);
		log.info("Created mapping job: id={}, projectId={}, versionId={}, runType={}",
				job.getId(), project.getId(), versionId, runType);

		// 6. 同步执行（PREVIEW 不在此执行）
		if (!"PREVIEW".equals(runType)) {
			// 在新事务中执行，不阻塞创建事务
			executeJob(job.getId());
		}

		return toVO(baseMapper.selectById(job.getId()));
	}

	// ==================== 执行作业 ====================

	@Override
	public void executeJob(Long jobId) {
		OntMappingJob job = baseMapper.selectById(jobId);
		if (job == null || "1".equals(job.getDelFlag())) {
			throw new IllegalArgumentException(MappingJobErrorCode.ONT_MAP_023.getMessage());
		}

		// 1. 领取租约：CAS QUEUED/RECOVERING → STARTING
		String leaseOwner = buildLeaseOwner();
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime leaseUntil = now.plusSeconds(LEASE_DURATION_SECONDS);
		int acquired = baseMapper.acquireLease(jobId, leaseOwner, leaseUntil, now, now);
		if (acquired == 0) {
			log.warn("Failed to acquire lease for job {}, may already be running", jobId);
			return;
		}

		job = baseMapper.selectById(jobId);

		try {
			// 2. 发 STARTED 事件
			publishEvent(job, OntologyEventTypes.MAPPING_JOB_STARTED, "STARTED");

			// 3. CAS: STARTING → RUNNING
			baseMapper.casUpdateStatus(jobId, "STARTING", "RUNNING");

			// 4. 再校验数据源、映射版本
			OntMappingVersion version = versionMapper.selectById(job.getMappingVersionId());
			OntMappingProject project = projectMapper.selectById(job.getMappingProjectId());
			validateExecutionPreconditions(job, version, project);

			// 5. 执行核心逻辑
			JobCounters counters = new JobCounters();
			MappingCursor cursor = cursorCodec.decode(job.getCursorBefore());

			if ("RETRY".equals(job.getRunType())) {
				executeRetry(job, version, project, cursor, counters);
			}
			else {
				// FULL / INCREMENTAL
				executeEntityPhase(job, version, project, cursor, counters);
				executeRelationPhase(job, version, project, cursor, counters);
				executePendingRetry(job, version, project, cursor, counters);

				if ("FULL".equals(job.getRunType())) {
					executeDeleteDetection(job, version, project, counters);
				}
			}

			// 6. 检查取消
			job = baseMapper.selectById(jobId);
			if ("1".equals(job.getCancelRequested())) {
				finishJob(jobId, "RUNNING", "CANCELLED",
						MappingJobErrorCode.ONT_MAP_028.getCode(),
						MappingJobErrorCode.ONT_MAP_028.getMessage(), counters, cursor);
				publishEvent(job, OntologyEventTypes.MAPPING_JOB_CANCELLED, "CANCELLED");
				return;
			}

			// 7. 汇总终态
			String finalStatus = determineFinalStatus(counters);
			String errorCode = null;
			String errorMessage = null;
			if ("FAILED".equals(finalStatus)) {
				errorCode = MappingJobErrorCode.ONT_MAP_027.getCode();
				errorMessage = MappingJobErrorCode.ONT_MAP_027.getMessage();
			}

			finishJob(jobId, "RUNNING", finalStatus, errorCode, errorMessage, counters, cursor);

			// 8. 更新工程 last_job_id 和游标
			updateProjectAfterJob(job.getMappingProjectId(), jobId, cursorCodec.encode(cursor));

			// 9. 发完成事件
			if ("SUCCEEDED".equals(finalStatus) || "PARTIAL_SUCCESS".equals(finalStatus)) {
				publishEvent(job, OntologyEventTypes.MAPPING_JOB_COMPLETED, finalStatus);
			}
			else {
				publishEvent(job, OntologyEventTypes.MAPPING_JOB_FAILED, finalStatus);
			}

			log.info("Job {} finished: status={}, read={}, created={}, updated={}, failed={}",
					jobId, finalStatus, counters.totalRead, counters.totalCreated,
					counters.totalUpdated, counters.totalFailed);

		}
		catch (Exception e) {
			log.error("Job {} execution failed: {}", jobId, e.getMessage(), e);
			finishJob(jobId, "RUNNING", "FAILED",
					MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500),
					new JobCounters(), cursorCodec.decode(job.getCursorAfter()));
			job = baseMapper.selectById(jobId);
			publishEvent(job, OntologyEventTypes.MAPPING_JOB_FAILED, "FAILED");
		}
	}

	// ==================== ENTITY 阶段 ====================

	private void executeEntityPhase(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project, MappingCursor cursor, JobCounters counters) {

		// 加载该版本下的实体映射，按 syncOrder 排序
		List<OntEntityMapping> entityMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, version.getId())
						.eq(OntEntityMapping::getDelFlag, "0")
						.eq(OntEntityMapping::getEnabled, "1")
						.orderByAsc(OntEntityMapping::getSyncOrder));

		for (OntEntityMapping em : entityMappings) {
			// 更新当前进度
			updateProgress(job.getId(), "ENTITY", em.getMappingCode(), counters, cursor);

			// 检查取消
			if (isCancelRequested(job.getId())) {
				return;
			}

			try {
				scanAndIngestEntity(job, project, em, cursor, counters);
			}
			catch (Exception e) {
				log.error("Entity mapping {} scan failed: {}", em.getMappingCode(), e.getMessage(), e);
				counters.totalFailed++;
				writeJobRecord(job.getId(), "ENTITY", em.getMappingCode(), em.getSourceObject(),
						null, null, "SKIP", "FAILED",
						MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500), 0);
			}
		}
	}

	/**
	 * 扫描源表并逐条摄入。
	 */
	private void scanAndIngestEntity(OntMappingJob job, OntMappingProject project,
			OntEntityMapping em, MappingCursor cursor, JobCounters counters) {

		// 加载数据源
		OntDataSource dataSource = dataSourceMapper.selectById(em.getSourceId());
		if (dataSource == null || "1".equals(dataSource.getDelFlag())
				|| !"ACTIVE".equals(dataSource.getStatus())) {
			throw new IllegalStateException(MappingJobErrorCode.ONT_MAP_025.getMessage());
		}

		// 解密凭证
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(dataSource);
		String jdbcUrl = buildJdbcUrl(dataSource);

		// 加载命名空间
		OntNamespace namespace = namespaceMapper.selectById(em.getTargetNamespaceId());
		String namespaceUri = namespace != null ? namespace.getUri() : "";

		// 加载字段映射
		List<OntFieldMapping> fieldMappings = fieldMappingMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, em.getId())
						.eq(OntFieldMapping::getDelFlag, "0")
						.orderByAsc(OntFieldMapping::getSortOrder));

		// 提取键列
		List<String> keyColumnNames = iriTemplateCompiler.extractKeyColumnNames(em.getKeyColumns());

		// 构建 SELECT 列列表
		List<String> columns = new ArrayList<>(keyColumnNames);
		for (OntFieldMapping fm : fieldMappings) {
			if ("COLUMN".equals(fm.getSourceKind()) && fm.getSourceColumn() != null
					&& !columns.contains(fm.getSourceColumn())) {
				columns.add(fm.getSourceColumn());
			}
		}
		// 增量列
		if (StrUtil.isNotBlank(em.getIncrementalColumn()) && !columns.contains(em.getIncrementalColumn())) {
			columns.add(em.getIncrementalColumn());
		}
		// 删除标记列
		if (StrUtil.isNotBlank(em.getSourceDeleteFlagColumn())
				&& !columns.contains(em.getSourceDeleteFlagColumn())) {
			columns.add(em.getSourceDeleteFlagColumn());
		}

		// 构建扫描计划
		DataSourceConnector.SourceScanPlan scanPlan = new DataSourceConnector.SourceScanPlan(
				em.getSourceSchema(), em.getSourceObject(), columns,
				DEFAULT_PAGE_SIZE, QUERY_TIMEOUT_SECONDS,
				keyColumnNames, em.getIncrementalColumn(), em.getFilterDsl());

		// 获取游标
		MappingCursor.EntityCursor entityCursor = cursor.getOrCreateEntity(em.getMappingCode());
		DataSourceConnector.SourceCursor sourceCursor = toSourceCursor(entityCursor,
				"INCREMENTAL".equals(job.getRunType()));

		// 获取连接器
		DataSourceConnector connector = connectorRegistry.getConnector(
				dataSource.getSourceType(), dataSource.getDatabaseType());

		// 构建摄入上下文
		IngestionContext ingestionContext = IngestionContext.builder()
				.jobId(job.getId())
				.requestedUserId(job.getRequestedUserId())
				.requestedUsername(job.getRequestedBy())
				.authorizationSummary(job.getAuthorizationSnapshot())
				.configHash(job.getConfigHash())
				.traceId(job.getTraceId())
				.executedAt(Instant.now())
				.preview(false)
				.allowManualOverride(false)
				.build();

		// 分页扫描
		try (Connection conn = poolManager.getConnection(dataSource.getId(), dataSource.getRevision(),
				jdbcUrl, credential.username(), credential.password(), CONNECT_TIMEOUT_SECONDS)) {

			long pageNo = 0;
			while (true) {
				// 检查取消
				if (isCancelRequested(job.getId())) {
					log.info("Job {} cancelled during entity scan", job.getId());
					return;
				}

				// 心跳续租
				if (counters.totalRead % HEARTBEAT_INTERVAL_RECORDS == 0 && counters.totalRead > 0) {
					baseMapper.renewLease(job.getId(),
							LocalDateTime.now().plusSeconds(LEASE_DURATION_SECONDS), LocalDateTime.now());
				}

				// 扫描一页
				DataSourceConnector.SourcePage page = connector.scanWithConnection(conn, scanPlan, sourceCursor);
				List<DataSourceConnector.SourceRow> rows = page.rows();

				if (rows.isEmpty()) {
					break;
				}

				// 逐条摄入
				for (DataSourceConnector.SourceRow row : rows) {
					counters.totalRead++;
					ingestSingleRecord(job, project, em, keyColumnNames, fieldMappings,
							namespaceUri, row, ingestionContext, counters);
				}

				// 更新游标
				if (page.hasMore() && page.nextCursor() != null) {
					updateEntityCursor(entityCursor, page.nextCursor(),
							"INCREMENTAL".equals(job.getRunType()));
					sourceCursor = page.nextCursor();
				}
				else {
					// 最后一批，更新游标为最后一条
					if (!rows.isEmpty()) {
						DataSourceConnector.SourceRow lastRow = rows.get(rows.size() - 1);
						updateEntityCursorFromRow(entityCursor, lastRow, keyColumnNames,
								em.getIncrementalColumn(), "INCREMENTAL".equals(job.getRunType()));
					}
					break;
				}

				pageNo++;
				// 更新进度
				updateProgress(job.getId(), "ENTITY", em.getMappingCode(), counters, cursor);
			}
		}
		catch (Exception e) {
			log.error("Entity scan failed for mapping {}: {}", em.getMappingCode(), e.getMessage(), e);
			throw new RuntimeException("Entity scan failed: " + e.getMessage(), e);
		}
	}

	/**
	 * 摄入单条源记录。
	 */
	private void ingestSingleRecord(OntMappingJob job, OntMappingProject project,
			OntEntityMapping em, List<String> keyColumnNames, List<OntFieldMapping> fieldMappings,
			String namespaceUri, DataSourceConnector.SourceRow row,
			IngestionContext context, JobCounters counters) {

		long startTime = System.currentTimeMillis();
		Map<String, String> rowValues = toStringMap(row.values());

		// 构建来源身份
		LinkedHashMap<String, String> keyPairs = new LinkedHashMap<>();
		for (String col : keyColumnNames) {
			keyPairs.put(col, rowValues.getOrDefault(col, ""));
		}
		String recordKey = SourceIdentity.buildRecordKey(keyPairs);

		SourceIdentity sourceIdentity = new SourceIdentity(
				em.getSourceId(), project.getId(), job.getMappingVersionId(),
				em.getMappingCode(), em.getSourceObject(), recordKey);

		// 渲染 IRI
		String iriLocalName;
		String expectedIri;
		try {
			iriLocalName = iriTemplateCompiler.renderTemplate(em.getIriTemplate(), rowValues);
			expectedIri = iriTemplateCompiler.renderIri(namespaceUri, em.getIriTemplate(), rowValues);
		}
		catch (IllegalArgumentException e) {
			counters.totalFailed++;
			writeJobRecord(job.getId(), "ENTITY", em.getMappingCode(), em.getSourceObject(),
					sourceIdentity.keyHash(), maskKey(recordKey), "SKIP", "FAILED",
					MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500),
					System.currentTimeMillis() - startTime);
			return;
		}

		// 渲染标签
		String label = null;
		if (StrUtil.isNotBlank(em.getLabelTemplate())) {
			label = iriTemplateCompiler.renderTemplate(em.getLabelTemplate(), rowValues);
		}

		// 构建值列表
		List<com.pig4cloud.pig.ontology.mapping.ingestion.ValueIngestionItem> values = new ArrayList<>();
		for (OntFieldMapping fm : fieldMappings) {
			String value = null;
			if ("COLUMN".equals(fm.getSourceKind()) && fm.getSourceColumn() != null) {
				value = rowValues.get(fm.getSourceColumn());
			}
			else if ("CONSTANT".equals(fm.getSourceKind())) {
				value = fm.getConstantValue();
			}
			else {
				continue;
			}

			// 空值处理
			if (value == null || value.isEmpty()) {
				if ("REJECT".equals(fm.getNullHandling()) || "SKIP".equals(fm.getNullHandling())) {
					continue;
				}
				value = fm.getDefaultValue();
				if (value == null) {
					continue;
				}
			}

			values.add(new com.pig4cloud.pig.ontology.mapping.ingestion.ValueIngestionItem(
					fm.getFieldMappingCode(), fm.getTargetDataPropertyId(),
					value, fm.getConstantLiteralType(), fm.getConstantUnitId(),
					null, fm.getSortOrder(),
					OwnershipPolicy.valueOf(em.getConflictPolicy()),
					fm.getSourceKind(),
					"COLUMN".equals(fm.getSourceKind()) ? fm.getSourceColumn() : "CONSTANT",
					null));
		}

		// 内容哈希
		String contentHash = computeContentHash(rowValues);

		// 源更新时间
		Instant sourceUpdatedAt = null;
		if (StrUtil.isNotBlank(em.getIncrementalColumn())) {
			String incValue = rowValues.get(em.getIncrementalColumn());
			if (incValue != null) {
				try {
					sourceUpdatedAt = parseInstant(incValue);
				}
				catch (Exception e) {
					// 解析失败不阻塞
				}
			}
		}

		// 构建摄入命令
		EntityIngestionCommand command = new EntityIngestionCommand(
				project.getOntologyId(), em.getTargetNamespaceId(), em.getTargetEntityTypeId(),
				sourceIdentity, iriLocalName, expectedIri, label, values,
				OwnershipPolicy.valueOf(em.getConflictPolicy()), contentHash,
				sourceUpdatedAt, context);

		// 执行摄入（内部 REQUIRES_NEW 事务）
		try {
			IngestionResult result = ingestionService.upsertEntity(command);
			switch (result.getStatus()) {
				case CREATED -> counters.totalCreated++;
				case UPDATED -> counters.totalUpdated++;
				case SKIPPED_UNCHANGED -> counters.totalUnchanged++;
				case FAILED -> {
					counters.totalFailed++;
					writeJobRecord(job.getId(), "ENTITY", em.getMappingCode(), em.getSourceObject(),
							sourceIdentity.keyHash(), maskKey(recordKey), "SKIP", "FAILED",
							result.getErrorCode(), truncate(result.getErrorMessage(), 500),
							System.currentTimeMillis() - startTime);
				}
				default -> counters.totalSkipped++;
			}

			// 更新绑定的 last_seen_job_id（FULL 删除检测用）
			if (result.getBindingId() != null && "FULL".equals(job.getRunType())) {
				updateBindingLastSeen(result.getBindingId(), job.getId());
			}
		}
		catch (Exception e) {
			counters.totalFailed++;
			writeJobRecord(job.getId(), "ENTITY", em.getMappingCode(), em.getSourceObject(),
					sourceIdentity.keyHash(), maskKey(recordKey), "SKIP", "FAILED",
					MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500),
					System.currentTimeMillis() - startTime);
		}

		// 错误率检查
		checkErrorRate(job, counters);
	}

	// ==================== RELATION 阶段 ====================

	private void executeRelationPhase(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project, MappingCursor cursor, JobCounters counters) {

		List<OntRelationMapping> relationMappings = relationMappingMapper.selectList(
				Wrappers.<OntRelationMapping>lambdaQuery()
						.eq(OntRelationMapping::getMappingVersionId, version.getId())
						.eq(OntRelationMapping::getDelFlag, "0")
						.orderByAsc(OntRelationMapping::getSyncOrder));

		for (OntRelationMapping rm : relationMappings) {
			if (isCancelRequested(job.getId())) {
				return;
			}

			updateProgress(job.getId(), "RELATION", rm.getMappingCode(), counters, cursor);

			// V1: 关系摄入通过源行内联或外键查询，此处简化为直接遍历已有绑定关系
			// 完整实现需按 relationMode 构造查询，V1 留 TODO
			try {
				scanAndIngestRelations(job, project, rm, counters);
			}
			catch (Exception e) {
				log.error("Relation mapping {} failed: {}", rm.getMappingCode(), e.getMessage(), e);
			}
		}
	}

	/**
	 * 扫描源数据并摄入关系。
	 * V1 简化：通过源行中主体和客体的键列构建关系命令。
	 */
	private void scanAndIngestRelations(OntMappingJob job, OntMappingProject project,
			OntRelationMapping rm, JobCounters counters) {
		// 加载主体实体映射
		OntEntityMapping subjectEm = entityMappingMapper.selectById(rm.getSubjectEntityMappingId());
		if (subjectEm == null || "1".equals(subjectEm.getDelFlag())) {
			return;
		}

		OntDataSource dataSource = dataSourceMapper.selectById(rm.getSourceId());
		if (dataSource == null || !"ACTIVE".equals(dataSource.getStatus())) {
			return;
		}

		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(dataSource);
		String jdbcUrl = buildJdbcUrl(dataSource);

		// 解析关系键映射，获取主体和客体键列
		List<String> subjectKeyColumns = iriTemplateCompiler.extractKeyColumnNames(rm.getSubjectKeyMapping());
		List<String> objectKeyColumns = iriTemplateCompiler.extractKeyColumnNames(rm.getObjectKeyMapping());

		List<String> columns = new ArrayList<>(subjectKeyColumns);
		for (String col : objectKeyColumns) {
			if (!columns.contains(col)) {
				columns.add(col);
			}
		}

		DataSourceConnector.SourceScanPlan scanPlan = DataSourceConnector.SourceScanPlan.of(
				rm.getSourceSchema(), rm.getSourceObject(), columns,
				DEFAULT_PAGE_SIZE, QUERY_TIMEOUT_SECONDS);

		DataSourceConnector connector = connectorRegistry.getConnector(
				dataSource.getSourceType(), dataSource.getDatabaseType());

		IngestionContext context = IngestionContext.builder()
				.jobId(job.getId())
				.requestedUserId(job.getRequestedUserId())
				.requestedUsername(job.getRequestedBy())
				.configHash(job.getConfigHash())
				.traceId(job.getTraceId())
				.executedAt(Instant.now())
				.build();

		try (Connection conn = poolManager.getConnection(dataSource.getId(), dataSource.getRevision(),
				jdbcUrl, credential.username(), credential.password(), CONNECT_TIMEOUT_SECONDS)) {

			DataSourceConnector.SourceCursor sourceCursor = DataSourceConnector.SourceCursor.initial();
			while (true) {
				if (isCancelRequested(job.getId())) {
					return;
				}

				DataSourceConnector.SourcePage page = connector.scanWithConnection(conn, scanPlan, sourceCursor);
				if (page.rows().isEmpty()) {
					break;
				}

				for (DataSourceConnector.SourceRow row : page.rows()) {
					Map<String, String> rowValues = toStringMap(row.values());
					counters.totalRelations++;

					// 构建主体和客体 SourceIdentity
					LinkedHashMap<String, String> subjectKeyPairs = new LinkedHashMap<>();
					for (String col : subjectKeyColumns) {
						subjectKeyPairs.put(col, rowValues.getOrDefault(col, ""));
					}
					LinkedHashMap<String, String> objectKeyPairs = new LinkedHashMap<>();
					for (String col : objectKeyColumns) {
						objectKeyPairs.put(col, rowValues.getOrDefault(col, ""));
					}

					SourceIdentity subjectIdentity = new SourceIdentity(
							rm.getSourceId(), project.getId(), job.getMappingVersionId(),
							subjectEm.getMappingCode(), rm.getSourceObject(),
							SourceIdentity.buildRecordKey(subjectKeyPairs));

					SourceIdentity objectIdentity = new SourceIdentity(
							rm.getSourceId(), project.getId(), job.getMappingVersionId(),
							subjectEm.getMappingCode(), rm.getSourceObject(),
							SourceIdentity.buildRecordKey(objectKeyPairs));

					com.pig4cloud.pig.ontology.mapping.ingestion.RelationIngestionCommand cmd =
							new com.pig4cloud.pig.ontology.mapping.ingestion.RelationIngestionCommand(
									project.getOntologyId(), rm.getObjectPropertyId(),
									subjectIdentity, objectIdentity, rm.getMappingCode(),
									subjectIdentity.keyHash() + "->" + objectIdentity.keyHash(),
									OwnershipPolicy.valueOf(rm.getOwnershipPolicy()), context);

					try {
						RelationIngestionResult result = ingestionService.upsertRelation(cmd);
						if (result.getStatus() == RelationIngestionResult.Status.FAILED) {
							counters.totalFailed++;
							writeJobRecord(job.getId(), "RELATION", rm.getMappingCode(),
									rm.getSourceObject(), subjectIdentity.keyHash(),
									maskKey(subjectIdentity.sourceRecordKey()), "RELATE", "FAILED",
									result.getErrorCode(), truncate(result.getErrorMessage(), 500), 0);
						}
					}
					catch (Exception e) {
						counters.totalFailed++;
						writeJobRecord(job.getId(), "RELATION", rm.getMappingCode(),
								rm.getSourceObject(), subjectIdentity.keyHash(),
								maskKey(subjectIdentity.sourceRecordKey()), "RELATE", "FAILED",
								MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500), 0);
					}
				}

				if (!page.hasMore()) {
					break;
				}
				sourceCursor = page.nextCursor();
			}
		}
		catch (Exception e) {
			log.error("Relation scan failed for mapping {}: {}", rm.getMappingCode(), e.getMessage(), e);
		}
	}

	// ==================== PENDING 重试 ====================

	private void executePendingRetry(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project, MappingCursor cursor, JobCounters counters) {

		List<OntMappingJobRecord> pendingRecords = jobRecordMapper.selectPendingRecords(job.getId());
		if (pendingRecords.isEmpty()) {
			return;
		}

		updateProgress(job.getId(), "PENDING", null, counters, cursor);

		for (OntMappingJobRecord record : pendingRecords) {
			// V1: 仅重试一次，标记为 SKIPPED
			record.setRecordStatus("SKIPPED");
			record.setRetryCount(record.getRetryCount() + 1);
			jobRecordMapper.updateById(record);
			counters.totalSkipped++;
		}
	}

	// ==================== FULL 删除检测 ====================

	private void executeDeleteDetection(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project, JobCounters counters) {

		// 查询所有未被本次作业见到的 ACTIVE 绑定
		List<OntEntityMapping> entityMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, version.getId())
						.eq(OntEntityMapping::getDelFlag, "0")
						.eq(OntEntityMapping::getEnabled, "1"));

		for (OntEntityMapping em : entityMappings) {
			List<OntSourceInstanceBinding> unseenBindings = bindingMapper.selectList(
					Wrappers.<OntSourceInstanceBinding>lambdaQuery()
							.eq(OntSourceInstanceBinding::getMappingProjectId, project.getId())
							.eq(OntSourceInstanceBinding::getEntityMappingCode, em.getMappingCode())
							.eq(OntSourceInstanceBinding::getBindingStatus, "ACTIVE")
							.eq(OntSourceInstanceBinding::getDelFlag, "0")
							.and(w -> w.isNull(OntSourceInstanceBinding::getLastSeenJobId)
									.or(w2 -> w2.ne(OntSourceInstanceBinding::getLastSeenJobId, job.getId()))));

			DeleteStrategy strategy = DeleteStrategy.valueOf(em.getDeleteStrategy());

			for (OntSourceInstanceBinding binding : unseenBindings) {
				// 增加 missCount
				int missCount = (binding.getMissCount() != null ? binding.getMissCount() : 0) + 1;
				binding.setMissCount(missCount);

				if (missCount >= FULL_MISS_THRESHOLD) {
					// 连续未见，执行删除策略
					SourceIdentity sourceIdentity = new SourceIdentity(
							binding.getSourceId(), binding.getMappingProjectId(),
							job.getMappingVersionId(), binding.getEntityMappingCode(),
							binding.getSourceObject(), binding.getSourceRecordKey());

					try {
						DeactivationResult result = ingestionService.deactivateEntity(
								sourceIdentity, strategy,
								IngestionContext.builder()
										.jobId(job.getId())
										.requestedUsername(job.getRequestedBy())
										.traceId(job.getTraceId())
										.executedAt(Instant.now())
										.build());

						if (result.getStatus() == DeactivationResult.Status.DEACTIVATED
								|| result.getStatus() == DeactivationResult.Status.SOFT_DELETED) {
							counters.totalSkipped++;
							writeJobRecord(job.getId(), "DELETE", em.getMappingCode(),
									binding.getSourceObject(), binding.getSourceRecordKeyHash(),
									maskKey(binding.getSourceRecordKey()), "DEACTIVATE", "SUCCESS",
									null, null, 0);
						}
						else if (result.getStatus() == DeactivationResult.Status.FAILED) {
							counters.totalFailed++;
							writeJobRecord(job.getId(), "DELETE", em.getMappingCode(),
									binding.getSourceObject(), binding.getSourceRecordKeyHash(),
									maskKey(binding.getSourceRecordKey()), "DEACTIVATE", "FAILED",
									result.getErrorCode(), truncate(result.getErrorMessage(), 500), 0);
						}
					}
					catch (Exception e) {
						counters.totalFailed++;
						writeJobRecord(job.getId(), "DELETE", em.getMappingCode(),
								binding.getSourceObject(), binding.getSourceRecordKeyHash(),
								maskKey(binding.getSourceRecordKey()), "DEACTIVATE", "FAILED",
								MappingJobErrorCode.ONT_MAP_029.getCode(), truncate(e.getMessage(), 500), 0);
					}
				}
				else {
					// 标记为 MISSING
					binding.setBindingStatus("MISSING");
				}
				bindingRepository.update(binding);
			}
		}
	}

	// ==================== RETRY 执行 ====================

	private void executeRetry(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project, MappingCursor cursor, JobCounters counters) {

		// 查询原作业的失败记录
		List<OntMappingJobRecord> failedRecords = jobRecordMapper.selectFailedRecords(
				job.getMappingVersionId(), MAX_RETRY_RECORDS);

		// TODO: V1 仅按原版本ID查询，实际应按 retry 作业关联的原作业ID查询
		// 临时方案：查全部该版本的失败记录

		for (OntMappingJobRecord record : failedRecords) {
			if (isCancelRequested(job.getId())) {
				return;
			}

			// 通过 findByKey 重新查询源记录
			OntEntityMapping em = entityMappingMapper.selectOne(
					Wrappers.<OntEntityMapping>lambdaQuery()
							.eq(OntEntityMapping::getMappingVersionId, version.getId())
							.eq(OntEntityMapping::getMappingCode, record.getMappingCode())
							.eq(OntEntityMapping::getDelFlag, "0"));

			if (em == null) {
				continue;
			}

			OntDataSource dataSource = dataSourceMapper.selectById(em.getSourceId());
			if (dataSource == null || !"ACTIVE".equals(dataSource.getStatus())) {
				continue;
			}

			try {
				retrySingleRecord(job, project, em, dataSource, record, counters);
			}
			catch (Exception e) {
				counters.totalFailed++;
				log.error("Retry record {} failed: {}", record.getId(), e.getMessage());
			}
		}
	}

	private void retrySingleRecord(OntMappingJob job, OntMappingProject project,
			OntEntityMapping em, OntDataSource dataSource, OntMappingJobRecord originalRecord,
			JobCounters counters) {

		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(dataSource);
		String jdbcUrl = buildJdbcUrl(dataSource);

		List<String> keyColumnNames = iriTemplateCompiler.extractKeyColumnNames(em.getKeyColumns());
		List<OntFieldMapping> fieldMappings = fieldMappingMapper.selectList(
				Wrappers.<OntFieldMapping>lambdaQuery()
						.eq(OntFieldMapping::getEntityMappingId, em.getId())
						.eq(OntFieldMapping::getDelFlag, "0"));

		List<String> columns = new ArrayList<>(keyColumnNames);
		for (OntFieldMapping fm : fieldMappings) {
			if ("COLUMN".equals(fm.getSourceKind()) && fm.getSourceColumn() != null
					&& !columns.contains(fm.getSourceColumn())) {
				columns.add(fm.getSourceColumn());
			}
		}

		DataSourceConnector.SourceLookupPlan lookupPlan = new DataSourceConnector.SourceLookupPlan(
				em.getSourceSchema(), em.getSourceObject(), columns, keyColumnNames);

		DataSourceConnector connector = connectorRegistry.getConnector(
				dataSource.getSourceType(), dataSource.getDatabaseType());

		try (Connection conn = poolManager.getConnection(dataSource.getId(), dataSource.getRevision(),
				jdbcUrl, credential.username(), credential.password(), CONNECT_TIMEOUT_SECONDS)) {

			// 构建查询键（从记录中恢复，V1简化：无法恢复完整键，跳过）
			// 实际应从 originalRecord.sourceRecordKeyHash 反查绑定表获取 sourceRecordKey
			OntSourceInstanceBinding binding = bindingMapper.selectOne(
					Wrappers.<OntSourceInstanceBinding>lambdaQuery()
							.eq(OntSourceInstanceBinding::getSourceRecordKeyHash,
									originalRecord.getSourceRecordKeyHash())
							.eq(OntSourceInstanceBinding::getDelFlag, "0")
							.last("LIMIT 1"));

			if (binding == null) {
				// 绑定不存在，按删除策略处理
				counters.totalSkipped++;
				return;
			}

			// 从 sourceRecordKey 解析键值（简化：V1 直接使用绑定记录做 upsert）
			// 完整实现应从源表 findByKey 获取最新行再摄入
			counters.totalRead++;

			IngestionContext context = IngestionContext.builder()
					.jobId(job.getId())
					.requestedUsername(job.getRequestedBy())
					.configHash(job.getConfigHash())
					.traceId(job.getTraceId())
					.executedAt(Instant.now())
					.build();

			// 构建命令并重试摄入
			SourceIdentity sourceIdentity = new SourceIdentity(
					dataSource.getId(), project.getId(), job.getMappingVersionId(),
					em.getMappingCode(), em.getSourceObject(), binding.getSourceRecordKey());

			// V1 简化：直接重新 upsert（幂等保证不重复）
			// 完整实现需从源表重新读取行数据
			EntityIngestionCommand command = new EntityIngestionCommand(
					project.getOntologyId(), em.getTargetNamespaceId(), em.getTargetEntityTypeId(),
					sourceIdentity, null, null, null, List.of(),
					OwnershipPolicy.SOURCE_WINS, null, null, context);

			try {
				IngestionResult result = ingestionService.upsertEntity(command);
				if (result.getStatus() == IngestionResult.Status.FAILED) {
					counters.totalFailed++;
					// 新增重试记录
					OntMappingJobRecord retryRecord = new OntMappingJobRecord();
					retryRecord.setJobId(job.getId());
					retryRecord.setRetryOfRecordId(originalRecord.getId());
					retryRecord.setPhase("ENTITY");
					retryRecord.setMappingCode(em.getMappingCode());
					retryRecord.setSourceObject(em.getSourceObject());
					retryRecord.setSourceRecordKeyHash(originalRecord.getSourceRecordKeyHash());
					retryRecord.setSourceRecordKeyMasked(originalRecord.getSourceRecordKeyMasked());
					retryRecord.setRecordAction("UPDATE");
					retryRecord.setRecordStatus("FAILED");
					retryRecord.setErrorCode(result.getErrorCode());
					retryRecord.setErrorMessage(truncate(result.getErrorMessage(), 500));
					retryRecord.setRetryCount(originalRecord.getRetryCount() + 1);
					retryRecord.setDurationMs(0L);
					jobRecordMapper.insert(retryRecord);
				}
				else {
					counters.totalCreated++;
					// 成功记录
					OntMappingJobRecord retryRecord = new OntMappingJobRecord();
					retryRecord.setJobId(job.getId());
					retryRecord.setRetryOfRecordId(originalRecord.getId());
					retryRecord.setPhase("ENTITY");
					retryRecord.setMappingCode(em.getMappingCode());
					retryRecord.setSourceObject(em.getSourceObject());
					retryRecord.setSourceRecordKeyHash(originalRecord.getSourceRecordKeyHash());
					retryRecord.setSourceRecordKeyMasked(originalRecord.getSourceRecordKeyMasked());
					retryRecord.setRecordAction("UPDATE");
					retryRecord.setRecordStatus("SUCCESS");
					retryRecord.setRetryCount(originalRecord.getRetryCount() + 1);
					retryRecord.setDurationMs(0L);
					jobRecordMapper.insert(retryRecord);
				}
			}
			catch (Exception e) {
				counters.totalFailed++;
			}
		}
		catch (Exception e) {
			counters.totalFailed++;
			log.error("Retry record {} connection/query failed: {}",
					originalRecord.getId(), e.getMessage());
		}
	}

	// ==================== 取消 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingJobVO cancelJob(Long jobId) {
		OntMappingJob job = baseMapper.selectById(jobId);
		if (job == null || "1".equals(job.getDelFlag())) {
			throw new IllegalArgumentException(MappingJobErrorCode.ONT_MAP_023.getMessage());
		}

		if ("SUCCEEDED".equals(job.getJobStatus()) || "FAILED".equals(job.getJobStatus())
				|| "CANCELLED".equals(job.getJobStatus()) || "PARTIAL_SUCCESS".equals(job.getJobStatus())) {
			throw new IllegalStateException(MappingJobErrorCode.ONT_MAP_024.getMessage()
					+ ": 作业已终态: " + job.getJobStatus());
		}

		baseMapper.requestCancel(jobId);
		log.info("Cancel requested for job {}", jobId);
		return toVO(baseMapper.selectById(jobId));
	}

	// ==================== 重试 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MappingJobVO retryJob(Long jobId, JobRetryRequest request) {
		OntMappingJob originalJob = baseMapper.selectById(jobId);
		if (originalJob == null || "1".equals(originalJob.getDelFlag())) {
			throw new IllegalArgumentException(MappingJobErrorCode.ONT_MAP_023.getMessage());
		}

		// 查询失败记录
		List<OntMappingJobRecord> failedRecords;
		if (request.getRecordIds() != null && !request.getRecordIds().isEmpty()) {
			failedRecords = jobRecordMapper.selectByIds(request.getRecordIds().stream()
					.limit(MAX_RETRY_RECORDS).toList());
		}
		else {
			failedRecords = jobRecordMapper.selectFailedRecords(jobId, MAX_RETRY_RECORDS);
		}

		if (failedRecords.isEmpty()) {
			throw new IllegalStateException(MappingJobErrorCode.ONT_MAP_030.getMessage());
		}

		// 创建 RETRY 作业
		JobCreateRequest createRequest = new JobCreateRequest();
		createRequest.setRunType("RETRY");
		createRequest.setMaxErrorRate(request.getMaxErrorRate());

		MappingJobVO retryJob = createJob(originalJob.getMappingVersionId(), createRequest);
		return retryJob;
	}

	// ==================== 查询 ====================

	@Override
	public MappingJobVO getJobDetail(Long jobId) {
		OntMappingJob job = baseMapper.selectById(jobId);
		if (job == null || "1".equals(job.getDelFlag())) {
			throw new IllegalArgumentException(MappingJobErrorCode.ONT_MAP_023.getMessage());
		}
		return toVO(job);
	}

	@Override
	public Page<MappingJobVO> page(Page<OntMappingJob> page, JobQuery query) {
		Page<OntMappingJob> result = baseMapper.selectPage(page,
				Wrappers.<OntMappingJob>lambdaQuery()
						.eq(query.getProjectId() != null, OntMappingJob::getMappingProjectId,
								query.getProjectId())
						.eq(StrUtil.isNotBlank(query.getRunType()), OntMappingJob::getRunType,
								query.getRunType())
						.eq(StrUtil.isNotBlank(query.getJobStatus()), OntMappingJob::getJobStatus,
								query.getJobStatus())
						.eq(StrUtil.isNotBlank(query.getTriggerType()), OntMappingJob::getTriggerType,
								query.getTriggerType())
						.eq(OntMappingJob::getDelFlag, "0")
						.orderByDesc(OntMappingJob::getCreateTime));

		Page<MappingJobVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	@Override
	public Page<MappingJobRecordVO> getJobRecords(Long jobId, Page<OntMappingJobRecord> page, String recordStatus) {
		Page<OntMappingJobRecord> result = jobRecordMapper.selectPage(page,
				Wrappers.<OntMappingJobRecord>lambdaQuery()
						.eq(OntMappingJobRecord::getJobId, jobId)
						.eq(OntMappingJobRecord::getDelFlag, "0")
						.eq(StrUtil.isNotBlank(recordStatus),
								OntMappingJobRecord::getRecordStatus, recordStatus)
						.orderByDesc(OntMappingJobRecord::getCreateTime));

		Page<MappingJobRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toRecordVO).toList());
		return voPage;
	}

	@Override
	public String getProjectCursor(Long projectId) {
		OntMappingProject project = projectMapper.selectById(projectId);
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}
		if (project.getLastJobId() == null) {
			return null;
		}
		OntMappingJob lastJob = baseMapper.selectById(project.getLastJobId());
		return lastJob != null ? lastJob.getCursorAfter() : null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateSchedule(Long projectId, boolean scheduleEnabled, String scheduleCron,
			String scheduleRunType) {
		OntMappingProject project = projectMapper.selectForUpdate(projectId);
		if (project == null || "1".equals(project.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_001.getMessage());
		}

		if (scheduleEnabled && project.getActiveVersionId() == null) {
			throw new IllegalStateException(MappingErrorCode.ONT_MAP_019.getMessage());
		}

		project.setScheduleEnabled(scheduleEnabled ? "1" : "0");
		project.setScheduleCron(scheduleCron);
		project.setScheduleRunType(scheduleRunType);
		projectMapper.updateById(project);

		// V1: 不注册 Quartz Trigger，仅持久化配置
		// TODO: 集成 Quartz 调度框架注册/重建 Trigger
		log.info("Updated schedule for project {}: enabled={}, cron={}, runType={}",
				projectId, scheduleEnabled, scheduleCron, scheduleRunType);
	}

	// ==================== 内部辅助方法 ====================

	private void validateExecutionPreconditions(OntMappingJob job, OntMappingVersion version,
			OntMappingProject project) {
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}
		if (!"PUBLISHED".equals(version.getVersionStatus())
				&& !("RETRY".equals(job.getRunType()) && "RETIRED".equals(version.getVersionStatus()))) {
			throw new IllegalStateException(MappingJobErrorCode.ONT_MAP_021.getMessage());
		}
	}

	private boolean isCancelRequested(Long jobId) {
		OntMappingJob job = baseMapper.selectById(jobId);
		return job != null && "1".equals(job.getCancelRequested());
	}

	private void checkErrorRate(OntMappingJob job, JobCounters counters) {
		double maxErrorRate = DEFAULT_MAX_ERROR_RATE;
		if (counters.totalRead >= ERROR_RATE_MIN_READS) {
			double errorRate = (double) counters.totalFailed / counters.totalRead;
			if (errorRate > maxErrorRate) {
				throw new RuntimeException(MappingJobErrorCode.ONT_MAP_027.getMessage()
						+ ": " + String.format("%.2f%%", errorRate * 100));
			}
		}
		else if (counters.totalFailed >= ERROR_RATE_ABSOLUTE_LIMIT) {
			throw new RuntimeException(MappingJobErrorCode.ONT_MAP_027.getMessage()
					+ ": 绝对失败数超过 " + ERROR_RATE_ABSOLUTE_LIMIT);
		}
	}

	private void updateProgress(Long jobId, String phase, String mappingCode,
			JobCounters counters, MappingCursor cursor) {
		baseMapper.updateProgress(jobId, phase, mappingCode,
				counters.getPageNo(), counters.totalRead, counters.totalCreated,
				counters.totalUpdated, counters.totalUnchanged, counters.totalSkipped,
				counters.totalFailed, counters.totalRelations,
				cursorCodec.encode(cursor), LocalDateTime.now());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void writeJobRecord(Long jobId, String phase, String mappingCode, String sourceObject,
			String keyHash, String maskedKey, String action, String status,
			String errorCode, String errorMessage, long durationMs) {
		if (!"FAILED".equals(status) && !"PENDING".equals(status)) {
			return; // 只持久化 FAILED/PENDING
		}
		OntMappingJobRecord record = new OntMappingJobRecord();
		record.setJobId(jobId);
		record.setPhase(phase);
		record.setMappingCode(mappingCode);
		record.setSourceObject(sourceObject);
		record.setSourceRecordKeyHash(keyHash != null ? keyHash : "unknown");
		record.setSourceRecordKeyMasked(maskedKey);
		record.setRecordAction(action);
		record.setRecordStatus(status);
		record.setErrorCode(errorCode);
		record.setErrorMessage(errorMessage);
		record.setRetryCount(0);
		record.setDurationMs(durationMs);
		jobRecordMapper.insert(record);
	}

	private void updateBindingLastSeen(Long bindingId, Long jobId) {
		OntSourceInstanceBinding binding = bindingMapper.selectById(bindingId);
		if (binding != null && "0".equals(binding.getDelFlag())) {
			binding.setLastSeenJobId(jobId);
			binding.setLastSeenAt(LocalDateTime.now());
			if ("MISSING".equals(binding.getBindingStatus())) {
				binding.setBindingStatus("ACTIVE");
				binding.setMissCount(0);
			}
			bindingRepository.update(binding);
		}
	}

	private void finishJob(Long jobId, String expectedStatus, String targetStatus,
			String errorCode, String errorMessage, JobCounters counters, MappingCursor cursor) {
		// 先更新最终计数和游标
		baseMapper.updateProgress(jobId, null, null, counters.getPageNo(),
				counters.totalRead, counters.totalCreated, counters.totalUpdated,
				counters.totalUnchanged, counters.totalSkipped, counters.totalFailed,
				counters.totalRelations, cursorCodec.encode(cursor), LocalDateTime.now());

		// CAS 到终态
		int updated = baseMapper.markFinished(jobId, expectedStatus, targetStatus,
				LocalDateTime.now(), errorCode, errorMessage);
		if (updated == 0) {
			log.warn("Failed to mark job {} as {} (expected {}), may already be in terminal state",
					jobId, targetStatus, expectedStatus);
		}
	}

	private void updateProjectAfterJob(Long projectId, Long jobId, String cursorJson) {
		OntMappingProject project = projectMapper.selectById(projectId);
		if (project != null && "0".equals(project.getDelFlag())) {
			project.setLastJobId(jobId);
			projectMapper.updateById(project);
		}
	}

	private void publishEvent(OntMappingJob job, String eventType, String operation) {
		try {
			eventPublisher.append(OntologyDomainEvent.builder()
					.eventType(eventType)
					.aggregateType("MAPPING_JOB")
					.aggregateId(job.getId().toString())
					.operation(operation)
					.payload(Map.of(
							"jobId", job.getId(),
							"projectId", job.getMappingProjectId(),
							"versionId", job.getMappingVersionId(),
							"runType", job.getRunType(),
							"status", job.getJobStatus()))
					.build());
		}
		catch (Exception e) {
			log.warn("Failed to publish event for job {}: {}", job.getId(), e.getMessage());
		}
	}

	private String getLastJobCursor(Long lastJobId) {
		OntMappingJob lastJob = baseMapper.selectById(lastJobId);
		return lastJob != null ? lastJob.getCursorAfter() : null;
	}

	private DataSourceConnector.SourceCursor toSourceCursor(MappingCursor.EntityCursor entityCursor,
			boolean incremental) {
		if (entityCursor.getLastKey() == null || entityCursor.getLastKey().isEmpty()) {
			return DataSourceConnector.SourceCursor.initial();
		}
		List<Object> keyValues = new ArrayList<>(entityCursor.getLastKey().values());
		String incValue = incremental ? entityCursor.getIncrementalValue() : null;
		return new DataSourceConnector.SourceCursor(keyValues, incValue);
	}

	private void updateEntityCursor(MappingCursor.EntityCursor entityCursor,
			DataSourceConnector.SourceCursor sourceCursor, boolean incremental) {
		if (sourceCursor.lastKeyValues() != null) {
			List<Object> keyValues = sourceCursor.lastKeyValues();
			Map<String, String> lastKey = new LinkedHashMap<>();
			for (int i = 0; i < keyValues.size(); i++) {
				lastKey.put("key_" + i, String.valueOf(keyValues.get(i)));
			}
			entityCursor.setLastKey(lastKey);
		}
		if (incremental && sourceCursor.incrementalValue() != null) {
			entityCursor.setIncrementalValue(sourceCursor.incrementalValue());
		}
	}

	private void updateEntityCursorFromRow(MappingCursor.EntityCursor entityCursor,
			DataSourceConnector.SourceRow lastRow, List<String> keyColumns,
			String incrementalColumn, boolean incremental) {
		Map<String, Object> values = lastRow.values();
		Map<String, String> lastKey = new LinkedHashMap<>();
		for (String col : keyColumns) {
			Object val = values.get(col);
			lastKey.put(col, val != null ? String.valueOf(val) : "");
		}
		entityCursor.setLastKey(lastKey);
		if (incremental && incrementalColumn != null) {
			Object incVal = values.get(incrementalColumn);
			if (incVal != null) {
				entityCursor.setIncrementalValue(String.valueOf(incVal));
			}
		}
	}

	private String determineFinalStatus(JobCounters counters) {
		if (counters.totalFailed == 0) {
			return "SUCCEEDED";
		}
		if (counters.totalCreated > 0 || counters.totalUpdated > 0 || counters.totalUnchanged > 0) {
			return "PARTIAL_SUCCESS";
		}
		return "FAILED";
	}

	private DataSourceCredentialCryptoService.DataSourceCredential decryptCredential(OntDataSource ds) {
		try {
			DataSourceCredentialCryptoService.EncryptedCredential encrypted =
					new DataSourceCredentialCryptoService.EncryptedCredential(
							ds.getCredentialCiphertext(), ds.getCredentialKeyId(), ds.getCredentialIv());
			return credentialCryptoService.decrypt(ds.getId(), ds.getSourceCode(),
					ds.getRevision(), encrypted);
		}
		catch (Exception e) {
			throw new RuntimeException(MappingJobErrorCode.ONT_MAP_026.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private String buildJdbcUrl(OntDataSource ds) {
		try {
			Map<String, Object> config = objectMapper.readValue(ds.getConnectionConfig(), Map.class);
			if ("JDBC_URL".equals(ds.getConnectionMode())) {
				String jdbcUrl = (String) config.get("jdbcUrl");
				PostgreSqlDialect.validateJdbcUrl(jdbcUrl);
				return jdbcUrl;
			}
			else {
				String host = (String) config.get("host");
				int port = config.get("port") != null ? ((Number) config.get("port")).intValue() : 5432;
				String database = (String) config.get("database");
				String sslMode = (String) config.get("sslMode");
				return PostgreSqlDialect.buildJdbcUrl(host, port, database, sslMode);
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to build JDBC URL", e);
		}
	}

	private String buildAuthorizationSnapshot() {
		try {
			PigUser user = SecurityUtils.getUser();
			if (user != null) {
				Map<String, Object> snapshot = new LinkedHashMap<>();
				snapshot.put("username", user.getUsername());
				snapshot.put("userId", user.getId());
				snapshot.put("roleIds", user.getRoleIds());
				snapshot.put("deptId", user.getDeptId());
				return objectMapper.writeValueAsString(snapshot);
			}
		}
		catch (Exception e) {
			// 安全上下文不可用时回退
		}
		return "{}";
	}

	private String getCurrentUsername() {
		try {
			PigUser user = SecurityUtils.getUser();
			if (user != null) {
				return user.getUsername();
			}
		}
		catch (Exception e) {
			// 回退
		}
		return "system";
	}

	private Long getCurrentUserId() {
		try {
			PigUser user = SecurityUtils.getUser();
			if (user != null) {
				return user.getId();
			}
		}
		catch (Exception e) {
			// 回退
		}
		return null;
	}

	private String buildLeaseOwner() {
		String appName = System.getProperty("spring.application.name", "pig-ontology");
		String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);
		String thread = Thread.currentThread().getName();
		return appName + ":" + instanceId + ":" + thread;
	}

	private Map<String, String> toStringMap(Map<String, Object> values) {
		Map<String, String> result = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			result.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : null);
		}
		return result;
	}

	private String computeContentHash(Map<String, String> values) {
		// 简单内容哈希：拼接值后 SHA-256
		String joined = values.entrySet().stream()
				.map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
				.collect(Collectors.joining("&"));
		return SourceIdentity.buildRecordKey(new LinkedHashMap<>(values));
	}

	private Instant parseInstant(String value) {
		try {
			return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
		}
		catch (Exception e) {
			return Instant.parse(value);
		}
	}

	private String maskKey(String recordKey) {
		if (recordKey == null || recordKey.length() <= 8) {
			return "***";
		}
		return recordKey.substring(0, 4) + "***" + recordKey.substring(recordKey.length() - 4);
	}

	private String truncate(String value, int maxLen) {
		if (value == null) {
			return null;
		}
		return value.length() > maxLen ? value.substring(0, maxLen) : value;
	}

	private MappingJobVO toVO(OntMappingJob job) {
		MappingJobVO vo = new MappingJobVO();
		vo.setId(job.getId());
		vo.setMappingProjectId(job.getMappingProjectId());
		vo.setMappingVersionId(job.getMappingVersionId());
		vo.setRunType(job.getRunType());
		vo.setTriggerType(job.getTriggerType());
		vo.setJobStatus(job.getJobStatus());
		vo.setRequestedBy(job.getRequestedBy());
		vo.setConfigHash(job.getConfigHash());
		vo.setOntologyVersionId(job.getOntologyVersionId());
		vo.setWorkspaceRevision(job.getWorkspaceRevision());
		vo.setCurrentPhase(job.getCurrentPhase());
		vo.setCurrentMappingCode(job.getCurrentMappingCode());
		vo.setCurrentPageNo(job.getCurrentPageNo());
		vo.setPageSize(job.getPageSize());
		vo.setTotalRead(job.getTotalRead());
		vo.setTotalCreated(job.getTotalCreated());
		vo.setTotalUpdated(job.getTotalUpdated());
		vo.setTotalUnchanged(job.getTotalUnchanged());
		vo.setTotalSkipped(job.getTotalSkipped());
		vo.setTotalFailed(job.getTotalFailed());
		vo.setTotalRelations(job.getTotalRelations());
		vo.setCancelRequested(job.getCancelRequested());
		vo.setStartedAt(job.getStartedAt());
		vo.setFinishedAt(job.getFinishedAt());
		vo.setErrorCode(job.getErrorCode());
		vo.setErrorMessage(job.getErrorMessage());
		vo.setTraceId(job.getTraceId());
		vo.setCreateTime(job.getCreateTime());
		vo.setUpdateTime(job.getUpdateTime());
		return vo;
	}

	private MappingJobRecordVO toRecordVO(OntMappingJobRecord record) {
		MappingJobRecordVO vo = new MappingJobRecordVO();
		vo.setId(record.getId());
		vo.setJobId(record.getJobId());
		vo.setRetryOfRecordId(record.getRetryOfRecordId());
		vo.setPhase(record.getPhase());
		vo.setMappingCode(record.getMappingCode());
		vo.setSourceObject(record.getSourceObject());
		vo.setSourceRecordKeyMasked(record.getSourceRecordKeyMasked());
		vo.setRecordAction(record.getRecordAction());
		vo.setRecordStatus(record.getRecordStatus());
		vo.setInstanceId(record.getInstanceId());
		vo.setErrorCode(record.getErrorCode());
		vo.setErrorMessage(record.getErrorMessage());
		vo.setRetryCount(record.getRetryCount());
		vo.setDurationMs(record.getDurationMs());
		vo.setSourceUpdatedAt(record.getSourceUpdatedAt());
		vo.setCreateTime(record.getCreateTime());
		return vo;
	}

	// ==================== 内部计数器 ====================

	/**
	 * 作业执行计数器。
	 */
	private static class JobCounters {

		long totalRead = 0;

		long totalCreated = 0;

		long totalUpdated = 0;

		long totalUnchanged = 0;

		long totalSkipped = 0;

		long totalFailed = 0;

		long totalRelations = 0;

		long pageNo = 0;

		long getPageNo() {
			return pageNo;
		}

	}

}
