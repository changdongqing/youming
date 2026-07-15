<template>
	<div class="layout-padding mapping-designer-page">
		<div class="layout-padding-auto layout-padding-view">
			<div v-loading="loading">
				<!-- ==================== 顶部 Header ==================== -->
				<MappingDesignerHeader
					v-if="project && version"
					:project="project"
					:version="version"
					:is-dirty="store.isDirty"
					@back="goBack"
					@save="handleSave"
					@validate="handleValidate"
					@publish="handlePublish"
				/>

				<!-- ==================== 三栏布局 ==================== -->
				<Splitpanes class="designer-body" v-if="project && version">
					<!-- 左：源数据 -->
					<Pane :size="25" min-size="18">
						<SourceSchemaPanel :source-id="selectedSourceId" @select="handleSourceSelect" />
					</Pane>

					<!-- 中：映射工作区 -->
					<Pane :size="50" min-size="30">
						<div class="panel-container">
							<div class="panel-title">
								<el-icon><Grid /></el-icon> 映射工作区
								<div class="panel-actions">
									<el-button size="small" type="primary" @click="handleAddEntityMapping" :disabled="!canEdit">
										<el-icon><Plus /></el-icon> 实体映射
									</el-button>
									<el-button size="small" type="primary" @click="handleAddRelationMapping" :disabled="!canEdit">
										<el-icon><Plus /></el-icon> 关系映射
									</el-button>
								</div>
							</div>
							<el-scrollbar class="panel-content">
								<!-- 实体映射列表 -->
								<el-divider content-position="left">实体映射（{{ entityMappings.length }}）</el-divider>
								<el-table :data="entityMappings" border size="small" highlight-current-row @row-click="handleEntityMappingClick" style="width: 100%">
									<el-table-column prop="mappingCode" label="编码" width="120" show-overflow-tooltip />
									<el-table-column prop="mappingName" label="名称" min-width="120" show-overflow-tooltip />
									<el-table-column label="源对象" min-width="120" show-overflow-tooltip>
										<template #default="{ row }">{{ row.sourceSchema }}.{{ row.sourceObject }}</template>
									</el-table-column>
									<el-table-column prop="syncOrder" label="顺序" width="60" />
									<el-table-column label="启用" width="55">
										<template #default="{ row }">
											<el-tag :type="row.enabled === '1' ? 'success' : 'info'" size="small">{{ row.enabled === '1' ? '是' : '否' }}</el-tag>
										</template>
									</el-table-column>
								</el-table>

								<!-- 实体映射编辑器 -->
								<el-divider v-if="selectedEntityMappingId" content-position="left">实体映射编辑</el-divider>
								<EntityMappingEditor
									v-if="selectedEntityMappingId"
									:entity-mapping-id="selectedEntityMappingId"
									:available-properties="availableProperties"
									:source-columns="sourceColumns"
									:readonly="!canEdit"
									@change="markEntityDirty"
								/>

								<!-- 关系映射列表 -->
								<el-divider content-position="left">关系映射（{{ relationMappings.length }}）</el-divider>
								<el-table
									:data="relationMappings"
									border
									size="small"
									highlight-current-row
									@row-click="handleRelationMappingClick"
									style="width: 100%"
								>
									<el-table-column prop="mappingCode" label="编码" width="120" show-overflow-tooltip />
									<el-table-column prop="mappingName" label="名称" min-width="120" show-overflow-tooltip />
									<el-table-column label="模式" width="90">
										<template #default="{ row }">{{ relationModeLabel(row.relationMode) }}</template>
									</el-table-column>
									<el-table-column prop="syncOrder" label="顺序" width="60" />
									<el-table-column label="启用" width="55">
										<template #default="{ row }">
											<el-tag :type="row.enabled === '1' ? 'success' : 'info'" size="small">{{ row.enabled === '1' ? '是' : '否' }}</el-tag>
										</template>
									</el-table-column>
								</el-table>

								<!-- 关系映射编辑器 -->
								<el-divider v-if="selectedRelationMappingId" content-position="left">关系映射编辑</el-divider>
								<RelationMappingEditor
									v-if="selectedRelationMappingId"
									:relation-mapping-id="selectedRelationMappingId"
									:readonly="!canEdit"
									@change="markRelationDirty"
								/>
							</el-scrollbar>
						</div>
					</Pane>

					<!-- 右：本体模型 -->
					<Pane :size="25" min-size="18">
						<OntologySchemaPanel :ontology-id="project.ontologyId" @select="handleOntologySelect" />
					</Pane>
				</Splitpanes>

				<!-- ==================== 底部抽屉触发 ==================== -->
				<div class="designer-footer" v-if="project && version">
					<el-button @click="previewDrawerRef?.open(entityMappings)">
						<el-icon><View /></el-icon> 预览
					</el-button>
					<el-button @click="validationDrawerRef?.open()">
						<el-icon><Checked /></el-icon> 校验报告
					</el-button>
					<el-button @click="versionHistoryRef?.open()">
						<el-icon><Clock /></el-icon> 版本历史
					</el-button>
					<el-button @click="diffDrawerRef?.open()">
						<el-icon><Connection /></el-icon> 版本差异
					</el-button>
				</div>
			</div>
		</div>

		<!-- ==================== 抽屉组件 ==================== -->
		<PreviewDrawer ref="previewDrawerRef" :version-id="version?.id || 0" :entity-mappings="entityMappings" />
		<ValidationReportDrawer
			ref="validationDrawerRef"
			:version-id="version?.id || 0"
			:current-revision="version?.revision"
			@validated="handleValidated"
		/>
		<VersionHistoryDrawer
			ref="versionHistoryRef"
			:project-id="project?.id || 0"
			@edit-draft="handleEditDraft"
			@publish="handlePublishVersion"
			@retire="handleRetireVersion"
		/>
		<MappingDiffDrawer ref="diffDrawerRef" :project-id="project?.id || 0" />

		<!-- ==================== 关系映射向导 ==================== -->
		<RelationMappingWizard ref="relationWizardRef" :version-id="version?.id || 0" @success="loadRelationMappings" />
	</div>
