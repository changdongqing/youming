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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.rm.api.entity.FlowNode;
import com.pig4cloud.pig.rm.mapper.FlowNodeMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程配置服务
 *
 * @author youming
 * @date 2026-07-29
 */
@Service
@AllArgsConstructor
public class FlowConfigService {

	private final FlowNodeMapper flowNodeMapper;

	/**
	 * 查询流程节点配置
	 */
	public R<List<FlowNode>> getByFlowCode(String flowCode) {
		List<FlowNode> nodes = flowNodeMapper
			.selectList(Wrappers.<FlowNode>lambdaQuery().eq(FlowNode::getFlowCode, flowCode)
				.orderByAsc(FlowNode::getSortOrder));
		return R.ok(nodes);
	}

}
