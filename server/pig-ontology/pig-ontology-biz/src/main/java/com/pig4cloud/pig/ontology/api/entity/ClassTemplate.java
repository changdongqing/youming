package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分类模板（外观 + 结构骨架 + 父子继承 + 编码，融合表）
 * <p>
 * 对应 PRD v1.2 9.2 / FR-2。融合外观（icon/color）+ 结构骨架（propertyRefs，见
 * {@link ClassTemplateRef}）+ 父子继承（parent_id）+ 分类编码（classification_code，FR-8）。
 * <p>
 * 边界（AC-2.8）：本表无任何 RDF 序列化字段（无 rdf:about/IRI），分类模板是治理资产侧原型，
 * 不直接进 RDF；类层级（subClassOf）由建模侧权威写入、本功能只读镜像（见 {@link ClassHierarchy}）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_template")
public class ClassTemplate extends Model<ClassTemplate> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一标识，人类可读，如 pump/spray-pump")
	@NotBlank(message = "模板标识不能为空")
	private String templateCode;

	@Schema(description = "规范分类编码，如 30-01-01（承载层级，FR-8）")
	private String classificationCode;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "中文名")
	private String labelCn;

	@Schema(description = "业务说明")
	private String description;

	@Schema(description = "父分类模板 id，NULL=根节点（FR-2 父子继承）")
	private Long parentId;

	@Schema(description = "所属分类树标识，如 equipment")
	@NotBlank(message = "分类树标识不能为空")
	private String treeRoot;

	@Schema(description = "外观：emoji 或图标类名")
	private String icon;

	@Schema(description = "外观：hex 色值")
	private String color;

	@Schema(description = "0/1 是否继承父外观（默认 1）")
	private String inheritAppearance;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识，如 brick（FR-7 导入）")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

	@Schema(description = "同级排序")
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
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
