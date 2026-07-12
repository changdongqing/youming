/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.serialization.dto.ExportRequest;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.service.SerializationService;
import com.pig4cloud.pig.ontology.serialization.vo.ExportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ExportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.SerializationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 序列化与交换 REST API。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/serialization")
@Tag(description = "本体序列化与交换", name = "本体序列化与交换")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntSerializationController {

	private final SerializationService serializationService;

	/**
	 * 导出本体（返回文本内容或文件下载流）。
	 */
	@GetMapping("/export")
	@SysLog("导出本体")
	@HasPermission("ontology_export_view")
	@Operation(summary = "导出本体", description = "支持Turtle/JSON-LD/RDF-XML/N-Triples格式")
	public void export(@ParameterObject ExportRequest request, HttpServletResponse response) {
		if (Boolean.TRUE.equals(request.getDownload())) {
			serializationService.exportToFile(request, response);
		}
		else {
			ExportResultVO result = serializationService.exportOntology(request);
			R<ExportResultVO> r;
			// 如果前置校验未通过，返回 code=1
			if (result.getPrecheck() != null && !result.getPrecheck().isPassed()) {
				r = R.failed(result.getPrecheck().getBlockedReason());
				r.setData(result);
			}
			else {
				r = R.ok(result);
			}
			response.setContentType("application/json;charset=UTF-8");
			try {
				response.getWriter().write(new ObjectMapper().writeValueAsString(r));
			}
			catch (Exception e) {
				throw new RuntimeException("写入导出响应失败", e);
			}
		}
	}

	/**
	 * 预览导出内容。
	 */
	@GetMapping("/preview")
	@HasPermission("ontology_export_view")
	@Operation(summary = "预览导出内容")
	public R<ExportPreviewVO> preview(@ParameterObject ExportRequest request) {
		return R.ok(serializationService.previewExport(request));
	}

	/**
	 * 获取支持的格式列表。
	 */
	@GetMapping("/formats")
	@Operation(summary = "获取支持的RDF格式")
	public R<List<Map<String, String>>> formats() {
		return R.ok(Arrays.stream(RdfFormat.values())
			.map(f -> Map.of("value", f.name(), "label", f.getDisplayName(),
				"extension", f.getFileExtension()))
			.collect(Collectors.toList()));
	}

	/**
	 * 获取支持的导出范围。
	 */
	@GetMapping("/scopes")
	@Operation(summary = "获取支持的导出范围")
	public R<List<Map<String, String>>> scopes() {
		return R.ok(Arrays.stream(com.pig4cloud.pig.ontology.serialization.export.ExportScope.values())
			.map(s -> Map.of("value", s.name(), "label", s.getDescription()))
			.collect(Collectors.toList()));
	}

	/**
	 * 审计日志查询。
	 */
	@GetMapping("/logs")
	@HasPermission("ontology_export_view")
	@Operation(summary = "导出/导入审计日志")
	public R<IPage<SerializationLogVO>> logs(
			@RequestParam(required = false) Long ontologyId,
			@RequestParam(required = false) String operationType,
			@RequestParam(defaultValue = "1") Integer page,
			@RequestParam(defaultValue = "20") Integer size) {
		return R.ok(serializationService.queryLogs(ontologyId, operationType, page, size));
	}

}
