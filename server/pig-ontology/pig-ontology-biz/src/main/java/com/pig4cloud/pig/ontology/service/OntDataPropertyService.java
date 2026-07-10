/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyQuery;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.vo.OntApplicableDataPropertyVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertyDetailVO;
import com.pig4cloud.pig.ontology.vo.OntDataPropertySummaryVO;

import java.util.List;

/**
 * 数据属性服务。
 *
 * @author youming
 */
public interface OntDataPropertyService extends IService<OntDataProperty> {

	/**
	 * 分页查询数据属性。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 数据属性分页列表
	 */
	IPage<OntDataPropertySummaryVO> pageSummary(Page<OntDataProperty> page, OntDataPropertyQuery query);

	/**
	 * 列表查询数据属性（不分页，供下拉引用）。
	 * @param query 查询条件
	 * @return 数据属性摘要列表
	 */
	List<OntDataPropertySummaryVO> listSummary(OntDataPropertyQuery query);

	/**
	 * 数据属性详情。
	 * @param id 数据属性ID
	 * @return 详情
	 */
	OntDataPropertyDetailVO getDetail(Long id);

	/**
	 * 按定义域查询适用数据属性（含继承）。
	 * @param entityTypeId 实体类型ID
	 * @return 适用数据属性列表
	 */
	List<OntApplicableDataPropertyVO> listApplicableByDomain(Long entityTypeId);

	/**
	 * 新增数据属性。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<OntDataProperty> saveDataProperty(OntDataPropertyCreateDTO request);

	/**
	 * 修改数据属性。
	 * @param request 修改请求
	 * @return 处理结果
	 */
	R<OntDataProperty> updateDataProperty(OntDataPropertyUpdateDTO request);

	/**
	 * 删除数据属性。
	 * @param id 数据属性ID
	 * @return 处理结果
	 */
	R<Boolean> removeDataProperty(Long id);

}
