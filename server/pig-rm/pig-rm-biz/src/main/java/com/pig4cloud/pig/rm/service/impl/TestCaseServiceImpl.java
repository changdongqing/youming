/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.rm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.entity.Requirement;
import com.pig4cloud.pig.rm.api.entity.TestCase;
import com.pig4cloud.pig.rm.api.vo.CoverageVO;
import com.pig4cloud.pig.rm.mapper.RequirementMapper;
import com.pig4cloud.pig.rm.mapper.TestCaseMapper;
import com.pig4cloud.pig.rm.service.CodeGeneratorService;
import com.pig4cloud.pig.rm.service.TestCaseService;
import com.pig4cloud.pig.rm.statemachine.RequirementStatusEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 测试用例服务实现
 *
 * @author youming
 * @date 2026-07-29
 */
@Service
@AllArgsConstructor
public class TestCaseServiceImpl extends ServiceImpl<TestCaseMapper, TestCase> implements TestCaseService {

	private final CodeGeneratorService codeGenerator;

	private final RequirementMapper requirementMapper;

	@Override
	public IPage<TestCase> page(Page page, Long requirementId, String keyword, String status) {
		return baseMapper.selectPage(page,
				Wrappers.<TestCase>lambdaQuery()
					.eq(requirementId != null, TestCase::getRequirementId, requirementId)
					.eq(StrUtil.isNotBlank(status), TestCase::getStatus, status)
					.like(StrUtil.isNotBlank(keyword), TestCase::getTitle, keyword)
					.orderByDesc(TestCase::getCreateTime));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveCase(TestCase testCase) {
		testCase.setCaseCode(codeGenerator.next("CASE"));
		if (StrUtil.isBlank(testCase.getStatus())) {
			testCase.setStatus("DRAFT");
		}
		if (StrUtil.isBlank(testCase.getCaseType())) {
			testCase.setCaseType("FUNCTIONAL");
		}
		save(testCase);
		return R.ok(testCase);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateCase(TestCase testCase) {
		TestCase existing = getById(testCase.getId());
		if (existing == null) {
			return R.failed("用例不存在");
		}
		updateById(testCase);
		return R.ok();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeCase(Long id) {
		TestCase testCase = getById(id);
		if (testCase == null) {
			return R.failed("用例不存在");
		}
		if (!"DRAFT".equals(testCase.getStatus()) && !"DEPRECATED".equals(testCase.getStatus())) {
			return R.failed("仅草稿/已废弃状态可删除");
		}
		removeById(id);
		return R.ok();
	}

	@Override
	public R coverage() {
		// 1. 查询所有开发中及以上的需求
		List<Requirement> activeReqs = requirementMapper.selectList(Wrappers.<Requirement>lambdaQuery()
			.in(Requirement::getStatus, RequirementStatusEnum.DEVELOPING.name(),
					RequirementStatusEnum.TESTING.name(), RequirementStatusEnum.PENDING_ACCEPTANCE.name(),
					RequirementStatusEnum.COMPLETED.name(), RequirementStatusEnum.RELEASED.name()));
		long totalReq = activeReqs.size();

		// 2. 查询已关联用例的需求数（去重）
		List<TestCase> cases = list(Wrappers.<TestCase>lambdaQuery()
			.isNotNull(TestCase::getRequirementId)
			.select(TestCase::getRequirementId));
		// 过滤出活跃需求中有关联用例的
		long coveredReq = activeReqs.stream()
			.filter(req -> cases.stream().anyMatch(c -> req.getId().equals(c.getRequirementId())))
			.count();

		// 3. 计算覆盖率
		BigDecimal rate = totalReq > 0
				? BigDecimal.valueOf(coveredReq)
					.multiply(BigDecimal.valueOf(100))
					.divide(BigDecimal.valueOf(totalReq), 2, RoundingMode.HALF_UP)
				: BigDecimal.ZERO;

		CoverageVO vo = new CoverageVO();
		vo.setTotalReq(totalReq);
		vo.setCoveredReq(coveredReq);
		vo.setCoverageRate(rate);
		return R.ok(vo);
	}

}
