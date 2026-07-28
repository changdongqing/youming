package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据属性 Entity（owl:DatatypeProperty，FR-12，表前移自 DD9）
 * <p>
 * 模板实例化时由 ClassInstantiationService 批量创建；DD9 实现手动 CRUD/单位绑定/枚举/基数。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "数据属性")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_datatype_property")
public class ModelDatatypeProperty extends Model<ModelDatatypeProperty> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "所属类 ID（domain）")
	private Long classId;

	@Schema(description = "属性 IRI（方案B: {classLocalName}_{propLocalName}）")
	@NotBlank(message = "属性 IRI 不能为空")
	private String propertyIri;

	@Schema(description = "本地名")
	@NotBlank(message = "本地名不能为空")
	private String localName;

	@Schema(description = "rdfs:label")
	private String label;

	@Schema(description = "溯源：来源属性模板 template_code，NULL=手建")
	private String templateCode;

	@Schema(description = "XSD 数据类型，如 xsd:string / xsd:integer")
	@NotBlank(message = "XSD 类型不能为空")
	private String xsdType;

	@Schema(description = "单位引用（QUDT IRI）")
	private String unitRef;

	@Schema(description = "枚举值（逗号分隔）")
	private String enumValues;

	@Schema(description = "最小基数")
	private Integer minCardinality;

	@Schema(description = "最大基数（-1=无限制）")
	private Integer maxCardinality;

	@Schema(description = "是否标识符")
	private String isIdentifier;

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
