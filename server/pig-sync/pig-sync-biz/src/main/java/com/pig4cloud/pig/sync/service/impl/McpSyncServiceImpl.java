/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary, with or without
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

package com.pig4cloud.pig.sync.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.sync.api.entity.McpTemplate;
import com.pig4cloud.pig.sync.api.vo.McpTemplateItemVO;
import com.pig4cloud.pig.sync.api.vo.McpTemplateListVO;
import com.pig4cloud.pig.sync.mapper.McpTemplateMapper;
import com.pig4cloud.pig.sync.service.McpSyncService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP 模板同步服务实现
 *
 * @author youming
 * @date 2026-07-31
 */
@Service
@AllArgsConstructor
public class McpSyncServiceImpl extends ServiceImpl<McpTemplateMapper, McpTemplate> implements McpSyncService {

	@Override
	public McpTemplateListVO listTemplates() {
		List<McpTemplate> records = list();
		List<McpTemplateItemVO> items = records.stream().map(this::toItem).toList();

		McpTemplateListVO vo = new McpTemplateListVO();
		vo.setTemplates(items);
		return vo;
	}

	private McpTemplateItemVO toItem(McpTemplate record) {
		McpTemplateItemVO item = new McpTemplateItemVO();
		item.setName(record.getName());
		item.setEndpoint(record.getEndpoint());
		item.setDescription(record.getDescription());
		item.setVersion(record.getVersion());
		item.setProtocol(record.getProtocol());
		item.setTransport(record.getTransport());
		item.setCredentialRef(record.getCredentialRef());
		item.setDefaultPolicy(record.getDefaultPolicy());
		item.setCapabilities(McpTemplateItemVO.parseCapabilities(record.getCapabilities()));
		return item;
	}

}
