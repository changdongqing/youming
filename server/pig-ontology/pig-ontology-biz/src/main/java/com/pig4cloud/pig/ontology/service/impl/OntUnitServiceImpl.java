/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import com.pig4cloud.pig.ontology.service.OntUnitCategoryService;
import com.pig4cloud.pig.ontology.service.OntUnitService;
import com.pig4cloud.pig.ontology.vo.OntUnitTreeNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 单位条目服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntUnitServiceImpl extends ServiceImpl<OntUnitMapper, OntUnit> implements OntUnitService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*$");

	private final OntUnitCategoryService categoryService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntUnit> saveUnit(OntUnit unit) {
		R<OntUnit> validation = validateUnit(unit, false);
		if (validation.getCode() != 0) {
			return validation;
		}
		unit.setId(null);
		unit.setIsBuiltin(EXTENSION);
		if (!StringUtils.hasText(unit.getIsBaseUnit())) {
			unit.setIsBaseUnit(EXTENSION);
		}
		if (unit.getSortOrder() == null) {
			unit.setSortOrder(0);
		}
		this.save(unit);
		return R.ok(unit);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntUnit> updateUnit(OntUnit unit) {
		if (unit.getId() == null) {
			return R.failed("单位ID不能为空");
		}
		OntUnit old = this.getById(unit.getId());
		if (old == null) {
			return R.failed("单位不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			OntUnit update = new OntUnit();
			update.setId(old.getId());
			update.setUnitName(unit.getUnitName());
			update.setSortOrder(unit.getSortOrder());
			update.setRemarks(unit.getRemarks());
			this.updateById(update);
			return R.ok(this.getById(old.getId()));
		}

		R<OntUnit> validation = validateUnit(unit, true);
		if (validation.getCode() != 0) {
			return validation;
		}
		unit.setIsBuiltin(EXTENSION);
		this.updateById(unit);
		return R.ok(this.getById(unit.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeUnit(Long id) {
		OntUnit unit = this.getById(id);
		if (unit == null) {
			return R.failed("单位不存在");
		}
		if (BUILTIN.equals(unit.getIsBuiltin())) {
			return R.failed("内置单位不可删除");
		}
		return R.ok(this.removeById(id));
	}

	@Override
	public List<OntUnitTreeNode> tree() {
		List<OntUnitCategory> categories = categoryService.list(Wrappers.<OntUnitCategory>lambdaQuery()
			.orderByAsc(OntUnitCategory::getSortOrder)
			.orderByAsc(OntUnitCategory::getId));
		List<OntUnit> units = this.list(Wrappers.<OntUnit>lambdaQuery()
			.orderByAsc(OntUnit::getSortOrder)
			.orderByAsc(OntUnit::getId));
		Map<Long, List<OntUnit>> unitMap = units.stream().collect(Collectors.groupingBy(OntUnit::getCategoryId));

		return categories.stream().map(category -> {
			OntUnitTreeNode node = new OntUnitTreeNode();
			node.setId(category.getId());
			node.setType("category");
			node.setLabel(category.getCategoryName());
			node.setCode(category.getCategoryCode());
			node.setSymbol(category.getBaseUnitSymbol());
			node.setIsBuiltin(category.getIsBuiltin());
			node.setChildren(unitMap.getOrDefault(category.getId(), List.of()).stream().map(unit -> {
				OntUnitTreeNode child = new OntUnitTreeNode();
				child.setId(unit.getId());
				child.setParentId(category.getId());
				child.setType("unit");
				child.setLabel(unit.getUnitName() + "（" + unit.getUnitSymbol() + "）");
				child.setCode(unit.getUnitCode());
				child.setSymbol(unit.getUnitSymbol());
				child.setIsBuiltin(unit.getIsBuiltin());
				return child;
			}).toList());
			return node;
		}).toList();
	}

	@Override
	public OntUnit getBySymbol(String symbol) {
		return this.getOne(Wrappers.<OntUnit>lambdaQuery()
			.eq(OntUnit::getUnitSymbol, symbol)
			.orderByAsc(OntUnit::getSortOrder)
			.orderByAsc(OntUnit::getId)
			.last("limit 1"), false);
	}

	@Override
	public R<BigDecimal> convert(Long from, Long to, BigDecimal value) {
		if (from == null || to == null || value == null) {
			return R.failed("来源单位、目标单位和数值不能为空");
		}
		OntUnit fromUnit = this.getById(from);
		OntUnit toUnit = this.getById(to);
		if (fromUnit == null || toUnit == null) {
			return R.failed("单位不存在");
		}
		if (!fromUnit.getCategoryId().equals(toUnit.getCategoryId())) {
			return R.failed("仅支持同一分类下的单位换算");
		}
		if (fromUnit.getFactor() == null || toUnit.getFactor() == null) {
			return R.failed("该单位未配置换算参数");
		}
		BigDecimal fromOffset = fromUnit.getOffsetValue() == null ? BigDecimal.ZERO : fromUnit.getOffsetValue();
		BigDecimal toOffset = toUnit.getOffsetValue() == null ? BigDecimal.ZERO : toUnit.getOffsetValue();
		BigDecimal baseValue = fromUnit.getFactor().multiply(value).add(fromOffset);
		BigDecimal targetValue = baseValue.subtract(toOffset).divide(toUnit.getFactor(), 12, RoundingMode.HALF_UP).stripTrailingZeros();
		return R.ok(targetValue);
	}

	private R<OntUnit> validateUnit(OntUnit unit, boolean edit) {
		if (unit.getCategoryId() == null) {
			return R.failed("单位分类不能为空");
		}
		if (categoryService.getById(unit.getCategoryId()) == null) {
			return R.failed("单位分类不存在");
		}
		if (!StringUtils.hasText(unit.getUnitCode())) {
			return R.failed("单位编码不能为空");
		}
		if (!CODE_PATTERN.matcher(unit.getUnitCode()).matches()) {
			return R.failed("单位编码仅支持小写英文、数字、下划线和中划线，且必须以小写英文开头");
		}
		if (!StringUtils.hasText(unit.getUnitSymbol())) {
			return R.failed("单位符号不能为空");
		}
		if (!StringUtils.hasText(unit.getUnitName())) {
			return R.failed("单位名称不能为空");
		}
		if (!BUILTIN.equals(unit.getIsBuiltin()) && !StringUtils.hasText(unit.getNamespace())) {
			return R.failed("扩展单位必须填写命名空间");
		}

		long codeCount = this.count(Wrappers.<OntUnit>lambdaQuery()
			.eq(OntUnit::getUnitCode, unit.getUnitCode())
			.ne(edit && unit.getId() != null, OntUnit::getId, unit.getId()));
		if (codeCount > 0) {
			return R.failed("单位编码已存在");
		}

		long symbolCount = this.count(Wrappers.<OntUnit>lambdaQuery()
			.eq(OntUnit::getCategoryId, unit.getCategoryId())
			.eq(OntUnit::getUnitSymbol, unit.getUnitSymbol())
			.ne(edit && unit.getId() != null, OntUnit::getId, unit.getId()));
		if (symbolCount > 0) {
			return R.failed("同一分类下单位符号已存在");
		}

		if (BUILTIN.equals(unit.getIsBaseUnit())) {
			long baseCount = this.count(Wrappers.<OntUnit>lambdaQuery()
				.eq(OntUnit::getCategoryId, unit.getCategoryId())
				.eq(OntUnit::getIsBaseUnit, BUILTIN)
				.ne(edit && unit.getId() != null, OntUnit::getId, unit.getId()));
			if (baseCount > 0) {
				return R.failed("同一分类只能有一个基准单位");
			}
		}
		return R.ok(unit);
	}

}
