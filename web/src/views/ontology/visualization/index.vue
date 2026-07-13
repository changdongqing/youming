<!--
  可视化主页面。
  Tab 切换：统计概览 / Schema 图谱 / 实例图谱。
  左图右面板布局，选中节点展示数据属性/公理标注。
-->
<script lang="ts" name="ontologyVisualization" setup>
import { ref, onMounted } from 'vue';
import { useMessage } from '/@/hooks/message';
import {
	fetchSchemaGraph,
	fetchInstanceGraph,
	fetchVisualizationStats,
} from '/@/api/ontology/visualization';
import type { GraphData, VisualizationStats, GraphLayout } from '/@/types/ontology/visualization';
import StatsOverview from './components/StatsOverview.vue';
import SchemaGraph from './components/SchemaGraph.vue';
import InstanceGraph from './components/InstanceGraph.vue';

const activeTab = ref<'stats' | 'schema' | 'instance'>('stats');
const ontologyId = ref<string>('935001');
const stats = ref<VisualizationStats>();
const schemaGraphData = ref<GraphData>();
const instanceGraphData = ref<GraphData>();
const loading = ref(false);
const { error: msgError } = useMessage();

async function loadStats() {
	try {
		const res: any = await fetchVisualizationStats(ontologyId.value);
		stats.value = res.data;
	} catch {
		msgError('加载统计失败');
	}
}

async function loadSchemaGraph() {
	loading.value = true;
	try {
		const res: any = await fetchSchemaGraph(ontologyId.value);
		schemaGraphData.value = res.data;
	} catch {
		msgError('加载Schema图谱失败');
	} finally {
		loading.value = false;
	}
}

async function loadInstanceGraph() {
	loading.value = true;
	try {
		const res: any = await fetchInstanceGraph(ontologyId.value);
		instanceGraphData.value = res.data;
	} catch {
		msgError('加载实例图谱失败');
	} finally {
		loading.value = false;
	}
}

function handleTabChange(tab: string) {
	if (tab === 'stats' && !stats.value) loadStats();
	if (tab === 'schema' && !schemaGraphData.value) loadSchemaGraph();
	if (tab === 'instance' && !instanceGraphData.value) loadInstanceGraph();
}

onMounted(loadStats);
</script>

<template>
	<div class="layout-padding ontology-visualization-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-tabs v-model="activeTab" @tab-change="handleTabChange">
				<el-tab-pane label="统计概览" name="stats">
					<StatsOverview v-if="stats" :stats="stats" />
					<el-empty v-else description="加载中..." />
				</el-tab-pane>
				<el-tab-pane label="Schema 图谱" name="schema">
					<SchemaGraph
						v-if="schemaGraphData"
						:graph-data="schemaGraphData"
						:loading="loading"
						@refresh="loadSchemaGraph"
					/>
					<el-empty v-else description="点击此页签加载Schema图谱" :image-size="60">
						<el-button type="primary" @click="loadSchemaGraph">加载 Schema 图谱</el-button>
					</el-empty>
				</el-tab-pane>
				<el-tab-pane label="实例图谱" name="instance">
					<InstanceGraph
						v-if="instanceGraphData"
						:graph-data="instanceGraphData"
						:ontology-id="ontologyId"
						:loading="loading"
						@refresh="loadInstanceGraph"
					/>
					<el-empty v-else description="点击此页签加载实例图谱" :image-size="60">
						<el-button type="primary" @click="loadInstanceGraph">加载实例图谱</el-button>
					</el-empty>
				</el-tab-pane>
			</el-tabs>
		</div>
	</div>
</template>

<style scoped>
.ontology-visualization-page {
	height: 100%;
}
</style>
