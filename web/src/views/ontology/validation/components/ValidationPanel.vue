<template>
	<div class="validation-panel">
		<div class="panel-header">
			<el-text strong>约束校验</el-text>
			<el-button size="small" type="primary" link :loading="validating" v-auth="'ontology_validation_run'" @click="handleValidate">
				<el-icon><Refresh /></el-icon> 执行校验
			</el-button>
		</div>

		<!-- 校验结果 -->
		<div v-if="results.length === 0 && !validating" class="empty-state">
			<el-text v-if="hasValidated" type="success">
				<el-icon><CircleCheck /></el-icon> 校验通过，无违规项
			</el-text>
			<el-text v-else type="info">点击上方按钮执行校验</el-text>
		</div>

		<div v-loading="validating" class="result-list">
			<div
				v-for="(r, index) in results"
				:key="index"
				class="result-item"
				:class="severityClass(r.severity)"
			>
				<div class="result-header">
					<el-tag :type="severityTagType(r.severity)" size="small" effect="dark">
						{{ severityLabel(r.severity) }}
					</el-tag>
					<el-text size="small" truncated>{{ r.ruleName }}</el-text>
				</div>
				<div class="result-message">{{ r.message }}</div>
				<div v-if="r.expectedValue || r.actualValue" class="result-values">
					<span v-if="r.expectedValue" class="value-pair">
						<el-text type="info" size="small">期望:</el-text> {{ r.expectedValue }}
					</span>
					<span v-if="r.actualValue" class="value-pair">
						<el-text type="info" size="small">实际:</el-text> {{ r.actualValue }}
					</span>
				</div>
				<div v-if="r.suggestion" class="result-suggestion">
					<el-text type="warning" size="small">
						<el-icon><InfoFilled /></el-icon> {{ r.suggestion }}
					</el-text>
				</div>
			</div>
		</div>
	</div>
</template>

<script lang="ts" name="validationPanel" setup>
import { ref } from 'vue';
import { Refresh, CircleCheck, InfoFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { validateInstance } from '/@/api/ontology/validation';
import type { ValidationResultItem } from '/@/types/ontology/validation';

const props = defineProps<{ instanceId: number | string }>();

const results = ref<ValidationResultItem[]>([]);
const validating = ref(false);
const hasValidated = ref(false);

const severityClass = (severity: string) => ({
	VIOLATION: 'violation',
	WARNING: 'warning',
	INFO: 'info'
}[severity] || 'info');

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

const handleValidate = async () => {
	validating.value = true;
	try {
		const res = await validateInstance(props.instanceId);
		results.value = res.data?.results || [];
		hasValidated.value = true;
		if (res.data?.conforms) {
			ElMessage.success('校验通过');
		} else {
			ElMessage.warning(`发现 ${res.data?.violationCount || 0} 个违规`);
		}
	} catch (error) {
		const msg = (error as { msg?: string })?.msg;
		ElMessage.error(msg || '校验执行失败');
	} finally {
		validating.value = false;
	}
};

defineExpose({
	validate: handleValidate
});
</script>

<style scoped>
.validation-panel {
	width: 100%;
}
.panel-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 8px 0;
	border-bottom: 1px solid var(--el-border-color-lighter);
}
.empty-state {
	padding: 24px 0;
	text-align: center;
}
.result-list {
	margin-top: 8px;
}
.result-item {
	padding: 8px 12px;
	margin-bottom: 8px;
	border-radius: 4px;
	border-left: 3px solid var(--el-border-color);
	background-color: var(--el-fill-color-light);
}
.result-item.violation {
	border-left-color: var(--el-color-danger);
}
.result-item.warning {
	border-left-color: var(--el-color-warning);
}
.result-item.info {
	border-left-color: var(--el-color-info);
}
.result-header {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 4px;
}
.result-message {
	font-size: 13px;
	margin-bottom: 4px;
}
.result-values {
	font-size: 12px;
	margin-bottom: 4px;
}
.value-pair {
	margin-right: 16px;
}
.result-suggestion {
	font-size: 12px;
}
</style>
