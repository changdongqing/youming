import { useI18n } from 'vue-i18n';

/**
 * 序列化与导入页共享选项
 */
export function useSerializeOptions() {
	const { t } = useI18n();

	/** 序列化格式选项 */
	const formatOptions = computed(() => [
		{ value: 'TTL', label: 'Turtle (.ttl)' },
		{ value: 'OWL_XML', label: 'OWL XML (.owl.xml)' },
	]);

	/** 导入冲突策略选项（AC-16.6） */
	const conflictStrategyOptions = computed(() => [
		{ value: 'OVERWRITE', label: t('modelSerialize.conflictOverwrite') },
		{ value: 'SKIP', label: t('modelSerialize.conflictSkip') },
		{ value: 'RENAME', label: t('modelSerialize.conflictRename') },
	]);

	return { formatOptions, conflictStrategyOptions };
}
