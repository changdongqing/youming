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

package com.pig4cloud.pig.rm.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办项 VO
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "待办项VO")
public class TodoItemVO {

	private Long id;

	private String todoType;

	private String billType;

	private Long billId;

	private String title;

	private String url;

	private String status;

	private LocalDateTime dueTime;

	private LocalDateTime createTime;

	@Schema(description = "是否超期")
	private Boolean overdue;

	@Schema(description = "单据编号")
	private String billCode;

}
