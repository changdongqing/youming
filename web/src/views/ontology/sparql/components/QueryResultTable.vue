<template>
	<div class="query-result-table">
		<!-- 结果摘要 -->
		<el-descriptions :column="4" border size="small" class="mb-4">
			<el-descriptions-item label="查询类型">
				<el-tag :type="result.queryType === 'ASK' ? 'warning' : 'primary'" size="small">
					{{ result.queryType }}
				</el-tag>
			</el-descriptions-item>
			<el-descriptions-item label="行数">{{ result.rowCount ?? 0 }}</el-descriptions-item>
			<el-descriptions-item label="耗时">{{ result.durationMs ?? 0 }}ms</el-descriptions-item>
			<el-descriptions-item label="截断">
				<el-tag :type="result.truncated ? 'danger' : 'success'" size="small">
					{{ result.truncated ? '是' : '否' }}
				</el-tag>
			</el-descriptions-item>
		</el-descriptions>

		<!-- ASK 布尔结果 -->
		<div v-if="result.queryType === 'ASK'" class="ask-result">
			<el-result :icon="result.booleanResult ? 'success' : 'info'" :title="result.booleanResult ? 'TRUE' : 'FALSE'" />
		</div>

		<!-- SELECT 表格（动态列） -->
		<el-table
			v-else-if="result.queryType === 'SELECT' && result.rows && result.rows.length > 0"
			:data="result.rows"
			border
			stripe
			style="width: 100%"
			:max-height="tableMaxHeight">
			<el-table-column type="index" label="#" width="50" fixed />
			<el-table-column
				v-for="varName in result.variables"
				:key="varName"
				:prop="varName"
				:label="varName"
				min-width="160"
				show-overflow-tooltip>
				<template #default="{ row }">
					<template v-if="row[varName]">
						<span :class="getNodeClass(row[varName].nodeType)" @click="copyValue(row[varName].value)">
							{{ formatCellValue(row[varName]) }}
						</span>
						<el-tag v-if="row[varName].language" size="small" class="ml6">{{ row[varName].language }}</el-tag>
					</template>
					<span v-else class="empty-binding">—</span>
				</template>
			</el-table-column>
		</el-table>

		<!-- 空结果 -->
		<el-empty v-else-if="result.queryType === 'SELECT'" description="查询结果为空" />
	</div>
</template>

<script lang="ts" name="SparqlQueryResultTable" setup>
import { useClipboard } from '@vueuse/core';
import { useMessage } from '/@/hooks/message';
import type { SparqlQueryResultVO, SparqlBindingVO } from '/@/types/ontology/sparql';

defineProps<{
	result: SparqlQueryResultVO;
	tableMaxHeight?: number | string;
}>();

const { copy } = useClipboard();
const { success: msgSuccess } = useMessage();

const getNodeClass = (nodeType: string): string => {
	if (nodeType === 'IRI') return 'cell-iri';
	if (nodeType === 'BNODE') return 'cell-bnode';
	return 'cell-literal';
};

const formatCellValue = (binding: SparqlBindingVO): string => {
	let value = binding.value || '';
	// 字面量值超过 200 字符时前端预览截断（后端结果值不改变）
	if (binding.nodeType === 'LITERAL' && value.length > 200) {
		value = value.substring(0, 200) + '...';
	}
	// IRI 显示为本地名（最后一个 # 或 / 之后的部分）
	if (binding.nodeType === 'IRI') {
		const hashIdx = value.lastIndexOf('#');
		const slashIdx = value.lastIndexOf('/');
		const idx = Math.max(hashIdx, slashIdx);
		if (idx >= 0 && idx < value.length - 1) {
			return value.substring(idx + 1);
		}
	}
	return value;
};

const copyValue = async (value: string) => {
	try {
		await copy(value);
		msgSuccess('已复制到剪贴板');
	} catch (e) {
		/* ignore */
	}
};
</script>

<style scoped>
.cell-iri {
	color: #409eff;
	cursor: pointer;
	text-decoration: underline dotted;
}
.cell-bnode {
	color: #e6a23c;
	font-style: italic;
}
.cell-literal {
	color: #303133;
}
.empty-binding {
	color: #c0c4cc;
}
.ask-result {
	display: flex;
	justify-content: center;
	padding: 20px;
}
</style>
