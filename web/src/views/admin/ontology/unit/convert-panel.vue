<template>
	<div class="convert-panel" v-if="quantityKindId">
		<div class="section-title mb8">
			{{ t('unit.convertTitle') }}
			<span class="muted ml8" v-if="quantityKindLabel">({{ quantityKindLabel }})</span>
		</div>
		<div class="convert-row">
			<el-input-number v-model="convertState.value" :controls="false" :placeholder="t('unit.convertValue')" style="width: 120px" />
			<span class="convert-label">{{ t('unit.convertFrom') }}</span>
			<el-select v-model="convertState.fromIri" :placeholder="t('unit.convertSelectTip')" filterable style="width: 180px" @change="doConvert">
				<el-option :key="u.qudtIri" :label="(u.labelCn || u.label) + ' [' + u.symbol + ']'" :value="u.qudtIri" v-for="u in units" />
			</el-select>
			<span class="convert-arrow">→</span>
			<span class="convert-label">{{ t('unit.convertTo') }}</span>
			<el-select v-model="convertState.toIri" :placeholder="t('unit.convertSelectTip')" filterable style="width: 180px" @change="doConvert">
				<el-option :key="u.qudtIri" :label="(u.labelCn || u.label) + ' [' + u.symbol + ']'" :value="u.qudtIri" v-for="u in units" />
			</el-select>
			<span class="convert-equal">{{ t('unit.convertEqual') }}</span>
			<el-tag v-if="convertState.reason === 'mismatch'" type="warning" size="default">{{ t('unit.convertMismatch') }}</el-tag>
			<el-tag v-else-if="convertState.reason === 'notfound'" type="info" size="default">{{ t('unit.convertNotFound') }}</el-tag>
			<span v-else class="convert-result">{{ convertState.result ?? '-' }}</span>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { useI18n } from 'vue-i18n';
import { useDebounceFn } from '@vueuse/core';
import { convertLocal } from './composables';

const props = defineProps({
	quantityKindId: { type: String, default: '' },
	quantityKindLabel: { type: String, default: '' },
	units: { type: Array as () => any[], default: () => [] },
});

const { t } = useI18n();

const convertState = reactive({
	value: 1,
	fromIri: '',
	toIri: '',
	result: null as number | null,
	reason: '' as '' | 'mismatch' | 'notfound',
});

/**
 * 本地换算（NFR-15：前端本地算系数，避免每次按键调 /convert 接口）。
 * 用 composables.convertLocal，算法对齐后端 UnitConversionService。
 */
const doConvert = () => {
	if (!convertState.fromIri || !convertState.toIri) {
		convertState.result = null;
		convertState.reason = '';
		return;
	}
	const from = props.units.find((u) => u.qudtIri === convertState.fromIri);
	const to = props.units.find((u) => u.qudtIri === convertState.toIri);
	const r = convertLocal(Number(convertState.value), from, to);
	convertState.result = r.result;
	convertState.reason = r.reason as any;
};

// NFR-15 防抖：value/from/to 变化时 debounce 300ms 后算
const debouncedConvert = useDebounceFn(doConvert, 300);
watch(() => [convertState.value, convertState.fromIri, convertState.toIri], debouncedConvert);

/**
 * 切换量纲时重置试算区，并默认选前两个单位。
 */
watch(
	() => props.units,
	(list) => {
		convertState.fromIri = list[0]?.qudtIri || '';
		convertState.toIri = list[1]?.qudtIri || '';
		convertState.value = 1;
		nextTick(doConvert);
	},
	{ immediate: true }
);
</script>

<style scoped>
.convert-panel {
	margin-top: 16px;
	padding: 12px 16px;
	background: #fafafa;
	border-radius: 4px;
	border: 1px solid #f0f0f0;
}
.section-title {
	font-size: 13px;
	font-weight: 600;
	color: #606266;
	border-left: 3px solid var(--el-color-primary);
	padding-left: 8px;
}
.convert-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 8px;
}
.convert-label {
	font-size: 13px;
	color: #909399;
}
.convert-arrow {
	color: var(--el-color-primary);
	font-size: 16px;
}
.convert-equal {
	font-size: 16px;
	font-weight: 600;
	color: #303133;
}
.convert-result {
	font-size: 16px;
	font-weight: 600;
	color: var(--el-color-success);
	min-width: 60px;
}
.muted {
	color: #999;
	font-weight: normal;
}
.ml8 {
	margin-left: 8px;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
