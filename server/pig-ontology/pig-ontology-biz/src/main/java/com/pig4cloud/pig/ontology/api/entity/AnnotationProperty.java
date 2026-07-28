package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 注释属性注册表
 * <p>
 * 对应 PRD v1.2 9.6。集中声明所有 ont:xxx 注释属性，驱动建模侧序列化/解析，而非散落硬编码
 * ont: 字面量（FR-4）。localName 即 ont:xxx 的 xxx（序列化时拼成 ont:icon 等），唯一约束
 * uk_ont_ap_local_name（AC-4.5）。appliesTo 约束作用对象
 * （class/datatypeProperty/objectProperty/individual/all，AC-4.3）。source 列由 V10 增补，
 * builtin 随 Flyway 分发只读、custom 为治理员新增。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "注释属性注册表")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_annotation_property")
public class AnnotationProperty extends Model<AnnotationProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 icon/unitRef（序列化时拼成 ont:icon）")
	@NotBlank(message = "localName 不能为空")
	private String localName;

	@Schema(description = "显示名")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "值域 XSD，如 xsd:string/xsd:boolean")
	private String rangeXsd;

	@Schema(description = "作用对象：class/datatypeProperty/objectProperty/individual/all")
	private String appliesTo;

	@Schema(description = "说明")
	private String description;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "builtin / custom")
	private String source;

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
