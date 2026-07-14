<template>
	<div class="layout-padding ontology-sparql-page">
		<div class="layout-padding-auto layout-padding-view">
			<!-- ==================== 工具栏 ==================== -->
			<div class="sparql-toolbar">
				<el-form :inline="true">
					<el-form-item label="本体工程">
						<el-select v-model="ontologyId" placeholder="选择本体工程" style="width: 360px" @change="onOntologyChange">
							<el-option :value="935001" label="标准本体核心工程（GB/T 31486—2024）" />
						</el-select>
					</el-form-item>

					<el-form-item label="模板">
						<el-select v-model="selectedTemplateId" placeholder="选择模板" style="width: 200px" @change="onTemplateChange" clearable>
							<el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
						</el-select>
					</el-form-item>

					<el-form-item>
						<el-button type="primary" @click="handleExecute" :loading="executing"
							:disabled="!ontologyId || !queryText.trim()" v-auth="'ontology_sparql_query'">
							<el-icon><CaretRight /></el-icon> 执行
						</el-button>
						<el-button v-if="executing" type="danger" plain @click="handleCancel">
							<el-icon><Close /></el-icon> 取消
						</el-button>
						<el-button @click="handleExportCsv" :loading="exporting"
							:disabled="!ontologyId || !queryText.trim()" v-auth="'ontology_sparql_export'">
							<el-icon><Download /></el-icon> 导出CSV
						</el-button>
						<el-button @click="historyVisible = true" v-auth="'ontology_sparql_view'">
							<el-icon><Clock /></el-icon> 历史
						</el-button>
					</el-form-item>
				</el-form>
			</div>

			<!-- ==================== SPARQL 编辑器 ==================== -->
			<div class="sparql-editor-section">
				<query-editor v-model="queryText" height="240px" />
			</div>

			<!-- ==================== 模板占位符填充 ==================== -->
			<div v-if="currentTemplate && currentTemplate.placeholders.length > 0" class="template-placeholders">
				<el-form :inline="true">
					<el-form-item v-for="ph in currentTemplate.placeholders" :key="ph" :label="ph">
						<el-input v-model="placeholderValues[ph]" :placeholder="'输入 ' + ph + ' 值（完整IRI）'" style="width: 400px" />
					</el-form-item>
					<el-form-item>
						<el-button type="primary" plain @click="applyPlaceholders">应用占位符</el-button>
					</el-form-item>
				</el-form>
			</div>

			<!-- ==================== 结果区 ==================== -->
			<div class="sparql-result-section">
				<el-card shadow="never">
					<template #header>
						<div class="result-header">
							<span>查询结果</span>
							<el-tag v-if="result" :type="result.queryType === 'ERROR' ? 'danger' : 'success'" size="small">
								{{ result.queryType }}
							</el-tag>
						</div>
					</template>

					<template v-if="result && result.queryType !== 'ERROR'">
						<query-result-table :result="result" :table-max-height="400" />
					</template>

					<el-empty v-else-if="!result" description="执行查询后在此查看结果" />

					<el-alert v-else type="error" :title="errorMessage || '查询失败'" show-icon :closable="false" />
				</el-card>
			</div>

			<!-- ==================== 历史抽屉 ==================== -->
			<query-history-drawer v-model="historyVisible" :ontology-id="ontologyId" />
		</div>
	</div>
</template>

<script lang="ts" name="ontologySparql" setup>
import { ref, reactive, onMounted } from 'vue';
import { CaretRight, Close, Download, Clock } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import {
	executeSparqlQuery,
	exportSparqlCsv,
	fetchSparqlTemplates
} from '/@/api/ontology/sparql';
import type { SparqlQueryResultVO, SparqlTemplateVO } from '/@/types/ontology/sparql';
import QueryEditor from './components/QueryEditor.vue';
import QueryResultTable from './components/QueryResultTable.vue';
import QueryHistoryDrawer from './components/QueryHistoryDrawer.vue';

const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();

// ==================== 状态 ====================
const ontologyId = ref<number>(935001);
const queryText = ref<string>(`PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?type (COUNT(?instance) AS ?count) ?label WHERE {
  ?instance rdf:type ?type .
  OPTIONAL { ?type rdfs:label ?label }
}
GROUP BY ?type ?label
ORDER BY DESC(?count)
LIMIT 100`);

const executing = ref(false);
const exporting = ref(false);
const result = ref<SparqlQueryResultVO | null>(null);
const errorMessage = ref<string>('');

