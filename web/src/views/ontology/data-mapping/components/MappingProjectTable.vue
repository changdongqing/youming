<template>
	<div>
		<!-- ==================== 搜索表单 ==================== -->
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="编码">
				<el-input v-model="query.mappingCode" placeholder="工程编码" clearable style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
			</el-form-item>
			<el-form-item label="名称">
				<el-input v-model="query.mappingName" placeholder="工程名称" clearable style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
			</el-form-item>
			<el-form-item label="状态">
				<el-select v-model="query.projectStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
					<el-option label="活跃" value="ACTIVE" />
					<el-option label="停用" value="INACTIVE" />
					<el-option label="已归档" value="ARCHIVED" />
				</el-select>
			</el-form-item>
			<el-form-item>
				<el-button type="primary" @click="loadData">
					<el-icon><Search /></el-icon> 搜索
				</el-button>
				<el-button @click="handleReset">重置</el-button>
				<el-button type="success" @click="handleAdd" v-auth="'ontology_mapping_project_admin'">
					<el-icon><Plus /></el-icon> 新建工程
				</el-button>
			</el-form-item>
		</el-form>

		<!-- ==================== 列表 ==================== -->
		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column prop="mappingCode" label="编码" width="140" show-overflow-tooltip />
			<el-table-column prop="mappingName" label="名称" min-width="140" show-overflow-tooltip />
			<el-table-column prop="ontologyId" label="本体工程ID" width="120" />
			<el-table-column label="状态" width="90">
				<template #default="{ row }">
					<el-tag :type="projectStatusTagType(row.projectStatus)" size="small">
						{{ projectStatusLabel(row.projectStatus) }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="活跃版本" width="100">
				<template #default="{ row }">
					<el-tag v-if="row.activeVersion" type="success" size="small">{{ row.activeVersion.versionNumber }}</el-tag>
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column label="草稿版本" width="100">
				<template #default="{ row }">
					<el-tag v-if="row.draftVersion" type="warning" size="small">{{ row.draftVersion.versionNumber }}</el-tag>
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column prop="lastJobId" label="最近作业" width="100" />
			<el-table-column prop="updateTime" label="更新时间" width="170" />
			<el-table-column label="操作" width="340" fixed="right">
				<template #default="{ row }">
					<el-button link type="primary" @click="handleEditDraft(row)" v-if="row.draftVersion" v-auth="'ontology_mapping_edit'">
						编辑DRAFT
					</el-button>
					<el-button link type="primary" @click="handleCreateVersion(row)" v-auth="'ontology_mapping_publish'">下一版本</el-button>
					<el-button link type="primary" @click="handleVersionHistory(row)">版本历史</el-button>
					<el-button link type="warning" @click="handleToggleStatus(row)" v-auth="'ontology_mapping_project_admin'">
						{{ row.projectStatus === 'ACTIVE' ? '停用' : '启用' }}
					</el-button>
					<el-button link type="success" @click="handleExecute(row, 'FULL')" v-if="row.activeVersionId" v-auth="'ontology_mapping_execute'"
						>FULL</el-button
					>
					<el-button link type="primary" @click="handleExecute(row, 'INCREMENTAL')" v-if="row.activeVersionId" v-auth="'ontology_mapping_execute'"
						>增量</el-button
					>
					<el-button link type="primary" @click="handleSchedule(row)" v-auth="'ontology_mapping_admin'">调度</el-button>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next, sizes"
			:page-sizes="[10, 20, 50]"
			@current-change="loadData"
			@size-change="loadData"
			class="mt8"
		/>

		<!-- ==================== 工程表单 ==================== -->
		<MappingProjectFormDialog ref="formDialogRef" @success="loadData" />

		<!-- ==================== 版本历史抽屉 ==================== -->
		<el-drawer v-model="versionHistoryVisible" :title="`版本历史 - ${selectedProject?.mappingName || ''}`" size="50%">
			<el-table v-loading="versionLoading" :data="versionList" border size="small">
				<el-table-column prop="versionNumber" label="版本号" width="100" />
				<el-table-column label="状态" width="90">
					<template #default="{ row }">
						<el-tag :type="versionStatusTagType(row.versionStatus)" size="small">
							{{ versionStatusLabel(row.versionStatus) }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="publishedAt" label="发布时间" width="170" />
				<el-table-column prop="createTime" label="创建时间" width="170" />
				<el-table-column label="操作" width="200">
					<template #default="{ row }">
						<el-button link type="primary" @click="handleEditDraft({ draftVersion: row, ...selectedProject })" v-if="row.versionStatus === 'DRAFT'">
							编辑
						</el-button>
						<el-button link type="primary" @click="handleViewSnapshot(row)">快照</el-button>
						<el-button
							link
							type="success"
							@click="handlePublishVersion(row)"
							v-if="row.versionStatus === 'VALIDATED' || row.versionStatus === 'DRAFT'"
						>
							发布
						</el-button>
						<el-button link type="warning" @click="handleRetire(row)" v-if="row.versionStatus === 'PUBLISHED'">停用</el-button>
					</template>
				</el-table-column>
			</el-table>
		</el-drawer>

		<!-- ==================== 作业启动对话框 ==================== -->
		<el-dialog v-model="jobDialogVisible" :title="`执行作业 - ${selectedProject?.mappingName || ''}`" width="520px" :close-on-click-modal="false">
			<el-form :model="jobForm" label-width="120px">
				<el-form-item label="运行类型">
					<el-radio-group v-model="jobForm.runType">
						<el-radio value="FULL">全量同步</el-radio>
						<el-radio value="INCREMENTAL">增量同步</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="映射版本" v-if="selectedVersion">
					<el-tag type="info">{{ selectedVersion.versionNumber }} ({{ versionStatusLabel(selectedVersion.versionStatus) }})</el-tag>
				</el-form-item>
				<el-form-item label="上次成功游标" v-if="jobForm.runType === 'INCREMENTAL'">
					<el-text type="info">{{ projectCursor || '无' }}</el-text>
				</el-form-item>
				<el-form-item label="试运行">
					<el-switch v-model="jobForm.dryRun" />
					<el-text type="warning" class="ml8">试运行不写实例</el-text>
				</el-form-item>
				<el-form-item label="最大错误率">
					<el-input-number v-model="jobForm.maxErrorRate" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" />
				</el-form-item>
			</el-form>
			<el-alert type="info" :closable="false" show-icon class="mb8">
				<template #title>不允许前端修改服务端 pageSize/timeout。高风险策略请确认后执行。</template>
			</el-alert>
			<template #footer>
				<el-button @click="jobDialogVisible = false">取消</el-button>
				<el-button type="primary" @click="handleStartJob" :loading="jobLoading">启动</el-button>
			</template>
		</el-dialog>

		<!-- ==================== 调度配置对话框 ==================== -->
		<ScheduleDialog ref="scheduleDialogRef" :project="selectedProject" @success="loadData" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Plus } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { useRouter } from 'vue-router';
import { mappingProjectApi, mappingJobApi } from '/@/api/ontology/data-mapping';
import type { MappingProjectVO, MappingProjectQuery, MappingVersionVO } from '/@/types/ontology/data-mapping';
import { projectStatusLabel, projectStatusTagType, versionStatusLabel, versionStatusTagType } from '../utils/mapping-status';
import MappingProjectFormDialog from './MappingProjectFormDialog.vue';
import ScheduleDialog from './ScheduleDialog.vue';

const { success: msgSuccess, error: msgError } = useMessage();
const router = useRouter();

const loading = ref(false);
const tableData = ref<MappingProjectVO[]>([]);
const total = ref(0);
const query = reactive<MappingProjectQuery>({
	mappingCode: '',
	mappingName: '',
	projectStatus: undefined,
	current: 1,
	size: 10,
});

const formDialogRef = ref();
const scheduleDialogRef = ref();

// 版本历史
const versionHistoryVisible = ref(false);
const versionLoading = ref(false);
const versionList = ref<MappingVersionVO[]>([]);
const selectedProject = ref<MappingProjectVO | null>(null);

// 作业启动
const jobDialogVisible = ref(false);
const jobLoading = ref(false);
const selectedVersion = ref<MappingVersionVO | null>(null);
const projectCursor = ref('');
const jobForm = reactive({
	runType: 'FULL' as 'FULL' | 'INCREMENTAL',
	dryRun: false,
	maxErrorRate: 0.1,
});

const loadData = async () => {
	loading.value = true;
	try {
		const { data } = await mappingProjectApi.projectPage(query);
		tableData.value = data?.records || [];
		total.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取映射工程列表失败');
	} finally {
		loading.value = false;
	}
};

const handleReset = () => {
	query.mappingCode = '';
	query.mappingName = '';
	query.projectStatus = undefined;
	query.current = 1;
	loadData();
};

const handleAdd = () => {
	formDialogRef.value?.openDialog();
};

const handleEditDraft = (row: MappingProjectVO & { draftVersion?: MappingVersionVO }) => {
	const versionId = row.draftVersion?.id;
	if (!versionId) {
		msgError('未找到DRAFT版本');
		return;
	}
	router.push({
		path: '/ontology/data-mapping/designer',
		query: { projectId: String(row.id), versionId: String(versionId) },
	});
};

const handleCreateVersion = async (row: MappingProjectVO) => {
	try {
		await ElMessageBox.confirm(`确认为工程「${row.mappingName}」创建下一版本？`, '创建版本', { type: 'info' });
		await mappingProjectApi.createNextVersion(row.id);
		msgSuccess('新版本创建成功');
		// 打开版本历史刷新
		handleVersionHistory(row);
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '创建版本失败');
	}
};

