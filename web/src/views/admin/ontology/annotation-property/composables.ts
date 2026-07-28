import { useI18n } from 'vue-i18n';

/**
 * 注释属性注册表页共享选项（index.vue / form.vue 复用，消除重复定义）
 */
export function useAnnotationPropertyOptions() {
	const { t } = useI18n();

	/**
	 * 作用对象下拉选项（取值与 PRD 9.6 applies_to 一致）
	 */
	const appliesToOptions = computed(() => [
		{ value: 'class', label: t('annotationProperty.appliesClass') },
		{ value: 'datatypeProperty', label: t('annotationProperty.appliesDatatypeProperty') },
		{ value: 'objectProperty', label: t('annotationProperty.appliesObjectProperty') },
		{ value: 'individual', label: t('annotationProperty.appliesIndividual') },
		{ value: 'all', label: t('annotationProperty.appliesAllValue') },
	]);

	/**
	 * 值域 XSD 下拉选项（常用 XSD，可自定义输入）
	 */
	const rangeXsdOptions = computed(() => [
		{ value: 'xsd:string', label: 'xsd:string' },
		{ value: 'xsd:boolean', label: 'xsd:boolean' },
		{ value: 'xsd:integer', label: 'xsd:integer' },
		{ value: 'xsd:decimal', label: 'xsd:decimal' },
		{ value: 'xsd:anyURI', label: 'xsd:anyURI' },
	]);

	/**
	 * 作用对象中文标签（列表展示用，按值查 label）
	 */
	const appliesToLabel = (value: string) => {
		return appliesToOptions.value.find((o) => o.value === value)?.label ?? value;
	};

	return {
		appliesToOptions,
		rangeXsdOptions,
		appliesToLabel,
	};
}
