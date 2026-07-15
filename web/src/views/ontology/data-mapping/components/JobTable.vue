<template>
	<div>
		<!-- ==================== 搜索表单 ==================== -->
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input v-model="query.projectId" placeholder="映射工程ID" clearable style="width: 140px" @keyup.enter="loadData" @clear="loadData" />
			</el-form-item>
			<el-form-item label="运行类型">
				<el-select v-model="query.runType" placeholder="全部" clearable style="width: 120px" @change="loadData">
					<el-option label="全量" value="FULL" />
					<el-option label="增量" value="INCREMENTAL" />
					<el-option label="失败重试" value="RETRY" />
					<el-option label="关系重试" value="RELATION_RETRY" />
				</el-select>
			</el-form-item>
			<el-form-item label="状态">
				<el-select v-model="query.jobStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
					<el-option label="等待中" value="PENDING" />
					<el-option label="运行中" value="RUNNING" />
					<el-option label="已完成" value="COMPLETED" />
					<el-option label="部分完成" value="PARTIALLY_COMPLETED" />
					<el-option label="失败" value="FAILED" />
					<el-option label="已取消" value="CANCELLED" />
				</el-select>
			</el-form-item>
			<el-form-item>
				<el-button type="primary" @click="loadData">
					<el-icon><Search /></el-icon> 搜索
				</el-button>
				<el-button @click="handleReset">重置</el-button>
			</el-form-item>
		</el-form>

		<!-- ==================== 列表 ==================== -->
		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column prop="id" label="作业ID" width="80" />
			<el-table-column prop="mappingProjectId" label="工程ID" width="80" />
			<el-table-column label="运行类型" width="80">
				<template #default="{ row }">{{ runTypeLabel(row.runType) }}</template>
			</el-table-column>
			<el-table-column label="触发方式" width="80">
				<template #default="{ row }">{{ triggerTypeLabel(row.triggerType) }}</template>
			</el-table-column>
			<el-table-column label="状态" width="100">
				<template #default="{ row }">
					<el-tag :type="jobStatusTagType(row.jobStatus)" size="small">
						{{ jobStatusLabel(row.jobStatus) }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="当前阶段" width="100">
				<template #default="{ row }">{{ row.currentPhase ? phaseLabel(row.currentPhase) : '-' }}</template>
			</el-table-column>
			<el-table-column label="计数" width="200">
				<template #default="{ row }">
					<span style="font-size: 12px"> 读{{ row.totalRead }} / 建{{ row.totalCreated }} / 改{{ row.totalUpdated }} / 败{{ row.totalFailed }} </span>
				</template>
			</el-table-column>
			<el-table-column prop="startedAt" label="开始时间" width="170" />
			<el-table-column prop="finishedAt" label="结束时间" width="170" />
			<el-table-column prop="errorCode" label="错误码" width="120" show-overflow-tooltip />
			<el-table-column label="操作" width="180" fixed="right">
				<template #default="{ row }">
					<el-button link type="primary" @click="handleDetail(row)">详情</el-button>
					<el-button
						link
						type="warning"
						@click="handleCancel(row)"
						v-if="row.jobStatus === 'RUNNING' || row.jobStatus === 'PENDING'"
						v-auth="'ontology_mapping_execute'"
						>取消</el-button
					>
					<el-button
						link
						type="primary"
						@click="handleRetry(row)"
						v-if="row.jobStatus === 'FAILED' || row.jobStatus === 'PARTIALLY_COMPLETED'"
						v-auth="'ontology_mapping_retry'"
						>重试</el-button
					>
				</template>
			</el-table-column>
		</el-table>

		<pagination :total="total" v-model:current="query.current" v-model:size="query.size" @pagination="loadData" />

		<!-- ==================== 作业详情抽屉 ==================== -->
		<JobDetailDrawer ref="detailDrawerRef" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { mappingJobApi } from '/@/api/ontology/data-mapping';
import type { MappingJobVO, JobQueryParams } from '/@/types/ontology/data-mapping';
import { jobStatusLabel, jobStatusTagType, runTypeLabel, triggerTypeLabel, phaseLabel, isJobTerminal } from '../utils/mapping-status';
import JobDetailDrawer from './JobDetailDrawer.vue';

const { success: msgSuccess, error: msgError } = useMessage();

const loading = ref(false);
const tableData = ref<MappingJobVO[]>([]);
const total = ref(0);
const query = reactive<JobQueryParams>({
	projectId: '',
	runType: undefined,
	jobStatus: undefined,
	triggerType: undefined,
	current: 1,
	size: 10,
});

const detailDrawerRef = ref();

// 进度轮询定时器
let pollTimer: ReturnType<typeof setInterval> | null = null;

const loadData = async () => {
	loading.value = true;
	try {
		const params = { ...query };
		// projectId 转为数字
		if (params.projectId) {
			params.projectId = Number(params.projectId);
		} else {
			delete params.projectId;
		}
		const { data } = await mappingJobApi.jobPage(params);
		tableData.value = data?.records || [];
		total.value = data?.total || 0;
		// 检查是否有运行中作业，启动轮询
		const hasRunning = tableData.value.some((j) => !isJobTerminal(j.jobStatus));
		if (hasRunning) {
			startPolling();
		} else {
			stopPolling();
		}
	} catch (e: any) {
		msgError(e.message || '获取作业列表失败');
	} finally {
		loading.value = false;
	}
};

const startPolling = () => {
	if (pollTimer) return;
	pollTimer = setInterval(loadData, 3000);
};

const stopPolling = () => {
	if (pollTimer) {
		clearInterval(pollTimer);
		pollTimer = null;
	}
};

const handleReset = () => {
	query.projectId = '';
	query.runType = undefined;
	query.jobStatus = undefined;
	query.triggerType = undefined;
	query.current = 1;
	loadData();
};

const handleDetail = (row: MappingJobVO) => {
	detailDrawerRef.value?.open(row.id);
};

const handleCancel = async (row: MappingJobVO) => {
	try {
		await ElMessageBox.confirm(`确认取消作业 ${row.id}？`, '取消作业', { type: 'warning' });
		await mappingJobApi.cancelJob(row.id);
		msgSuccess('取消请求已发送');
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '取消失败');
	}
};

const handleRetry = async (row: MappingJobVO) => {
	try {
		await ElMessageBox.confirm(`确认重试作业 ${row.id} 的失败记录？`, '重试作业', { type: 'info' });
		const { data } = await mappingJobApi.retryJob(row.id);
		msgSuccess(`重试作业已创建（ID: ${data.id}）`);
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '重试失败');
	}
};

onMounted(loadData);

onUnmounted(() => {
	stopPolling();
});
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mt8 {
	margin-top: 8px;
}
</style>
