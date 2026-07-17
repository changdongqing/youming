<template>
	<el-drawer v-model="visible" title="校验报告" size="60%" destroy-on-close>
		<div v-loading="loading">
			<template v-if="report">
				<!-- 顶部计数 -->
				<el-row :gutter="12" class="mb12">
					<el-col :span="6">
						<el-card shadow="hover"
							><el-statistic title="违规" :value="report.violationCount" :value-style="{ color: 'var(--el-color-danger)' }"
						/></el-card>
					</el-col>
					<el-col :span="6">
						<el-card shadow="hover"
							><el-statistic title="警告" :value="report.warningCount" :value-style="{ color: 'var(--el-color-warning)' }"
						/></el-card>
					</el-col>
					<el-col :span="6">
						<el-card shadow="hover"><el-statistic title="信息" :value="report.infoCount" /></el-card>
					</el-col>
					<el-col :span="6">
						<el-card shadow="hover">
							<div style="text-align: center">
								<el-tag :type="reportStatusTagType(report.reportStatus)" size="large">{{ reportStatusLabel(report.reportStatus) }}</el-tag>
							</div>
						</el-card>
					</el-col>
				</el-row>

				<el-alert v-if="isReportStale" type="warning" :closable="false" show-icon class="mb8">
					<template #title>报告已过期（配置已修改），请重新校验后再发布</template>
				</el-alert>

				<!-- 问题列表 -->
				<el-form :inline="true" class="mb8">
					<el-form-item label="严重级别">
						<el-select v-model="issueFilter.severity" placeholder="全部" clearable style="width: 120px" @change="loadIssues">
							<el-option label="违规" value="VIOLATION" />
							<el-option label="警告" value="WARNING" />
							<el-option label="信息" value="INFO" />
						</el-select>
					</el-form-item>
					<el-form-item label="确认状态" v-if="issueFilter.severity === 'WARNING'">
						<el-select v-model="issueFilter.acknowledged" placeholder="全部" clearable style="width: 120px" @change="loadIssues">
							<el-option label="未确认" value="0" />
							<el-option label="已确认" value="1" />
						</el-select>
					</el-form-item>
				</el-form>

				<el-table v-loading="issueLoading" :data="issueList" border size="small" style="width: 100%">
					<el-table-column label="级别" width="80">
						<template #default="{ row }">
							<el-tag :type="severityTagType(row.severity)" size="small">{{ severityLabel(row.severity) }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="issueCode" label="编码" width="140" show-overflow-tooltip />
					<el-table-column prop="scopeType" label="范围" width="90" />
					<el-table-column prop="scopeRef" label="引用" width="120" show-overflow-tooltip />
					<el-table-column prop="message" label="描述" min-width="200" show-overflow-tooltip />
					<el-table-column prop="suggestion" label="建议" min-width="160" show-overflow-tooltip />
					<el-table-column label="操作" width="100" fixed="right">
						<template #default="{ row }">
							<el-button
								v-if="row.severity === 'WARNING' && row.acknowledged === '0'"
								link
								type="primary"
								size="small"
								@click="handleAcknowledge(row)"
							>
								确认
							</el-button>
							<el-tag v-if="row.severity === 'VIOLATION'" type="danger" size="small">不可忽略</el-tag>
						</template>
					</el-table-column>
				</el-table>

				<el-pagination
					v-model:current-page="issueFilter.current"
					v-model:page-size="issueFilter.size"
					:total="issueTotal"
					layout="total, prev, pager, next"
					@current-change="loadIssues"
					class="mt8"
				/>
			</template>
			<el-empty v-else description="请先执行校验" :image-size="60" />
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingValidationApi } from '/@/api/ontology/data-mapping';
import type { ValidationReportVO, ValidationIssueVO, MappingVersionVO } from '/@/types/ontology/data-mapping';
import { reportStatusLabel, reportStatusTagType, severityLabel, severityTagType } from '../utils/mapping-status';

const props = defineProps<{ versionId: number; currentRevision?: number; version?: MappingVersionVO | null }>();
const emit = defineEmits<{ (e: 'validated', report: ValidationReportVO): void }>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const issueLoading = ref(false);
const report = ref<ValidationReportVO | null>(null);
const issueList = ref<ValidationIssueVO[]>([]);
const issueTotal = ref(0);

const issueFilter = reactive({
	severity: undefined as string | undefined,
	acknowledged: undefined as string | undefined,
	current: 1,
	size: 10,
});

// 报告是否过期：当前配置 revision 与报告的 configRevision 不一致
const isReportStale = computed(() => {
	if (!report.value || !props.currentRevision) return false;
	return report.value.configRevision !== props.currentRevision;
});

const open = async (forceValidate = false) => {
	visible.value = true;
	report.value = null;
	issueList.value = [];

	// 只读模式：版本已有校验报告且非强制重新校验时，直接加载已有报告，
	// 避免对 VALIDATED/VALIDATING 等非 DRAFT 状态重复触发校验而报错
	const existingReportId = props.version?.validationReportId;
	if (!forceValidate && existingReportId) {
		await loadExistingReport(existingReportId);
		return;
	}
	await runValidation();
};

const loadExistingReport = async (reportId: number) => {
	loading.value = true;
	try {
		const { data } = await mappingValidationApi.getReport(reportId);
		report.value = data;
		await loadIssues();
	} catch (e: any) {
		// 已有报告加载失败则回退到重新校验
		await runValidation();
	} finally {
		loading.value = false;
	}
};

const runValidation = async () => {
	loading.value = true;
	try {
		const { data } = await mappingValidationApi.validate(props.versionId);
		report.value = data;
		emit('validated', data);
		await loadIssues();
	} catch (e: any) {
		msgError(e.message || '校验失败');
	} finally {
		loading.value = false;
	}
};

const loadIssues = async () => {
	if (!report.value) return;
	issueLoading.value = true;
	try {
		const { data } = await mappingValidationApi.getIssues(report.value.id, {
			severity: issueFilter.severity as any,
			acknowledged: issueFilter.acknowledged as any,
			current: issueFilter.current,
			size: issueFilter.size,
		});
		issueList.value = data?.records || [];
		issueTotal.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取问题列表失败');
	} finally {
		issueLoading.value = false;
	}
};

const handleAcknowledge = async (issue: ValidationIssueVO) => {
	try {
		await mappingValidationApi.acknowledgeIssue(issue.id);
		msgSuccess('已确认');
		loadIssues();
	} catch (e: any) {
		msgError(e.message || '确认失败');
	}
};

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
