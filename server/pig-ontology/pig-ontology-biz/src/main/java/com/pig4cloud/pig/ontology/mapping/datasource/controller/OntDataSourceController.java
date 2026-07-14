/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceCreateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.DataSourceUpdateDTO;
import com.pig4cloud.pig.ontology.mapping.datasource.dto.MetadataObjectQuery;
import com.pig4cloud.pig.ontology.mapping.datasource.entity.OntDataSource;
import com.pig4cloud.pig.ontology.mapping.datasource.service.OntDataSourceService;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.DataSourceVO;
import com.pig4cloud.pig.ontology.mapping.datasource.vo.SourceObjectMetadataVO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源注册管理 API（18-02 §11）。
 *
 * @author youming
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ontology/data-mapping/sources")
@Tag(name = "数据源注册管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntDataSourceController {

	private final OntDataSourceService dataSourceService;

	@GetMapping("/page")
	@HasPermission("ontology_mapping_view")
	public R<Page<DataSourceVO>> page(@ParameterObject Page<OntDataSource> page,
			@RequestParam(required = false) String sourceCode,
			@RequestParam(required = false) String sourceName,
			@RequestParam(required = false) String status) {
		return R.ok(dataSourceService.page(page, sourceCode, sourceName, status));
	}

	@GetMapping("/{id}")
	@HasPermission("ontology_mapping_view")
	public R<DataSourceVO> getDetail(@PathVariable Long id) {
		return R.ok(dataSourceService.getDetail(id));
	}

	@PostMapping
	@SysLog("新增数据源")
	@HasPermission("ontology_mapping_source_manage")
	public R<DataSourceVO> create(@Valid @RequestBody DataSourceCreateDTO dto) {
		return R.ok(dataSourceService.create(dto));
	}

	@PutMapping("/{id}")
	@SysLog("修改数据源")
	@HasPermission("ontology_mapping_source_manage")
	public R<DataSourceVO> update(@PathVariable Long id, @Valid @RequestBody DataSourceUpdateDTO dto) {
		dto.setId(id);
		return R.ok(dataSourceService.update(dto));
	}

	@DeleteMapping("/{id}")
	@SysLog("删除数据源")
	@HasPermission("ontology_mapping_admin")
	public R<Boolean> remove(@PathVariable Long id) {
		return R.ok(dataSourceService.remove(id));
	}

	@PostMapping("/{id}/test")
	@SysLog("测试数据源连接")
	@HasPermission("ontology_mapping_source_manage")
	public R<OntDataSourceService.ConnectionTestOutcome> testConnection(@PathVariable Long id) {
		return R.ok(dataSourceService.testConnection(id));
	}

	@PutMapping("/{id}/status")
	@SysLog("更新数据源状态")
	@HasPermission("ontology_mapping_source_manage")
	public R<DataSourceVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
		return R.ok(dataSourceService.updateStatus(id, status));
	}

	@PostMapping("/{id}/metadata/refresh")
	@SysLog("刷新数据源元数据")
	@HasPermission("ontology_mapping_source_manage")
	public R<Integer> refreshMetadata(@PathVariable Long id) {
		return R.ok(dataSourceService.refreshMetadata(id));
	}

	@GetMapping("/{id}/schemas")
	@HasPermission("ontology_mapping_view")
	public R<List<String>> listSchemas(@PathVariable Long id) {
		return R.ok(dataSourceService.listSchemas(id));
	}

	@GetMapping("/{id}/objects")
	@HasPermission("ontology_mapping_view")
	public R<List<SourceObjectMetadataVO.SourceObjectSummary>> listObjects(@PathVariable Long id,
			@ParameterObject MetadataObjectQuery query) {
		return R.ok(dataSourceService.listObjects(id, query));
	}

	@GetMapping("/{id}/objects/{schema}/{object}")
	@HasPermission("ontology_mapping_view")
	public R<SourceObjectMetadataVO> getObjectMetadata(@PathVariable Long id,
			@PathVariable String schema, @PathVariable String object) {
		return R.ok(dataSourceService.getObjectMetadata(id, schema, object));
	}

}
