package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本体类实体 Entity（owl:Class，建模域权威，FR-11）
 * <p>
 * 区别于治理域 ClassTemplate：本表是实际 owl:Class 实体，有独立 IRI（rdf:about），
 * 进入 RDF 序列化。ClassTemplate 是治理资产侧原型，不含 IRI，不进 RDF。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "本体类实体")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_class")
public class ModelClass extends Model<ModelClass> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "类的 IRI（rdf:about），项目内唯一")
	private String classIri;

	@Schema(description = "IRI 本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "中文标签")
	private String labelCn;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "溯源：来源分类模板 template_code，NULL=非模板创建")
	private String templateCode;

	@Schema(description = "溯源：来源分类模板 classification_code，NULL=非模板创建")
	private String classificationCode;

	@Schema(description = "外观：图标")
	private String icon;

	@Schema(description = "外观：颜色")
	private String color;

	@Schema(description = "排序")
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
