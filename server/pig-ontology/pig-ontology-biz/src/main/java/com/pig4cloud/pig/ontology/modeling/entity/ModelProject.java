package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本体项目 Entity（建模域顶层容器，FR-10）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "本体项目")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_project")
public class ModelProject extends Model<ModelProject> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "项目编码（唯一），如 fire-equipment")
	@NotBlank(message = "项目编码不能为空")
	private String projectCode;

	@Schema(description = "项目名称")
	@NotBlank(message = "项目名称不能为空")
	private String name;

	@Schema(description = "项目描述")
	private String description;

	@Schema(description = "IRI 命名空间基址，类/属性 IRI = namespace_base + localName")
	@NotBlank(message = "命名空间基址不能为空")
	private String namespaceBase;

	@Schema(description = "默认序列化格式：TTL / OWL_XML")
	private String defaultFormat;

	@Schema(description = "序列化策略：B=方案B(默认) / A=方案A(预留,v1禁用)")
	private String serializationStrategy;

	@Schema(description = "项目状态：draft / active / archived")
	private String status;

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
