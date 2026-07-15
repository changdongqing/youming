/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.preview.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.ontology.mapping.datasource.DataSourceErrorCode;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapping.ValidationErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.IriTemplateCompiler;
import com.pig4cloud.pig.ontology.mapping.datasource.DataSourcePoolManager;
import com.pig4cloud.pig.ontology.mapping.datasource.connector.postgresql.PostgreSqlDialect;
import com.pig4cloud.pig.ontology.mapping.datasource.crypto.DataSourceCredentialCryptoService;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.mapper.OntDataSourceMapper;
import com.pig4cloud.pig.ontology.mapping.entity.OntEntityMapping;
import com.pig4cloud.pig.ontology.mapping.entity.OntFieldMapping;
import com.pig4cloud.pig.ontology.mapping.mapper.OntEntityMappingMapper;
import com.pig4cloud.pig.ontology.mapping.mapper.OntFieldMappingMapper;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewRequest;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewResult;
import com.pig4cloud.pig.ontology.mapping.preview.MappingPreviewService;
import com.pig4cloud.pig.ontology.mapping.preview.PreviewProjectionService;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 映射预览服务实现（18-06 §6）。
 * <p>
 * 通过数据源只读连接执行 SELECT ... LIMIT N 查询（FIRST_N 模式），
 * 渲染 IRI 和值后返回脱敏结果，不写任何表。
 * V1样本上限100且超时15秒，同步返回。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingPreviewServiceImpl implements MappingPreviewService {

	private static final int MAX_SAMPLE_SIZE = 100;

	private static final int QUERY_TIMEOUT_SECONDS = 15;

	private static final int CONNECT_TIMEOUT_SECONDS = 5;

	private final OntMappingVersionMapper versionMapper;

	private final OntEntityMappingMapper entityMappingMapper;

	private final OntFieldMappingMapper fieldMappingMapper;

	private final OntDataSourceMapper dataSourceMapper;

	private final OntNamespaceMapper namespaceMapper;

	private final IriTemplateCompiler iriTemplateCompiler;

	private final PreviewProjectionService projectionService;

	private final DataSourcePoolManager poolManager;

	private final DataSourceCredentialCryptoService credentialCryptoService;

	private final ObjectMapper objectMapper;

	@Override
	public MappingPreviewResult preview(MappingPreviewRequest request) {
		// 校验请求
		int sampleSize = request.getSampleSize() != null
				? Math.min(request.getSampleSize(), MAX_SAMPLE_SIZE) : 10;
		String sampleMode = request.getSampleMode() != null ? request.getSampleMode() : "FIRST_N";

		if (!"FIRST_N".equals(sampleMode) && !"KEYS".equals(sampleMode)) {
			throw new IllegalArgumentException(ValidationErrorCode.ONT_MAP_201.getMessage()
					+ ": 不支持的样本模式: " + sampleMode + "（V1支持 FIRST_N / KEYS）");
		}

		// 加载版本
		OntMappingVersion version = versionMapper.selectById(request.getMappingVersionId());
		if (version == null || "1".equals(version.getDelFlag())) {
			throw new IllegalArgumentException("映射版本不存在: " + request.getMappingVersionId());
		}

		// 加载启用的实体映射
		List<OntEntityMapping> entityMappings = entityMappingMapper.selectList(
				Wrappers.<OntEntityMapping>lambdaQuery()
						.eq(OntEntityMapping::getMappingVersionId, version.getId())
						.eq(OntEntityMapping::getDelFlag, "0")
						.eq(OntEntityMapping::getEnabled, "1")
						.orderByAsc(OntEntityMapping::getSyncOrder));

		// 按请求过滤
		if (request.getEntityMappingCodes() != null && !request.getEntityMappingCodes().isEmpty()) {
			entityMappings = entityMappings.stream()
					.filter(em -> request.getEntityMappingCodes().contains(em.getMappingCode()))
					.collect(Collectors.toList());
		}

		MappingPreviewResult result = new MappingPreviewResult();
		result.setEntityResults(new ArrayList<>());
		result.setRelationResults(new ArrayList<>());
		result.setTruncated(false);

		// 逐实体映射预览
		for (OntEntityMapping em : entityMappings) {
			try {
				List<MappingPreviewResult.EntityPreviewResult> entityResults = previewEntityMapping(em, sampleSize);
				result.getEntityResults().addAll(entityResults);
				if (entityResults.size() >= sampleSize) {
					result.setTruncated(true);
				}
			}
			catch (Exception e) {
				log.warn("Preview failed for entity mapping {}: {}", em.getMappingCode(), e.getMessage());
				MappingPreviewResult.EntityPreviewResult errorResult = new MappingPreviewResult.EntityPreviewResult();
				errorResult.setMappingCode(em.getMappingCode());
				errorResult.setIssues(List.of("预览失败: " + e.getMessage()));
				result.getEntityResults().add(errorResult);
			}
		}

		return result;
	}

	private List<MappingPreviewResult.EntityPreviewResult> previewEntityMapping(OntEntityMapping em, int sampleSize) {
		// 加载数据源
		OntDataSource dataSource = dataSourceMapper.selectById(em.getSourceId());
		if (dataSource == null || "1".equals(dataSource.getDelFlag())) {
			throw new IllegalStateException("数据源不存在: " + em.getSourceId());
		}

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

		// 查询样本数据
		List<Map<String, String>> sampleRows = querySampleRows(dataSource, em, keyColumnNames, fieldMappings,
				sampleSize);

		// 渲染预览
		List<MappingPreviewResult.EntityPreviewResult> results = new ArrayList<>();
		for (Map<String, String> row : sampleRows) {
			MappingPreviewResult.EntityPreviewResult result = new MappingPreviewResult.EntityPreviewResult();
			result.setMappingCode(em.getMappingCode());

			// 计算源记录键哈希
			String keyContent = keyColumnNames.stream()
					.map(col -> col + "=" + row.getOrDefault(col, ""))
					.collect(Collectors.joining(","));
			result.setSourceRecordKeyHash(projectionService.hashRecordKey(keyContent));

			// 渲染 IRI
			try {
				String fullIri = iriTemplateCompiler.renderIri(namespaceUri, em.getIriTemplate(), row);
				result.setGeneratedIri(fullIri);
			}
			catch (IllegalArgumentException e) {
				result.setGeneratedIri("IRI_ERROR: " + e.getMessage());
			}

			// 渲染标签
			if (em.getLabelTemplate() != null && !em.getLabelTemplate().isBlank()) {
				result.setLabel(iriTemplateCompiler.renderTemplate(em.getLabelTemplate(), row));
			}

			result.setAction("WOULD_CREATE");

			// 渲染值预览
			List<MappingPreviewResult.ValuePreview> valuePreviews = new ArrayList<>();
			for (OntFieldMapping fm : fieldMappings) {
				MappingPreviewResult.ValuePreview vp = new MappingPreviewResult.ValuePreview();

				if ("COLUMN".equals(fm.getSourceKind()) && fm.getSourceColumn() != null) {
					String rawValue = row.get(fm.getSourceColumn());
					boolean sensitive = projectionService.isSensitiveColumn(fm.getSourceColumn());
					vp.setMaskedPreview(projectionService.projectValue(rawValue, sensitive));
					vp.setResultType(sensitive ? "MASKED" : "STRING");
				}
				else if ("CONSTANT".equals(fm.getSourceKind())) {
					vp.setMaskedPreview(projectionService.truncate(fm.getConstantValue()));
					vp.setResultType("CONSTANT");
				}
				else {
					continue;
				}

				vp.setPropertyId(fm.getTargetDataPropertyId());
				valuePreviews.add(vp);
			}
			result.setValues(valuePreviews);
			result.setIssues(new ArrayList<>());

			results.add(result);
		}

		return results;
	}

	/**
	 * 通过数据源连接查询样本数据（FIRST_N 模式）。
	 * <p>
	 * 构建受控 SQL：SELECT key_cols + field_cols FROM schema.object LIMIT N
	 * 必须设置 statement timeout。
	 */
	private List<Map<String, String>> querySampleRows(OntDataSource dataSource, OntEntityMapping em,
			List<String> keyColumnNames, List<OntFieldMapping> fieldMappings, int sampleSize) {

		// 收集需要查询的列（键列 + 字段映射的源列）
		List<String> columns = new ArrayList<>(keyColumnNames);
		for (OntFieldMapping fm : fieldMappings) {
			if ("COLUMN".equals(fm.getSourceKind()) && fm.getSourceColumn() != null
					&& !columns.contains(fm.getSourceColumn())) {
				columns.add(fm.getSourceColumn());
			}
		}

		if (columns.isEmpty()) {
			return List.of();
		}

		// 构建受控 SQL
		String columnList = columns.stream()
				.map(this::quoteIdentifier)
				.collect(Collectors.joining(", "));
		String schemaName = em.getSourceSchema() != null ? quoteIdentifier(em.getSourceSchema()) : null;
		String objectName = quoteIdentifier(em.getSourceObject());
		String qualifiedName = schemaName != null ? schemaName + "." + objectName : objectName;

		String sql = "SELECT " + columnList + " FROM " + qualifiedName + " LIMIT " + sampleSize;

		log.debug("Preview SQL: {}", sql);

		// 解密凭证并获取连接
		DataSourceCredentialCryptoService.DataSourceCredential credential = decryptCredential(dataSource);
		String jdbcUrl = buildJdbcUrl(dataSource);

		List<Map<String, String>> rows = new ArrayList<>();

		try (Connection conn = poolManager.getConnection(dataSource.getId(), dataSource.getRevision(),
				jdbcUrl, credential.username(), credential.password(), CONNECT_TIMEOUT_SECONDS);
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			// 设置查询超时
			stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					Map<String, String> row = new HashMap<>();
					for (String col : columns) {
						Object value = rs.getObject(col);
						row.put(col, value != null ? value.toString() : null);
					}
					rows.add(row);
				}
			}
		}
		catch (Exception e) {
			log.error("Preview query failed for entity mapping {}: {}", em.getMappingCode(), e.getMessage());
			throw new RuntimeException("预览查询失败: " + e.getMessage(), e);
		}

		return rows;
	}

	/**
	 * 解密数据源凭证。
	 */
	private DataSourceCredentialCryptoService.DataSourceCredential decryptCredential(OntDataSource ds) {
		try {
			DataSourceCredentialCryptoService.EncryptedCredential encrypted =
					new DataSourceCredentialCryptoService.EncryptedCredential(
							ds.getCredentialCiphertext(), ds.getCredentialKeyId(), ds.getCredentialIv());
			return credentialCryptoService.decrypt(ds.getId(), ds.getSourceCode(), ds.getRevision(), encrypted);
		}
		catch (Exception e) {
			log.error("Credential decryption failed for sourceId={}", ds.getId());
			throw new RuntimeException(DataSourceErrorCode.ONT_DS_003.getMessage(), e);
		}
	}

	/**
	 * 从 connectionConfig 构建 JDBC URL。
	 */
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
			throw new IllegalArgumentException(DataSourceErrorCode.ONT_DS_016.getMessage(), e);
		}
	}

	/**
	 * 引号包裹标识符，防止 SQL 注入。
	 */
	private String quoteIdentifier(String identifier) {
		if (identifier == null || identifier.isBlank()) {
			throw new IllegalArgumentException("标识符不能为空");
		}
		// 仅允许字母、数字、下划线
		if (!identifier.matches("[a-zA-Z0-9_]+")) {
			throw new IllegalArgumentException("非法标识符: " + identifier);
		}
		return "\"" + identifier + "\"";
	}

}
