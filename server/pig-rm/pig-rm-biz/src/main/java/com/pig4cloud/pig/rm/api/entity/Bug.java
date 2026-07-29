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

package com.pig4cloud.pig.rm.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bug
 *
 * @author youming
 * @date 2026-07-29
 */
@Data
@Schema(description = "Bug")
@EqualsAndHashCode(callSuper = true)
@TableName("rm_bug")
public class Bug extends Model<Bug> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "bug编号 BUG-XXX")
	private String bugCode;

	@Schema(description = "bug标题")
	@NotBlank(message = "bug标题不能为空")
	private String title;

	@Schema(description = "关联测试任务单ID")
	private Long testTaskId;

	@Schema(description = "关联测试执行记录ID")
	private Long executionId;

	@Schema(description = "关联需求ID（冗余）")
	private Long requirementId;

	@Schema(description = "来源 TEST/PROJECT/OPS——考核点7")
	private String source;

	@Schema(description = "严重等级 CRITICAL/MAJOR/MINOR/TRIVIAL")
	private String severity;

	@Schema(description = "处理人ID")
	private Long assigneeId;

	@Schema(description = "状态 NEW/IN_PROGRESS/RESOLVED/VERIFIED/CLOSED")
	private String status;

	@Schema(description = "提报日期（判断当日bug）")
	private LocalDate createDate;

	@Schema(description = "解决时间")
	private LocalDateTime resolveTime;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记")
	private String delFlag;

}
