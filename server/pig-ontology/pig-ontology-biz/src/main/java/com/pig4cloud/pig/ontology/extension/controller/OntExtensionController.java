/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.extension.component.ExtensionComponentRegistry;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleCreateDTO;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleQuery;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleUpdateDTO;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionResourceAssociateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.export.ExtensionExporter;
import com.pig4cloud.pig.ontology.extension.impact.ExtensionImpactAnalyzer;
import com.pig4cloud.pig.ontology.extension.service.OntExtensionModuleService;
import com.pig4cloud.pig.ontology.extension.service.OntExtensionResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 扩展管理。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/extension")
@Tag(description = "ontology-extension", name = "扩展管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntExtensionController {

	private final OntExtensionModuleService moduleService;

	private final OntExtensionResourceService resourceService;

	private final ExtensionImpactAnalyzer impactAnalyzer;

	private final ExtensionComponentRegistry componentRegistry;

	/**
	 * 分页查询扩展模块列表。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 扩展模块分页列表
	 */
	@GetMapping("/modules")
	@HasPermission("ontology_extension_view")
	public R<IPage<OntExtensionModule>> page(@ParameterObject Page<OntExtensionModule> page,
			@ParameterObject ExtensionModuleQuery query) {
		return R.ok(moduleService.page(page, Wrappers.<OntExtensionModule>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getModuleCode()), OntExtensionModule::getModuleCode, query.getModuleCode())
			.like(StrUtil.isNotBlank(query.getModuleName()), OntExtensionModule::getModuleName, query.getModuleName())
			.eq(query.getNamespaceId() != null, OntExtensionModule::getNamespaceId, query.getNamespaceId())
			.orderByAsc(OntExtensionModule::getSortOrder)
			.orderByAsc(OntExtensionModule::getId)));
	}

	/**
	 * 查询扩展模块列表（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 扩展模块列表
	 */
	@GetMapping("/modules/list")
	@HasPermission("ontology_extension_view")
	public R list(@ParameterObject ExtensionModuleQuery query) {
		return R.ok(moduleService.list(Wrappers.<OntExtensionModule>lambdaQuery()
			.like(StrUtil.isNotBlank(query.getModuleCode()), OntExtensionModule::getModuleCode, query.getModuleCode())
			.orderByAsc(OntExtensionModule::getSortOrder)
			.orderByAsc(OntExtensionModule::getId)));
	}

	/**
	 * 查询扩展模块详情。
	 * @param id 模块ID
	 * @return 模块详情
	 */
	@GetMapping("/modules/{id}")
	@HasPermission("ontology_extension_view")
	public R getById(@PathVariable Long id) {
		return R.ok(moduleService.getDetail(id));
	}

	/**
	 * 新建扩展模块。
	 * @param request 创建请求
	 * @return 新建结果
	 */
	@SysLog("新增扩展模块")
	@PostMapping("/modules")
	@HasPermission("ontology_extension_add")
	public R save(@Valid @RequestBody ExtensionModuleCreateDTO request) {
		return moduleService.saveModule(request);
	}

	/**
	 * 修改扩展模块。
	 * @param request 修改请求
	 * @return 修改结果
	 */
	@SysLog("修改扩展模块")
	@PutMapping("/modules")
	@HasPermission("ontology_extension_edit")
	public R update(@Valid @RequestBody ExtensionModuleUpdateDTO request) {
		return moduleService.updateModule(request);
	}

	/**
	 * 删除扩展模块。
	 * @param id 模块ID
	 * @return 删除结果
	 */
	@SysLog("删除扩展模块")
	@DeleteMapping("/modules/{id}")
	@HasPermission("ontology_extension_del")
	public R remove(@PathVariable Long id) {
		return moduleService.removeModule(id);
	}

	/**
	 * 查询模块关联的资源列表。
	 * @param id 模块ID
	 * @param resourceType 资源类型（可选过滤）
	 * @return 资源列表
	 */
	@GetMapping("/modules/{id}/resources")
	@HasPermission("ontology_extension_view")
	public R listResources(@PathVariable Long id,
			@RequestParam(required = false) String resourceType) {
		return R.ok(resourceService.listByModule(id, resourceType));
	}

	/**
	 * 批量关联资源到模块。
	 * @param id 模块ID
	 * @param request 关联请求
	 * @return 关联结果
	 */
	@SysLog("关联扩展资源")
	@PostMapping("/modules/{id}/resources")
	@HasPermission("ontology_extension_edit")
	public R associateResources(@PathVariable Long id,
			@Valid @RequestBody ExtensionResourceAssociateDTO request) {
		return resourceService.associateResources(id, request);
	}

	/**
	 * 解除资源关联。
	 * @param id 模块ID
	 * @param resourceId 资源ID
	 * @param resourceType 资源类型
	 * @return 解除结果
	 */
	@SysLog("解除扩展资源关联")
	@DeleteMapping("/modules/{id}/resources/{resourceId}")
	@HasPermission("ontology_extension_edit")
	public R removeResource(@PathVariable Long id, @PathVariable Long resourceId,
			@RequestParam String resourceType) {
		return resourceService.removeResource(id, resourceId, resourceType);
	}

	/**
	 * 执行扩展合法性校验。
	 * @param id 模块ID
	 * @return 校验报告
	 */
	@SysLog("扩展合法性校验")
	@PostMapping("/modules/{id}/validate")
	@HasPermission("ontology_extension_validate")
	public R validate(@PathVariable Long id) {
		return R.ok(moduleService.validateModule(id));
	}

	/**
	 * 变更影响分析。
	 * @param id 模块ID
	 * @return 影响摘要
	 */
	@GetMapping("/modules/{id}/impact")
	@HasPermission("ontology_extension_view")
	public R impact(@PathVariable Long id) {
		return R.ok(impactAnalyzer.analyze(id));
	}

	/**
	 * 导出扩展模块。
	 * @param id 模块ID
	 * @param format RDF格式
	 * @return 文件流
	 */
	@SysLog("导出扩展模块")
	@GetMapping("/modules/{id}/export")
	@HasPermission("ontology_extension_export")
	public ResponseEntity<byte[]> exportModule(@PathVariable Long id,
			@RequestParam(defaultValue = "TURTLE") String format) {
		String content = moduleService.exportModule(id, format);
		if (content == null) {
			return ResponseEntity.notFound().build();
		}
		String filename = moduleService.getExportFilename(id, format);
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + filename + "\"")
			.contentType(MediaType.parseMediaType(rdfContentType(format)))
			.body(content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 查询已注册扩展组件（推理引擎）。
	 * @return 组件列表
	 */
	@GetMapping("/components/registered")
	@HasPermission("ontology_extension_view")
	public R listRegisteredComponents() {
		return R.ok(componentRegistry.listRegisteredReasoners());
	}

	private String rdfContentType(String format) {
		return switch (format) {
			case "TURTLE" -> "text/turtle";
			case "JSON-LD" -> "application/ld+json";
			case "RDF-XML" -> "application/rdf+xml";
			case "N-TRIPLES" -> "application/n-triples";
			default -> "text/plain";
		};
	}

}
