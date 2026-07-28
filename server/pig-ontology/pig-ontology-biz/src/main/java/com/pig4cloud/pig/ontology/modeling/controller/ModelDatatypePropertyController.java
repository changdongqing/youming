package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.service.ModelDatatypePropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 数据属性 Controller（FR-12）
 * <p>
 * 路径 /ont/model/datatype-property/**，对外 /admin/ont/model/datatype-property/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/datatype-property")
@Tag(name = "数据属性建模", description = "数据属性 CRUD + 单位绑定 + 模板实例化（FR-12）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelDatatypePropertyController {

	private final ModelDatatypePropertyService datatypePropertyService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按 classId 过滤（AC-12.6）")
	@HasPermission("ont_prop_model_view")
	public R<IPage<ModelDatatypeProperty>> page(@ParameterObject Page page,
			@ParameterObject ModelDatatypeProperty prop) {
		return R.ok(datatypePropertyService.page(page, prop));
	}

	@GetMapping("/{id}")
	@Operation(summary = "属性详情")
	@HasPermission("ont_prop_model_view")
	public R<ModelDatatypeProperty> getById(@PathVariable Long id) {
		return R.ok(datatypePropertyService.getById(id));
	}

	@SysLog("新增数据属性")
	@PostMapping
	@Operation(summary = "新增数据属性", description = "localName 查重 + 单位校验 + 基数（AC-12.1~12.5）")
	@HasPermission("ont_prop_model_manage")
	public R save(@Valid @RequestBody ModelDatatypeProperty prop) {
		return datatypePropertyService.saveProp(prop);
	}

	@SysLog("编辑数据属性")
	@PutMapping("/{id}")
	@Operation(summary = "编辑数据属性")
	@HasPermission("ont_prop_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelDatatypeProperty prop) {
		prop.setId(id);
		return datatypePropertyService.updateProp(prop);
	}

	@SysLog("删除数据属性")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除数据属性", description = "软删（AC-12.6）")
	@HasPermission("ont_prop_model_manage")
	public R removeById(@PathVariable Long id) {
		return datatypePropertyService.removeProp(id);
	}

}
