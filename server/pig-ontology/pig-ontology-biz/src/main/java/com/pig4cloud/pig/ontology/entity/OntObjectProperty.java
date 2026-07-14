/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

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
 * 对象属性。
 *
 * @author youming
 */
@Data
@TableName("ont_object_property")
@Schema(description = "对象属性")
@EqualsAndHashCode(callSuper = true)
public class OntObjectProperty extends Model<OntObjectProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "对象属性ID")
	private Long id;

	@Schema(description = "平台内部全局唯一IRI")
	private String iri;

	@Schema(description = "IRI本地标识符，用于拼接iri")
	private String iriLocalName;

	@Schema(description = "附录A.2 Name，核心值保持国标原名")
	private String name;

	@Schema(description = "定义")
	private String definition;

	@Schema(description = "逆属性ID")
	private Long inverseOfId;

	@Schema(description = "是否函数型属性，1是0否")
	private String isFunctional;

	@Schema(description = "是否逆函数型属性，1是0否")
	private String isInverseFunctional;

	@Schema(description = "是否传递属性，1是0否")
	private String isTransitive;

	@Schema(description = "是否对称属性，1是0否")
	private String isSymmetric;

	@Schema(description = "来源类型")
	private String sourceType;

	@Schema(description = "来源引用")
	private String sourceReference;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "平台治理备注")
	private String remarks;

	@Schema(description = "安全级别编码，默认INTERNAL")
	private String securityLevelCode;

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
