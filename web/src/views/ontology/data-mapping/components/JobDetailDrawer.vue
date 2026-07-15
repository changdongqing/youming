<template>
	<el-drawer v-model="visible" title="作业详情" size="70%" destroy-on-close>
		<div v-loading="loading">
			<template v-if="job">
				<!-- ==================== 基本信息 ==================== -->
				<el-descriptions :column="3" border title="基本信息" class="mb12">
					<el-descriptions-item label="作业ID">{{ job.id }}</el-descriptions-item>
					<el-descriptions-item label="工程ID">{{ job.mappingProjectId }}</el-descriptions-item>
					<el-descriptions-item label="版本ID">{{ job.mappingVersionId }}</el-descriptions-item>
					<el-descriptions-item label="运行类型">{{ runTypeLabel(job.runType) }}</el-descriptions-item>
					<el-descriptions-item label="触发方式">{{ triggerTypeLabel(job.triggerType) }}</el-descriptions-item>
					<el-descriptions-item label="状态">
						<el-tag :type="jobStatusTagType(job.jobStatus)" size="small">{{ jobStatusLabel(job.jobStatus) }}</el-tag>
					</el-descriptions-item>
					<el-descriptions-item label="当前阶段">{{ job.currentPhase ? phaseLabel(job.currentPhase) : '-' }}</el-descriptions-item>
					<el-descriptions-item label="当前映射编码">{{ job.currentMappingCode || '-' }}</el-descriptions-item>
					<el-descriptions-item label="当前页号">{{ job.currentPageNo || '-' }}</el-descriptions-item>
					<el-descriptions-item label="请求人">{{ job.requestedBy || '-' }}</el-descriptions-item>
					<el-descriptions-item label="开始时间">{{ job.startedAt || '-' }}</el-descriptions-item>
					<el-descriptions-item label="结束时间">{{ job.finishedAt || '-' }}</el-descriptions-item>
					<el-descriptions-item label="traceId">
						<el-text size="small" type="info">{{ job.traceId || '-' }}</el-text>
					</el-descriptions-item>
				</el-descriptions>

				<!-- ==================== 计数卡片 ==================== -->
				<el-row :gutter="12" class="mb12">
					<el-col :span="4">
						<el-card shadow="hover"><el-statistic title="总读取" :value="job.totalRead" /></el-card>
					</el-col>
					<el-col :span="4">
						<el-card shadow="hover"
							><el-statistic title="新建" :value="job.totalCreated" :value-style="{ color: 'var(--el-color-success)' }"
						/></el-card>
					</el-col>
					<el-col :span="4">
						<el-card shadow="hover"
							><el-statistic title="更新" :value="job.totalUpdated" :value-style="{ color: 'var(--el-color-primary)' }"
						/></el-card>
					</el-col>
					<el-col :span="4">
						<el-card shadow="hover"><el-statistic title="未变化" :value="job.totalUnchanged" /></el-card>
					</el-col>
					<el-col :span="4">
						<el-card shadow="hover"><el-statistic title="跳过" :value="job.totalSkipped" /></el-card>
					</el-col>
					<el-col :span="4">
						<el-card shadow="hover"
							><el-statistic title="失败" :value="job.totalFailed" :value-style="{ color: 'var(--el-color-danger)' }"
						/></el-card>
					</el-col>
				</el-row>
				<el-row :gutter="12" class="mb12">
					<el-col :span="6">
						<el-card shadow="hover"><el-statistic title="关系总数" :value="job.totalRelations" /></el-card>
					</el-col>
				</el-row>

				<!-- ==================== 错误信息 ==================== -->
				<template v-if="job.errorCode || job.errorMessage">
					<el-alert type="error" :closable="false" show-icon class="mb12">
						<template #title>错误码: {{ job.errorCode }}</template>
						<template #default>{{ job.errorMessage }}</template>
					</el-alert>
				</template>

				<!-- ==================== 失败记录 ==================== -->
				<el-divider content-position="left">失败记录</el-divider>
				<el-form :inline="true" class="mb8">
					<el-form-item label="状态">
						<el-select v-model="recordQuery.recordStatus" placeholder="全部" clearable style="width: 140px" @change="loadRecords">
							<el-option label="失败" value="FAILED" />
							<el-option label="成功" value="SUCCESS" />
							<el-option label="跳过" value="SKIPPED" />
						</el-select>
					</el-form-item>
				</el-form>
				<el-table v-loading="recordLoading" :data="recordList" border size="small">
					<el-table-column prop="mappingCode" label="映射编码" width="140" show-overflow-tooltip />
					<el-table-column prop="phase" label="阶段" width="80">
						<template #default="{ row }">{{ phaseLabel(row.phase) }}</template>
					</el-table-column>
					<el-table-column prop="sourceRecordKeyMasked" label="记录键掩码" min-width="160" show-overflow-tooltip />
					<el-table-column prop="recordAction" label="动作" width="100" />
					<el-table-column prop="recordStatus" label="状态" width="80" />
					<el-table-column prop="errorCode" label="错误码" width="120" show-overflow-tooltip />
					<el-table-column prop="errorMessage" label="错误消息" min-width="200" show-overflow-tooltip />
					<el-table-column prop="durationMs" label="耗时(ms)" width="90" />
					<el-table-column prop="createTime" label="时间" width="170" />
				</el-table>
				<el-pagination
					v-model:current-page="recordQuery.current"
					v-model:page-size="recordQuery.size"
					:total="recordTotal"
					layout="total, prev, pager, next"
					@current-change="loadRecords"
					class="mt8"
				/>

				<!-- ==================== 操作 ==================== -->
				<el-divider content-position="left">操作</el-divider>
				<el-button
					type="warning"
					@click="handleCancel"
					v-if="job.jobStatus === 'RUNNING' || job.jobStatus === 'PENDING'"
					v-auth="'ontology_mapping_execute'"
				>
					取消作业
				</el-button>
				<el-button
					type="primary"
					@click="handleRetry"
					v-if="job.jobStatus === 'FAILED' || job.jobStatus === 'PARTIALLY_COMPLETED'"
					v-auth="'ontology_mapping_retry'"
				>
					重试失败记录
				</el-button>
			</template>
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, reactive, onUnmounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { mappingJobApi } from '/@/api/ontology/data-mapping';
import type { MappingJobVO, MappingJobRecordVO } from '/@/types/ontology/data-mapping';
import { jobStatusLabel, jobStatusTagType, runTypeLabel, triggerTypeLabel, phaseLabel, isJobTerminal } from '../utils/mapping-status';

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const jobId = ref(0);
const job = ref<MappingJobVO | null>(null);

