/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion.entity;

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
 * 对象关系来源。
 * <p>
 * 记录映射来源的对象属性断言的溯源和所有权信息。
 *
 * @author youming
 */
@Data
@TableName("ont_instance_relation_provenance")
@Schema(description = "对象关系来源")
@EqualsAndHashCode(callSuper = true)
public class OntInstanceRelationProvenance extends Model<OntInstanceRelationProvenance> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "关系来源ID")
	private Long id;

	@Schema(description = "关系断言ID")
	private Long relationId;

	@Schema(description = "映射工程ID")
	private Long mappingProjectId;

	@Schema(description = "主体来源绑定ID")
	private Long subjectBindingId;

	@Schema(description = "客体来源绑定ID")
	private Long objectBindingId;

	@Schema(description = "映射版本ID")
	private Long mappingVersionId;

	@Schema(description = "关系映射编码")
	private String relationMappingCode;

	@Schema(description = "所有权策略：SOURCE_WINS/MANUAL_WINS/REJECT_CONFLICT")
	private String ownershipPolicy;

	@Schema(description = "溯源状态：ACTIVE/OVERRIDDEN/STALE")
	private String provenanceStatus;

	@Schema(description = "来源关系键")
	private String sourceRelationKey;

	@Schema(description = "来源关系键SHA-256哈希")
	private String sourceRelationKeyHash;

	@Schema(description = "最近作业ID")
	private Long lastJobId;

	@Schema(description = "生成时间")
	private LocalDateTime generatedAt;

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
