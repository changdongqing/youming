package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 量纲（引用 QUDT）
 * <p>
 * 对应 PRD v1.2 9.4。量纲是单位的分组维度，承载 dimensionVector（量纲向量，
 * 如长度 A0E0L1I0M0H0T0D0）。单位按量纲归属，同量纲方可换算（AC-3.3）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "量纲")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_quantity_kind")
public class QuantityKind extends Model<QuantityKind> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 .../quantitykind/Length")
	@NotBlank(message = "QUDT IRI 不能为空")
	private String qudtIri;

	@Schema(description = "英文 label，如 Length")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "中文 label，如 长度")
	private String labelCn;

	@Schema(description = "量纲向量，如 A0E0L1I0M0H0T0D0")
	private String dimensionVector;

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
