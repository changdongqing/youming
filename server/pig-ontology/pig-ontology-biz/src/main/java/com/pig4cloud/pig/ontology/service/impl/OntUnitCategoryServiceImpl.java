/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.entity.OntUnitCategory;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.mapper.OntUnitCategoryMapper;
import com.pig4cloud.pig.ontology.service.OntUnitCategoryService;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 单位分类服务实现。
 *
 * @author youming
 */
@Service
@AllArgsConstructor
public class OntUnitCategoryServiceImpl extends ServiceImpl<OntUnitCategoryMapper, OntUnitCategory>
		implements OntUnitCategoryService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*$");

	private final OntUnitMapper unitMapper;

	private final OntDataPropertyMapper dataPropertyMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntUnitCategory> saveCategory(OntUnitCategory category) {
		R<OntUnitCategory> validation = validateCategory(category, false);
		if (validation.getCode() != 0) {
			return validation;
		}
		category.setId(null);
		category.setIsBuiltin(EXTENSION);
		if (category.getSortOrder() == null) {
			category.setSortOrder(0);
		}
		this.save(category);
		return R.ok(category);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntUnitCategory> updateCategory(OntUnitCategory category) {
		if (category.getId() == null) {
			return R.failed("分类ID不能为空");
		}
		OntUnitCategory old = this.getById(category.getId());
		if (old == null) {
			return R.failed("单位分类不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			if (!StringUtils.hasText(category.getCategoryName())) {
				return R.failed("分类名称不能为空");
			}
			OntUnitCategory update = new OntUnitCategory();
			update.setId(old.getId());
			update.setCategoryName(category.getCategoryName());
			update.setSortOrder(category.getSortOrder());
			update.setRemarks(category.getRemarks());
			this.updateById(update);
			return R.ok(this.getById(old.getId()));
		}

		R<OntUnitCategory> validation = validateCategory(category, true);
		if (validation.getCode() != 0) {
			return validation;
		}
		category.setIsBuiltin(EXTENSION);
		this.updateById(category);
		return R.ok(this.getById(category.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeCategory(Long id) {
		OntUnitCategory category = this.getById(id);
		if (category == null) {
			return R.failed("单位分类不存在");
		}
		if (BUILTIN.equals(category.getIsBuiltin())) {
			return R.failed("内置单位分类不可删除");
		}
		long unitCount = unitMapper.selectCount(Wrappers.<OntUnit>lambdaQuery().eq(OntUnit::getCategoryId, id));
		if (unitCount > 0) {
			return R.failed("分类下存在单位条目，不能删除");
		}
		long dataPropertyCount = dataPropertyMapper.selectCount(Wrappers.<OntDataProperty>lambdaQuery()
			.eq(OntDataProperty::getUnitCategoryId, id));
		if (dataPropertyCount > 0) {
			return R.failed("该单位分类被数据属性引用，不能删除");
		}
		return R.ok(this.removeById(id));
	}

	private R<OntUnitCategory> validateCategory(OntUnitCategory category, boolean edit) {
		if (!StringUtils.hasText(category.getCategoryCode())) {
			return R.failed("分类编码不能为空");
		}
		if (!CODE_PATTERN.matcher(category.getCategoryCode()).matches()) {
			return R.failed("分类编码仅支持小写英文、数字、下划线和中划线，且必须以小写英文开头");
		}
		if (!StringUtils.hasText(category.getCategoryName())) {
			return R.failed("分类名称不能为空");
		}
		if (!StringUtils.hasText(category.getBaseUnitSymbol())) {
			return R.failed("基准单位符号不能为空");
		}
		long count = this.count(Wrappers.<OntUnitCategory>lambdaQuery()
			.eq(OntUnitCategory::getCategoryCode, category.getCategoryCode())
			.ne(edit && category.getId() != null, OntUnitCategory::getId, category.getId()));
		if (count > 0) {
			return R.failed("分类编码已存在");
		}
		return R.ok(category);
	}

}
