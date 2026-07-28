package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.api.vo.AnnotationPropertySupplyVO;
import com.pig4cloud.pig.ontology.mapper.AnnotationPropertyMapper;
import com.pig4cloud.pig.ontology.service.AnnotationPropertyService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 注释属性注册表 Service 实现（FR-4）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class AnnotationPropertyServiceImpl
		extends ServiceImpl<AnnotationPropertyMapper, AnnotationProperty> implements AnnotationPropertyService {

	/**
	 * appliesTo 合法取值（PRD 9.6）。
	 */
	private static final Set<String> APPLIES_TO_VALUES = Set.of(
			"class", "datatypeProperty", "objectProperty", "individual", "all");

	@Override
	public List<AnnotationProperty> list(String appliesTo) {
		return list(Wrappers.<AnnotationProperty>lambdaQuery()
				.eq(StrUtil.isNotBlank(appliesTo), AnnotationProperty::getAppliesTo, appliesTo)
				.orderByAsc(AnnotationProperty::getSortOrder)
				.orderByAsc(AnnotationProperty::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveAp(AnnotationProperty ap) {
		// 1. appliesTo 枚举校验（AC-4.3，空值允许=不限作用对象）
		if (StrUtil.isNotBlank(ap.getAppliesTo()) && !APPLIES_TO_VALUES.contains(ap.getAppliesTo())) {
			return R.failed("appliesTo 取值非法，允许：class/datatypeProperty/objectProperty/individual/all");
		}
		// 2. localName 预查重（AC-4.5）
		long count = count(Wrappers.<AnnotationProperty>lambdaQuery()
				.eq(AnnotationProperty::getLocalName, ap.getLocalName()));
		if (count > 0) {
			return R.failed("localName '" + ap.getLocalName() + "' 已存在");
		}
		ap.setSource("custom");
		try {
			// DB 唯一约束 uk_ont_ap_local_name 不受逻辑删除过滤，是权威兜底：
			// 若 localName 曾被软删，预查重查不到，由约束抛 DuplicateKeyException，转友好提示
			return R.ok(save(ap));
		}
		catch (DuplicateKeyException e) {
			return R.failed("localName '" + ap.getLocalName() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateAp(AnnotationProperty ap) {
		AnnotationProperty existing = getById(ap.getId());
		if (existing == null) {
			return R.failed("注释属性不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置注释属性不可编辑");
		}
		// appliesTo 枚举校验（AC-4.3）
		if (StrUtil.isNotBlank(ap.getAppliesTo()) && !APPLIES_TO_VALUES.contains(ap.getAppliesTo())) {
			return R.failed("appliesTo 取值非法，允许：class/datatypeProperty/objectProperty/individual/all");
		}
		// localName 不可改（引用稳定性，对齐 DD2 templateCode / DD4 qudtIri 范式）
		ap.setLocalName(existing.getLocalName());
		return R.ok(updateById(ap));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeAp(Long id) {
		AnnotationProperty existing = getById(id);
		if (existing == null) {
			return R.failed("注释属性不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置注释属性不可删除");
		}
		return R.ok(removeById(id));
	}

	@Override
	public List<AnnotationPropertySupplyVO> supplyList(String appliesTo) {
		List<AnnotationProperty> list = list(appliesTo);
		// 转稳定化 VO（屏蔽审计字段，AC-5.7），供建模侧序列化器/解析器遍历驱动
		return BeanUtil.copyToList(list, AnnotationPropertySupplyVO.class);
	}

	@Override
	public String exportMarkdown(String appliesTo) {
		List<AnnotationProperty> list = list(appliesTo);
		StringBuilder sb = new StringBuilder();
		sb.append("# 注释属性注册表\n\n");
		sb.append("> 共 ").append(list.size()).append(" 项")
				.append(StrUtil.isNotBlank(appliesTo) ? "（appliesTo=" + appliesTo + "）" : "").append("\n\n");
		sb.append("| # | localName | label | rangeXsd | appliesTo | description |\n");
		sb.append("|---|---|---|---|---|---|\n");
		if (CollUtil.isEmpty(list)) {
			sb.append("| - | - | - | - | - | - |\n");
			return sb.toString();
		}
		int i = 1;
		for (AnnotationProperty ap : list) {
			sb.append("| ").append(i++).append(" | ")
					.append(nullSafe(ap.getLocalName())).append(" | ")
					.append(nullSafe(ap.getLabel())).append(" | ")
					.append(nullSafe(ap.getRangeXsd())).append(" | ")
					.append(nullSafe(ap.getAppliesTo())).append(" | ")
					.append(nullSafe(ap.getDescription())).append(" |\n");
		}
		return sb.toString();
	}

	private String nullSafe(String s) {
		return s == null ? "" : s;
	}

}
