package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分类编码规则（GB/T 51269 式，FR-8）
 * <p>
 * 对应 PRD v1.2 9.7。定义编码规则：分隔符（默认 '-'）、每级位数（默认 2）、编码基数
 * （如大类从 30 起）、是否零填充。按 tree_root 维度配置，编码生成器据本表生成/校验编码。
 * <p>
 * 规则变更只影响新节点（AC-8.1/R-14），旧编码冻结。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "分类编码规则")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_classification_rule")
public class ClassificationRule extends Model<ClassificationRule> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属分类树标识，对应 ont_class_template.tree_root")
	@NotBlank(message = "分类树标识不能为空")
	private String treeRoot;

	@Schema(description = "分隔符，默认 -")
	private String separator;

	@Schema(description = "每级位数，默认 2")
	private Integer levelDigits;

	@Schema(description = "根级编码基数，如 30")
	private Integer baseNumber;

	@Schema(description = "0/1 是否零填充（默认 1）")
	private String zeroPad;

	@Schema(description = "说明")
	private String description;

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
