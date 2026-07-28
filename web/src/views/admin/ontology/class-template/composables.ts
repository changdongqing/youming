import request from '/@/utils/request';
import { useI18n } from 'vue-i18n';

/**
 * 分类模板页共享选项与下拉数据来源（index.vue / form.vue / inherited-panel.vue 复用）。
 */
export function useClassTemplateOptions() {
	const { t } = useI18n();

	/**
	 * 弃用状态下拉选项（列表筛选用）。
	 */
	const deprecatedOptions = computed(() => [
		{ value: '0', label: t('classTemplate.normal') },
		{ value: '1', label: t('classTemplate.deprecatedLabel') },
	]);

	/**
	 * 引用类型下拉选项（property / relationship）。
	 */
	const refTypeOptions = computed(() => [
		{ value: 'property', label: t('classTemplate.refTypeProperty') },
		{ value: 'relationship', label: t('classTemplate.refTypeRelationship') },
	]);

	/**
	 * 外观预设（business-blue / retail-warm / nature-green，PRD FR-2 内置 ≥3 套）。
	 */
	const appearancePresets = computed(() => [
		{ icon: '🏗️', color: '#1890ff', label: t('classTemplate.appearanceBlue') },
		{ icon: '🏪', color: '#fa8c16', label: t('classTemplate.appearanceWarm') },
		{ icon: '🌿', color: '#52c41a', label: t('classTemplate.appearanceGreen') },
	]);

	/**
	 * 继承视图 source 三态标记的 tag 配置（node / inherited / overridden）。
	 * type 为 undefined 时使用 ElTag 默认样式（ElTag 不接受空串）。
	 */
	const sourceTagConfig = computed(() => ({
		node: { type: undefined as '' | undefined, label: t('classTemplate.sourceNode') },
		inherited: { type: 'info' as const, label: t('classTemplate.sourceInherited') },
		overridden: { type: 'warning' as const, label: t('classTemplate.sourceOverridden') },
	}));

	return {
		deprecatedOptions,
		refTypeOptions,
		appearancePresets,
		sourceTagConfig,
	};
}

/**
 * 拉取属性模板下拉数据（供结构骨架引用选择）。复用 DD2 属性模板供给接口。
 */
export function fetchPropertyTemplateOptions(kind?: string) {
	return request({
		url: '/admin/ont/supply/v1/property-templates',
		method: 'get',
		params: kind ? { kind } : {},
	});
}
