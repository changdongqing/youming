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

package com.pig4cloud.pig.rm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.dto.RequirementApproveDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementDesignDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementQueryDTO;
import com.pig4cloud.pig.rm.api.dto.RequirementScheduleDTO;
import com.pig4cloud.pig.rm.api.entity.Requirement;

/**
 * 软件需求申请单服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface RequirementService extends IService<Requirement> {

	/** 分页查询 */
	IPage<Requirement> page(Page page, RequirementQueryDTO query);

	/** 详情（含审批记录、关联任务概要） */
	R detail(Long id);

	/** 创建需求（草稿），自动生成编号 */
	R saveRequirement(Requirement requirement);

	/** 编辑需求（仅草稿/已驳回状态可编辑） */
	R updateRequirement(Requirement requirement);

	/** 删除需求（仅草稿状态可删除） */
	R removeRequirement(Long id);

	/** 提交需求（草稿→待审批），触发审批流 */
	R submit(Long id);

	/** 审批（通过/驳回） */
	R approve(RequirementApproveDTO dto);

	/** 讨论会评审结论 */
	R review(RequirementApproveDTO dto);

	/** 保存需求设计 */
	R saveDesign(RequirementDesignDTO dto);

	/** 提交设计评审（设计中→设计评审中） */
	R submitDesignReview(Long id);

	/** 需求设计评审（通过/退回） */
	R designReview(RequirementApproveDTO dto);

	/** 开发排期 */
	R schedule(RequirementScheduleDTO dto);

	/** 产品经理质量确认 */
	R qualityConfirm(Long id);

	/** 发起人验收 */
	R accept(Long id, String conclusion, String remark);

	/** 需求统计（按来源/状态/时间） */
	R statistics();

}
