package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.ClassInstantiateDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.vo.ModelClassDetailVO;

/**
 * 本体类实体 Service 接口（FR-11）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelClassService extends IService<ModelClass> {

	/**
	 * 分页查询（AC-11.7，projectId 过滤 + label 模糊 + 模板溯源过滤）
	 */
	IPage<ModelClass> page(Page page, ModelClass cls);

	/**
	 * 类详情（AC-11.8，含属性列表+父类子类+溯源）
	 */
	ModelClassDetailVO getDetail(Long id);

	/**
	 * 新建类（AC-11.1~11.5，支持基于模板实例化）
	 */
	R saveClass(ModelClass cls);

	/**
	 * 编辑类（AC-11.6，锁定 IRI/溯源字段）
	 */
	R updateClass(ModelClass cls);

	/**
	 * 删除类（AC-11.6，校验属性引用 + 软删）
	 */
	R removeClass(Long id);

	/**
	 * 对已有类追加模板实例化（场景三，PRD 10.2 POST /{id}/instantiate）
	 */
	R instantiateFromClass(Long id, ClassInstantiateDTO dto);

}
