/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.serialization.dto.ExportRequest;
import com.pig4cloud.pig.ontology.serialization.dto.ImportConfirmRequest;
import com.pig4cloud.pig.ontology.serialization.export.ExportScope;
import com.pig4cloud.pig.ontology.serialization.export.OntologyModelExporter;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategy;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializer;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializerRegistry;
import com.pig4cloud.pig.ontology.serialization.imp.ImportPreviewCache;
import com.pig4cloud.pig.ontology.serialization.imp.ImportPreviewResult;
import com.pig4cloud.pig.ontology.serialization.imp.InstanceImportMapper;
import com.pig4cloud.pig.ontology.serialization.imp.IriMergeMode;
import com.pig4cloud.pig.ontology.serialization.imp.RdfFileParser;
import com.pig4cloud.pig.ontology.serialization.log.SerializationLogService;
import com.pig4cloud.pig.ontology.serialization.log.entity.OntSerializationLog;
import com.pig4cloud.pig.ontology.serialization.precheck.ExportPrecheckResult;
import com.pig4cloud.pig.ontology.serialization.precheck.ExportPrecheckService;
import com.pig4cloud.pig.ontology.serialization.service.SerializationService;
import com.pig4cloud.pig.ontology.serialization.vo.ExportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ExportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.ImportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ImportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.SerializationLogVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.entity.OntInstanceDataValue;
import com.pig4cloud.pig.ontology.entity.OntInstanceObjectRelation;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper;
import com.pig4cloud.pig.ontology.mapper.OntInstanceObjectRelationMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.entity.OntNamespace;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 序列化与交换编排服务实现。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerializationServiceImpl implements SerializationService {

	private final OntologyModelExporter modelExporter;

	private final RdfSerializerRegistry serializerRegistry;

	private final ExportPrecheckService precheckService;

	private final SerializationLogService logService;

	private final RdfFileParser rdfFileParser;

	private final InstanceImportMapper instanceImportMapper;

	private final ImportPreviewCache importPreviewCache;

	private final OntEntityInstanceMapper entityInstanceMapper;

	private final OntInstanceDataValueMapper instanceDataValueMapper;

	private final OntInstanceObjectRelationMapper instanceObjectRelationMapper;

	private final OntNamespaceMapper namespaceMapper;

	private static final Long CORE_ONTOLOGY_ID = 935001L;

	private static final int PREVIEW_MAX_LINES = 200;

	@Override
	public ExportResultVO exportOntology(ExportRequest request) {
		Long ontologyId = request.getOntologyId() != null ? request.getOntologyId() : CORE_ONTOLOGY_ID;
		RdfFormat format = parseFormat(request.getFormat());
		ExportScope scope = parseScope(request.getScope());
		PredicateStrategy strategy = parseStrategy(request.getPredicateStrategy());
		boolean force = Boolean.TRUE.equals(request.getForce());

		// 校验参数
		if (scope == ExportScope.INSTANCE_SUBTREE && request.getTargetTypeId() == null) {
			throw new IllegalArgumentException("scope=INSTANCE_SUBTREE 时 targetTypeId 必填");
		}

		// 导出前置校验
		ExportPrecheckResult precheckResult = precheckService.check(ontologyId, force);
		if (!precheckResult.isPassed()) {
			ExportResultVO vo = new ExportResultVO();
			vo.setFormat(format.getDisplayName());
			vo.setScope(scope.name());
			vo.setPredicateStrategy(strategy.name());
			ExportResultVO.PrecheckVO precheckVO = new ExportResultVO.PrecheckVO();
			precheckVO.setPassed(false);
			precheckVO.setBlockedReason(precheckResult.getBlockedReason());
			precheckVO.setValidationReportId(precheckResult.getValidationReportId());
			vo.setPrecheck(precheckVO);
			return vo;
		}

		// 组装 Model
		Model model = modelExporter.buildCompleteModel(ontologyId, scope, strategy, request.getTargetTypeId());
		long tripleCount = model.size();

		// 序列化
		RdfSerializer serializer = serializerRegistry.getSerializer(format);
		String content = serializer.serialize(model);
		long contentSize = content.getBytes(StandardCharsets.UTF_8).length;

		// 记录审计日志
		OntSerializationLog logEntry = buildExportLog(ontologyId, format, scope, strategy,
			request.getTargetTypeId(), tripleCount, contentSize, precheckResult, force);
		Long logId = logService.saveLog(logEntry);

		// 构建返回
		ExportResultVO vo = new ExportResultVO();
		vo.setFormat(format.getDisplayName());
		vo.setContent(content);
		vo.setTripleCount(tripleCount);
		vo.setScope(scope.name());
		vo.setPredicateStrategy(strategy.name());
		ExportResultVO.PrecheckVO precheckVO = new ExportResultVO.PrecheckVO();
		precheckVO.setPassed(true);
		vo.setPrecheck(precheckVO);
		vo.setLogId(logId);
		return vo;
	}

	@Override
	public ExportPreviewVO previewExport(ExportRequest request) {
		ExportResultVO exportResult = exportOntology(request);
		// 截取前200行
		String content = exportResult.getContent();
		String previewContent = truncateLines(content, PREVIEW_MAX_LINES);

		ExportPreviewVO vo = new ExportPreviewVO();
		vo.setFormat(exportResult.getFormat());
		vo.setContent(previewContent);
		vo.setTripleCount(exportResult.getTripleCount());
		vo.setScope(exportResult.getScope());
		vo.setPredicateStrategy(exportResult.getPredicateStrategy());
		vo.setContentSize(content != null ? content.getBytes(StandardCharsets.UTF_8).length : 0);
		vo.setPrecheck(exportResult.getPrecheck());
		return vo;
	}

	@Override
	public void exportToFile(ExportRequest request, HttpServletResponse response) {
		ExportResultVO result = exportOntology(request);

		if (!result.getPrecheck().isPassed()) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json;charset=UTF-8");
			try {
				response.getWriter().write("{\"code\":1,\"msg\":\""
					+ result.getPrecheck().getBlockedReason() + "\"}");
			}
			catch (Exception e) {
				log.error("写入导出拦截响应失败", e);
			}
			return;
		}

		RdfFormat format = parseFormat(request.getFormat());
		String fileName = "ontology-" + (request.getOntologyId() != null
			? request.getOntologyId() : CORE_ONTOLOGY_ID)
			+ "-" + System.currentTimeMillis() + "." + format.getFileExtension();

		response.setContentType(getContentType(format));
		response.setHeader("Content-Disposition",
			"attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");

		try (OutputStream out = response.getOutputStream()) {
			out.write(result.getContent().getBytes(StandardCharsets.UTF_8));
			out.flush();
		}
		catch (Exception e) {
			log.error("写入导出文件流失败", e);
		}
	}

	@Override
	public IPage<SerializationLogVO> queryLogs(Long ontologyId, String operationType, int page, int size) {
		return logService.queryLogs(ontologyId, operationType, page, size);
	}

	@Override
	public ImportPreviewVO importPreview(MultipartFile file, Long ontologyId, RdfFormat format) {
		Long targetOntologyId = ontologyId != null ? ontologyId : CORE_ONTOLOGY_ID;

		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("上传文件不能为空");
		}

		try {
			String content = new String(file.getBytes(), StandardCharsets.UTF_8);
			Model model = rdfFileParser.parse(content, format);
			ImportPreviewResult preview = instanceImportMapper.mapToPreview(model, targetOntologyId);
			String previewId = importPreviewCache.put(preview);

			return buildImportPreviewVO(previewId, preview);
		}
		catch (Exception e) {
			log.error("导入预检失败", e);
			throw new RuntimeException("RDF文件解析失败: " + e.getMessage(), e);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ImportResultVO importConfirm(ImportConfirmRequest request) {
		long startTime = System.currentTimeMillis();
		Long ontologyId = request.getOntologyId() != null ? request.getOntologyId() : CORE_ONTOLOGY_ID;
		IriMergeMode mergeMode = request.getIriMergeMode() != null
			? IriMergeMode.valueOf(request.getIriMergeMode().toUpperCase())
			: IriMergeMode.SKIP;

		ImportPreviewResult preview = importPreviewCache.get(request.getPreviewId());
		if (preview == null) {
			throw new IllegalStateException("预检结果已过期，请重新上传");
		}

		int importedCount = 0;
		int skippedCount = 0;
		int failedCount = 0;
		int totalDataValues = 0;
		int totalRelations = 0;

		// 构建命名空间查找
		OntNamespace defaultNs = namespaceMapper.selectOne(
			Wrappers.<OntNamespace>lambdaQuery().eq(OntNamespace::getIsDefault, "1").last("LIMIT 1"));
		Long namespaceId = defaultNs != null ? defaultNs.getId() : null;

		for (ImportPreviewResult.ImportInstance instance : preview.getInstances()) {
			if (instance.getRdfTypeId() == null) {
				failedCount++;
				continue;
			}

			// 检查冲突
			boolean exists = "IRI_EXISTS".equals(instance.getConflictType());
			if (exists && mergeMode == IriMergeMode.SKIP) {
				skippedCount++;
				continue;
			}

			try {
				// 查找已有实例
				OntEntityInstance existing = entityInstanceMapper.selectOne(
					Wrappers.<OntEntityInstance>lambdaQuery()
						.eq(OntEntityInstance::getIri, instance.getIri())
						.last("LIMIT 1"));

				if (existing != null) {
					if (mergeMode == IriMergeMode.OVERWRITE) {
						// 删除旧数据值和关系
						instanceDataValueMapper.delete(
							Wrappers.<OntInstanceDataValue>lambdaQuery()
								.eq(OntInstanceDataValue::getInstanceId, existing.getId()));
						instanceObjectRelationMapper.delete(
							Wrappers.<OntInstanceObjectRelation>lambdaQuery()
								.eq(OntInstanceObjectRelation::getSubjectInstanceId, existing.getId()));
					}
					// MERGE: 保留已有数据，追加新数据
					writeInstanceData(existing.getId(), instance, totalDataValues, totalRelations);
					importedCount++;
				}
				else {
					// 新建实例
					OntEntityInstance newInst = new OntEntityInstance();
					newInst.setIri(instance.getIri());
					newInst.setIriLocalName(extractLocalName(instance.getIri()));
					newInst.setRdfTypeId(instance.getRdfTypeId());
					newInst.setLabel(instance.getLabel());
					newInst.setNamespaceId(namespaceId);
					newInst.setOntologyId(ontologyId);
					newInst.setSourceType("EXTENSION");
					newInst.setDeclarationMode("EXPLICIT");
					newInst.setIsBuiltin("0");
					newInst.setSortOrder(0);
					newInst.setCreateBy(getCurrentUser());
					newInst.setCreateTime(LocalDateTime.now());
					entityInstanceMapper.insert(newInst);

					writeInstanceData(newInst.getId(), instance, totalDataValues, totalRelations);
					importedCount++;
				}
			}
			catch (Exception e) {
				log.error("导入实例 {} 失败: {}", instance.getIri(), e.getMessage());
				failedCount++;
			}
		}

		long durationMs = System.currentTimeMillis() - startTime;

		// 记录审计日志
		OntSerializationLog logEntry = new OntSerializationLog();
		logEntry.setOntologyId(ontologyId);
		logEntry.setOperationType("IMPORT");
		logEntry.setRdfFormat("TURTLE");
		logEntry.setInstanceCount(importedCount);
		logEntry.setSkippedCount(skippedCount);
		logEntry.setFailedCount(failedCount);
		logEntry.setDataValueCount(totalDataValues);
		logEntry.setObjectRelationCount(totalRelations);
		logEntry.setImportIriMergeMode(mergeMode.name());
		logEntry.setDurationMs(durationMs);
		logEntry.setCreateBy(getCurrentUser());
		logEntry.setCreateTime(LocalDateTime.now());
		Long logId = logService.saveLog(logEntry);

		// 清理缓存
		importPreviewCache.remove(request.getPreviewId());

		ImportResultVO vo = new ImportResultVO();
		vo.setTotalInstances(preview.getInstances().size());
		vo.setImportedCount(importedCount);
		vo.setSkippedCount(skippedCount);
		vo.setFailedCount(failedCount);
		vo.setDataValueCount(totalDataValues);
		vo.setObjectRelationCount(totalRelations);
		vo.setDurationMs(durationMs);
		vo.setLogId(logId);
		return vo;
	}

	private void writeInstanceData(Long instanceId, ImportPreviewResult.ImportInstance instance,
			int totalDataValues, int totalRelations) {
		for (ImportPreviewResult.ImportDataValue dv : instance.getDataValues()) {
			OntInstanceDataValue entity = new OntInstanceDataValue();
			entity.setInstanceId(instanceId);
			entity.setDataPropertyId(dv.getDataPropertyId());
			entity.setLiteralValue(dv.getLiteralValue());
			entity.setLiteralType(dv.getLiteralType());
			entity.setUnitId(dv.getUnitId());
			entity.setLiteralSymbol(dv.getUnitSymbol());
			entity.setSortOrder(0);
			entity.setCreateBy(getCurrentUser());
			entity.setCreateTime(LocalDateTime.now());
			instanceDataValueMapper.insert(entity);
			totalDataValues++;
		}

		for (ImportPreviewResult.ImportObjectRelation rel : instance.getObjectRelations()) {
			OntInstanceObjectRelation entity = new OntInstanceObjectRelation();
			entity.setSubjectInstanceId(instanceId);
			entity.setObjectPropertyId(rel.getObjectPropertyId());
			entity.setObjectKind(rel.getObjectKind());
			entity.setSortOrder(0);
			entity.setCreateBy(getCurrentUser());
			entity.setCreateTime(LocalDateTime.now());
			instanceObjectRelationMapper.insert(entity);
			totalRelations++;
		}
	}

	private String extractLocalName(String iri) {
		if (iri == null) {
			return null;
		}
		int hashIdx = iri.lastIndexOf('#');
		int slashIdx = iri.lastIndexOf('/');
		int idx = Math.max(hashIdx, slashIdx);
		return idx >= 0 && idx < iri.length() - 1 ? iri.substring(idx + 1) : iri;
	}

	private ImportPreviewVO buildImportPreviewVO(String previewId, ImportPreviewResult preview) {
		ImportPreviewVO vo = new ImportPreviewVO();
		vo.setPreviewId(previewId);
		vo.setOntologyId(preview.getOntologyId());

		// 摘要
		ImportPreviewVO.Summary summary = new ImportPreviewVO.Summary();
		summary.setTotalInstances(preview.getInstances().size());
		summary.setTotalDataValues(preview.getInstances().stream()
			.mapToInt(ImportPreviewResult.ImportInstance::getDataValueCount).sum());
		summary.setTotalObjectRelations(preview.getInstances().stream()
			.mapToInt(ImportPreviewResult.ImportInstance::getObjectRelationCount).sum());
		summary.setSchemaSkipped(preview.getSchemaSkipped());
		summary.setConflictCount(preview.getConflictCount());
		summary.setErrorCount(preview.getErrors().size());
		summary.setWarningCount(preview.getWarnings().size());
		vo.setSummary(summary);

		// 实例列表
		List<ImportPreviewVO.InstanceInfo> instances = preview.getInstances().stream()
			.map(inst -> {
				ImportPreviewVO.InstanceInfo info = new ImportPreviewVO.InstanceInfo();
				info.setIri(inst.getIri());
				info.setLabel(inst.getLabel());
				info.setRdfTypeIri(inst.getRdfTypeIri());
				info.setRdfTypeId(inst.getRdfTypeId());
				info.setConflictType(inst.getConflictType());
				info.setDataValueCount(inst.getDataValueCount());
				info.setObjectRelationCount(inst.getObjectRelationCount());
				return info;
			})
			.toList();
		vo.setInstances(instances);

		// 冲突列表
		List<ImportPreviewVO.ConflictInfo> conflicts = preview.getInstances().stream()
			.filter(inst -> inst.getConflictType() != null)
			.map(inst -> {
				ImportPreviewVO.ConflictInfo ci = new ImportPreviewVO.ConflictInfo();
				ci.setIri(inst.getIri());
				ci.setConflictType(inst.getConflictType());
				return ci;
			})
			.toList();
		vo.setConflicts(conflicts);

		// 错误列表
		vo.setErrors(preview.getErrors().stream()
			.map(e -> {
				ImportPreviewVO.IssueInfo ii = new ImportPreviewVO.IssueInfo();
				ii.setInstanceIri(e.getInstanceIri());
				ii.setMessage(e.getMessage());
				return ii;
			})
			.toList());

		// 警告列表
		vo.setWarnings(preview.getWarnings().stream()
			.map(w -> {
				ImportPreviewVO.IssueInfo ii = new ImportPreviewVO.IssueInfo();
				ii.setInstanceIri(w.getInstanceIri());
				ii.setMessage(w.getMessage());
				return ii;
			})
			.toList());

		return vo;
	}

	// ==================== 辅助方法 ====================

	private RdfFormat parseFormat(String format) {
		if (format == null || format.isBlank()) {
			return RdfFormat.TURTLE;
		}
		try {
			return RdfFormat.valueOf(format.replace("-", "_").toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("不支持的RDF格式: " + format);
		}
	}

	private ExportScope parseScope(String scope) {
		if (scope == null || scope.isBlank()) {
			return ExportScope.FULL;
		}
		try {
			return ExportScope.valueOf(scope.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("不支持的导出范围: " + scope);
		}
	}

	private PredicateStrategy parseStrategy(String strategy) {
		if (strategy == null || strategy.isBlank()) {
			return PredicateStrategy.PREFERRED_ALIAS;
		}
		try {
			return PredicateStrategy.valueOf(strategy.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("不支持的谓词策略: " + strategy);
		}
	}

	private String truncateLines(String content, int maxLines) {
		if (content == null) {
			return "";
		}
		String[] lines = content.split("\n");
		if (lines.length <= maxLines) {
			return content;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < maxLines; i++) {
			sb.append(lines[i]).append("\n");
		}
		sb.append("# ... 预览仅显示前 ").append(maxLines).append(" 行，完整内容请下载文件\n");
		return sb.toString();
	}

	private String getContentType(RdfFormat format) {
		switch (format) {
			case TURTLE:
				return "text/turtle;charset=UTF-8";
			case JSON_LD:
				return "application/ld+json;charset=UTF-8";
			case RDF_XML:
				return "application/rdf+xml;charset=UTF-8";
			case N_TRIPLES:
				return "application/n-triples;charset=UTF-8";
			default:
				return "text/plain;charset=UTF-8";
		}
	}

	private OntSerializationLog buildExportLog(Long ontologyId, RdfFormat format, ExportScope scope,
			PredicateStrategy strategy, Long targetTypeId, long tripleCount, long contentSize,
			ExportPrecheckResult precheckResult, boolean force) {
		OntSerializationLog logEntry = new OntSerializationLog();
		logEntry.setOntologyId(ontologyId);
		logEntry.setOperationType("EXPORT");
		logEntry.setRdfFormat(format.name().replace("_", "-"));
		logEntry.setExportScope(scope.name());
		logEntry.setPredicateStrategy(strategy.name());
		logEntry.setTargetTypeId(targetTypeId);
		logEntry.setTripleCount((int) tripleCount);
		logEntry.setContentSize(contentSize);
		logEntry.setPrecheckPassed(precheckResult.isPassed() ? "1" : "0");
		logEntry.setValidationReportId(precheckResult.getValidationReportId());
		logEntry.setForceFlag(force ? "1" : "0");
		logEntry.setDurationMs(0L);
		logEntry.setCreateBy(getCurrentUser());
		logEntry.setCreateTime(LocalDateTime.now());
		return logEntry;
	}

	private String getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.getName() != null) ? auth.getName() : "system";
	}

}
