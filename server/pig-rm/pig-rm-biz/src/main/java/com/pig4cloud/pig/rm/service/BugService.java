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
import com.pig4cloud.pig.rm.api.dto.BugHandleDTO;
import com.pig4cloud.pig.rm.api.entity.Bug;

import java.time.LocalDate;

/**
 * Bug 服务接口
 *
 * @author youming
 * @date 2026-07-29
 */
public interface BugService extends IService<Bug> {

	/** Bug 分页（支持按来源/状态/处理人/日期筛选） */
	IPage<Bug> page(Page page, String source, String status, Long assigneeId, LocalDate startDate, LocalDate endDate);

	/** Bug 详情 */
	R detail(Long id);

	/** Bug 处理（状态流转：NEW→IN_PROGRESS→RESOLVED→VERIFIED→CLOSED） */
	R handle(BugHandleDTO dto);

}
