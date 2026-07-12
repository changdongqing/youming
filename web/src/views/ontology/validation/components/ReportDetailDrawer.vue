<template>
	<el-drawer v-model="show" title="校验报告详情" size="60%" direction="rtl" destroy-on-close>
		<template v-if="reportDetail">
			<!-- 报告摘要 -->
			<el-descriptions :column="3" border class="mb8">
				<el-descriptions-item label="报告ID">{{ reportDetail.report.id }}</el-descriptions-item>
				<el-descriptions-item label="状态">
					<el-tag :type="statusTagType(reportDetail.report.status)" size="small">{{ reportDetail.report.status }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="结果">
					<el-tag v-if="reportDetail.report.status === 'COMPLETED'" :type="reportDetail.report.conforms ? 'success' : 'danger'" size="small">
						{{ reportDetail.report.conforms ? '全部通过' : '存在违规' }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="校验时间">{{ reportDetail.report.triggeredAt }}</el-descriptions-item>
				<el-descriptions-item label="耗时">{{ reportDetail.report.durationMs }}ms</el-descriptions-item>
				<el-descriptions-item label="触发人">{{ reportDetail.report.triggeredBy }}</el-descriptions-item>
				<el-descriptions-item label="执行规则数">{{ reportDetail.report.ruleCount }}</el-descriptions-item>
				<el-descriptions-item label="校验实例数">{{ reportDetail.report.instanceCount }}</el-descriptions-item>
				<el-descriptions-item label="总结果数">{{ reportDetail.report.totalCount }}</el-descriptions-item>
			</el-descriptions>

			<!-- 严重程度过滤 -->
			<div class="mb8" style="display: flex; gap: 8px; align-items: center">
				<el-button size="small" :type="severityFilter === 'VIOLATION' ? 'danger' : 'default'" @click="changeSeverity('VIOLATION')">
					违规({{ reportDetail.report.violationCount }})
				</el-button>
				<el-button size="small" :type="severityFilter === 'WARNING' ? 'warning' : 'default'" @click="changeSeverity('WARNING')">
					警告({{ reportDetail.report.warningCount }})
				</el-button>
				<el-button size="small" :type="severityFilter === 'INFO' ? 'info' : 'default'" @click="changeSeverity('INFO')">
					信息({{ reportDetail.report.infoCount }})
				</el-button>
				<el-button size="small" :type="severityFilter === '' ? 'primary' : 'default'" @click="changeSeverity('')">
					全部
				</el-button>
			</div>

			<!-- 违规明细表 -->
			<el-table :data="reportDetail.results.records" border max-height="500">
				<el-table-column label="级别" width="80">
					<template #default="{ row }">
						<el-tag :type="severityTagType(row.severity)" size="small">{{ severityLabel(row.severity) }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column prop="ruleName" label="规则" min-width="150" show-overflow-tooltip />
				<el-table-column prop="focusNode" label="违规实例" min-width="200" show-overflow-tooltip />
				<el-table-column prop="message" label="说明" min-width="200" show-overflow-tooltip />
				<el-table-column prop="expectedValue" label="期望值" min-width="120" show-overflow-tooltip />
				<el-table-column prop="actualValue" label="实际值" min-width="120" show-overflow-tooltip />
				<el-table-column prop="suggestion" label="修复建议" min-width="200" show-overflow-tooltip />
			</el-table>

			<pagination
				:total="resultPagination.total"
				v-model:current="resultPagination.current"
				v-model:size="resultPagination.size"
				@pagination="loadResults"
			/>
		</template>
		<el-empty v-else description="暂无报告数据" />
	</el-drawer>
</template>

<script lang="ts" name="reportDetailDrawer" setup>
import { ref, reactive, computed, watch } from 'vue';
import { fetchValidationReport } from '/@/api/ontology/validation';
import type { ValidationReportDetail } from '/@/types/ontology/validation';

const props = defineProps<{ visible: boolean; reportId: number }>();
const emit = defineEmits(['update:visible']);

const show = computed({
	get: () => props.visible,
	set: (v: boolean) => emit('update:visible', v)
});

const reportDetail = ref<ValidationReportDetail>();
const severityFilter = ref('VIOLATION');
const resultPagination = reactive({ current: 1, size: 20, total: 0 });

const statusTagType = (status: string) => {
	const map: Record<string, string> = {
		RUNNING: 'warning',
		COMPLETED: 'success',
		FAILED: 'danger'
	};
	return map[status] || 'info';
};

const severityTagType = (severity: string) => {
	const map: Record<string, string> = {
		VIOLATION: 'danger',
		WARNING: 'warning',
		INFO: 'info'
	};
	return map[severity] || 'info';
};

const severityLabel = (severity: string) => {
	const map: Record<string, string> = {
		VIOLATION: '违规',
		WARNING: '警告',
		INFO: '信息'
	};
	return map[severity] || severity;
};

const changeSeverity = (severity: string) => {
	severityFilter.value = severity;
	resultPagination.current = 1;
	loadResults();
};

const loadResults = async () => {
	if (!props.reportId) return;
	try {
		const res = await fetchValidationReport(props.reportId, {
			severity: severityFilter.value || undefined,
			page: resultPagination.current,
			size: resultPagination.size
		});
		reportDetail.value = res.data;
		resultPagination.total = res.data?.results?.total || 0;
	} catch {
		// 忽略
	}
};

watch(
	() => props.reportId,
	() => {
		if (props.reportId) {
			resultPagination.current = 1;
			loadResults();
		}
	},
	{ immediate: true }
);
</script>
