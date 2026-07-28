package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.UnitConvertResultVO;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 单位换算引擎（FR-3，AC-3.3/3.4/3.8）
 * <p>
 * QUDT 换算约定：base = (value + offset) × multiplier（base = 基准单位，如 Kelvin；offset 先加再乘），
 * 逆向 value = base / multiplier - offset。同量纲换算经基准中转；跨量纲返回 null（AC-3.3）。
 * <p>
 * 精度：系数优先 SN 科学计数（conversionMultiplierSn，如 "1.0E3"），用 new BigDecimal(String) 构造
 * 避免 Double.parseDouble 精度损失；全程 BigDecimal + MathContext.DECIMAL64，结果去尾保留 10 位（AC-3.8/NFR-9/R-6）。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class UnitConversionService {

	/**
	 * 换算结果保留的有效小数位（去尾，NFR-9/R-6）。
	 */
	private static final int RESULT_SCALE = 10;

	private final UnitMapper unitMapper;

	/**
	 * 同量纲换算：fromIri 的 value 换算为 toIri 的值；跨量纲或单位不存在返回 result=null。
	 * @param value 输入值
	 * @param fromIri 源单位 QUDT IRI
	 * @param toIri 目标单位 QUDT IRI
	 * @return 换算结果（含 reason）
	 */
	public UnitConvertResultVO convert(BigDecimal value, String fromIri, String toIri) {
		UnitConvertResultVO vo = new UnitConvertResultVO();
		vo.setValue(value);
		vo.setFromIri(fromIri);
		vo.setToIri(toIri);

		Unit from = unitMapper.selectOne(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, fromIri));
		Unit to = unitMapper.selectOne(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, toIri));
		if (from == null || to == null) {
			vo.setReason("unit not found");
			return vo;
		}
		// 量纲校验：跨量纲直接返 null（AC-3.3 convertValue(1, KiloGM, USD)===null）
		if (!from.getQuantityKindId().equals(to.getQuantityKindId())) {
			vo.setReason("quantity kind mismatch");
			return vo;
		}

		// 系数：优先 SN（AC-3.8），SN 为空退回 conversionMultiplier（默认 1）；偏移同理
		BigDecimal fromMul = multiplier(from);
		BigDecimal toMul = multiplier(to);
		BigDecimal fromOff = offset(from);
		BigDecimal toOff = offset(to);

		BigDecimal result;
		if (fromOff == null && toOff == null) {
			// 无偏移（长度/质量/时间/面积/体积/货币/数据量）：result = value × fromMul / toMul（AC-3.3）
			// 例：convertValue(1, KiloM, M) = 1×1000/1 = 1000；convertValue(100, CentiM, M) = 100×0.01/1 = 1
			result = value.multiply(fromMul, MathContext.DECIMAL64)
					.divide(toMul, MathContext.DECIMAL64);
		}
		else {
			// 有偏移（温度等）：经基准（Kelvin）中转（AC-3.4）
			// base = (value + fromOff) × fromMul；result = base / toMul - toOff
			// 例：convertValue(32, DEG_F, DEG_C) = (32+459.67)×0.5556/1.0 - 273.15 = 273.15 - 273.15 = 0
			BigDecimal fOff = fromOff == null ? BigDecimal.ZERO : fromOff;
			BigDecimal tOff = toOff == null ? BigDecimal.ZERO : toOff;
			BigDecimal base = value.add(fOff, MathContext.DECIMAL64)
					.multiply(fromMul, MathContext.DECIMAL64);
			result = base.divide(toMul, MathContext.DECIMAL64).subtract(tOff, MathContext.DECIMAL64);
		}
		// 精度：保留 10 位有效位去尾（NFR-9/R-6）
		vo.setResult(result.setScale(RESULT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros());
		return vo;
	}

	/**
	 * 取换算系数：优先 SN 科学计数（如 "1.0E3"），SN 为空退回 conversionMultiplier（默认 1）。
	 * <p>
	 * SN 用 new BigDecimal(String) 构造，避免 Double.parseDouble 的二进制精度损失（AC-3.8）。
	 * @param unit 单位
	 * @return 换算系数
	 */
	private BigDecimal multiplier(Unit unit) {
		if (StrUtil.isNotBlank(unit.getConversionMultiplierSn())) {
			return new BigDecimal(unit.getConversionMultiplierSn());
		}
		return unit.getConversionMultiplier() != null ? unit.getConversionMultiplier() : BigDecimal.ONE;
	}

	/**
	 * 取换算偏移：优先 SN 科学计数（如 "4.5967E2"），SN 为空退回 conversionOffset（可能为 null）。
	 * @param unit 单位
	 * @return 换算偏移；无偏移单位返回 null
	 */
	private BigDecimal offset(Unit unit) {
		if (StrUtil.isNotBlank(unit.getConversionOffsetSn())) {
			return new BigDecimal(unit.getConversionOffsetSn());
		}
		return unit.getConversionOffset();
	}

}
