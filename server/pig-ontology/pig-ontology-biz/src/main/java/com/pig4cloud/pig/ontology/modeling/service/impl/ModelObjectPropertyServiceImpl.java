package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.InverseSuggestDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelObjectPropertyService;
import com.pig4cloud.pig.ontology.modeling.vo.InverseSuggestVO;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 对象属性 Service 实现（FR-13）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelObjectPropertyServiceImpl
		extends ServiceImpl<ModelObjectPropertyMapper, ModelObjectProperty>
		implements ModelObjectPropertyService {

	private final ModelClassMapper modelClassMapper;

	@Override
	public IPage<ModelObjectProperty> page(Page page, ModelObjectProperty prop) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelObjectProperty>lambdaQuery()
					.eq(prop.getDomainClassId() != null, ModelObjectProperty::getDomainClassId,
							prop.getDomainClassId())
					.like(StrUtil.isNotBlank(prop.getLabel()), ModelObjectProperty::getLabel, prop.getLabel())
					.orderByAsc(ModelObjectProperty::getSortOrder));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProp(ModelObjectProperty prop) {
		// 1. range 校验（AC-13.1）：新建时 range 必填（模板实例化的 range=NULL 需 DD9 补全）
		if (prop.getRangeClassId() == null) {
			return R.failed("值域类（range）不能为空");
		}
		// 2. 拼接 propertyIri（方案B: {classLocalName}_{propLocalName}）
		if (StrUtil.isBlank(prop.getPropertyIri())) {
			ModelClass cls = modelClassMapper.selectById(prop.getDomainClassId());
			if (cls == null) {
				return R.failed("域类不存在");
			}
			prop.setPropertyIri(cls.getLocalName() + "_" + prop.getLocalName());
			prop.setProjectId(cls.getProjectId());
		}
		// 3. localName 同类唯一
		long count = count(Wrappers.<ModelObjectProperty>lambdaQuery()
			.eq(ModelObjectProperty::getDomainClassId, prop.getDomainClassId())
			.eq(ModelObjectProperty::getLocalName, prop.getLocalName()));
		if (count > 0) {
			return R.failed("属性名 '" + prop.getLocalName() + "' 在该类下已存在");
		}
		// 4. 基数默认值
		if (prop.getMinCardinality() == null) {
			prop.setMinCardinality(0);
		}
		if (prop.getMaxCardinality() == null) {
			prop.setMaxCardinality(-1);
		}
		try {
			return R.ok(save(prop));
		}
		catch (DuplicateKeyException e) {
			return R.failed("属性 IRI '" + prop.getPropertyIri() + "' 在项目内已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProp(ModelObjectProperty prop) {
		ModelObjectProperty existing = getById(prop.getId());
		if (existing == null) {
			return R.failed("属性不存在");
		}
		// localName / domainClassId 不可改
		prop.setLocalName(existing.getLocalName());
		prop.setDomainClassId(existing.getDomainClassId());
		prop.setPropertyIri(existing.getPropertyIri());
		prop.setTemplateCode(existing.getTemplateCode());
		prop.setProjectId(existing.getProjectId());
		// range 可改（DD9 补全 DD8 实例化的 NULL range）
		return R.ok(updateById(prop));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProp(Long id) {
		return R.ok(removeById(id));
	}

	@Override
	public InverseSuggestVO suggestInverse(InverseSuggestDTO dto) {
		// AC-13.5：建立 contains(domain=Order, range=Product) 后，
		// 建议在 Product 建立 belongsTo(domain=Product, range=Order)
		InverseSuggestVO vo = new InverseSuggestVO();
		vo.setSuggestedLocalName(mapInverseLocalName(dto.getLocalName()));
		vo.setSuggestedLabel(mapInverseLabel(dto.getLocalName()));
		vo.setSuggestedDomainClassId(dto.getRangeClassId()); // 反转：原 range -> 新 domain
		vo.setSuggestedRangeClassId(dto.getDomainClassId()); // 反转：原 domain -> 新 range
		return vo;
	}

	/**
	 * 反向属性名映射（AC-13.5）
	 * contains -> belongsTo, hasPart -> partOf, references -> referencedBy
	 */
	private String mapInverseLocalName(String localName) {
		if (localName == null) {
			return "inverseOf_" + System.currentTimeMillis();
		}
		return switch (localName) {
			case "contains" -> "belongsTo";
			case "hasPart" -> "partOf";
			case "references" -> "referencedBy";
			case "belongsTo" -> "contains";
			case "partOf" -> "hasPart";
			case "referencedBy" -> "references";
			default -> "inverseOf_" + localName;
		};
	}

	private String mapInverseLabel(String localName) {
		return switch (localName) {
			case "contains" -> "属于";
			case "hasPart" -> "所属";
			case "references" -> "被引用";
			case "belongsTo" -> "包含";
			case "partOf" -> "包含";
			case "referencedBy" -> "引用";
			default -> "反向_" + localName;
		};
	}

	/**
	 * defaultCardinality -> min/max 基数映射（AC-13.3）
	 * one-to-one -> [0,1]、one-to-many -> [0,-1]、many-to-one -> [0,1]、many-to-many -> [0,-1]
	 */
	public static int[] mapCardinality(String defaultCardinality) {
		if (defaultCardinality == null) {
			return new int[] { 0, -1 };
		}
		return switch (defaultCardinality) {
			case "one-to-one" -> new int[] { 0, 1 };
			case "one-to-many" -> new int[] { 0, -1 };
			case "many-to-one" -> new int[] { 0, 1 };
			case "many-to-many" -> new int[] { 0, -1 };
			default -> new int[] { 0, -1 };
		};
	}

}