</template>

<script lang="ts" name="ontologyDataMappingDesigner" setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { mappingProjectApi, mappingConfigApi, mappingValidationApi } from '/@/api/ontology/data-mapping';
import type { MappingProjectVO, MappingVersionVO, EntityMappingVO, RelationMappingVO } from '/@/types/ontology/data-mapping';
import { versionStatusLabel, versionStatusTagType, relationModeLabel } from './utils/mapping-status';
import { isEditableStatus, canAddEntityMapping, canAddRelationMapping } from './utils/designer-state';
import { useMappingDesignerStore } from '/@/stores/ontology/useMappingDesignerStore';
import { Grid, Plus, View, Checked, Clock, Connection } from '@element-plus/icons-vue';

import MappingDesignerHeader from './components/MappingDesignerHeader.vue';
import SourceSchemaPanel from './components/SourceSchemaPanel.vue';
import OntologySchemaPanel from './components/OntologySchemaPanel.vue';
import EntityMappingEditor from './components/EntityMappingEditor.vue';
import RelationMappingEditor from './components/RelationMappingEditor.vue';
import RelationMappingWizard from './components/RelationMappingWizard.vue';
import PreviewDrawer from './components/PreviewDrawer.vue';
import ValidationReportDrawer from './components/ValidationReportDrawer.vue';
import VersionHistoryDrawer from './components/VersionHistoryDrawer.vue';
import MappingDiffDrawer from './components/MappingDiffDrawer.vue';

const route = useRoute();
const router = useRouter();
const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const store = useMappingDesignerStore();

const loading = ref(false);
const project = ref<MappingProjectVO | null>(null);
const version = ref<MappingVersionVO | null>(null);

// 实体/关系映射列表
const entityMappings = ref<EntityMappingVO[]>([]);
const relationMappings = ref<RelationMappingVO[]>([]);

