package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.ClassTemplateSaveDTO;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateDetailVO;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateNodeVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类模板管理 Controller（FR-2/FR-8）
 * <p>
 * 路径 /ont/class-template/**，对外 /admin/ont/class-template/**（对齐 PRD 10.2）。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 全局重写 StripPrefix=1，pig-boot 的
 * context-path=/admin 也只剥 /admin，故控制器必须带 /ont 前缀。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/class-template")
@Tag(name = "分类模板管理", description = "分类模板树 + 继承 + 编码（FR-2/FR-8）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassTemplateController {

	private final ClassTemplateService classTemplateService;

	@GetMapping("/tree")
	@Operation(summary = "分类模板树", description = "按 treeRoot 查询，含编码/继承预览/外观合并（10.2）")
	@HasPermission("ont_class_tpl_view")
	public R<List<ClassTemplateNodeVO>> tree(@RequestParam String treeRoot,
			@RequestParam(defaultValue = "false") Boolean includeDeprecated) {
		return R.ok(classTemplateService.tree(treeRoot, includeDeprecated));
	}

	@GetMapping("/{id}")
	@Operation(summary = "详情", description = "含外观合并（inherit_appearance=1 取父外观）+ 本节点 refs")
	@HasPermission("ont_class_tpl_view")
	public R<ClassTemplateDetailVO> getById(@PathVariable Long id) {
		return R.ok(classTemplateService.getDetail(id));
	}

	@GetMapping("/{id}/inherited")
	@Operation(summary = "继承视图", description = "父链合并后的全部属性（区分继承/新增/覆盖）+ 外观（AC-2.3）")
	@HasPermission("ont_class_tpl_view")
	public R<InheritedViewVO> inherited(@PathVariable Long id) {
		return R.ok(classTemplateService.inheritedView(id));
	}

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "支持 classificationCode 前缀查子树（AC-8.4）+ templateCode 模糊")
	@HasPermission("ont_class_tpl_view")
	public R<IPage<ClassTemplate>> page(@ParameterObject Page page, @ParameterObject ClassTemplate template) {
		return R.ok(classTemplateService.page(page, template));
	}

	@SysLog("新增分类模板")
	@PostMapping
	@Operation(summary = "新增", description = "含 parent_id；classification_code 自动生成或校验（FR-8）")
	@HasPermission("ont_class_tpl_manage")
	public R save(@Valid @RequestBody ClassTemplateSaveDTO dto) {
		return classTemplateService.saveTemplate(dto);
	}

	@SysLog("编辑分类模板")
	@PutMapping("/{id}")
	@Operation(summary = "编辑", description = "builtin 拒绝；改 parent 校验不成环（AC-2.6）")
	@HasPermission("ont_class_tpl_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ClassTemplateSaveDTO dto) {
		dto.setId(id);
		return classTemplateService.updateTemplate(dto);
	}

	@SysLog("删除分类模板")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除", description = "有子节点拒绝（AC-2.7）；custom 软删")
	@HasPermission("ont_class_tpl_manage")
	public R removeById(@PathVariable Long id) {
		return classTemplateService.removeTemplate(id);
	}

	@SysLog("弃用分类模板")
	@PutMapping("/{id}/deprecate")
	@Operation(summary = "弃用/恢复")
	@HasPermission("ont_class_tpl_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return classTemplateService.deprecate(id, deprecated);
	}

	@GetMapping("/code/preview")
	@Operation(summary = "编码预览", description = "据 parentId + 规则生成下一编码（AC-8.2）")
	@HasPermission("ont_class_tpl_view")
	public R<String> previewCode(@RequestParam(required = false) Long parentId) {
		return R.ok(classTemplateService.previewClassificationCode(parentId));
	}

}
