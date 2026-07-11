/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceQuery;
import com.pig4cloud.pig.ontology.dto.OntEntityInstanceUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceDataValueDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceObjectRelationDTO;
import com.pig4cloud.pig.ontology.dto.OntInstanceOptionQuery;
import com.pig4cloud.pig.ontology.entity.OntEntityInstance;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceDetailVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceOptionVO;
import com.pig4cloud.pig.ontology.vo.OntEntityInstanceSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceDataValueVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceFormMetaVO;
import com.pig4cloud.pig.ontology.vo.OntInstanceObjectRelationVO;

import java.util.List;

/**
 * 实体对象实例服务。
 *
 * @author youming
 */
public interface OntEntityInstanceService extends IService<OntEntityInstance> {

	/**
	 * 核心本体工程ID。
	 */
	long CORE_ONTOLOGY_ID = OntEntityTypeService.CORE_ONTOLOGY_ID;

	/**
	 * 分页查询实例摘要。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 实例摘要分页列表
	 */
	IPage<OntEntityInstanceSummaryVO> pageSummary(Page<OntEntityInstance> page, OntEntityInstanceQuery query);

	/**
	 * 实例详情（基本信息+数据值+出向断言+入向引用）。
	 * @param id 实例ID
	 * @return 详情
	 */
	OntEntityInstanceDetailVO getDetail(Long id);

	/**
	 * 动态表单Schema（按实体类型收集适用数据属性和对象属性，含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 表单元数据
	 */
	OntInstanceFormMetaVO getFormMeta(Long entityTypeId);

	/**
	 * 分页实例选择器（按对象属性值域过滤）。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 实例选项分页列表
	 */
	IPage<OntEntityInstanceOptionVO> pageOptions(Page<OntEntityInstance> page, OntInstanceOptionQuery query);

	/**
	 * 新增实例（含数据值和指向既有实例的断言）。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<OntEntityInstance> createInstance(OntEntityInstanceCreateDTO request);

	/**
	 * 修改实例元数据/类型。
	 * @param request 修改请求
	 * @return 处理结果
	 */
	R<OntEntityInstance> updateInstance(OntEntityInstanceUpdateDTO request);

	/**
	 * 删除扩展实例。
	 * @param id 实例ID
	 * @return 处理结果
	 */
	R<Boolean> deleteInstance(Long id);

	/**
	 * 查询实例全部数据值。
	 * @param instanceId 实例ID
	 * @return 数据值视图列表
	 */
	List<OntInstanceDataValueVO> getDataValues(Long instanceId);

	/**
	 * 整体替换实例数据值。
	 * @param instanceId 实例ID
	 * @param dataValues 数据值集合
	 * @return 处理结果
	 */
	R<Boolean> replaceDataValues(Long instanceId, List<OntInstanceDataValueDTO> dataValues);

	/**
	 * 删除实例某属性全部数据值。
	 * @param instanceId 实例ID
	 * @param dataPropertyId 数据属性ID
	 * @return 处理结果
	 */
	R<Boolean> deleteDataValues(Long instanceId, Long dataPropertyId);

	/**
	 * 查询实例出向/入向显式断言。
	 * @param instanceId 实例ID
	 * @param direction OUTGOING或INCOMING
	 * @return 断言视图列表
	 */
	List<OntInstanceObjectRelationVO> getRelations(Long instanceId, String direction);

	/**
	 * 新增INSTANCE客体断言。
	 * @param instanceId 主体实例ID
	 * @param request 断言请求
	 * @return 处理结果
	 */
	R<OntInstanceObjectRelationVO> addRelation(Long instanceId, OntInstanceObjectRelationDTO request);

	/**
	 * 删除指定显式断言。
	 * @param instanceId 主体实例ID
	 * @param relationId 断言ID
	 * @return 处理结果
	 */
	R<Boolean> removeRelation(Long instanceId, Long relationId);

}
