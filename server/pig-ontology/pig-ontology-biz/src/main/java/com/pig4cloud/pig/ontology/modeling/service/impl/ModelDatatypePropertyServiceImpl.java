package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelDatatypePropertyService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 数据属性 Service 实现（FR-12）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelDatatypePropertyServiceImpl
		extends ServiceImpl<ModelDatatypePropertyMapper, ModelDatatypeProperty>
		implements ModelDatatypePropertyService {

	/** 可绑单位的数据类型（数值型，AC-12.2） */
	private static final Set<String> NUMERIC_TYPES = Set.of(
			"xsd:integer", "xsd:decimal", "xsd:double", "xsd:float", "xsd:long", "xsd:int");

	private final ModelClassMapper modelClassMapper;

	@Override
	public IPage<ModelDatatypeProperty> page(Page page, ModelDatatypeProperty prop) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelDatatypeProperty>lambdaQuery()
					.eq(prop.getClassId() != null, ModelDatatypeProperty::getClassId, prop.getClassId())
					.like(StrUtil.isNotBlank(prop.getLabel()), ModelDatatypeProperty::getLabel, prop.getLabel())
					.orderByAsc(ModelDatatypeProperty::getSortOrder));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProp(ModelDatatypeProperty prop) {
		// 1. 拼接 propertyIri（方案B: {classLocalName}_{propLocalName}）
		if (StrUtil.isBlank(prop.getPropertyIri())) {
			ModelClass cls = modelClassMapper.selectById(prop.getClassId());
			if (cls == null) {
				return R.failed("所属类不存在");
			}
			prop.setPropertyIri(cls.getLocalName() + "_" + prop.getLocalName());
			prop.setProjectId(cls.getProjectId());
		}
		// 2. localName 同类唯一校验（AC-12.1）
		long count = count(Wrappers.<ModelDatatypeProperty>lambdaQuery()
			.eq(ModelDatatypeProperty::getClassId, prop.getClassId())
			.eq(ModelDatatypeProperty::getLocalName, prop.getLocalName()));
		if (count > 0) {
			return R.failed("属性名 '" + prop.getLocalName() + "' 在该类下已存在");
		}
		// 3. 单位绑定校验：非数值型不可绑单位（AC-12.2）
		if (StrUtil.isNotBlank(prop.getUnitRef()) && !NUMERIC_TYPES.contains(prop.getXsdType())) {
			return R.failed("非数值型属性（" + prop.getXsdType() + "）不可绑定单位");
		}
		// 4. 基数校验（AC-12.4）
		if (prop.getMinCardinality() == null) {
			prop.setMinCardinality(0);
		}
		if (prop.getMaxCardinality() == null) {
			prop.setMaxCardinality(-1);
		}
		if (prop.getMinCardinality() < 0) {
			return R.failed("最小基数不能为负");
		}
		if (prop.getMaxCardinality() != -1 && prop.getMaxCardinality() < prop.getMinCardinality()) {
			return R.failed("最大基数不能小于最小基数");
		}
		// 5. 标识符默认值（AC-12.5）
		if (StrUtil.isBlank(prop.getIsIdentifier())) {
			prop.setIsIdentifier("0");
		}
		// 6. propertyIri 预查重（uk: project_id + property_iri）
		try {
			return R.ok(save(prop));
		}
		catch (DuplicateKeyException e) {
			return R.failed("属性 IRI '" + prop.getPropertyIri() + "' 在项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProp(ModelDatatypeProperty prop) {
		ModelDatatypeProperty existing = getById(prop.getId());
		if (existing == null) {
			return R.failed("属性不存在");
		}
		// localName / classId / propertyIri / templateCode 不可改（锁定）
		prop.setLocalName(existing.getLocalName());
		prop.setClassId(existing.getClassId());
		prop.setPropertyIri(existing.getPropertyIri());
		prop.setTemplateCode(existing.getTemplateCode());
		prop.setProjectId(existing.getProjectId());
		// 单位校验
		if (StrUtil.isNotBlank(prop.getUnitRef()) && !NUMERIC_TYPES.contains(prop.getXsdType())) {
			return R.failed("非数值型属性（" + prop.getXsdType() + "）不可绑定单位");
		}
		return R.ok(updateById(prop));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProp(Long id) {
		return R.ok(removeById(id));
	}

}
