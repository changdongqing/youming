import { useI18n } from 'vue-i18n';

/**
 * 属性模板页共享选项（index.vue / form.vue 复用，消除重复定义）
 */
export function usePropertyTemplateOptions() {
	const { t } = useI18n();

	/**
	 * 分组下拉选项（basic/contact/monetary/temporal/status/containment/attribution）
	 * 取值与后端 ont_property_template.category 种子一致。
	 */
	const categoryOptions = computed(() => [
		{ value: 'basic', label: t('propertyTemplate.categoryBasic') },
		{ value: 'contact', label: t('propertyTemplate.categoryContact') },
		{ value: 'monetary', label: t('propertyTemplate.categoryMonetary') },
		{ value: 'temporal', label: t('propertyTemplate.categoryTemporal') },
		{ value: 'status', label: t('propertyTemplate.categoryStatus') },
		{ value: 'containment', label: t('propertyTemplate.categoryContainment') },
		{ value: 'attribution', label: t('propertyTemplate.categoryAttribution') },
	]);

	/**
	 * 弃用状态下拉选项（列表筛选用）
	 */
	const deprecatedOptions = computed(() => [
		{ value: '0', label: t('propertyTemplate.normal') },
		{ value: '1', label: t('propertyTemplate.deprecatedLabel') },
	]);

	return {
		categoryOptions,
		deprecatedOptions,
	};
}
