package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对象属性 Entity（owl:ObjectProperty，FR-13，表前移自 DD9）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "对象属性")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_object_property")
public class ModelObjectProperty extends Model<ModelObjectProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "域类 ID（domain）")
	private Long domainClassId;

	@Schema(description = "值域类 ID（range），NULL=待补全（模板实例化的对象属性）")
	private Long rangeClassId;

	@Schema(description = "属性 IRI")
	@NotBlank(message = "属性 IRI 不能为空")
	private String propertyIri;

	@Schema(description = "本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "溯源：来源属性模板 template_code，NULL=手建")
	private String templateCode;

	@Schema(description = "最小基数")
	private Integer minCardinality;

	@Schema(description = "最大基数（-1=无限制）")
	private Integer maxCardinality;

	@Schema(description = "反向属性 ID，可空（owl:inverseOf）")
	private Long inverseOf;

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
