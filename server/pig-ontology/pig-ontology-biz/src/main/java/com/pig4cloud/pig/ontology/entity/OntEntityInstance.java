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
 * 实体对象实例。
 *
 * @author youming
 */
@Data
@TableName("ont_entity_instance")
@Schema(description = "实体对象实例")
@EqualsAndHashCode(callSuper = true)
public class OntEntityInstance extends Model<OntEntityInstance> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "实例ID")
	private Long id;

	@Schema(description = "完整IRI，由namespace.uri+iriLocalName拼接")
	private String iri;

	@Schema(description = "IRI本地标识符")
	private String iriLocalName;

	@Schema(description = "rdf:type实体类型ID")
	private Long rdfTypeId;

	@Schema(description = "UI显示标签，不替代本体数据属性")
	private String label;

	@Schema(description = "命名空间ID")
	private Long namespaceId;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "来源类型：APPENDIX_D/EXTENSION")
	private String sourceType;

	@Schema(description = "来源引用，如D-Step5")
	private String sourceReference;

	@Schema(description = "声明模式：EXPLICIT/REFERENCE_ONLY")
	private String declarationMode;

	@Schema(description = "是否内置，1是0否")
	private String isBuiltin;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "治理说明/源异常说明")
	private String remarks;

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
