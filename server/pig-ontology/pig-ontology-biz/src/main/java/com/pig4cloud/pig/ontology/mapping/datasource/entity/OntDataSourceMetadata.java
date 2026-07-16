/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.datasource.entity;

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
import com.pig4cloud.pig.common.data.handler.StringToJsonbTypeHandler;

/**
 * 数据源元数据缓存表。
 * <p>
 * 缓存外部数据源的 schema/表/视图/列/主键/唯一键/外键结构信息，供映射设计器和漂移检测使用。
 *
 * @author youming
 */
@Data
@TableName("ont_data_source_metadata")
@Schema(description = "数据源元数据缓存")
@EqualsAndHashCode(callSuper = true)
public class OntDataSourceMetadata extends Model<OntDataSourceMetadata> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "元数据ID")
	private Long id;

	@Schema(description = "数据源ID")
	private Long sourceId;

	@Schema(description = "Schema名")
	private String schemaName;

	@Schema(description = "对象名")
	private String objectName;

	@Schema(description = "对象类型: TABLE / VIEW")
	private String objectType;

	@Schema(description = "元数据JSON")
	@TableField(typeHandler = StringToJsonbTypeHandler.class)
	private String metadataJson;

	@Schema(description = "元数据指纹哈希")
	private String metadataHash;

	@Schema(description = "数据源版本号")
	private Long sourceRevision;

	@Schema(description = "刷新时间")
	private LocalDateTime refreshedAt;

	@Schema(description = "过期时间")
	private LocalDateTime expiresAt;

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
