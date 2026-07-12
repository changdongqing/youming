<template>
	<div class="layout-padding ontology-validation-page">
		<!-- 顶部操作栏 -->
		<el-card shadow="never" class="mb8">
			<div style="display: flex; justify-content: space-between; align-items: center">
				<el-select v-model="selectedOntologyId" placeholder="选择本体工程" style="width: 320px" @change="handleOntologyChange">
					<el-option label="核心本体工程（GB/T 31486—2024）" :value="935001" />
				</el-select>
				<div>
					<el-button
						type="primary"
						icon="VideoPlay"
						:disabled="!selectedOntologyId || isRunning"
						:loading="isRunning"
						v-auth="'ontology_validation_run'"
						@click="handleRunValidation"
					>
						{{ isRunning ? '校验中...' : '执行全量校验' }}
					</el-button>
				</div>
			</div>
		</el-card>

		<!-- 最近一次校验摘要 -->
		<el-card v-if="latestReport" shadow="never" class="mb8">
			<template #header>
				<div style="display: flex; justify-content: space-between; align-items: center">
					<span>最近一次全量校验</span>
					<el-tag :type="latestReport.conforms ? 'success' : 'danger'" size="small">
						{{ latestReport.conforms ? '校验通过' : '存在违规' }}
					</el-tag>
				</div>
			</template>
			<el-descriptions :column="4" border>
				<el-descriptions-item label="校验时间">{{ latestReport.triggeredAt }}</el-descriptions-item>
				<el-descriptions-item label="耗时">{{ latestReport.durationMs }}ms</el-descriptions-item>
				<el-descriptions-item label="执行规则数">{{ latestReport.ruleCount }}</el-descriptions-item>
				<el-descriptions-item label="校验实例数">{{ latestReport.instanceCount }}</el-descriptions-item>
				<el-descriptions-item label="违规(VIOLATION)">
					<el-text :type="latestReport.violationCount > 0 ? 'danger' : 'info'">{{ latestReport.violationCount }}</el-text>
				</el-descriptions-item>
				<el-descriptions-item label="警告(WARNING)">
					<el-text type="warning">{{ latestReport.warningCount }}</el-text>
				</el-descriptions-item>
				<el-descriptions-item label="信息(INFO)">
					<el-text type="info">{{ latestReport.infoCount }}</el-text>
				</el-descriptions-item>
				<el-descriptions-item label="触发人">{{ latestReport.triggeredBy }}</el-descriptions-item>
			</el-descriptions>
			<div style="margin-top: 12px">
				<el-button type="primary" link @click="handleViewReport(latestReport.id)">查看完整报告 →</el-button>
			</div>
		</el-card>

		<!-- 报告历史列表 -->
		<el-card shadow="never">
			<el-table v-loading="tableLoading" :data="reportList" border style="width: 100%">
				<el-table-column prop="id" label="报告ID" width="100" />
				<el-table-column label="状态" width="120">
					<template #default="{ row }">
						<el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="结果" width="120">
					<template #default="{ row }">
						<el-tag v-if="row.status === 'COMPLETED'" :type="row.conforms ? 'success' : 'danger'" size="small">
							{{ row.conforms ? '通过' : '有违规' }}
						</el-tag>
						<span v-else>-</span>
					</template>
				</el-table-column>
				<el-table-column label="违规/警告/信息" width="160">
					<template #default="{ row }">
						<el-text type="danger">{{ row.violationCount }}</el-text> /
						<el-text type="warning">{{ row.warningCount }}</el-text> /
						<el-text type="info">{{ row.infoCount }}</el-text>
					</template>
				</el-table-column>
				<el-table-column prop="ruleCount" label="规则数" width="80" />
				<el-table-column prop="instanceCount" label="实例数" width="80" />
				<el-table-column prop="durationMs" label="耗时(ms)" width="100" />
				<el-table-column prop="triggeredBy" label="触发人" width="100" />
				<el-table-column prop="triggeredAt" label="触发时间" min-width="180" />
				<el-table-column label="操作" fixed="right" width="120">
					<template #default="{ row }">
						<el-button link type="primary" v-auth="'ontology_validation_report'" @click="handleViewReport(row.id)">查看报告</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @pagination="loadReportList" />
		</el-card>

		<!-- 报告详情抽屉 -->
		<report-detail-drawer v-model:visible="reportDrawerVisible" :report-id="selectedReportId" />
	</div>
