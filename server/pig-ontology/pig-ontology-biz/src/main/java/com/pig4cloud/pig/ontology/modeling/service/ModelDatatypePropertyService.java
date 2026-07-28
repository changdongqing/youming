package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;

/**
 * 数据属性 Service 接口（FR-12）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelDatatypePropertyService extends IService<ModelDatatypeProperty> {

	/**
	 * 分页查询（AC-12.6，按 classId 过滤）
	 */
	IPage<ModelDatatypeProperty> page(Page page, ModelDatatypeProperty prop);

	/**
	 * 新增数据属性（AC-12.1~12.5，localName 查重 + 单位校验 + 基数）
	 */
	R saveProp(ModelDatatypeProperty prop);

	/**
	 * 编辑数据属性（AC-12.6，锁定 localName/classId/propertyIri/templateCode）
	 */
	R updateProp(ModelDatatypeProperty prop);

	/**
	 * 删除数据属性（AC-12.6）
	 */
	R removeProp(Long id);

}
