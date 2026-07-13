<!--
  实例图谱页。
  支持按实体类型过滤、子图展开（点击实例节点展开 N 跳邻居）、节点详情面板。
-->
<script lang="ts" name="InstanceGraph" setup>
import { ref, onMounted } from 'vue';
import OntologyGraph from './OntologyGraph.vue';
import GraphToolbar from './GraphToolbar.vue';
import type { GraphData, GraphNode, GraphEdge, GraphLayout, OntologyId } from '/@/types/ontology/visualization';
import { fetchInstanceSubgraph } from '/@/api/ontology/visualization';
import { fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchInstanceRelations } from '/@/api/ontology/instance';

const props = defineProps<{
	graphData: GraphData;
	ontologyId: OntologyId;
	loading: boolean;
}>();

const emit = defineEmits<{ (e: 'refresh'): void }>();

const graphRef = ref<InstanceType<typeof OntologyGraph>>();
const layout = ref<GraphLayout>('force');
const searchText = ref('');
const filterTypeId = ref<OntologyId>('');
const selectedNode = ref<GraphNode>();
const selectedEdge = ref<GraphEdge>();
const sidePanelVisible = ref(false);
const instanceDetail = ref<any>();
const entityTypeTree = ref<any[]>([]);

/** 加载实体类型树（用于筛选下拉） */
onMounted(async () => {
	try {
		const res = await fetchEntityTypeTree(props.ontologyId);
		entityTypeTree.value = res.data || [];
	} catch {
		entityTypeTree.value = [];
	}
});

/** 节点点击：展示实例详情或展开子图 */
async function handleNodeClick(node: any) {
	selectedNode.value = node as GraphNode;
	selectedEdge.value = undefined;
	sidePanelVisible.value = true;
	const nodeData = node as GraphNode;
	if (nodeData.instanceId) {
		try {
			const res = await fetchInstanceRelations(nodeData.instanceId, 'BOTH');
			instanceDetail.value = res.data;
		} catch {
			instanceDetail.value = undefined;
		}
	}
}

/** 边点击 */
function handleEdgeClick(edge: any) {
	selectedEdge.value = edge as GraphEdge;
	selectedNode.value = undefined;
	sidePanelVisible.value = true;
}

/** 展开子图（1跳邻居） */
async function handleExpand() {
	if (!selectedNode.value?.instanceId) return;
	try {
		await fetchInstanceSubgraph({
			instanceId: selectedNode.value.instanceId,
			depth: 1,
			direction: 'BOTH',
		});
		emit('refresh');
	} catch {
		// 子图展开失败时静默处理
	}
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

function handleExportPNG() {
	graphRef.value?.exportPNG('instance-graph');
}

function handleExportSVG() {
	graphRef.value?.exportSVG('instance-graph');
}

function handleLayoutChange(newLayout: GraphLayout) {
	layout.value = newLayout;
	graphRef.value?.changeLayout(newLayout);
}
</script>

<template>
	<div class="instance-graph-page">
		<GraphToolbar
			v-model:search-text="searchText"
			v-model:layout="layout"
			:summary="graphData.summary"
			@search="handleSearch"
			@export-png="handleExportPNG"
			@export-svg="handleExportSVG"
			@layout-change="handleLayoutChange"
			@refresh="emit('refresh')"
		>
			<template #filter>
				<el-tree-select
					v-model="filterTypeId"
					:data="entityTypeTree"
					:props="{ label: 'label', value: 'id', children: 'children' }"
					placeholder="按类型筛选"
					clearable
					check-strictly
					style="width: 180px"
					@change="emit('refresh')"
				/>
			</template>
		</GraphToolbar>
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
				<div v-show="sidePanelVisible" class="graph-side-panel">
					<el-card v-if="selectedNode" shadow="never">
						<template #header>
							<div class="panel-header">
								<span>{{ selectedNode.label }}</span>
								<el-tag v-if="selectedNode.rdfTypeLabel" size="small">{{ selectedNode.rdfTypeLabel }}</el-tag>
							</div>
						</template>
						<el-button type="primary" size="small" @click="handleExpand" :disabled="!selectedNode.instanceId">
							展开邻居
						</el-button>
						<el-divider />
						<el-descriptions v-if="instanceDetail" :column="1" size="small" border>
							<el-descriptions-item label="IRI">{{ selectedNode.id }}</el-descriptions-item>
							<el-descriptions-item label="出边数">
								{{ instanceDetail.outgoingRelations?.length || 0 }}
							</el-descriptions-item>
							<el-descriptions-item label="入边数">
								{{ instanceDetail.inboundRelations?.length || 0 }}
							</el-descriptions-item>
						</el-descriptions>
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
.instance-graph-page {
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
