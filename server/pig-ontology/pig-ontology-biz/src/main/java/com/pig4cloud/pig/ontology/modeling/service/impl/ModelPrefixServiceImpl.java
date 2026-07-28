package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelPrefixMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelPrefixService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * IRI 前缀 Service 实现（FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelPrefixServiceImpl extends ServiceImpl<ModelPrefixMapper, ModelPrefix>
		implements ModelPrefixService {

	/** NCName 规范：字母/下划线开头，含字母数字下划线连句点 */
	private static final Pattern NCNAME_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.\\-]*$");

	@Override
	public List<ModelPrefix> listByProject(Long projectId) {
		return list(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, projectId)
			.orderByDesc(ModelPrefix::getIsDefault)
			.orderByAsc(ModelPrefix::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R savePrefix(ModelPrefix prefix) {
		// 1. NCName 校验（AC-10.2）
		if (!NCNAME_PATTERN.matcher(prefix.getPrefix()).matches()) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 不符合 NCName 规范");
		}
		// 2. 同项目查重
		long count = count(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, prefix.getProjectId())
			.eq(ModelPrefix::getPrefix, prefix.getPrefix()));
		if (count > 0) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 在本项目内已存在");
		}
		if (StrUtil.isBlank(prefix.getIsDefault())) {
			prefix.setIsDefault("0");
		}
		try {
			return R.ok(save(prefix));
		}
		catch (DuplicateKeyException e) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 在本项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updatePrefix(ModelPrefix prefix) {
		if (!NCNAME_PATTERN.matcher(prefix.getPrefix()).matches()) {
			return R.failed("前缀名 '" + prefix.getPrefix() + "' 不符合 NCName 规范");
		}
		return R.ok(updateById(prefix));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removePrefix(Long id) {
		return R.ok(removeById(id));
	}

}
