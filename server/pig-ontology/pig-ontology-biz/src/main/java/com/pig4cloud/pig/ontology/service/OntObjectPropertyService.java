/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntObjectPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyByRangeVO;
import com.pig4cloud.pig.ontology.vo.OntApplicableObjectPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntObjectPropertySummaryVO;

import java.util.List;

/**
 * 对象属性服务。
 *
 * @author youming
 */
public interface OntObjectPropertyService extends IService<OntObjectProperty> {

	/**
	 * 分页查询对象属性。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 对象属性分页列表
	 */
	IPage<OntObjectPropertySummaryVO> pageSummary(Page<OntObjectProperty> page, OntObjectPropertyQuery query);

	/**
	 * 列表查询对象属性（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 对象属性摘要列表
	 */
	List<OntObjectPropertySummaryVO> listSummary(OntObjectPropertyQuery query);

	/**
	 * 对象属性详情。
	 * @param id 对象属性ID
	 * @return 详情
	 */
	OntObjectPropertyDetailVO getDetail(Long id);

	/**
	 * 按定义域查询适用对象属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用对象属性列表
	 */
	List<OntApplicableObjectPropertyVO> listApplicableByDomain(Long entityTypeId);

	/**
	 * 按值域查询适用对象属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用对象属性列表
	 */
	List<OntApplicableObjectPropertyByRangeVO> listApplicableByRange(Long entityTypeId);

	/**
	 * 新增对象属性。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<OntObjectProperty> saveObjectProperty(OntObjectPropertyCreateDTO request);

	/**
	 * 修改对象属性。
	 * @param request 修改请求
	 * @return 处理结果
	 */
	R<OntObjectProperty> updateObjectProperty(OntObjectPropertyUpdateDTO request);

	/**
	 * 删除对象属性。
	 * @param id 对象属性ID
	 * @return 处理结果
	 */
	R<Boolean> removeObjectProperty(Long id);

}