// 选中的映射
const selectedEntityMappingId = ref<number | undefined>();
const selectedRelationMappingId = ref<number | undefined>();

// 源数据选择
const selectedSourceId = ref<number | undefined>();
const sourceColumns = ref<string[]>([]);

// 本体属性
const availableProperties = ref<any[]>([]);

// 抽屉 refs
const previewDrawerRef = ref();
const validationDrawerRef = ref();
const versionHistoryRef = ref();
const diffDrawerRef = ref();
const relationWizardRef = ref();

const canEdit = computed(() => (version.value ? isEditableStatus(version.value.versionStatus) : false));

const loadProjectAndVersion = async () => {
	const projectId = Number(route.query.projectId);
	const versionId = Number(route.query.versionId);
	if (!projectId || !versionId) {
		msgError('缺少 projectId 或 versionId 参数');
		return;
	}

	loading.value = true;
	try {
		const [{ data: projectData }, { data: versionData }] = await Promise.all([
			mappingProjectApi.getProjectDetail(projectId),
			mappingProjectApi.getVersionDetail(versionId),
		]);
		project.value = projectData;
		version.value = versionData;
		store.setProject(projectData);
		store.setVersion(versionData);
		// 加载映射列表
		await Promise.all([loadEntityMappings(), loadRelationMappings()]);
	} catch (e: any) {
		msgError(e.message || '加载工程/版本详情失败');
	} finally {
		loading.value = false;
	}
};

const loadEntityMappings = async () => {
	if (!version.value) return;
	try {
		const { data } = await mappingConfigApi.listEntityMappings(version.value.id, { current: 1, size: 100 });
		entityMappings.value = data?.records || [];
		store.setEntityMappings(entityMappings.value);
	} catch (e: any) {
		msgError(e.message || '获取实体映射列表失败');
	}
};

const loadRelationMappings = async () => {
	if (!version.value) return;
	try {
		const { data } = await mappingConfigApi.listRelationMappings(version.value.id, { current: 1, size: 100 });
		relationMappings.value = data?.records || [];
		store.setRelationMappings(relationMappings.value);
	} catch (e: any) {
		msgError(e.message || '获取关系映射列表失败');
	}
};

const goBack = () => {
	router.push('/ontology/data-mapping/index');
};

const handleSourceSelect = (payload: { schema: string; object: string; columns: any[] }) => {
	sourceColumns.value = payload.columns.map((c) => c.name);
};

const handleOntologySelect = (_entityTypeId: string) => {
	// 可在此加载选中实体类型的适用属性
};

const handleEntityMappingClick = (row: EntityMappingVO) => {
	selectedEntityMappingId.value = row.id;
};

const handleRelationMappingClick = (row: RelationMappingVO) => {
	selectedRelationMappingId.value = row.id;
};

const handleAddEntityMapping = () => {
	// V1: 简化版，提示用户在实体映射列表中查看
	msgWarning('请通过API或后端创建实体映射，前端表单创建将在后续完善');
};

const handleAddRelationMapping = () => {
	relationWizardRef.value?.open();
};

const markEntityDirty = () => {
	store.markEntityMappingsDirty(true);
};

const markRelationDirty = () => {
	store.markRelationMappingsDirty(true);
};

const handleSave = () => {
	// 子表独立保存由各编辑器组件自行处理，此处为全局提示
	if (store.isDirty) {
		msgWarning('请使用各编辑器中的保存按钮逐个保存子表');
	} else {
		msgSuccess('无未保存的修改');
	}
};

const handleValidate = () => {
	validationDrawerRef.value?.open();
};

const handleValidated = () => {
	// 校验完成后刷新状态
	if (version.value) {
		mappingProjectApi.getVersionDetail(version.value.id).then(({ data }) => {
			version.value = data;
			store.setVersion(data);
		});
	}
};

