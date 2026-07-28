import { useI18n } from 'vue-i18n';

/**
 * 属性建模页共享选项/工具函数（index.vue / datatype-form.vue / object-form.vue 复用）
 */
export function useModelPropertyOptions() {
	const { t } = useI18n();

	/** XSD 类型选项（numeric 标记控制单位绑定可用性，AC-12.2） */
	const xsdTypeOptions = computed(() => [
		{ value: 'xsd:string', label: 'xsd:string', numeric: false },
		{ value: 'xsd:integer', label: 'xsd:integer', numeric: true },
		{ value: 'xsd:decimal', label: 'xsd:decimal', numeric: true },
		{ value: 'xsd:boolean', label: 'xsd:boolean', numeric: false },
		{ value: 'xsd:dateTime', label: 'xsd:dateTime', numeric: false },
	]);

	/** 基数选项（AC-13.3） */
	const cardinalityOptions = computed(() => [
		{ value: 'one-to-one', label: t('modelProperty.cardOneToOne') },
		{ value: 'one-to-many', label: t('modelProperty.cardOneToMany') },
		{ value: 'many-to-one', label: t('modelProperty.cardManyToOne') },
		{ value: 'many-to-many', label: t('modelProperty.cardManyToMany') },
	]);

	/** 基数格式化：min..max（-1 显示 n） */
	const formatCardinality = (min: number, max: number) => {
		return `${min}..${max === -1 ? 'n' : max}`;
	};

	/** XSD 类型是否数值型（控制单位绑定可用性，AC-12.2） */
	const isNumericType = (xsdType: string) => {
		return xsdTypeOptions.value.find((o) => o.value === xsdType)?.numeric ?? false;
	};

	return { xsdTypeOptions, cardinalityOptions, formatCardinality, isNumericType };
}
