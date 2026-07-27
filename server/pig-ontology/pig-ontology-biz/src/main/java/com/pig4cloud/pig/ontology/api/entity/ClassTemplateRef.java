package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分类模板属性/关系引用（结构骨架）
 * <p>
 * 对应 PRD v1.2 9.3 / FR-2。指向 {@link PropertyTemplate} 的 template_code，
 * 形成「分类模板 → 属性模板」嵌套复用。ref_type 区分 property（数据/对象属性）与
 * relationship（关系）。inherit_flag 标记本条是「继承自父」还是「本节点新增」。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类模板属性/关系引用")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_template_ref")
public class ClassTemplateRef extends Model<ClassTemplateRef> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "-> ont_class_template.id")
	private Long classTemplateId;

	@Schema(description = "-> ont_property_template.template_code")
	private String propertyTemplateCode;

	@Schema(description = "property / relationship")
	private String refType;

	@Schema(description = "注入顺序")
	private Integer sortOrder;

	@Schema(description = "0/1 继承自父 vs 本节点新增")
	private String inheritFlag;

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
