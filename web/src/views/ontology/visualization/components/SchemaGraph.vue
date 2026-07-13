<!--
  Schema 图谱页。
  左侧：echarts 有向图（继承树+对象属性关系+公理约束标注）
  右侧：数据属性面板 / 公理标注面板（选中节点时展示）
  顶部：工具栏（搜索/布局切换/导出）
-->
<script lang="ts" name="SchemaGraph" setup>
import { ref } from 'vue';
import OntologyGraph from './OntologyGraph.vue';
import GraphToolbar from './GraphToolbar.vue';
import DataPropertyPanel from './DataPropertyPanel.vue';
import AxiomAnnotationPanel from './AxiomAnnotationPanel.vue';
import type { GraphData, GraphNode, GraphEdge, GraphLayout } from '/@/types/ontology/visualization';
import { fetchDataPropertiesByDomain } from '/@/api/ontology/data-property';
import { fetchEntityTypeById } from '/@/api/ontology/entity-type';

const props = defineProps<{
	graphData: GraphData;
	loading: boolean;
}>();

const emit = defineEmits<{ (e: 'refresh'): void }>();

const graphRef = ref<InstanceType<typeof OntologyGraph>>();
const layout = ref<GraphLayout>('force');
const searchText = ref('');
const selectedNode = ref<GraphNode>();
const selectedEdge = ref<GraphEdge>();
const rightPanelVisible = ref(false);
const dataProperties = ref<any[]>([]);
const entityTypeDetail = ref<any>();

/** 节点点击：加载右侧面板 */
async function handleNodeClick(node: any) {
	selectedNode.value = node as GraphNode;
	selectedEdge.value = undefined;
	rightPanelVisible.value = true;
	const nodeData = node as GraphNode;
	if (nodeData.entityTypeId) {
		try {
			const res = await fetchDataPropertiesByDomain(nodeData.entityTypeId);
			dataProperties.value = res.data || [];
			const detail = await fetchEntityTypeById(nodeData.entityTypeId);
			entityTypeDetail.value = detail.data;
		} catch {
			dataProperties.value = [];
			entityTypeDetail.value = undefined;
		}
	}
}

/** 边点击：展示边属性面板 */
function handleEdgeClick(edge: any) {
	selectedEdge.value = edge as GraphEdge;
	selectedNode.value = undefined;
	rightPanelVisible.value = true;
}

/** 搜索定位 */
function handleSearch() {
	if (searchText.value && graphRef.value) {
		const matched = props.graphData.nodes.find(
			(n) => n.label.includes(searchText.value) || n.id.includes(searchText.value),
		);
		if (matched) {
			graphRef.value.focusNode(matched.id);
		}
	}
}

/** 导出 PNG */
function handleExportPNG() {
	graphRef.value?.exportPNG('schema-graph');
}

/** 导出 SVG */
function handleExportSVG() {
	graphRef.value?.exportSVG('schema-graph');
}

/** 布局切换 */
function handleLayoutChange(newLayout: GraphLayout) {
	layout.value = newLayout;
	graphRef.value?.changeLayout(newLayout);
}
</script>

<template>
	<div class="schema-graph-page">
		<GraphToolbar
			v-model:search-text="searchText"
			v-model:layout="layout"
			:summary="graphData.summary"
			@search="handleSearch"
			@export-png="handleExportPNG"
			@export-svg="handleExportSVG"
			@layout-change="handleLayoutChange"
			@refresh="emit('refresh')"
		/>
		<div class="graph-body">
			<div class="graph-canvas">
				<OntologyGraph
					ref="graphRef"
					:graph-data="graphData"
					:layout="layout"
					@node-click="handleNodeClick"
					@edge-click="handleEdgeClick"
				/>
			</div>
			<transition name="el-zoom-in-right">
				<div v-show="rightPanelVisible" class="graph-side-panel">
					<el-card v-if="selectedNode" shadow="never">
						<template #header>
							<div class="panel-header">
								<span>{{ selectedNode.label }}</span>
								<el-tag v-if="selectedNode.isBuiltin" type="primary" size="small">核心内置</el-tag>
								<el-tag v-else type="warning" size="small">扩展</el-tag>
							</div>
						</template>
						<DataPropertyPanel :properties="dataProperties" :entity-type="selectedNode" />
						<el-divider />
						<AxiomAnnotationPanel :entity-type-detail="entityTypeDetail" :node="selectedNode" />
					</el-card>
					<el-card v-else-if="selectedEdge" shadow="never">
						<template #header>{{ selectedEdge.label }} - 边属性</template>
						<el-descriptions :column="1" border>
							<el-descriptions-item label="边类型">{{ selectedEdge.edgeType }}</el-descriptions-item>
							<el-descriptions-item label="起点">{{ selectedEdge.source }}</el-descriptions-item>
							<el-descriptions-item label="终点">{{ selectedEdge.target }}</el-descriptions-item>
							<el-descriptions-item label="功能属性">
								<el-tag v-if="selectedEdge.isFunctional" type="danger" size="small">是</el-tag>
								<span v-else>否</span>
							</el-descriptions-item>
						</el-descriptions>
					</el-card>
				</div>
			</transition>
		</div>
	</div>
</template>

<style scoped>
.schema-graph-page {
	flex: 1;
	min-height: 0;
	display: flex;
	flex-direction: column;
}
.graph-body {
	flex: 1;
	display: flex;
	gap: 8px;
	overflow: hidden;
}
.graph-canvas {
	flex: 1;
	min-width: 0;
}
.graph-side-panel {
	width: 360px;
	flex-shrink: 0;
	overflow-y: auto;
}
.panel-header {
	display: flex;
	align-items: center;
	gap: 8px;
}
</style>
