package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * IRI 前缀注册 Entity（项目级，FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "IRI 前缀注册")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_prefix")
public class ModelPrefix extends Model<ModelPrefix> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "前缀名，如 ex / qudt / brick（NCName 规范）")
	@NotBlank(message = "前缀名不能为空")
	private String prefix;

	@Schema(description = "命名空间 URI")
	@NotBlank(message = "命名空间不能为空")
	private String namespace;

	@Schema(description = "1=项目默认前缀")
	private String isDefault;

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