// 失败记录
const recordLoading = ref(false);
const recordList = ref<MappingJobRecordVO[]>([]);
const recordTotal = ref(0);
const recordQuery = reactive({ recordStatus: '', current: 1, size: 10 });

// 轮询
let pollTimer: ReturnType<typeof setInterval> | null = null;

const open = async (id: number) => {
	visible.value = true;
	jobId.value = id;
	job.value = null;
	recordList.value = [];
	recordTotal.value = 0;
	recordQuery.recordStatus = '';
	recordQuery.current = 1;
	await loadDetail();
	await loadRecords();
};

const loadDetail = async () => {
	loading.value = true;
	try {
		const { data } = await mappingJobApi.getJobDetail(jobId.value);
		job.value = data;
		// 非终态轮询
		if (!isJobTerminal(data.jobStatus)) {
			startPolling();
		} else {
			stopPolling();
		}
	} catch (e: any) {
		msgError(e.message || '获取作业详情失败');
	} finally {
		loading.value = false;
	}
};

const loadRecords = async () => {
	recordLoading.value = true;
	try {
		const { data } = await mappingJobApi.getJobRecords(jobId.value, {
			recordStatus: recordQuery.recordStatus || undefined,
			current: recordQuery.current,
			size: recordQuery.size,
		});
		recordList.value = data?.records || [];
		recordTotal.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取记录列表失败');
	} finally {
		recordLoading.value = false;
	}
};

const startPolling = () => {
	if (pollTimer) return;
	pollTimer = setInterval(loadDetail, 3000);
};

const stopPolling = () => {
	if (pollTimer) {
		clearInterval(pollTimer);
		pollTimer = null;
	}
};

const handleCancel = async () => {
	try {
		await ElMessageBox.confirm(`确认取消作业 ${jobId.value}？`, '取消作业', { type: 'warning' });
		await mappingJobApi.cancelJob(jobId.value);
		msgSuccess('取消请求已发送');
		loadDetail();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '取消失败');
	}
};

const handleRetry = async () => {
	try {
		await ElMessageBox.confirm(`确认重试作业 ${jobId.value} 的失败记录？`, '重试作业', { type: 'info' });
		const { data } = await mappingJobApi.retryJob(jobId.value);
		msgSuccess(`重试作业已创建（ID: ${data.id}）`);
		visible.value = false;
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '重试失败');
	}
};

onUnmounted(() => {
	stopPolling();
});

defineExpose({ open });
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mb12 {
	margin-bottom: 12px;
}
.mt8 {
	margin-top: 8px;
}
</style>