const handlePublish = async () => {
	if (!version.value) return;
	if (store.isDirty) {
		msgWarning('有未保存的修改，请先保存');
		return;
	}
	try {
		const { data: prepare } = await mappingProjectApi.preparePublish(version.value.id);
		if (!prepare.publishable) {
			msgError(`不可发布：${prepare.violationCount} 违规未解决`);
			return;
		}

		// 展示风险摘要
		const highRiskText =
			prepare.highRiskChanges.length > 0
				? `\n高风险变化：\n${prepare.highRiskChanges.map((r) => `  - ${r.riskType}: ${r.description}`).join('\n')}`
				: '';
		const gateText =
			prepare.gateChecks.length > 0
				? `\n门禁检查：\n${prepare.gateChecks.map((g) => `  - ${g.checkName}: ${g.passed ? '通过' : g.message}`).join('\n')}`
				: '';

		// 高风险项要求输入映射工程编码确认
		const hasHighRisk = prepare.highRiskChanges.length > 0;
		const confirmText = hasHighRisk
			? `存在高风险变化，请输入映射工程编码「${project.value?.mappingCode}」确认发布：${highRiskText}${gateText}`
			: `确认发布版本 ${version.value.versionNumber}？${highRiskText}${gateText}`;

		if (hasHighRisk) {
			const result = await ElMessageBox.prompt(confirmText, '发布确认', {
				confirmButtonText: '确认发布',
				cancelButtonText: '取消',
				inputPlaceholder: '输入映射工程编码',
				type: 'warning',
			});
			if (result.value !== project.value?.mappingCode) {
				msgError('映射工程编码不匹配，已取消发布');
				return;
			}
		} else {
			await ElMessageBox.confirm(confirmText, '发布确认', { type: 'warning' });
		}

		await mappingProjectApi.publishVersion(version.value.id);
		msgSuccess('版本发布成功');
		// 刷新状态
		await loadProjectAndVersion();
	} catch (e: any) {
		if (e !== 'cancel' && e !== 'close') msgError(e.message || '发布失败');
	}
};

const handleEditDraft = (versionRow: MappingVersionVO) => {
	router.push({
		path: '/ontology/data-mapping/designer',
		query: { projectId: String(project.value?.id), versionId: String(versionRow.id) },
	});
};

const handlePublishVersion = async (versionRow: MappingVersionVO) => {
	try {
		await ElMessageBox.confirm(`确认发布版本 ${versionRow.versionNumber}？`, '发布确认', { type: 'warning' });
		await mappingProjectApi.publishVersion(versionRow.id);
		msgSuccess('版本发布成功');
		versionHistoryRef.value?.open();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '发布失败');
	}
};

const handleRetireVersion = async (versionRow: MappingVersionVO) => {
	try {
		await ElMessageBox.confirm(`确认停用版本 ${versionRow.versionNumber}？`, '停用确认', { type: 'warning' });
		await mappingProjectApi.retireVersion(versionRow.id);
		msgSuccess('版本已停用');
		versionHistoryRef.value?.open();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '停用失败');
	}
};

onMounted(() => {
	loadProjectAndVersion();
});

onBeforeUnmount(() => {
	store.reset();
});
</script>

<style scoped>
.mapping-designer-page {
	height: 100%;
}
.designer-body {
	height: calc(100% - 80px - 50px);
}
.panel-container {
	height: 100%;
	display: flex;
	flex-direction: column;
}
.panel-title {
	padding: 8px 12px;
	font-weight: 600;
	font-size: 13px;
	border-bottom: 1px solid var(--el-border-color-light);
	background-color: var(--el-fill-color-light);
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 6px;
}
.panel-actions {
	display: flex;
	gap: 4px;
}
.panel-content {
	flex: 1;
	overflow: auto;
	padding: 8px 12px;
}
.designer-footer {
	height: 50px;
	border-top: 1px solid var(--el-border-color-light);
	display: flex;
	align-items: center;
	padding: 0 12px;
	gap: 8px;
	background-color: var(--el-bg-color);
}
</style>
