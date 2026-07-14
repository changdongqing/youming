<template>
	<div class="layout-padding ontology-version-page">
		<div class="layout-padding-auto layout-padding-view">
			<!-- ==================== 工具栏 ==================== -->
			<div class="version-toolbar">
				<el-form :inline="true">
					<el-form-item label="本体工程">
						<el-select v-model="query.ontologyId" placeholder="选择本体工程" style="width: 360px" @change="getList">
							<el-option :value="935001" label="标准本体核心工程" />
						</el-select>
					</el-form-item>

					<el-form-item label="版本号">
						<el-input v-model="query.versionNumber" placeholder="版本号" clearable style="width: 160px" @keyup.enter="getList" @clear="getList" />
					</el-form-item>

					<el-form-item label="状态">
						<el-select v-model="query.releaseStatus" placeholder="全部" clearable style="width: 140px" @change="getList">
							<el-option label="准备中" value="PREPARED" />
							<el-option label="迁移中" value="MIGRATING" />
							<el-option label="已发布" value="PUBLISHED" />
							<el-option label="失败" value="FAILED" />
							<el-option label="已取消" value="CANCELLED" />
						</el-select>
					</el-form-item>

					<el-form-item>
						<el-button type="primary" @click="getList">
							<el-icon><Search /></el-icon> 搜索
						</el-button>
						<el-button @click="handleReset">重置</el-button>
						<el-button type="success" @click="handlePrepare" v-auth="'ontology_version_publish'">
							<el-icon><Plus /></el-icon> 发布版本
						</el-button>
						<el-button @click="handleVersionConfig" v-auth="'ontology_version_publish'">
							<el-icon><Setting /></el-icon> 版本配置
						</el-button>
					</el-form-item>
				</el-form>
			</div>

			<!-- ==================== 工作区状态 ==================== -->
			<div class="workspace-status-bar" v-if="workspaceStatus">
				<el-alert :type="workspaceStatusType" :closable="false" show-icon>
					<template #title>
						工作区状态：<el-tag :type="workspaceStatusType" size="small">{{ workspaceStatusText }}</el-tag>
						<span v-if="currentVersion" style="margin-left: 16px">
							当前版本：<el-tag type="success" size="small">{{ currentVersion.versionNumber }}</el-tag>
						</span>
					</template>
				</el-alert>
			</div>

			<!-- ==================== 版本列表 ==================== -->
			<el-card shadow="never">
				<el-table :data="versionList" v-loading="loading" border style="width: 100%"
					:row-class-name="rowClassName">
					<el-table-column prop="versionNumber" label="版本号" width="120" />
					<el-table-column label="兼容性" width="160">
						<template #default="{ row }">
							<el-tag :type="compatibilityTagType(row.compatibility)" size="small">
								{{ compatibilityText(row.compatibility) }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="状态" width="100">
						<template #default="{ row }">
							<el-tag :type="statusTagType(row.releaseStatus)" size="small">
								{{ statusText(row.releaseStatus) }}
							</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="publishedBy" label="发布人" width="100" />
					<el-table-column prop="publishedAt" label="发布时间" width="180">
						<template #default="{ row }">
							{{ row.publishedAt || '-' }}
						</template>
					</el-table-column>
					<el-table-column prop="releaseNotes" label="发布说明" show-overflow-tooltip min-width="200" />
					<el-table-column label="操作" width="320" fixed="right">
						<template #default="{ row }">
							<el-button link type="primary" @click="handleDetail(row)">详情</el-button>
							<el-button link type="primary" @click="handleDiff(row)" v-if="row.releaseStatus === 'PUBLISHED'">对比</el-button>
							<el-button link type="primary" @click="handleSnapshot(row)">快照</el-button>
							<el-button link type="success" @click="handleActivate(row)"
								v-if="row.releaseStatus === 'PREPARED' || row.releaseStatus === 'MIGRATING'"
								v-auth="'ontology_version_publish'">激活</el-button>
							<el-button link type="warning" @click="handleMigration(row)"
								v-if="row.releaseStatus === 'PREPARED' && row.compatibility === 'BREAKING'"
								v-auth="'ontology_version_publish'">迁移</el-button>
							<el-button link type="danger" @click="handleCancel(row)"
								v-if="row.releaseStatus === 'PREPARED' || row.releaseStatus === 'MIGRATING'"
								v-auth="'ontology_version_publish'">取消</el-button>
							<el-button link type="primary" @click="handleRestore(row)"
								v-if="row.releaseStatus === 'PUBLISHED'"
								v-auth="'ontology_version_restore'">恢复</el-button>
						</template>
					</el-table-column>
				</el-table>

				<pagination :total="total" v-model:current="query.pageNum" v-model:size="query.pageSize" @pagination="getList" />
			</el-card>
		</div>

		<!-- ==================== 对话框 ==================== -->
		<prepare-version-dialog v-model:visible="prepareVisible" :ontology-id="query.ontologyId"
			@success="getList" />
		<version-detail-drawer v-model:visible="detailVisible" :version-id="selectedVersionId" />
		<version-diff-drawer v-model:visible="diffVisible" :version-id="selectedVersionId" />
		<migration-job-drawer v-model:visible="migrationVisible" :version-id="selectedVersionId"
			:ontology-id="query.ontologyId" />
		<restore-plan-dialog v-model:visible="restoreVisible" :version-id="selectedVersionId"
			:ontology-id="query.ontologyId" />
		<version-config-dialog v-model:visible="configVisible" :project-id="query.ontologyId"
			@success="getList" />
	</div>
</template>

<script lang="ts" name="ontologyVersion" setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { Search, Plus, Setting } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { fetchVersionPage, activateVersion, cancelVersion, fetchVersionById, fetchVersionSnapshot } from '/@/api/ontology/version';
import type { VersionDetailVO, VersionQuery } from '/@/types/ontology/version';
import PrepareVersionDialog from './components/PrepareVersionDialog.vue';
import VersionDetailDrawer from './components/VersionDetailDrawer.vue';
import VersionDiffDrawer from './components/VersionDiffDrawer.vue';
import MigrationJobDrawer from './components/MigrationJobDrawer.vue';
import RestorePlanDialog from './components/RestorePlanDialog.vue';
import VersionConfigDialog from './components/VersionConfigDialog.vue';

const { success: msgSuccess, error: msgError } = useMessage();

const loading = ref(false);
const versionList = ref<VersionDetailVO[]>([]);
const total = ref(0);

const query = reactive<VersionQuery>({
	ontologyId: 935001,
	versionNumber: '',
	releaseStatus: undefined,
	pageNum: 1,
	pageSize: 10,
});

const workspaceStatus = ref<string>('');
const currentVersion = ref<VersionDetailVO | null>(null);

const prepareVisible = ref(false);
const detailVisible = ref(false);
const diffVisible = ref(false);
const migrationVisible = ref(false);
const restoreVisible = ref(false);
const configVisible = ref(false);
const selectedVersionId = ref<number>(0);

const workspaceStatusType = computed(() => {
	switch (workspaceStatus.value) {
		case 'EDITABLE': return 'success';
		case 'PREPARED': return 'warning';
		case 'MIGRATING': return 'error';
		default: return 'info';
	}
});

const workspaceStatusText = computed(() => {
	switch (workspaceStatus.value) {
		case 'EDITABLE': return '可编辑';
		case 'PREPARED': return '候选版本已准备';
		case 'MIGRATING': return '迁移中（锁定）';
		default: return workspaceStatus.value;
	}
});

const getList = async () => {
	if (!query.ontologyId) return;
	loading.value = true;
	try {
		const { data } = await fetchVersionPage(query);
		versionList.value = data.records;
		total.value = data.total;

		// 找到当前版本
		const current = versionList.value.find((v) => v.isCurrent);
		currentVersion.value = current || null;
	} catch (e: any) {
		msgError(e.message || '获取版本列表失败');
	} finally {
		loading.value = false;
	}
};

const handleReset = () => {
	query.versionNumber = '';
	query.releaseStatus = undefined;
	query.pageNum = 1;
	getList();
};

const rowClassName = ({ row }: { row: VersionDetailVO }) => {
	return row.isCurrent ? 'current-version-row' : '';
};

const compatibilityTagType = (compat: string) => {
	switch (compat) {
		case 'PATCH_ONLY': return 'info';
		case 'BACKWARD_COMPATIBLE': return 'success';
		case 'BREAKING': return 'danger';
		default: return 'info';
	}
};

const compatibilityText = (compat: string) => {
	switch (compat) {
		case 'PATCH_ONLY': return '补丁';
		case 'BACKWARD_COMPATIBLE': return '向后兼容';
		case 'BREAKING': return '破坏性';
		default: return compat;
	}
};

const statusTagType = (status: string) => {
	switch (status) {
		case 'PUBLISHED': return 'success';
		case 'PREPARED': return 'warning';
		case 'MIGRATING': return 'danger';
		case 'FAILED': return 'danger';
		case 'CANCELLED': return 'info';
		default: return 'info';
	}
};

const statusText = (status: string) => {
	switch (status) {
		case 'PREPARED': return '准备中';
		case 'MIGRATING': return '迁移中';
		case 'PUBLISHED': return '已发布';
		case 'FAILED': return '失败';
		case 'CANCELLED': return '已取消';
		default: return status;
	}
};

const handlePrepare = () => {
	prepareVisible.value = true;
};

const handleVersionConfig = () => {
	configVisible.value = true;
};

const handleDetail = (row: VersionDetailVO) => {
	selectedVersionId.value = row.id;
	detailVisible.value = true;
};

const handleDiff = (row: VersionDetailVO) => {
	selectedVersionId.value = row.id;
	diffVisible.value = true;
};

const handleSnapshot = async (row: VersionDetailVO) => {
	try {
		const { data } = await fetchVersionSnapshot(row.id);
		ElMessageBox.alert(`<pre style="max-height:500px;overflow:auto;font-size:12px;">${JSON.stringify(JSON.parse(data), null, 2)}</pre>`, '版本快照', {
			dangerouslyUseHTMLString: true,
		});
	} catch (e: any) {
		msgError(e.message || '获取快照失败');
	}
};

const handleActivate = async (row: VersionDetailVO) => {
	try {
		await ElMessageBox.confirm(`确认激活版本 ${row.versionNumber} 为当前版本？`, '激活确认', { type: 'warning' });
		await activateVersion(row.id);
		msgSuccess('版本激活成功');
		getList();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '激活失败');
	}
};

const handleCancel = async (row: VersionDetailVO) => {
	try {
		await ElMessageBox.confirm(`确认取消版本 ${row.versionNumber}？`, '取消确认', { type: 'warning' });
		await cancelVersion(row.id);
		msgSuccess('版本已取消');
		getList();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '取消失败');
	}
};

const handleMigration = (row: VersionDetailVO) => {
	selectedVersionId.value = row.id;
	migrationVisible.value = true;
};

const handleRestore = (row: VersionDetailVO) => {
	selectedVersionId.value = row.id;
	restoreVisible.value = true;
};

onMounted(() => {
	getList();
});
</script>

<style scoped>
.ontology-version-page .version-toolbar {
	margin-bottom: 12px;
}
.ontology-version-page .workspace-status-bar {
	margin-bottom: 12px;
}
.ontology-version-page :deep(.current-version-row) {
	background-color: var(--el-color-success-light-9);
}
</style>
