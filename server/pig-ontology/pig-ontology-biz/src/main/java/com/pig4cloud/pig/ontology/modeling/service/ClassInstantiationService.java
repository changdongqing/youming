package com.pig4cloud.pig.ontology.modeling.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.api.vo.InheritedPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import com.pig4cloud.pig.ontology.service.PropertyTemplateService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板实例化核心算法 Service（FR-11.2~11.5）
 * <p>
 * 消费治理域能力（方案 B：同模块只读 Service 注入，PRD R-23 "同模块内只读查询"豁免）：
 * - {@link ClassTemplateService#getByCode} + {@link ClassTemplateService#inheritedView} 取继承视图
 * - {@link PropertyTemplateService} 取属性模板详情
 * <p>
 * 不走 HTTP，不写治理域表，只读调用；强类型 VO/Entity，无 Map 字段名脆弱转换。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class ClassInstantiationService {

	private final ClassTemplateService classTemplateService;
	private final PropertyTemplateService propertyTemplateService;
	private final ModelClassMapper modelClassMapper;
	private final ModelDatatypePropertyMapper datatypePropertyMapper;
	private final ModelObjectPropertyMapper objectPropertyMapper;

	/**
	 * 从分类模板实例化属性到指定类（AC-11.2~11.5）
	 *
	 * @param cls 已保存的类实体（含 id/projectId/localName/templateCode）
	 * @return R.ok() 成功 / R.failed() 失败
	 */
	@Transactional(rollbackFor = Exception.class)
	public R instantiate(ModelClass cls) {
		// 1. 取分类模板实体（按 templateCode），不存在则失败
		ClassTemplate template = classTemplateService.getByCode(cls.getTemplateCode());
		if (template == null) {
			return R.failed("分类模板不存在: " + cls.getTemplateCode());
		}
		// 2. 取继承视图（InheritedViewService 缓存，强类型）
		InheritedViewVO inheritedView = classTemplateService.inheritedView(template.getId());
		if (inheritedView == null) {
			return R.failed("获取分类模板继承视图失败");
		}

		// 3. 从继承视图复制类级溯源字段（AC-11.5）
		cls.setClassificationCode(inheritedView.getClassificationCode());
		// 外观从继承视图复制（如类自身未设）
		if (cls.getIcon() == null) {
			cls.setIcon(inheritedView.getIcon());
		}
		if (cls.getColor() == null) {
			cls.setColor(inheritedView.getColor());
		}
		modelClassMapper.updateById(cls);

		// 4. 取属性清单（已合并父链，子覆盖父，最终生效版本）
		List<InheritedPropertyVO> properties = inheritedView.getProperties();
		if (CollUtil.isEmpty(properties)) {
			return R.ok("模板无属性，已创建空白类");
		}

		// 5. 批量取属性模板详情，按 templateCode 建索引（强类型 PropertyTemplate）
		Map<String, PropertyTemplate> templateMap = fetchPropertyTemplates();

		// 6. 遍历属性清单，按 kind 分流创建
		int sortOrder = 0;
		int created = 0;
		for (InheritedPropertyVO prop : properties) {
			String propertyTemplateCode = prop.getPropertyTemplateCode();
			sortOrder++;

			PropertyTemplate propTpl = templateMap.get(propertyTemplateCode);
			if (propTpl == null) {
				log.warn("属性模板不存在或已弃用: {}，跳过", propertyTemplateCode);
				continue;
			}
			String kind = propTpl.getKind();

			if ("datatype".equals(kind)) {
				// 创建数据属性（复制 type/unitRef/enumValues/isIdentifier，AC-11.4）
				ModelDatatypeProperty dtProp = new ModelDatatypeProperty();
				dtProp.setProjectId(cls.getProjectId());
				dtProp.setClassId(cls.getId());
				dtProp.setLocalName(propertyTemplateCode);
				dtProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
				dtProp.setLabel(propTpl.getLabel());
				dtProp.setTemplateCode(propertyTemplateCode);
				dtProp.setXsdType(mapXsdType(propTpl.getType()));
				dtProp.setUnitRef(propTpl.getUnitRef());
				dtProp.setEnumValues(propTpl.getEnumValues());
				dtProp.setIsIdentifier(propTpl.getIsIdentifier());
				dtProp.setMinCardinality(0);
				dtProp.setMaxCardinality(-1);
				dtProp.setSortOrder(sortOrder);
				datatypePropertyMapper.insert(dtProp);
				created++;
			}
			else if ("object".equals(kind)) {
				// 创建对象属性（range 留空，DD9 手动补全，AC-11.4）
				ModelObjectProperty objProp = new ModelObjectProperty();
				objProp.setProjectId(cls.getProjectId());
				objProp.setDomainClassId(cls.getId());
				objProp.setRangeClassId(null); // 对象属性 range 需手动选定（模板不含 range，PRD FR-13.2）
				objProp.setLocalName(propertyTemplateCode);
				objProp.setPropertyIri(cls.getLocalName() + "_" + propertyTemplateCode);
				objProp.setLabel(propTpl.getLabel());
				objProp.setTemplateCode(propertyTemplateCode);
				objProp.setMinCardinality(0);
				objProp.setMaxCardinality(-1);
				objProp.setSortOrder(sortOrder);
				objectPropertyMapper.insert(objProp);
				created++;
			}
		}
		return R.ok("实例化 " + created + " 个属性");
	}

	/**
	 * 批量拉取未弃用的属性模板，按 templateCode 建索引（强类型）
	 */
	private Map<String, PropertyTemplate> fetchPropertyTemplates() {
		List<PropertyTemplate> list = propertyTemplateService.list(Wrappers.<PropertyTemplate>lambdaQuery()
			.eq(PropertyTemplate::getDeprecated, "0"));
		Map<String, PropertyTemplate> map = new HashMap<>();
		for (PropertyTemplate item : list) {
			map.put(item.getTemplateCode(), item);
		}
		return map;
	}

	/**
	 * 属性模板 type -> XSD 类型映射
	 */
	private String mapXsdType(String templateType) {
		if (templateType == null) {
			return "xsd:string";
		}
		return switch (templateType) {
			case "string" -> "xsd:string";
			case "integer" -> "xsd:integer";
			case "decimal" -> "xsd:decimal";
			case "boolean" -> "xsd:boolean";
			case "datetime" -> "xsd:dateTime";
			default -> "xsd:string";
		};
	}

}
