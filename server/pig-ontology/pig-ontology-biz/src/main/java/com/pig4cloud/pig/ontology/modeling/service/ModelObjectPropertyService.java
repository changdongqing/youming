package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.InverseSuggestDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.vo.InverseSuggestVO;

/**
 * 对象属性 Service 接口（FR-13）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelObjectPropertyService extends IService<ModelObjectProperty> {

	/**
	 * 分页查询（AC-13.4，按 domainClassId 过滤）
	 */
	IPage<ModelObjectProperty> page(Page page, ModelObjectProperty prop);

	/**
	 * 新增对象属性（AC-13.1，domain/range + 基数）
	 */
	R saveProp(ModelObjectProperty prop);

	/**
	 * 编辑对象属性（AC-13.4，补全 range）
	 */
	R updateProp(ModelObjectProperty prop);

	/**
	 * 删除对象属性（AC-13.4）
	 */
	R removeProp(Long id);

	/**
	 * 反向关系建议（AC-13.5）
	 */
	InverseSuggestVO suggestInverse(InverseSuggestDTO dto);

}
