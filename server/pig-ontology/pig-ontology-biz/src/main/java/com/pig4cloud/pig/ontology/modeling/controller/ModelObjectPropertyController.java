package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.dto.InverseSuggestDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.service.ModelObjectPropertyService;
import com.pig4cloud.pig.ontology.modeling.vo.InverseSuggestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 对象属性 Controller（FR-13）
 * <p>
 * 路径 /ont/model/object-property/**，对外 /admin/ont/model/object-property/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/object-property")
@Tag(name = "对象属性建模", description = "对象属性 CRUD + domain/range + 反向关系（FR-13）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelObjectPropertyController {

	private final ModelObjectPropertyService objectPropertyService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 domainClassId 过滤（AC-13.4）")
	@HasPermission("ont_prop_model_view")
	public R<IPage<ModelObjectProperty>> page(@ParameterObject Page page,
			@ParameterObject ModelObjectProperty prop) {
		return R.ok(objectPropertyService.page(page, prop));
	}

	@GetMapping("/{id}")
	@Operation(summary = "属性详情")
	@HasPermission("ont_prop_model_view")
	public R<ModelObjectProperty> getById(@PathVariable Long id) {
		return R.ok(objectPropertyService.getById(id));
	}

	@SysLog("新增对象属性")
	@PostMapping
	@Operation(summary = "新增对象属性", description = "domain/range + 基数映射（AC-13.1/13.3）")
	@HasPermission("ont_prop_model_manage")
	public R save(@Valid @RequestBody ModelObjectProperty prop) {
		return objectPropertyService.saveProp(prop);
	}

	@SysLog("编辑对象属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑对象属性", description = "补全 range（AC-13.4）")
	@HasPermission("ont_prop_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelObjectProperty prop) {
		prop.setId(id);
		return objectPropertyService.updateProp(prop);
	}

	@SysLog("删除对象属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除对象属性")
	@HasPermission("ont_prop_model_manage")
	public R removeById(@PathVariable Long id) {
		return objectPropertyService.removeProp(id);
	}

	@PostMapping("/suggest-inverse")
	@Operation(summary = "反向关系建议", description = "建立 contains(O->P) 后建议在 P 建 belongsTo(P->O)（AC-13.5）")
	@HasPermission("ont_prop_model_view")
	public R<InverseSuggestVO> suggestInverse(@Valid @RequestBody InverseSuggestDTO dto) {
		return R.ok(objectPropertyService.suggestInverse(dto));
	}

}
