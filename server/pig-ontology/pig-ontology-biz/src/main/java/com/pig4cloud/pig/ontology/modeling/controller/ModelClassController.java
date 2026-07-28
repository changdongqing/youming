package com.pig4cloud.pig.ontology.modeling.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.dto.ClassInstantiateDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.service.ModelClassService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelClassDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 本体类实体 Controller（FR-11）
 * <p>
 * 路径 /ont/model/class/**，对外 /admin/ont/model/class/**
 * （复用 pig-gateway 已有路由 Path=/admin/ont/**）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/class")
@Tag(name = "本体类建模", description = "类实体 CRUD + 模板实例化（FR-11）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelClassController {

	private final ModelClassService modelClassService;

	@GetMapping("/page")
	@Operation(summary = "分页查询", description = "按项目/名称/模板溯源过滤（AC-11.7）")
	@HasPermission("ont_class_model_view")
	public R<IPage<ModelClass>> page(@ParameterObject Page page, @ParameterObject ModelClass cls) {
		return R.ok(modelClassService.page(page, cls));
	}

	@GetMapping("/{id}")
	@Operation(summary = "类详情", description = "含数据属性/对象属性列表 + 父类子类 IRI + 溯源（AC-11.8）")
	@HasPermission("ont_class_model_view")
	public R<ModelClassDetailVO> getById(@PathVariable Long id) {
		return R.ok(modelClassService.getDetail(id));
	}

	@SysLog("新增本体类")
	@PostMapping
	@Operation(summary = "新建类", description = "支持基于模板创建（传 templateCode 触发实例化，AC-11.1~11.5）")
	@HasPermission("ont_class_model_manage")
	public R save(@Valid @RequestBody ModelClass cls) {
		return modelClassService.saveClass(cls);
	}

	@SysLog("编辑本体类")
	@PutMapping("/{id}")
	@Operation(summary = "编辑类", description = "可修改 label/描述/外观（AC-11.6）")
	@HasPermission("ont_class_model_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody ModelClass cls) {
		cls.setId(id);
		return modelClassService.updateClass(cls);
	}

	@SysLog("删除本体类")
	@DeleteMapping("/{id}")
	@Operation(summary = "删除类", description = "校验被 subClassOf 引用 + 有属性，软删（AC-11.6）")
	@HasPermission("ont_class_model_manage")
	public R removeById(@PathVariable Long id) {
		return modelClassService.removeClass(id);
	}

	@SysLog("模板实例化属性")
	@PostMapping("/{id}/instantiate")
	@Operation(summary = "基于分类模板实例化属性", description = "对已有空白类追加模板属性（场景三，PRD 10.2）")
	@HasPermission("ont_class_model_manage")
	public R instantiate(@PathVariable Long id, @Valid @RequestBody ClassInstantiateDTO dto) {
		return modelClassService.instantiateFromClass(id, dto);
	}

}
