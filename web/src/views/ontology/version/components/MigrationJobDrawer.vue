<template>
	<el-drawer v-model="show" title="实例迁移作业" size="720px">
		<div v-loading="loading">
			<div style="margin-bottom: 16px">
				<el-button type="primary" @click="handleCreate" :disabled="!versionId">创建迁移作业</el-button>
				<el-button @click="loadList">刷新</el-button>
			</div>

			<el-table :data="jobList" border size="small">
				<el-table-column prop="id" label="作业ID" width="80" />
				<el-table-column label="状态" width="100">
					<template #default="{ row }">
						<el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="进度" width="160">
					<template #default="{ row }">
						<el-progress :percentage="calcProgress(row)" :status="progressStatus(row.status)" />
					</template>
				</el-table-column>
				<el-table-column label="统计" width="180">
					<template #default="{ row }">
						<span>总{{ row.totalCount }} / 成功{{ row.successCount }} / 失败{{ row.failedCount }}</span>
					</template>
				</el-table-column>
				<el-table-column prop="createTime" label="创建时间" width="160" />
				<el-table-column label="操作" width="100">
					<template #default="{ row }">
						<el-button link type="danger" @click="handleCancel(row.id)"
							v-if="row.status === 'PENDING' || row.status === 'RUNNING'">取消</el-button>
					</template>
				</el-table-column>
			</el-table>
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { fetchMigrationJobPage, createMigrationJob, cancelMigrationJob } from '/@/api/ontology/version';
import type { MigrationJob } from '/@/types/ontology/version';

const props = defineProps<{ visible: boolean; versionId: number; ontologyId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>();

const { success: msgSuccess, error: msgError } = useMessage();
const loading = ref(false);
const jobList = ref<MigrationJob[]>([]);
let pollTimer: ReturnType<typeof setInterval> | null = null;

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const loadList = async () => {
	if (!props.versionId) return;
	loading.value = true;
	try {
		const { data } = await fetchMigrationJobPage({
			candidateVersionId: props.versionId,
			pageNum: 1,
			pageSize: 50,
		});
		jobList.value = data.records;

		// 如果有运行中的作业，启动轮询
		const hasRunning = jobList.value.some((j) => j.status === 'PENDING' || j.status === 'RUNNING');
		if (hasRunning && !pollTimer) {
			pollTimer = setInterval(loadList, 5000);
		}
		else if (!hasRunning && pollTimer) {
			clearInterval(pollTimer);
			pollTimer = null;
		}
	} catch (e) {
		// ignore
	} finally {
		loading.value = false;
	}
};

const handleCreate = async () => {
	try {
		await ElMessageBox.confirm('确认为此候选版本创建迁移作业？', '创建迁移作业', { type: 'info' });
		await createMigrationJob(props.versionId, props.versionId);
		msgSuccess('迁移作业已创建');
		loadList();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '创建失败');
	}
};

const handleCancel = async (jobId: number) => {
	try {
		await ElMessageBox.confirm('确认取消此迁移作业？', '取消作业', { type: 'warning' });
		await cancelMigrationJob(jobId);
		msgSuccess('作业已取消');
		loadList();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '取消失败');
	}
};

const statusType = (s: string) => {
	switch (s) {
		case 'SUCCEEDED': return 'success';
		case 'RUNNING': return 'warning';
		case 'FAILED': case 'PARTIAL_FAILED': return 'danger';
		default: return 'info';
	}
};

const calcProgress = (job: MigrationJob) => {
	if (!job.totalCount || job.totalCount === 0) return 0;
	return Math.round((job.processedCount / job.totalCount) * 100);
};

const progressStatus = (s: string) => {
	if (s === 'SUCCEEDED') return 'success';
	if (s === 'FAILED' || s === 'PARTIAL_FAILED') return 'exception';
	return undefined;
};

watch(() => props.visible, (v) => {
	if (v) {
		loadList();
	} else if (pollTimer) {
		clearInterval(pollTimer);
		pollTimer = null;
	}
});

onUnmounted(() => {
	if (pollTimer) {
		clearInterval(pollTimer);
		pollTimer = null;
	}
});
</script>