</template>

<script lang="ts" name="ontologyValidation" setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
	fetchValidationReportPage,
	runFullValidation,
	fetchLatestReport,
	fetchValidationStatus
} from '/@/api/ontology/validation';
import type { ValidationSummary } from '/@/types/ontology/validation';
import ReportDetailDrawer from './components/ReportDetailDrawer.vue';

const selectedOntologyId = ref<number>();
const isRunning = ref(false);
const latestReport = ref<ValidationSummary>();
const reportList = ref<ValidationSummary[]>([]);
const tableLoading = ref(false);
const pagination = reactive({ current: 1, size: 20, total: 0 });
const reportDrawerVisible = ref(false);
const selectedReportId = ref<number>();
let pollTimer: number | null = null;

const statusTagType = (status: string) => {
	const map: Record<string, string> = {
		RUNNING: 'warning',
		COMPLETED: 'success',
		FAILED: 'danger'
	};
	return map[status] || 'info';
};

const loadReportList = async () => {
	tableLoading.value = true;
	try {
		const res = await fetchValidationReportPage({
			ontologyId: selectedOntologyId.value,
			page: pagination.current,
			size: pagination.size
		});
		reportList.value = res.data?.records || [];
		pagination.total = res.data?.total || 0;
	} catch (error) {
		getErrorMessage(error, '加载报告列表失败');
	} finally {
		tableLoading.value = false;
	}
};

const handleOntologyChange = async () => {
	if (selectedOntologyId.value) {
		try {
			const res = await fetchLatestReport(selectedOntologyId.value);
			latestReport.value = res.data || undefined;
		} catch (error) {
			getErrorMessage(error, '加载最近报告失败');
		}
	}
	await loadReportList();
};

const handleRunValidation = async () => {
	if (!selectedOntologyId.value) return;
	isRunning.value = true;
	try {
		const res = await runFullValidation({ ontologyId: selectedOntologyId.value });
		ElMessage.success(res.data?.message || '校验任务已提交');
		pollStatus(res.data?.reportId);
	} catch (error) {
		getErrorMessage(error, '提交校验失败');
		isRunning.value = false;
	}
};

const pollStatus = (reportId: number) => {
	pollTimer = window.setInterval(async () => {
		try {
			const res = await fetchValidationStatus(reportId);
			const status = res.data?.status;
			if (status === 'COMPLETED' || status === 'FAILED') {
				if (pollTimer) {
					clearInterval(pollTimer);
					pollTimer = null;
				}
				isRunning.value = false;
				await handleOntologyChange();
				if (status === 'COMPLETED') {
					if (res.data?.conforms) {
						ElMessage.success('校验通过，无违规项');
					} else {
						ElMessage.warning(`校验完成，发现 ${res.data?.violationCount || 0} 个违规`);
					}
				} else {
					ElMessage.error('校验执行失败');
				}
			}
		} catch {
			// 轮询失败，忽略
		}
	}, 2000);
};

const handleViewReport = (reportId: number) => {
	selectedReportId.value = reportId;
	reportDrawerVisible.value = true;
};

const getErrorMessage = (error: unknown, fallback: string) => {
	const msg = (error as { msg?: string })?.msg;
	ElMessage.error(msg || fallback);
};

onMounted(() => {
	selectedOntologyId.value = 935001;
	handleOntologyChange();
});

onUnmounted(() => {
	if (pollTimer) clearInterval(pollTimer);
});
</script>
