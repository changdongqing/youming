/**
 * 单位注册表页共享选项（index.vue / form.vue / convert-panel.vue 复用，消除重复定义）
 */

/**
 * 弃用状态下拉选项（列表筛选用）。
 * 取值与后端 ont_unit.deprecated 一致（对齐 property-template/composables.ts）。
 */
export function useUnitOptions() {
	const { t } = useI18n();

	const deprecatedOptions = computed(() => [
		{ value: '0', label: t('unit.normal') },
		{ value: '1', label: t('unit.deprecatedLabel') },
	]);

	return {
		deprecatedOptions,
	};
}

/**
 * 本地换算（NFR-15 前端本地算系数，避免每次按键调 /convert 接口）。
 * 算法对齐后端 UnitConversionService：
 *   无偏移：result = value × fromMul / toMul
 *   有偏移：base = (value + fromOff) × fromMul；result = base / toMul - toOff
 * SN 优先：conversionMultiplierSn 如 "1.0E3"，用 Number() 解析（前端展示精度要求低，无需 BigDecimal）。
 *
 * @param value 输入值
 * @param from 源单位（含系数/偏移）
 * @param to 目标单位
 * @returns 换算结果；量纲不同返回 { result: null, reason: 'mismatch' }
 */
export function convertLocal(value: number, from: any, to: any): { result: number | null; reason: string } {
	if (!from || !to) {
		return { result: null, reason: 'notfound' };
	}
	if (from.quantityKindId !== to.quantityKindId) {
		return { result: null, reason: 'mismatch' };
	}
	const fromMul = snOr(from.conversionMultiplierSn, from.conversionMultiplier, 1);
	const toMul = snOr(to.conversionMultiplierSn, to.conversionMultiplier, 1);
	const fromOff = snOr(from.conversionOffsetSn, from.conversionOffset, null);
	const toOff = snOr(to.conversionOffsetSn, to.conversionOffset, null);

	let result: number;
	if (fromOff == null && toOff == null) {
		result = (value * fromMul) / toMul;
	} else {
		const fOff = fromOff ?? 0;
		const tOff = toOff ?? 0;
		const base = (value + fOff) * fromMul;
		result = base / toMul - tOff;
	}
	// 精度：去尾保留 10 位，去除浮点尾噪（对齐后端 setScale(10)）
	return { result: Number(result.toFixed(10)), reason: '' };
}

/**
 * SN 科学计数系数优先，为空退回普通系数，再为空用默认值。
 */
function snOr(sn: string | undefined, fallback: any, def: number | null): number | null {
	if (sn !== undefined && sn !== null && sn !== '') {
		const n = Number(sn);
		if (!Number.isNaN(n)) {
			return n;
		}
	}
	if (fallback !== undefined && fallback !== null && fallback !== '') {
		const n = Number(fallback);
		if (!Number.isNaN(n)) {
			return n;
		}
	}
	return def;
}
