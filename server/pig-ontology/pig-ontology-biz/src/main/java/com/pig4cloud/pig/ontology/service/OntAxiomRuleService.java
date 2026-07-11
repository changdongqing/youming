/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleEnabledDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleQuery;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleUpdateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeRelationCreateDTO;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleDetailVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleSummaryVO;
import com.pig4cloud.pig.ontology.vo.OntAxiomRuleTemplateVO;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeRelationVO;

import java.util.List;

/**
 * 公理规则服务。
 *
 * @author youming
 */
public interface OntAxiomRuleService extends IService<OntAxiomRule> {

	/**
	 * 分页查询公理规则。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 公理规则分页列表
	 */
	IPage<OntAxiomRuleSummaryVO> pageSummary(Page<OntAxiomRule> page, OntAxiomRuleQuery query);

	/**
	 * 列表查询公理规则。
	 * @param query 查询条件
	 * @return 公理规则摘要列表
	 */
	List<OntAxiomRuleSummaryVO> listSummary(OntAxiomRuleQuery query);

	/**
	 * 公理规则详情。
	 * @param id 公理规则ID
	 * @return 详情
	 */
	OntAxiomRuleDetailVO getDetail(Long id);

	/**
	 * 获取可用模板列表。
	 * @return 模板列表
	 */
	List<OntAxiomRuleTemplateVO> listTemplates();

	/**
	 * 新增公理规则。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<OntAxiomRule> saveAxiomRule(OntAxiomRuleCreateDTO request);

	/**
	 * 修改公理规则。
	 * @param request 修改请求
	 * @return 处理结果
	 */
	R<OntAxiomRule> updateAxiomRule(OntAxiomRuleUpdateDTO request);

	/**
	 * 设置扩展规则启用状态（幂等）。
	 * @param id 规则ID
	 * @param request 启用状态请求
	 * @return 处理结果
	 */
	R<OntAxiomRule> setEnabled(Long id, OntAxiomRuleEnabledDTO request);

	/**
	 * 删除公理规则。
	 * @param id 公理规则ID
	 * @return 处理结果
	 */
	R<Boolean> removeAxiomRule(Long id);

	/**
	 * 查询不相交关系列表。
	 * @return 不相交关系列表
	 */
	List<OntEntityTypeRelationVO> listDisjoint();

	/**
	 * 新增不相交关系。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<Boolean> saveDisjoint(OntEntityTypeRelationCreateDTO request);

	/**
	 * 删除不相交关系。
	 * @param typeAId 类型A ID
	 * @param typeBId 类型B ID
	 * @return 处理结果
	 */
	R<Boolean> removeDisjoint(Long typeAId, Long typeBId);

	/**
	 * 查询等价关系列表。
	 * @return 等价关系列表
	 */
	List<OntEntityTypeRelationVO> listEquivalent();

	/**
	 * 新增等价关系。
	 * @param request 新增请求
	 * @return 处理结果
	 */
	R<Boolean> saveEquivalent(OntEntityTypeRelationCreateDTO request);

	/**
	 * 删除等价关系。
	 * @param typeAId 类型A ID
	 * @param typeBId 类型B ID
	 * @return 处理结果
	 */
	R<Boolean> removeEquivalent(Long typeAId, Long typeBId);

}
