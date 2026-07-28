package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 注释属性注册表 Controller（FR-4）
 * <p>
 * 路径 /ont/annotation-property/**，对外 /admin/ont/annotation-property/**（对齐 PRD 10.4）。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 全局重写 StripPrefix=1，pig-boot 的
 * context-path=/admin 也只剥 /admin，故控制器必须带 /ont 前缀。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/annotation-property")
@Tag(name = "注释属性注册表", description = "注释属性 CRUD + 导出（FR-4）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class AnnotationPropertyController {

	private final AnnotationPropertyService annotationPropertyService;

	@GetMapping("/list")
	@Operation(summary = "全量列表", description = "按 appliesTo 过滤（10.4，AC-4.3）；注册表体量小，前端客户端分页")
	@HasPermission("ont_ap_view")
	public R<List<AnnotationProperty>> list(@RequestParam(required = false) String appliesTo) {
		return R.ok(annotationPropertyService.list(appliesTo));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情")
	@HasPermission("ont_ap_view")
	public R<AnnotationProperty> getById(@PathVariable Long id) {
		return R.ok(annotationPropertyService.getById(id));
	}

	@SysLog("新增注释属性")
	@PostMapping
	@Operation(summary = "新增 custom 注释属性", description = "appliesTo 枚举校验 + localName 查重（AC-4.2/4.5）")
	@HasPermission("ont_ap_manage")
	public R save(@Valid @RequestBody AnnotationProperty ap) {
		return annotationPropertyService.saveAp(ap);
	}

	@SysLog("编辑注释属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_ap_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody AnnotationProperty ap) {
		ap.setId(id);
		return annotationPropertyService.updateAp(ap);
	}

	@SysLog("删除注释属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_ap_manage")
	public R removeById(@PathVariable Long id) {
		return annotationPropertyService.removeAp(id);
	}

	@GetMapping("/export")
	@Operation(summary = "导出清单", description = "Markdown 表格，含 localName/label/rangeXsd/appliesTo/description（AC-4.4）")
	@HasPermission("ont_ap_view")
	public R<String> export(@RequestParam(required = false) String appliesTo) {
		return R.ok(annotationPropertyService.exportMarkdown(appliesTo));
	}

}
