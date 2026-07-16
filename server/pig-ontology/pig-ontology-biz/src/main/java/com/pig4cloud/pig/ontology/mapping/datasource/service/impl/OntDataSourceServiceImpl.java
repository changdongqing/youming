/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.DataSourceErrorCode;
import com.pig4cloud.pig.ontology.mapping.datasource.DataSourcePoolManager;
import com.pig4cloud.pig.ontology.mapping.datasource.crypto.DataSourceCredentialCryptoService;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceCreateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.MetadataObjectQuery;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.SchemaPreviewRequest;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSourceMetadata;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMetadataMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.ConnectorRegistry;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.DataSourceConnector;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql.PostgreSqlDialect;
import com.pig4cloud.pig.ontology.mapping.datasource.service.OntDataSourceService;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.DataSourceVO;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.SourceObjectMetadataVO;
import com.pig4cloud.pig.ontology.mapping.integration.MappingAuditFacade;
import com.pig4cloud.pig.ontology.mapping.integration.MappingMetrics;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubjectResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据源注册服务实现。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntDataSourceServiceImpl extends ServiceImpl<OntDataSourceMapper, OntDataSource>
		implements OntDataSourceService {

	private final OntDataSourceMetadataMapper metadataMapper;

	private final DataSourceCredentialCryptoService cryptoService;

	private final DataSourcePoolManager poolManager;

	private final ConnectorRegistry connectorRegistry;

	private final ObjectMapper objectMapper;

	private final SecuritySubjectResolver securitySubjectResolver;

	private final MappingAuditFacade auditFacade;

	private final MappingMetrics metrics;

	private static final int DEFAULT_TIMEOUT_SECONDS = 5;

	private static final int MAX_METADATA_OBJECTS = 500;

	// ==================== 查询 ====================

	@Override
	public Page<DataSourceVO> page(Page<OntDataSource> page, String sourceCode, String sourceName, String status) {
		Page<OntDataSource> result = baseMapper.selectPage(page,
				Wrappers.<OntDataSource>lambdaQuery()
						.like(StrUtil.isNotBlank(sourceCode), OntDataSource::getSourceCode, sourceCode)
						.like(StrUtil.isNotBlank(sourceName), OntDataSource::getSourceName, sourceName)
						.eq(StrUtil.isNotBlank(status), OntDataSource::getStatus, status)
						.eq(OntDataSource::getDelFlag, "0")
						.orderByDesc(OntDataSource::getCreateTime));

		Page<DataSourceVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	@Override
	public DataSourceVO getDetail(Long id) {
		OntDataSource ds = findByIdOrThrow(id);
		return toVO(ds);
	}

	// ==================== 创建 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DataSourceVO create(DataSourceCreateDTO dto) {
		// 校验 sourceCode 唯一
		Long existing = baseMapper.selectCount(Wrappers.<OntDataSource>lambdaQuery()
				.eq(OntDataSource::getSourceCode, dto.getSourceCode())
				.eq(OntDataSource::getDelFlag, "0"));
		if (existing > 0) {
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_017.getMessage());
		}

		// 校验连接配置
		validateConnectionConfig(dto.getConnectionMode(), dto.getConnectionConfig());

		OntDataSource ds = new OntDataSource();
		ds.setSourceCode(dto.getSourceCode());
		ds.setSourceName(dto.getSourceName());
		ds.setSourceType("JDBC");
		ds.setDatabaseType("POSTGRESQL");
		ds.setConnectionMode(dto.getConnectionMode());
		ds.setConnectionConfig(dto.getConnectionConfig());
		ds.setAllowedSchemas(serializeList(dto.getAllowedSchemas()));
		ds.setAllowedObjects(serializeList(dto.getAllowedObjects()));
		ds.setStatus("DRAFT");
		ds.setRevision(0L);
		ds.setSecurityLevelCode(StrUtil.isBlank(dto.getSecurityLevelCode()) ? "RESTRICTED" : dto.getSecurityLevelCode());
		ds.setRemarks(dto.getRemarks());

		// 加密凭证
		DataSourceCredentialCryptoService.DataSourceCredential credential =
				new DataSourceCredentialCryptoService.DataSourceCredential(dto.getUsername(), dto.getPassword());
		DataSourceCredentialCryptoService.EncryptedCredential encrypted =
				cryptoService.encrypt(null, dto.getSourceCode(), 0L, credential);
		ds.setCredentialCiphertext(encrypted.getCiphertext());
		ds.setCredentialIv(encrypted.getIv());
		ds.setCredentialKeyId(encrypted.getKeyId());

		baseMapper.insert(ds);

		// 更新 AAD 中的 sourceId（重新加密以确保 AAD 包含真实 sourceId）
		DataSourceCredentialCryptoService.EncryptedCredential reencrypted =
				cryptoService.encrypt(ds.getId(), ds.getSourceCode(), ds.getRevision(), credential);
		ds.setCredentialCiphertext(reencrypted.getCiphertext());
		ds.setCredentialIv(reencrypted.getIv());
		ds.setCredentialKeyId(reencrypted.getKeyId());
		baseMapper.updateById(ds);

		log.info("Created data source: id={}, code={}", ds.getId(), ds.getSourceCode());

		// 审计（18-08 §9）
		SecuritySubject subject = securitySubjectResolver.resolve();
		auditFacade.auditDataSourceOperation(null, subject, ds.getId(), "CREATED", "SUCCESS", null, null);

		return toVO(ds);
	}

	// ==================== 更新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DataSourceVO update(DataSourceUpdateDTO dto) {
		OntDataSource ds = findByIdOrThrow(dto.getId());

		validateConnectionConfig(dto.getConnectionMode(), dto.getConnectionConfig());

		// 递增 revision
		Long newRevision = ds.getRevision() + 1;

		ds.setSourceName(dto.getSourceName());
		ds.setConnectionMode(dto.getConnectionMode());
		ds.setConnectionConfig(dto.getConnectionConfig());
		ds.setAllowedSchemas(serializeList(dto.getAllowedSchemas()));
		ds.setAllowedObjects(serializeList(dto.getAllowedObjects()));
		ds.setSecurityLevelCode(StrUtil.isBlank(dto.getSecurityLevelCode()) ? ds.getSecurityLevelCode()
				: dto.getSecurityLevelCode());
		ds.setRemarks(dto.getRemarks());

		// 如果提供了新凭证则重新加密
		if (StrUtil.isNotBlank(dto.getUsername()) && StrUtil.isNotBlank(dto.getPassword())) {
			DataSourceCredentialCryptoService.DataSourceCredential credential =
					new DataSourceCredentialCryptoService.DataSourceCredential(dto.getUsername(), dto.getPassword());
			DataSourceCredentialCryptoService.EncryptedCredential encrypted =
					cryptoService.encrypt(ds.getId(), ds.getSourceCode(), newRevision, credential);
			ds.setCredentialCiphertext(encrypted.getCiphertext());
			ds.setCredentialIv(encrypted.getIv());
			ds.setCredentialKeyId(encrypted.getKeyId());
		}

		ds.setRevision(newRevision);
		// 修改连接/凭证/白名单：回 DRAFT 并清空测试状态
		ds.setStatus("DRAFT");
		ds.setLastTestStatus(null);
		ds.setLastTestRevision(null);
		ds.setLastTestAt(null);
		ds.setLastTestLatencyMs(null);
		ds.setLastTestErrorCode(null);

		baseMapper.updateById(ds);

		// 失效旧连接池
		poolManager.invalidateBySourceId(ds.getId());

		log.info("Updated data source: id={}, revision={}", ds.getId(), newRevision);

		// 审计（18-08 §9）
		SecuritySubject subject = securitySubjectResolver.resolve();
		auditFacade.auditDataSourceOperation(null, subject, ds.getId(), "UPDATED", "SUCCESS", null, null);

		return toVO(ds);
	}

	// ==================== 删除 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		OntDataSource ds = findByIdOrThrow(id);

		// 只允许 DISABLED 删除
		if (!"DISABLED".equals(ds.getStatus())) {
			throw new IllegalStateException(
					DataSourceErrorCode.ONT_DS_013.getMessage() + ": 仅 DISABLED 状态可删除");
		}

		// TODO: 校验是否被 PUBLISHED 映射版本/运行中作业/ACTIVE 来源绑定引用
		// 暂留接口，待 18-03/18-07 实现后补充

		// 失效连接池
		poolManager.invalidateBySourceId(id);

		// 逻辑删除
		ds.setDelFlag("1");
		baseMapper.updateById(ds);

		log.info("Removed data source: id={}", id);
		return true;
	}

	// ==================== 连接测试 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ConnectionTestOutcome testConnection(Long id) {
		OntDataSource ds = findByIdOrThrow(id);

		// 解密凭证
		DataSourceCredentialCryptoService.DataSourceCredential credential;
		try {
			DataSourceCredentialCryptoService.EncryptedCredential encrypted =
					new DataSourceCredentialCryptoService.EncryptedCredential(
							ds.getCredentialCiphertext(), ds.getCredentialKeyId(), ds.getCredentialIv());
			credential = cryptoService.decrypt(ds.getId(), ds.getSourceCode(), ds.getRevision(), encrypted);
		}
		catch (Exception e) {
			log.error("Credential decryption failed for sourceId={}", id);
			// 记录失败状态
			ds.setLastTestStatus("FAILED");
			ds.setLastTestRevision(ds.getRevision());
			ds.setLastTestAt(LocalDateTime.now());
			ds.setLastTestErrorCode(DataSourceErrorCode.ONT_DS_003.getCode());
			baseMapper.updateById(ds);

			// 审计凭证解密失败 + 指标（18-08 §9 §13）
			SecuritySubject subject = securitySubjectResolver.resolve();
			auditFacade.auditCredentialFailure(null, subject, id,
					DataSourceErrorCode.ONT_DS_003.getCode(), null);
			metrics.recordDataSourceTest("FAILED", ds.getDatabaseType());

			return new ConnectionTestOutcome(false, 0,
					DataSourceErrorCode.ONT_DS_003.getCode(), DataSourceErrorCode.ONT_DS_003.getMessage());
		}

		// 构建 JDBC URL
		String jdbcUrl = buildJdbcUrl(ds);
		int timeoutSeconds = extractTimeoutSeconds(ds);

		// 获取 Connector 并测试
		DataSourceConnector connector = connectorRegistry.getConnector(ds.getSourceType(), ds.getDatabaseType());
		DataSourceConnector.ConnectionTestResult result = connector.test(
				new DataSourceConnector.ConnectionRequest(jdbcUrl, credential.username(), credential.password(),
						timeoutSeconds));

		// 更新测试状态
		ds.setLastTestStatus(result.success() ? "SUCCESS" : "FAILED");
		ds.setLastTestRevision(ds.getRevision());
		ds.setLastTestAt(LocalDateTime.now());
		ds.setLastTestLatencyMs(result.latencyMs());
		ds.setLastTestErrorCode(result.success() ? null : result.errorCode());
		baseMapper.updateById(ds);

		// 指标 + 审计（18-08 §9 §13）
		metrics.recordDataSourceTest(result.success() ? "SUCCESS" : "FAILED", ds.getDatabaseType());
		SecuritySubject subject = securitySubjectResolver.resolve();
		auditFacade.auditDataSourceOperation(null, subject, id, "TESTED",
				result.success() ? "SUCCESS" : "FAILED",
				result.success() ? null : result.errorCode(), null);

		return new ConnectionTestOutcome(result.success(), result.latencyMs(),
				result.errorCode(), result.errorMessage());
	}

	// ==================== 状态管理 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public DataSourceVO updateStatus(Long id, String status) {
		OntDataSource ds = findByIdOrThrow(id);

		if (!"DRAFT".equals(status) && !"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
			throw new IllegalArgumentException("非法状态: " + status);
		}

		// ACTIVE 需最近测试成功且版本一致
		if ("ACTIVE".equals(status)) {
			if (!"SUCCESS".equals(ds.getLastTestStatus())) {
				throw new IllegalStateException(DataSourceErrorCode.ONT_DS_014.getMessage());
			}
			if (ds.getLastTestRevision() == null
					|| !ds.getLastTestRevision().equals(ds.getRevision())) {
				throw new IllegalStateException(DataSourceErrorCode.ONT_DS_014.getMessage());
			}
		}

		// DISABLED 时失效连接池
		if ("DISABLED".equals(status)) {
			poolManager.invalidateBySourceId(id);
		}

		ds.setStatus(status);
		baseMapper.updateById(ds);

		log.info("Updated data source status: id={}, status={}", id, status);

		// 审计（18-08 §9）
		SecuritySubject subject = securitySubjectResolver.resolve();
		auditFacade.auditDataSourceOperation(null, subject, id,
				"ACTIVE".equals(status) ? "ENABLED" : "DISABLED", "SUCCESS", null, null);

		return toVO(ds);
	}

	// ==================== 元数据刷新 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int refreshMetadata(Long id) {
		OntDataSource ds = findByIdOrThrow(id);

		// 解密凭证并建立连接
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(ds);
		String jdbcUrl = buildJdbcUrl(ds);
		int timeoutSeconds = extractTimeoutSeconds(ds);
		List<String> allowedSchemas = deserializeList(ds.getAllowedSchemas());
		List<String> allowedObjects = deserializeList(ds.getAllowedObjects());

		DataSourceConnector connector = connectorRegistry.getConnector(ds.getSourceType(), ds.getDatabaseType());

		int objectCount = 0;
		try (Connection conn = openReadonlyConnection(jdbcUrl, credential.username(), credential.password(),
				timeoutSeconds)) {

			// 列出所有 Schema
			List<String> schemas = connector.listSchemas(
					new DataSourceConnector.MetadataRequest(conn, allowedSchemas));

			// 遍历每个 Schema 下的对象
			for (String schema : schemas) {
				List<DataSourceConnector.SourceObject> objects = connector.listObjects(
						new DataSourceConnector.ObjectQuery(conn, schema, allowedSchemas, allowedObjects,
								null, null));

				for (DataSourceConnector.SourceObject obj : objects) {
					if (objectCount >= MAX_METADATA_OBJECTS) {
						log.warn("Metadata refresh reached max object limit: {}", MAX_METADATA_OBJECTS);
						throw new IllegalStateException(DataSourceErrorCode.ONT_DS_018.getMessage());
					}

					// 描述对象
					SourceObjectMetadataVO metadata = connector.describeObject(
							new DataSourceConnector.ObjectRef(conn, schema, obj.objectName()));

					// 计算哈希
					String metadataJson = serializeMetadata(metadata);
					String metadataHash = computeMetadataHash(metadataJson);

					// upsert 元数据缓存
					upsertMetadata(ds.getId(), schema, obj.objectName(), obj.objectType(),
							metadataJson, metadataHash, ds.getRevision());
					objectCount++;
				}
			}
		}
		catch (Exception e) {
			if (e instanceof IllegalStateException) {
				throw (IllegalStateException) e;
			}
			log.error("Metadata refresh failed: {}", e.getMessage());
			throw new RuntimeException("Metadata refresh failed", e);
		}

		// 更新刷新时间
		ds.setMetadataRefreshedAt(LocalDateTime.now());
		baseMapper.updateById(ds);

		log.info("Refreshed metadata: sourceId={}, objects={}", id, objectCount);

		// 指标 + 审计（18-08 §9 §13）
		metrics.recordMetadataRefreshDuration(java.time.Duration.ofMillis(0));
		SecuritySubject subject = securitySubjectResolver.resolve();
		auditFacade.auditDataSourceOperation(null, subject, id, "METADATA_REFRESHED", "SUCCESS", null, null);

		return objectCount;
	}

	@Override
	public List<String> listSchemas(Long id) {
		OntDataSource ds = findByIdOrThrow(id);
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(ds);
		String jdbcUrl = buildJdbcUrl(ds);
		int timeoutSeconds = extractTimeoutSeconds(ds);
		List<String> allowedSchemas = deserializeList(ds.getAllowedSchemas());

		DataSourceConnector connector = connectorRegistry.getConnector(ds.getSourceType(), ds.getDatabaseType());

		try (Connection conn = openReadonlyConnection(jdbcUrl, credential.username(), credential.password(),
				timeoutSeconds)) {
			return connector.listSchemas(new DataSourceConnector.MetadataRequest(conn, allowedSchemas));
		}
		catch (Exception e) {
			log.error("Failed to list schemas: {}", e.getMessage());
			throw new RuntimeException("Failed to list schemas", e);
		}
	}

	@Override
	public List<String> previewSchemas(SchemaPreviewRequest request) {
		// 校验连接配置（复用已有校验逻辑，拒绝包含凭证字段的配置）
		validateConnectionConfig(request.getConnectionMode(), request.getConnectionConfig());

		// 从连接配置构建 JDBC URL（不落库，不经过 OntDataSource 实体）
		String jdbcUrl = buildJdbcUrlFromConfig(request.getConnectionMode(), request.getConnectionConfig());
		int timeoutSeconds = extractTimeoutSecondsFromConfig(request.getConnectionConfig());

		// 数据源类型固定为 JDBC/POSTGRESQL（V1 仅支持 PostgreSQL）
		DataSourceConnector connector = connectorRegistry.getConnector("JDBC", "POSTGRESQL");

		try (Connection conn = openReadonlyConnection(jdbcUrl, request.getUsername(), request.getPassword(),
				timeoutSeconds)) {
			return connector.listSchemas(new DataSourceConnector.MetadataRequest(conn, null));
		}
		catch (Exception e) {
			log.error("Failed to preview schemas: {}", e.getMessage());
			throw new RuntimeException("Schema预览失败: " + e.getMessage(), e);
		}
	}

	@Override
	public List<SourceObjectMetadataVO.SourceObjectSummary> listObjects(Long id, MetadataObjectQuery query) {
		OntDataSource ds = findByIdOrThrow(id);
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(ds);
		String jdbcUrl = buildJdbcUrl(ds);
		int timeoutSeconds = extractTimeoutSeconds(ds);
		List<String> allowedSchemas = deserializeList(ds.getAllowedSchemas());
		List<String> allowedObjects = deserializeList(ds.getAllowedObjects());

		DataSourceConnector connector = connectorRegistry.getConnector(ds.getSourceType(), ds.getDatabaseType());

		try (Connection conn = openReadonlyConnection(jdbcUrl, credential.username(), credential.password(),
				timeoutSeconds)) {
			List<DataSourceConnector.SourceObject> objects = connector.listObjects(
					new DataSourceConnector.ObjectQuery(conn, query.getSchemaName(),
							allowedSchemas, allowedObjects, query.getObjectType(), query.getObjectNamePrefix()));

			return objects.stream()
					.map(o -> new SourceObjectMetadataVO.SourceObjectSummary(
							o.schemaName(), o.objectName(), o.objectType()))
					.toList();
		}
		catch (Exception e) {
			log.error("Failed to list objects: {}", e.getMessage());
			throw new RuntimeException("Failed to list objects", e);
		}
	}

	@Override
	public SourceObjectMetadataVO getObjectMetadata(Long id, String schemaName, String objectName) {
		OntDataSource ds = findByIdOrThrow(id);

		// 优先从缓存读取
		OntDataSourceMetadata cached = metadataMapper.selectOne(Wrappers.<OntDataSourceMetadata>lambdaQuery()
				.eq(OntDataSourceMetadata::getSourceId, id)
				.eq(OntDataSourceMetadata::getSchemaName, schemaName)
				.eq(OntDataSourceMetadata::getObjectName, objectName)
				.eq(OntDataSourceMetadata::getDelFlag, "0"));

		if (cached != null) {
			return deserializeMetadata(cached.getMetadataJson(), cached.getMetadataHash(),
					schemaName, objectName, cached.getObjectType());
		}

		// 缓存未命中则实时发现
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(ds);
		String jdbcUrl = buildJdbcUrl(ds);
		int timeoutSeconds = extractTimeoutSeconds(ds);

		DataSourceConnector connector = connectorRegistry.getConnector(ds.getSourceType(), ds.getDatabaseType());

		try (Connection conn = openReadonlyConnection(jdbcUrl, credential.username(), credential.password(),
				timeoutSeconds)) {
			return connector.describeObject(
					new DataSourceConnector.ObjectRef(conn, schemaName, objectName));
		}
		catch (Exception e) {
			log.error("Failed to get object metadata: {}", e.getMessage());
			throw new RuntimeException("Failed to get object metadata", e);
		}
	}

	// ==================== 内部方法 ====================

	private OntDataSource findByIdOrThrow(Long id) {
		OntDataSource ds = baseMapper.selectById(id);
		if (ds == null || "1".equals(ds.getDelFlag())) {
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_001.getMessage());
		}
		return ds;
	}

	private DataSourceVO toVO(OntDataSource ds) {
		DataSourceVO vo = new DataSourceVO();
		vo.setId(ds.getId());
		vo.setSourceCode(ds.getSourceCode());
		vo.setSourceName(ds.getSourceName());
		vo.setSourceType(ds.getSourceType());
		vo.setDatabaseType(ds.getDatabaseType());
		vo.setConnectionMode(ds.getConnectionMode());
		vo.setConnectionConfig(ds.getConnectionConfig());
		vo.setAllowedSchemas(deserializeList(ds.getAllowedSchemas()));
		vo.setAllowedObjects(deserializeList(ds.getAllowedObjects()));
		vo.setStatus(ds.getStatus());
		vo.setRevision(ds.getRevision());
		vo.setLastTestStatus(ds.getLastTestStatus());
		vo.setLastTestAt(ds.getLastTestAt());
		vo.setLastTestLatencyMs(ds.getLastTestLatencyMs());
		vo.setLastTestErrorCode(ds.getLastTestErrorCode());
		vo.setMetadataRefreshedAt(ds.getMetadataRefreshedAt());
		vo.setSecurityLevelCode(ds.getSecurityLevelCode());
		vo.setRemarks(ds.getRemarks());
		vo.setCreateTime(ds.getCreateTime());
		vo.setUpdateTime(ds.getUpdateTime());

		// 凭证脱敏
		vo.setCredentialConfigured(ds.getCredentialCiphertext() != null);
		vo.setCredentialKeyId(ds.getCredentialKeyId());

		// 脱敏用户名：从凭证解密（如果密钥可用），否则返回 null
		if (ds.getCredentialCiphertext() != null) {
			try {
				DataSourceCredentialCryptoService.EncryptedCredential encrypted =
						new DataSourceCredentialCryptoService.EncryptedCredential(
								ds.getCredentialCiphertext(), ds.getCredentialKeyId(), ds.getCredentialIv());
				DataSourceCredentialCryptoService.DataSourceCredential credential =
						cryptoService.decrypt(ds.getId(), ds.getSourceCode(), ds.getRevision(), encrypted);
				vo.setUsernameMasked(maskUsername(credential.username()));
			}
			catch (Exception e) {
				// 解密失败不暴露错误，仅标记已配置
				vo.setUsernameMasked(null);
			}
		}

		return vo;
	}

	private String maskUsername(String username) {
		if (username == null || username.length() <= 3) {
			return "***";
		}
		return username.substring(0, 3) + "***" + username.substring(username.length() - 2);
	}

	private DataSourceCredentialCryptoService.DataSourceCredential decryptCredential(OntDataSource ds) {
		try {
			DataSourceCredentialCryptoService.EncryptedCredential encrypted =
					new DataSourceCredentialCryptoService.EncryptedCredential(
							ds.getCredentialCiphertext(), ds.getCredentialKeyId(), ds.getCredentialIv());
			return cryptoService.decrypt(ds.getId(), ds.getSourceCode(), ds.getRevision(), encrypted);
		}
		catch (Exception e) {
			log.error("Credential decryption failed for sourceId={}", ds.getId());
			throw new RuntimeException(DataSourceErrorCode.ONT_DS_003.getMessage(), e);
		}
	}

	private String buildJdbcUrl(OntDataSource ds) {
		try {
			@SuppressWarnings("unchecked")
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
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage(), e);
		}
	}

	private int extractTimeoutSeconds(OntDataSource ds) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> config = objectMapper.readValue(ds.getConnectionConfig(), Map.class);
			Object val = config.get("connectTimeoutSeconds");
			return val != null ? ((Number) val).intValue() : DEFAULT_TIMEOUT_SECONDS;
		}
		catch (Exception e) {
			return DEFAULT_TIMEOUT_SECONDS;
		}
	}

	/**
	 * 从连接配置 JSON 构建 JDBC URL（不落库预览用，不依赖 OntDataSource 实体）。
	 */
	private String buildJdbcUrlFromConfig(String connectionMode, String connectionConfig) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> config = objectMapper.readValue(connectionConfig, Map.class);

			if ("JDBC_URL".equals(connectionMode)) {
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
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage(), e);
		}
	}

	/**
	 * 从连接配置 JSON 提取超时秒数（不落库预览用）。
	 */
	private int extractTimeoutSecondsFromConfig(String connectionConfig) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> config = objectMapper.readValue(connectionConfig, Map.class);
			Object val = config.get("connectTimeoutSeconds");
			return val != null ? ((Number) val).intValue() : DEFAULT_TIMEOUT_SECONDS;
		}
		catch (Exception e) {
			return DEFAULT_TIMEOUT_SECONDS;
		}
	}

	private void validateConnectionConfig(String connectionMode, String connectionConfig) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> config = objectMapper.readValue(connectionConfig, Map.class);

			if ("HOST".equals(connectionMode)) {
				if (config.get("host") == null || config.get("database") == null) {
					throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage());
				}
				// 拒绝包含 username/password 的 connection_config
				if (config.containsKey("username") || config.containsKey("password")
						|| config.containsKey("user") || config.containsKey("token")) {
					throw new IllegalArgumentException(
							DataSourceErrorCode.ONT_DS_016.getMessage() + ": 连接配置不得包含凭证字段");
				}
			}
			else if ("JDBC_URL".equals(connectionMode)) {
				String jdbcUrl = (String) config.get("jdbcUrl");
				if (jdbcUrl == null || jdbcUrl.isBlank()) {
					throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage());
				}
				PostgreSqlDialect.validateJdbcUrl(jdbcUrl);
				if (config.containsKey("username") || config.containsKey("password")
						|| config.containsKey("user") || config.containsKey("token")) {
					throw new IllegalArgumentException(
							DataSourceErrorCode.ONT_DS_016.getMessage() + ": 连接配置不得包含凭证字段");
				}
			}
			else {
				throw new IllegalArgumentException("非法连接模式: " + connectionMode);
			}
		}
		catch (IllegalArgumentException e) {
			throw e;
		}
		catch (Exception e) {
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage(), e);
		}
	}

	private Connection openReadonlyConnection(String jdbcUrl, String username, String password, int timeoutSeconds)
			throws Exception {
		java.util.Properties props = new java.util.Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		props.setProperty("readOnly", "true");
		props.setProperty("loginTimeout", String.valueOf(Math.max(1, timeoutSeconds)));
		props.setProperty("socketTimeout", String.valueOf(timeoutSeconds * 1000));
		props.setProperty("statementTimeout", String.valueOf(timeoutSeconds * 1000));

		Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, props);
		conn.setReadOnly(true);
		try (var stmt = conn.createStatement()) {
			stmt.execute("SET LOCAL statement_timeout = '" + (timeoutSeconds * 1000) + "'");
			stmt.execute("SET LOCAL lock_timeout = '5000'");
		}
		return conn;
	}

	private void upsertMetadata(Long sourceId, String schemaName, String objectName, String objectType,
			String metadataJson, String metadataHash, Long sourceRevision) {
		OntDataSourceMetadata existing = metadataMapper.selectOne(
				Wrappers.<OntDataSourceMetadata>lambdaQuery()
						.eq(OntDataSourceMetadata::getSourceId, sourceId)
						.eq(OntDataSourceMetadata::getSchemaName, schemaName)
						.eq(OntDataSourceMetadata::getObjectName, objectName)
						.eq(OntDataSourceMetadata::getDelFlag, "0"));

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expires = now.plusHours(24);

		if (existing != null) {
			existing.setObjectType(objectType);
			existing.setMetadataJson(metadataJson);
			existing.setMetadataHash(metadataHash);
			existing.setSourceRevision(sourceRevision);
			existing.setRefreshedAt(now);
			existing.setExpiresAt(expires);
			metadataMapper.updateById(existing);
		}
		else {
			OntDataSourceMetadata meta = new OntDataSourceMetadata();
			meta.setSourceId(sourceId);
			meta.setSchemaName(schemaName);
			meta.setObjectName(objectName);
			meta.setObjectType(objectType);
			meta.setMetadataJson(metadataJson);
			meta.setMetadataHash(metadataHash);
			meta.setSourceRevision(sourceRevision);
			meta.setRefreshedAt(now);
			meta.setExpiresAt(expires);
			metadataMapper.insert(meta);
		}
	}

	private String serializeMetadata(SourceObjectMetadataVO metadata) {
		try {
			return objectMapper.writeValueAsString(metadata);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to serialize metadata", e);
		}
	}

	private SourceObjectMetadataVO deserializeMetadata(String json, String hash,
			String schemaName, String objectName, String objectType) {
		try {
			SourceObjectMetadataVO vo = objectMapper.readValue(json, SourceObjectMetadataVO.class);
			vo.setMetadataHash(hash);
			return vo;
		}
		catch (Exception e) {
			SourceObjectMetadataVO vo = new SourceObjectMetadataVO();
			vo.setSchemaName(schemaName);
			vo.setObjectName(objectName);
			vo.setObjectType(objectType);
			vo.setMetadataHash(hash);
			return vo;
		}
	}

	private String computeMetadataHash(String metadataJson) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(metadataJson.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hashBytes) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to compute metadata hash", e);
		}
	}

	private String serializeList(List<String> list) {
		if (list == null || list.isEmpty()) {
			return "[]";
		}
		try {
			return objectMapper.writeValueAsString(list);
		}
		catch (Exception e) {
			return "[]";
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> deserializeList(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyList();
		}
		try {
			return objectMapper.readValue(json, List.class);
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

}