// 模板
const templates = ref<SparqlTemplateVO[]>([]);
const selectedTemplateId = ref<string>('');
const currentTemplate = ref<SparqlTemplateVO | null>(null);
const placeholderValues = reactive<Record<string, string>>({});

// 历史
const historyVisible = ref(false);

// ==================== 模板 ====================
const loadTemplates = async () => {
	try {
		const { data } = await fetchSparqlTemplates();
		templates.value = data || [];
	} catch (e) { /* ignore */ }
};

const onTemplateChange = (templateId: string) => {
	if (!templateId) {
		currentTemplate.value = null;
		return;
	}
	const tpl = templates.value.find((t: SparqlTemplateVO) => t.id === templateId);
	if (tpl) {
		currentTemplate.value = tpl;
		// 初始化占位符值
		Object.keys(placeholderValues).forEach((k) => delete placeholderValues[k]);
		tpl.placeholders.forEach((ph: string) => {
			placeholderValues[ph] = '';
		});
		// 如果没有占位符，直接加载模板
		if (tpl.placeholders.length === 0) {
			queryText.value = tpl.query;
		}
	}
};

const applyPlaceholders = () => {
	if (!currentTemplate.value) return;
	let query = currentTemplate.value.query;
	for (const ph of currentTemplate.value.placeholders) {
		const val = placeholderValues[ph];
		if (!val) {
			msgWarning(`请填写占位符 ${ph}`);
			return;
		}
		// 占位符值必须作为完整 IRI 放入 <...>
		query = query.replaceAll(`\${${ph}}`, val.startsWith('<') ? val : `<${val}>`);
	}
	queryText.value = query;
};

const onOntologyChange = () => {
	result.value = null;
	errorMessage.value = '';
};

// ==================== 执行查询 ====================
const handleExecute = async () => {
	if (!queryText.value.trim()) {
		msgWarning('请输入查询语句');
		return;
	}

	executing.value = true;
	result.value = null;
	errorMessage.value = '';

	try {
		const { data } = await executeSparqlQuery({
			ontologyId: ontologyId.value,
			query: queryText.value,
			format: 'JSON'
		});

		if (data && data.queryType === 'ERROR') {
			errorMessage.value = '查询执行失败';
			result.value = data;
		} else {
			result.value = data;
			msgSuccess(`查询完成：${data.rowCount ?? 0} 行，耗时 ${data.durationMs ?? 0}ms`);
		}
	} catch (e: any) {
		errorMessage.value = e.msg || '查询失败';
		result.value = {
			queryType: 'ERROR',
			rowCount: 0,
			durationMs: 0,
			truncated: false
		};
	} finally {
		executing.value = false;
	}
};

// ==================== 取消 ====================
const handleCancel = () => {
	// 前端无法直接中断已发出的 HTTP 请求（需要 AbortController）
	// 标记状态，后端超时会自动释放资源
	executing.value = false;
	msgWarning('已请求取消，等待服务端释放资源');
};

// ==================== CSV 导出 ====================
const handleExportCsv = async () => {
	if (!queryText.value.trim()) {
		msgWarning('请输入查询语句');
		return;
	}

	exporting.value = true;
	try {
		const response = await exportSparqlCsv({
			ontologyId: ontologyId.value,
			query: queryText.value,
			format: 'CSV'
		});
		const blob = new Blob([response.data || response], { type: 'text/csv;charset=UTF-8' });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		link.download = `sparql-export-${Date.now()}.csv`;
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
		URL.revokeObjectURL(url);
		msgSuccess('导出成功');
	} catch (e: any) {
		msgError(e.msg || '导出失败');
	} finally {
		exporting.value = false;
	}
};

// ==================== 初始化 ====================
onMounted(() => {
	loadTemplates();
});
</script>

<style scoped>
.ontology-sparql-page {
	height: 100%;
}
.sparql-toolbar {
	padding: 12px 0;
}
.sparql-editor-section {
	margin-bottom: 12px;
}
.template-placeholders {
	padding: 8px 0;
	background-color: var(--el-fill-color-light);
	border-radius: 4px;
	padding: 12px;
	margin-bottom: 12px;
}
.sparql-result-section {
	margin-top: 12px;
}
.result-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.mt-4 {
	margin-top: 16px;
}
.mb-4 {
	margin-bottom: 16px;
}
.ml6 {
	margin-left: 6px;
}
</style>
