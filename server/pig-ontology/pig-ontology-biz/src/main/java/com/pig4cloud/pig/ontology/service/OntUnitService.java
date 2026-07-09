/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.vo.OntUnitTreeNode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单位条目服务。
 *
 * @author youming
 */
public interface OntUnitService extends IService<OntUnit> {

	/**
	 * 新增单位。
	 * @param unit 单位
	 * @return 处理结果
	 */
	R<OntUnit> saveUnit(OntUnit unit);

	/**
	 * 修改单位。
	 * @param unit 单位
	 * @return 处理结果
	 */
	R<OntUnit> updateUnit(OntUnit unit);

	/**
	 * 删除单位。
	 * @param id 单位ID
	 * @return 处理结果
	 */
	R<Boolean> removeUnit(Long id);

	/**
	 * 分类+单位两级树。
	 * @return 树
	 */
	List<OntUnitTreeNode> tree();

	/**
	 * 按符号解析单位。
	 * @param symbol 单位符号
	 * @return 单位
	 */
	OntUnit getBySymbol(String symbol);

	/**
	 * 单位换算。
	 * @param from 来源单位ID
	 * @param to 目标单位ID
	 * @param value 数值
	 * @return 换算结果
	 */
	R<BigDecimal> convert(Long from, Long to, BigDecimal value);

}
