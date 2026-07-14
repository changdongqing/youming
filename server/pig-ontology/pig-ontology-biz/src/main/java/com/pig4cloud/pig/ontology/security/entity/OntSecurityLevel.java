/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 安全级别定义。
 *
 * @author youming
 */
@Data
@TableName("ont_security_level")
@Schema(description = "安全级别定义")
@EqualsAndHashCode(callSuper = true)
public class OntSecurityLevel extends Model<OntSecurityLevel> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "安全级别ID")
	private Long id;

	@Schema(description = "级别编码，唯一")
	private String levelCode;

	@Schema(description = "级别名称")
	private String levelName;

	@Schema(description = "级别排序值，越大越敏感")
	private Integer levelRank;

	@Schema(description = "默认查看效果：ALLOW/MASK/DENY")
	private String defaultViewEffect;

	@Schema(description = "默认导出效果：ALLOW/MASK/DENY")
	private String defaultExportEffect;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "是否内置：1是 0否")
	private String isBuiltin;

	@Schema(description = "排序值")
	private Integer sortOrder;

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
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
