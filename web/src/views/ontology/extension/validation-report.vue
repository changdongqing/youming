<template>
	<el-dialog v-model="visible" title="扩展合法性校验报告" width="720px">
		<template v-if="report">
			<el-result :icon="report.conforms ? 'success' : 'error'" :title="report.conforms ? '校验通过' : '校验未通过'"
				:sub-title="`VIOLATION: ${report.violationCount}  WARNING: ${report.warningCount}`" />
			<el-table :data="report.results" border style="margin-top: 16px">
				<el-table-column prop="ruleCode" label="规则" width="60" />
				<el-table-column prop="ruleName" label="规则名称" width="120" />
				<el-table-column label="级别" width="90">
					<template #default="{ row }">
						<el-tag :type="row.severity === 'VIOLATION' ? 'danger' : row.severity === 'WARNING' ? 'warning' : 'info'"
							size="small">{{ row.severity }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="通过" width="50">
					<template #default="{ row }">
						<el-icon :color="row.passed ? '#67c23a' : '#f56c6c'">
							<component :is="row.passed ? 'CircleCheck' : 'CircleClose'" />
						</el-icon>
					</template>
				</el-table-column>
				<el-table-column prop="message" label="说明" show-overflow-tooltip />
				<el-table-column prop="suggestion" label="修复建议" width="200" show-overflow-tooltip />
			</el-table>
		</template>
		<el-empty v-else description="无校验结果" />
		<template #footer>
			<el-button @click="visible = false">关闭</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ExtensionValidationReport } from '/@/types/ontology/extension';

const props = defineProps<{ visible: boolean; report: ExtensionValidationReport | null }>();
const emit = defineEmits<{ 'update:visible': [value: boolean] }>();

const visible = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});
</script>