const handleVersionHistory = async (row: MappingProjectVO) => {
	selectedProject.value = row;
	versionHistoryVisible.value = true;
	versionLoading.value = true;
	try {
		const { data } = await mappingProjectApi.listVersions(row.id, { current: 1, size: 50 });
		versionList.value = data?.records || [];
	} catch (e: any) {
		msgError(e.message || '获取版本列表失败');
	} finally {
		versionLoading.value = false;
	}
};

const handleViewSnapshot = async (version: MappingVersionVO) => {
	try {
		const { data } = await mappingProjectApi.getSnapshot(version.id);
		ElMessageBox.alert(
			`<pre style="max-height:500px;overflow:auto;font-size:12px;">${JSON.stringify(
				typeof data === 'string' ? JSON.parse(data) : data,
				null,
				2
			)}</pre>`,
			'版本快照',
			{
				dangerouslyUseHTMLString: true,
			}
		);
	} catch (e: any) {
		msgError(e.message || '获取快照失败');
	}
};

const handlePublishVersion = async (version: MappingVersionVO) => {
	try {
		await ElMessageBox.confirm(`确认发布版本 ${version.versionNumber}？`, '发布确认', { type: 'warning' });
		await mappingProjectApi.publishVersion(version.id);
		msgSuccess('版本发布成功');
		if (selectedProject.value) handleVersionHistory(selectedProject.value);
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '发布失败');
	}
};

