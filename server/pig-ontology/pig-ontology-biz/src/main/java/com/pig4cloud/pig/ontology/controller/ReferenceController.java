package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.vo.ReferenceAnnotationPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceClassVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceOntologyVO;
import com.pig4cloud.pig.ontology.api.vo.ReferenceUnitVO;
import com.pig4cloud.pig.ontology.service.ReferenceImportService;
import com.pig4cloud.pig.ontology.service.ReferenceOntologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 参考本体库 Controller（FR-7）
 * <p>
 * 路径 /ont/reference/**，对外 /admin/ont/reference/**（对齐 PRD 10.6）。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 全局重写 StripPrefix=1，pig-boot 的
 * context-path=/admin 也只剥 /admin，故控制器必须带 /ont 前缀。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/reference")
@Tag(name = "参考本体库", description = "QUDT/Brick/CCO 只读浏览 + 引用式导入（FR-7）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ReferenceController {

	private final ReferenceOntologyService referenceOntologyService;

	private final ReferenceImportService referenceImportService;

	@GetMapping("/ontologies")
	@Operation(summary = "参考本体列表", description = "列出三套本体元信息（10.6）")
	@HasPermission("ont_ref_view")
	public R<List<ReferenceOntologyVO>> ontologies() {
		return R.ok(referenceOntologyService.ontologies());
	}

	@GetMapping("/{ont}/units")
	@Operation(summary = "QUDT 单位分页", description = "按量纲过滤，展示 label/IRI（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceUnitVO>> units(@PathVariable String ont, @ParameterObject Page page,
			@RequestParam(required = false) String quantityKindIri,
			@RequestParam(required = false) String keyword) {
		return R.ok(referenceOntologyService.pageUnits(ont, page, quantityKindIri, keyword));
	}

	@GetMapping("/{ont}/classes")
	@Operation(summary = "Brick 类分页", description = "展示 label/IRI/subClassOf（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceClassVO>> classes(@PathVariable String ont, @ParameterObject Page page,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String parentIri) {
		return R.ok(referenceOntologyService.pageClasses(ont, page, keyword, parentIri));
	}

	@GetMapping("/{ont}/annotation-properties")
	@Operation(summary = "CCO 注释属性分页", description = "展示 label/IRI（10.6，AC-7.1）")
	@HasPermission("ont_ref_view")
	public R<IPage<ReferenceAnnotationPropertyVO>> annotationProperties(@PathVariable String ont,
			@ParameterObject Page page, @RequestParam(required = false) String keyword) {
		return R.ok(referenceOntologyService.pageAnnotationProperties(ont, page, keyword));
	}

	@SysLog("导入QUDT单位")
	@PostMapping("/qudt/unit/import")
	@Operation(summary = "导入 QUDT 单位", description = "导入到 ont_unit，含 qudtIri/symbol/换算系数（10.6，AC-7.2/7.3/7.5）")
	@HasPermission("ont_unit_manage")
	public R importQudtUnit(@RequestParam String iri) {
		return referenceImportService.importQudtUnit(iri);
	}

	@SysLog("导入Brick类")
	@PostMapping("/brick/class/import")
	@Operation(summary = "导入 Brick 类", description = "导入为分类模板，subClassOf->parent_id（10.6，AC-7.3/7.5，R-16）")
	@HasPermission("ont_class_tpl_manage")
	public R importBrickClass(@RequestParam String iri) {
		return referenceImportService.importBrickClass(iri);
	}

}
