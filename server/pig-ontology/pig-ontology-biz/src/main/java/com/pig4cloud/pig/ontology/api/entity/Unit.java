package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单位（引用 QUDT）
 * <p>
 * 对应 PRD v1.2 9.5。单位以 qudtIri 为规范身份，按 quantityKindId 归属量纲。
 * 换算系数 conversionMultiplier（相对基准单位）+ 科学计数 conversionMultiplierSn（换算引擎优先用，
 * AC-3.8）；温度等有偏移单位用 conversionOffset/conversionOffsetSn。
 * <p>
 * QUDT 换算约定：base = (value + offset) × multiplier（base = 基准单位，如 Kelvin；
 * offset 先加再乘），逆向 value = base / multiplier - offset。同量纲换算经基准中转（AC-3.4）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "单位")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_unit")
public class Unit extends Model<Unit> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "唯一，如 .../unit/KiloGM")
	@NotBlank(message = "QUDT IRI 不能为空")
	private String qudtIri;

	@Schema(description = "符号，如 kg（冗余存储，降级用）")
	private String symbol;

	@Schema(description = "英文 label，如 Kilogram")
	@NotBlank(message = "显示名不能为空")
	private String label;

	@Schema(description = "中文 label，如 千克")
	private String labelCn;

	@Schema(description = "所属量纲 id -> ont_quantity_kind.id")
	@NotNull(message = "所属量纲不能为空")
	private Long quantityKindId;

	@Schema(description = "相对基准换算系数，如 KiloM=1000")
	private BigDecimal conversionMultiplier;

	@Schema(description = "QUDT 科学计数系数（如 1.0E3），换算引擎优先用此列减少浮点误差（AC-3.8）")
	private String conversionMultiplierSn;

	@Schema(description = "换算偏移（温度等有偏移单位，相对基准）")
	private BigDecimal conversionOffset;

	@Schema(description = "偏移科学计数系数（如 4.5967E2）")
	private String conversionOffsetSn;

	@Schema(description = "基准单位 qudt_iri（如 KiloM 的 scalingOf=.../unit/M）")
	private String scalingOf;

	@Schema(description = "UCUM 编码，如 m")
	private String ucumCode;

	@Schema(description = "builtin / custom")
	private String source;

	@Schema(description = "来源本体标识，如 qudt")
	private String sourceRef;

	@Schema(description = "0/1 弃用标记")
	private String deprecated;

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