const handleRetire = async (version: MappingVersionVO) => {
	try {
		await ElMessageBox.confirm(`确认停用版本 ${version.versionNumber}？`, '停用确认', { type: 'warning' });
		await mappingProjectApi.retireVersion(version.id);
		msgSuccess('版本已停用');
		if (selectedProject.value) handleVersionHistory(selectedProject.value);
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '停用失败');
	}
};

const handleToggleStatus = async (row: MappingProjectVO) => {
	const newStatus = row.projectStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
	try {
		await ElMessageBox.confirm(`确认${newStatus === 'ACTIVE' ? '启用' : '停用'}工程「${row.mappingName}」？`, '状态变更', { type: 'warning' });
		await mappingProjectApi.updateProjectStatus(row.id, newStatus);
		msgSuccess('状态变更成功');
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '状态变更失败');
	}
};

const handleExecute = async (row: MappingProjectVO, runType: 'FULL' | 'INCREMENTAL') => {
	if (!row.activeVersionId) {
		msgError('该工程没有活跃版本，无法执行');
		return;
	}
	selectedProject.value = row;
	selectedVersion.value = null;
	jobForm.runType = runType;
	jobForm.dryRun = false;
	projectCursor.value = '';

	// 获取活跃版本详情
	try {
		const { data: version } = await mappingProjectApi.getVersionDetail(row.activeVersionId);
		selectedVersion.value = version;
	} catch (e: any) {
		msgError(e.message || '获取版本详情失败');
		return;
	}

	// 增量模式获取游标
	if (runType === 'INCREMENTAL') {
		try {
			const { data } = await mappingJobApi.getProjectCursor(row.id);
			projectCursor.value = data || '';
		} catch {
			// 游标获取失败不阻断流程
		}
	}

	jobDialogVisible.value = true;
};

const handleStartJob = async () => {
	if (!selectedVersion.value || !selectedProject.value) return;
	jobLoading.value = true;
	try {
		const { data } = await mappingJobApi.createJob(selectedVersion.value.id, {
			runType: jobForm.runType,
			dryRun: jobForm.dryRun,
			maxErrorRate: jobForm.maxErrorRate,
		});
		msgSuccess(`作业已创建（ID: ${data.id}）`);
		jobDialogVisible.value = false;
		// 跳转作业详情（通过切换到作业Tab）
	} catch (e: any) {
		msgError(e.message || '启动作业失败');
	} finally {
		jobLoading.value = false;
	}
};

const handleSchedule = (row: MappingProjectVO) => {
	selectedProject.value = row;
	scheduleDialogRef.value?.open(row);
};

onMounted(loadData);
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.ml8 {
	margin-left: 8px;
}
.mt8 {
	margin-top: 8px;
}
</style>
