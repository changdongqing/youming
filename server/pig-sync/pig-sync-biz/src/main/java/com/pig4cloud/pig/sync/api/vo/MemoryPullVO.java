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

package com.pig4cloud.pig.sync.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 记忆增量下行响应
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "记忆增量下行响应")
public class MemoryPullVO {

	@Schema(description = "记忆条目列表")
	private List<MemoryPullItemVO> entries;

	@Schema(description = "本批最后一条的 seq，作为下次拉取的 since 游标")
	private Long nextSeq;

	@Schema(description = "是否还有更多记录")
	private Boolean hasMore;

}
