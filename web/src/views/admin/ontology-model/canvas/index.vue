<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view canvas-page">
			<!-- 顶部工具栏 -->
			<el-row class="ml10 mb8">
				<el-form :inline="true">
					<el-form-item :label="t('modelCanvas.selectProject')">
						<el-select v-model="state.projectId" :placeholder="t('modelCanvas.selectProject')" style="width: 200px" @change="onProjectChange">
							<el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button @click="handleAutoLayout" icon="Grid" :disabled="!state.projectId">{{ t('modelCanvas.autoLayout') }}</el-button>
						<el-button v-auth="'ont_canvas_manage'" @click="handleSave" icon="Download" type="success" :disabled="!state.projectId">{{ t('modelCanvas.saveCanvas') }}</el-button>
						<el-button @click="togglePreview" icon="View">{{ t('modelCanvas.serializePreview') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>

			<!-- 三栏布局 -->
			<splitpanes class="canvas-splitpanes">
				<pane size="20">
					<template-sidebar />
				</pane>
				<pane size="60">
					<div ref="canvasRef" class="x6-canvas" @drop="handleDrop" @dragover.prevent />
				</pane>
				<pane size="20">
					<property-drawer ref="propertyDrawerRef" />
				</pane>
			</splitpanes>

			<!-- 底部序列化预览 -->
			<serialize-preview-panel ref="previewPanelRef" :project-id="state.projectId" />
		</div>
	</div>
</template>

<script lang="ts" name="ontologyModelCanvas" setup>
import { Splitpanes, Pane } from 'splitpanes';
import { useCanvas } from './composables/useCanvas';
import { useCanvasDnd } from './composables/useCanvasDnd';
import { getState, getGraph, saveState } from '/@/api/ontology-model/canvas';
import { pageList as pageProject, getObj as getProject } from '/@/api/ontology-model/project';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import emitter from '/@/utils/mitt';

const TemplateSidebar = defineAsyncComponent(() => import('./components/TemplateSidebar.vue'));
const PropertyDrawer = defineAsyncComponent(() => import('./components/PropertyDrawer.vue'));
const SerializePreviewPanel = defineAsyncComponent(() => import('./components/SerializePreviewPanel.vue'));
const { t } = useI18n();

const canvasRef = ref<HTMLDivElement>();
const propertyDrawerRef = ref();
const previewPanelRef = ref();
const projectList = ref<any[]>([]);
const namespaceBase = ref('');

const state = reactive({
	projectId: '',
	loading: false,
});

const { initGraph, renderGraph, destroyGraph, getGraph, autoLayout } = useCanvas();
const { handleDrop, handleEdgeConnected } = useCanvasDnd(toRef(state, 'projectId'), namespaceBase);

// 初始化画布
onMounted(async () => {
	if (canvasRef.value) {
		initGraph(canvasRef.value);
	}
	await loadProjects();

	// 注册 mitt 事件
	emitter.on('openPropertyDrawer', (data: any) => {
		propertyDrawerRef.value?.open(data.classId);
	});
	emitter.on('edgeConnected', handleEdgeConnected);
	emitter.on('canvasNodeAdded', () => {
		previewPanelRef.value?.refresh();
	});
	emitter.on('canvasEdgeAdded', () => {
		previewPanelRef.value?.refresh();
	});
});

onUnmounted(() => {
	destroyGraph();
	emitter.all.clear();
});

const loadProjects = async () => {
	try {
		const { data } = await pageProject({ size: 200 });
		projectList.value = data?.records ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const onProjectChange = async () => {
	if (!state.projectId) return;
	state.loading = true;
	try {
		// 拉项目命名空间基址
		const projRes = await getProject(state.projectId);
		namespaceBase.value = projRes.data.namespaceBase;

		// 1. 拉图数据（类节点 + 边）
		const graphRes = await getGraph(state.projectId);
		renderGraph(graphRes.data.nodes, graphRes.data.edges);

		// 2. 拉画布状态（节点位置/缩放，覆盖布局）
		const stateRes = await getState(state.projectId);
		if (stateRes.data?.graphData) {
			const graph = getGraph();
			graph?.fromJSON(JSON.parse(stateRes.data.graphData));
		}

		// 3. 触发序列化预览
		previewPanelRef.value?.refresh();
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		state.loading = false;
	}
};

const handleAutoLayout = () => {
	autoLayout();
};

const handleSave = async () => {
	if (!state.projectId) return;
	const graph = getGraph();
	if (!graph) return;
	const graphData = JSON.stringify(graph.toJSON());
	try {
		await saveState(state.projectId, graphData);
		useMessage().success(t('modelCanvas.saveSuccess'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const togglePreview = () => {
	previewPanelRef.value?.refresh();
};
</script>

<style scoped>
.canvas-page {
	display: flex;
	flex-direction: column;
}
.canvas-splitpanes {
	flex: 1;
	min-height: 400px;
}
.x6-canvas {
	width: 100%;
	height: 100%;
	min-height: 400px;
}
</style>
