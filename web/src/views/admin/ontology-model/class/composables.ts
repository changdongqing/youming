import { useI18n } from 'vue-i18n';

/**
 * 本体类建模页共享选项/工具函数（index.vue / form.vue / inherited-preview.vue 复用）
 */
export function useModelClassOptions() {
	const { t } = useI18n();

	/** refType 选项（继承视图属性引用类型） */
	const refTypeOptions = computed(() => [
		{ value: 'property', label: t('modelClass.refTypeProperty'), tagType: 'primary' },
		{ value: 'relationship', label: t('modelClass.refTypeRelationship'), tagType: 'success' },
	]);

	/** source 三态选项（继承视图标记：本节点/继承/覆盖） */
	const sourceOptions = computed(() => [
		{ value: 'node', label: t('modelClass.sourceNode'), tagType: 'primary' },
		{ value: 'inherited', label: t('modelClass.sourceInherited'), tagType: 'info' },
		{ value: 'overridden', label: t('modelClass.sourceOverridden'), tagType: 'warning' },
	]);

	const refTypeLabel = (value: string) => refTypeOptions.value.find((o) => o.value === value)?.label ?? value;
	const refTypeTagType = (value: string) => refTypeOptions.value.find((o) => o.value === value)?.tagType ?? 'info';
	const sourceLabel = (value: string) => sourceOptions.value.find((o) => o.value === value)?.label ?? value;
	const sourceTagType = (value: string) => sourceOptions.value.find((o) => o.value === value)?.tagType ?? 'info';

	return { refTypeOptions, sourceOptions, refTypeLabel, refTypeTagType, sourceLabel, sourceTagType };
}
