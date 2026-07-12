/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.serialization.dto.ExportRequest;
import com.pig4cloud.pig.ontology.serialization.export.ExportScope;
import com.pig4cloud.pig.ontology.serialization.export.OntologyModelExporter;
import com.pig4cloud.pig.ontology.serialization.export.PredicateStrategy;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializer;
import com.pig4cloud.pig.ontology.serialization.format.RdfSerializerRegistry;
import com.pig4cloud.pig.ontology.serialization.log.SerializationLogService;
import com.pig4cloud.pig.ontology.serialization.log.entity.OntSerializationLog;
import com.pig4cloud.pig.ontology.serialization.precheck.ExportPrecheckResult;
import com.pig4cloud.pig.ontology.serialization.precheck.ExportPrecheckService;
import com.pig4cloud.pig.ontology.serialization.service.SerializationService;
import com.pig4cloud.pig.ontology.serialization.vo.ExportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ExportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.SerializationLogVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
