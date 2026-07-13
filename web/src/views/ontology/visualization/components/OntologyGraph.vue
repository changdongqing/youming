<!--
  可复用本体图谱组件。
  基于 echarts graph series，封装有向图渲染、节点着色、边样式、
  缩放/拖拽/高亮、搜索定位、布局切换、PNG/SVG 导出能力。
-->
<script lang="ts" name="OntologyGraph" setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { GraphChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent, TitleComponent, ToolboxComponent } from 'echarts/components';
import { CanvasRenderer, SVGRenderer } from 'echarts/renderers';
import type { GraphData, GraphLayout } from '/@/types/ontology/visualization';
import { transformToEchartsOption } from '../graph-utils';

use([GraphChart, TooltipComponent, LegendComponent, TitleComponent, ToolboxComponent, CanvasRenderer, SVGRenderer]);

const props = defineProps<{
	graphData: GraphData;
	layout?: GraphLayout;
	renderer?: 'canvas' | 'svg';
}>();

const emit = defineEmits<{
	(e: 'nodeClick', node: any): void;
	(e: 'edgeClick', edge: any): void;
}>();

const chartRef = ref<InstanceType<typeof VChart>>();

const chartOption = ref<Record<string, any>>({});

/** 渲染图谱 */
function renderGraph() {
	if (!props.graphData) return;
	chartOption.value = transformToEchartsOption(props.graphData, props.layout || 'force');
}

/** 导出 PNG */
function exportPNG(filename: string = 'ontology-graph') {
	const instance = chartRef.value;
	if (!instance) return;
	const url = (instance as any).getDataURL({
		type: 'png',
		pixelRatio: 2,
		backgroundColor: '#fff',
	});
	const link = document.createElement('a');
	link.href = url;
	link.download = `${filename}.png`;
	link.click();
}

/** 导出 SVG */
function exportSVG(filename: string = 'ontology-graph') {
	const instance = chartRef.value;
	if (!instance) return;
	// SVG 渲染模式下直接获取 SVG 字符串
	const svgStr = (instance as any).renderToSVGString?.();
	if (!svgStr) return;
	const blob = new Blob([svgStr], { type: 'image/svg+xml' });
	const url = URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = `${filename}.svg`;
	link.click();
	URL.revokeObjectURL(url);
}

/** 搜索定位节点 */
function focusNode(nodeId: string) {
	const instance = chartRef.value as any;
	if (!instance) return;
	instance.dispatchAction({ type: 'highlight', seriesIndex: 0, name: nodeId });
	instance.dispatchAction({ type: 'showTip', seriesIndex: 0, name: nodeId });
}

/** 切换布局 */
function changeLayout(layout: GraphLayout) {
	const instance = chartRef.value as any;
	if (!instance || !props.graphData) return;
	chartOption.value = transformToEchartsOption(props.graphData, layout);
}

/** 处理节点/边点击 */
function handleClick(params: any) {
	if (params.dataType === 'node') {
		emit('nodeClick', params.data);
	} else if (params.dataType === 'edge') {
		emit('edgeClick', params.data);
	}
}

onMounted(() => {
	renderGraph();
});

onBeforeUnmount(() => {
	// echarts 实例由 v-chart autoresize 自动管理
});

watch(() => props.graphData, renderGraph, { deep: true });
watch(() => props.layout, () => renderGraph());

defineExpose({ exportPNG, exportSVG, focusNode, changeLayout });
</script>

<template>
	<div class="ontology-graph-container">
		<v-chart
			ref="chartRef"
			class="h-full w-full"
			:option="chartOption"
			:renderer="renderer || 'canvas'"
			autoresize
			@click="handleClick"
		/>
	</div>
</template>

<style scoped>
.ontology-graph-container {
	width: 100%;
	height: 100%;
	min-height: 400px;
}
</style>
