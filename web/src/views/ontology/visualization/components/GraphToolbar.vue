<!--
  图谱工具栏。
  搜索框 | 布局切换 | 导出PNG | 导出SVG | 刷新
-->
<script lang="ts" name="GraphToolbar" setup>
import { Search, Refresh, Download } from '@element-plus/icons-vue';
import type { GraphLayout, GraphSummary } from '/@/types/ontology/visualization';

defineProps<{ summary?: GraphSummary }>();
const emit = defineEmits<{
	(e: 'search'): void;
	(e: 'export-png'): void;
	(e: 'export-svg'): void;
	(e: 'layout-change', layout: GraphLayout): void;
	(e: 'refresh'): void;
}>();

const searchText = defineModel<string>('searchText');
const layout = defineModel<GraphLayout>('layout');

const layoutOptions: { label: string; value: GraphLayout }[] = [
	{ label: '力导向', value: 'force' },
	{ label: '环形', value: 'circular' },
	{ label: '固定', value: 'none' },
];
</script>

<template>
	<div class="graph-toolbar">
		<el-input
			v-model="searchText"
			placeholder="搜索节点名称或IRI"
			:prefix-icon="Search"
			clearable
			style="width: 240px"
			@keyup.enter="emit('search')"
		/>
		<el-select v-model="layout" placeholder="布局" style="width: 100px" @change="emit('layout-change', layout!)">
			<el-option v-for="opt in layoutOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
		</el-select>
		<slot name="filter" />
		<el-button-group>
			<el-button :icon="Download" @click="emit('export-png')">PNG</el-button>
			<el-button :icon="Download" @click="emit('export-svg')">SVG</el-button>
		</el-button-group>
		<el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>
		<div v-if="summary" class="summary-info">
			<el-tag size="small">节点 {{ summary.nodeCount }}</el-tag>
			<el-tag size="small" type="info">边 {{ summary.edgeCount }}</el-tag>
		</div>
	</div>
</template>

<style scoped>
.graph-toolbar {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 0;
	flex-wrap: wrap;
}
.summary-info {
	margin-left: auto;
	display: flex;
	gap: 4px;
}
</style>
