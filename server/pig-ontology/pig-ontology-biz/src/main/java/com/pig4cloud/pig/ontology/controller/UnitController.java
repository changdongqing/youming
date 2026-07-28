package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.QuantityKindNodeVO;
import com.pig4cloud.pig.ontology.api.vo.UnitConvertResultVO;
import com.pig4cloud.pig.ontology.service.QuantityKindService;
import com.pig4cloud.pig.ontology.service.UnitConversionService;
import com.pig4cloud.pig.ontology.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单位注册表 Controller（FR-3）
 * <p>
 * 量纲走 /ont/quantity-kind/**，单位与换算走 /ont/unit/**，对外
 * /admin/ont/quantity-kind/**、/admin/ont/unit/**（对齐 PRD 10.3）。
 * <p>
 * 注意：pig-gateway 的 PigRequestGlobalFilter 全局重写 StripPrefix=1，pig-boot 的
 * context-path=/admin 也只剥 /admin，故控制器必须带 /ont 前缀。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@Tag(name = "单位注册表", description = "量纲 + 单位 + 换算（FR-3）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class UnitController {

	private final QuantityKindService quantityKindService;

	private final UnitService unitService;

	private final UnitConversionService unitConversionService;

	// ---------- 量纲 ----------

	@GetMapping("/ont/quantity-kind/list")
	@Operation(summary = "量纲列表", description = "全量量纲（按 sort_order），含每量纲单位计数（10.3）")
	@HasPermission("ont_unit_view")
	public R<List<QuantityKindNodeVO>> quantityKindList() {
		return R.ok(quantityKindService.listWithCount());
	}

	// ---------- 单位 ----------

	@GetMapping("/ont/unit/page")
	@Operation(summary = "单位分页", description = "按 quantityKindId 过滤（10.3）")
	@HasPermission("ont_unit_view")
	public R<IPage<Unit>> page(@ParameterObject Page page, @ParameterObject Unit unit) {
		return R.ok(unitService.page(page, unit));
	}

	@GetMapping("/ont/unit/{id}")
	@Operation(summary = "单位详情", description = "含换算系数/SN（AC-3.2）")
	@HasPermission("ont_unit_view")
	public R<Unit> getById(@PathVariable Long id) {
		return R.ok(unitService.getById(id));
	}

	@SysLog("新增单位")
	@PostMapping("/ont/unit")
	@Operation(summary = "新增 custom 单位", description = "量纲存在性 + qudtIri 查重（AC-3.5）")
	@HasPermission("ont_unit_manage")
	public R save(@Valid @RequestBody Unit unit) {
		return unitService.saveUnit(unit);
	}

	@SysLog("编辑单位")
	@PutMapping("/ont/unit/{id}")
	@Operation(summary = "编辑（builtin 拒绝）")
	@HasPermission("ont_unit_manage")
	public R updateById(@PathVariable Long id, @Valid @RequestBody Unit unit) {
		unit.setId(id);
		return unitService.updateUnit(unit);
	}

	@SysLog("删除单位")
	@DeleteMapping("/ont/unit/{id}")
	@Operation(summary = "删除（custom，软删）")
	@HasPermission("ont_unit_manage")
	public R removeById(@PathVariable Long id) {
		return unitService.removeUnit(id);
	}

	@SysLog("弃用单位")
	@PutMapping("/ont/unit/{id}/deprecate")
	@Operation(summary = "弃用/恢复")
	@HasPermission("ont_unit_manage")
	public R deprecate(@PathVariable Long id, @RequestParam(defaultValue = "1") String deprecated) {
		return unitService.deprecate(id, deprecated);
	}

	// ---------- 换算 ----------

	@GetMapping("/ont/unit/convert")
	@Operation(summary = "换算", description = "同量纲返回换算值，跨量纲返回 null（AC-3.3/3.4/3.8）")
	@HasPermission("ont_unit_view")
	public R<UnitConvertResultVO> convert(@RequestParam BigDecimal value, @RequestParam String fromIri,
			@RequestParam String toIri) {
		return R.ok(unitConversionService.convert(value, fromIri, toIri));
	}

}
