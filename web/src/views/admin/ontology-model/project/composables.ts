import { useI18n } from 'vue-i18n';

/**
 * 本体项目管理页共享选项（index.vue / form.vue / prefix-dialog.vue 复用，消除重复定义）
 */
export function useModelProjectOptions() {
	const { t } = useI18n();

	/**
	 * 项目状态下拉选项（取值：draft / active / archived，对齐 PRD FR-10.4）
	 */
	const statusOptions = computed(() => [
		{ value: 'draft', label: t('modelProject.statusDraft'), tagType: 'info' },
		{ value: 'active', label: t('modelProject.statusActive'), tagType: 'success' },
		{ value: 'archived', label: t('modelProject.statusArchived'), tagType: 'warning' },
	]);

	/**
	 * 序列化格式下拉选项（对齐 PRD FR-10.1 default_format：TTL / OWL_XML）
	 */
	const formatOptions = computed(() => [
		{ value: 'TTL', label: 'Turtle (.ttl)' },
		{ value: 'OWL_XML', label: 'OWL XML (.owl.xml)' },
	]);

	/**
	 * 状态中文标签（列表展示用，按值查 label）
	 */
	const statusLabel = (value: string) => {
		return statusOptions.value.find((o) => o.value === value)?.label ?? value;
	};

	/**
	 * 状态标签颜色（el-tag type）
	 */
	const statusTagType = (value: string) => {
		return statusOptions.value.find((o) => o.value === value)?.tagType ?? 'info';
	};

	return {
		statusOptions,
		formatOptions,
		statusLabel,
		statusTagType,
	};
}
