<template>
	<div class="layout-padding ontology-serialization-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-tabs v-model="activeTab" type="border-card" class="serialization-tabs">
				<!-- ==================== 导出面板 ==================== -->
				<el-tab-pane label="导出" name="export">
					<el-form :model="exportForm" label-width="120px" class="export-form mt-4">
						<el-form-item label="本体工程">
							<el-select v-model="exportForm.ontologyId" placeholder="选择本体工程" style="width: 360px">
								<el-option :value="935001" label="标准本体核心工程（GB/T 31486—2024）" />
							</el-select>
						</el-form-item>

						<el-form-item label="导出格式">
							<el-radio-group v-model="exportForm.format">
								<el-radio-button v-for="f in formats" :key="f.value" :label="f.value">
									{{ f.label }}（.{{ f.extension }}）
								</el-radio-button>
							</el-radio-group>
						</el-form-item>

						<el-form-item label="导出范围">
							<el-select v-model="exportForm.scope" placeholder="选择导出范围" style="width: 360px">
								<el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" />
							</el-select>
						</el-form-item>

						<el-form-item label="谓词策略">
							<el-select v-model="exportForm.predicateStrategy" style="width: 360px">
								<el-option label="首选别名（附录D兼容）" value="PREFERRED_ALIAS" />
								<el-option label="国标标准IRI（严格口径）" value="STANDARD_IRI" />
								<el-option label="平台内部IRI" value="INTERNAL_IRI" />
							</el-select>
							<el-tooltip placement="right" content="选择导出时数据属性谓词的IRI策略。首选别名输出 measurementUnit 等示例别名；国标标准IRI输出 unit 等国标原名。">
								<el-icon class="ml-2"><InfoFilled /></el-icon>
							</el-tooltip>
						</el-form-item>

						<el-form-item v-if="exportForm.scope === 'INSTANCE_SUBTREE'" label="实体类型">
							<el-tree-select
								v-model="exportForm.targetTypeId"
								:data="entityTypeTree"
								:props="{ label: 'label', value: 'key', children: 'children' }"
								check-strictly
								placeholder="选择实体类型（含子树）"
								filterable
								style="width: 360px" />
						</el-form-item>

						<el-form-item label="校验状态">
							<el-alert v-if="precheckStatus === 'passed'" type="success" :closable="false"
								title="最近全量校验通过，可安全导出" show-icon />
							<el-alert v-else-if="precheckStatus === 'blocked'" type="error" :closable="false"
								:title="`存在 ${violationCount} 条违规，导出已拦截`" show-icon>
								<template #default>
									<span>请先修复违规，或使用强制导出。
										<el-link type="primary" @click="goToValidation">查看校验报告</el-link>
									</span>
								</template>
							</el-alert>
							<el-alert v-else type="warning" :closable="false"
								title="尚未执行全量校验，建议先校验再导出" show-icon />
						</el-form-item>

						<el-form-item>
							<el-button @click="handlePreview" :loading="previewing">
								<el-icon><View /></el-icon> 预览
							</el-button>
							<el-button type="primary" @click="handleExport" :loading="exporting"
								:disabled="precheckStatus === 'blocked' && !canForceExport">
								<el-icon><Download /></el-icon> 导出
							</el-button>
							<el-button v-if="canForceExport && precheckStatus === 'blocked'"
								type="danger" plain @click="handleForceExport" :loading="exporting">
								<el-icon><WarningFilled /></el-icon> 强制导出
							</el-button>
						</el-form-item>
					</el-form>
				</el-tab-pane>

				<!-- ==================== 导入面板 ==================== -->
				<el-tab-pane label="导入" name="import">
					<div v-if="!importPreview" class="mt-4">
						<el-upload
							drag
							accept=".ttl,.jsonld,.rdf,.nt,.json"
							:auto-upload="true"
							:http-request="handleFileUpload"
							:show-file-list="false"
							:disabled="uploading">
							<el-icon class="el-icon--upload"><UploadFilled /></el-icon>
							<div class="el-upload__text">
								拖拽 RDF 文件到此处，或 <em>点击上传</em>
							</div>
							<template #tip>
								<div class="el-upload__tip">
									支持 Turtle(.ttl)、JSON-LD(.jsonld)、RDF/XML(.rdf)、N-Triples(.nt) 格式，文件 ≤ 10MB
								</div>
							</template>
						</el-upload>
					</div>

					<template v-else>
						<!-- 摘要统计 -->
						<el-row :gutter="16" class="mt-4 mb-4">
							<el-col :span="6">
								<el-card shadow="hover">
									<el-statistic title="解析实例数" :value="importPreview.summary.totalInstances" />
								</el-card>
							</el-col>
							<el-col :span="6">
								<el-card shadow="hover">
									<el-statistic title="数据值数" :value="importPreview.summary.totalDataValues" />
								</el-card>
							</el-col>
							<el-col :span="6">
								<el-card shadow="hover">
									<el-statistic title="对象关系数" :value="importPreview.summary.totalObjectRelations" />
								</el-card>
							</el-col>
							<el-col :span="6">
								<el-card shadow="hover">
									<el-statistic title="冲突数" :value="importPreview.summary.conflictCount"
										:value-style="{ color: importPreview.summary.conflictCount > 0 ? '#f56c6c' : '#67c23a' }" />
								</el-card>
							</el-col>
						</el-row>

						<!-- 冲突列表 -->
						<el-card v-if="importPreview.conflicts.length > 0" class="mb-4">
							<template #header><span>IRI 冲突（{{ importPreview.conflicts.length }} 条）</span></template>
							<el-table :data="importPreview.conflicts" stripe>
								<el-table-column prop="iri" label="实例IRI" show-overflow-tooltip />
								<el-table-column prop="conflictType" label="冲突类型" width="150" />
							</el-table>
						</el-card>

						<!-- 错误列表 -->
						<el-card v-if="importPreview.errors.length > 0" class="mb-4">
							<template #header><span style="color: #f56c6c">错误（{{ importPreview.errors.length }} 条，不可导入）</span></template>
							<el-table :data="importPreview.errors" stripe>
								<el-table-column prop="instanceIri" label="实例IRI" show-overflow-tooltip />
								<el-table-column prop="message" label="错误信息" show-overflow-tooltip />
							</el-table>
						</el-card>

						<!-- 警告列表 -->
						<el-card v-if="importPreview.warnings.length > 0" class="mb-4">
							<template #header><span style="color: #e6a23c">警告（{{ importPreview.warnings.length }} 条）</span></template>
							<el-table :data="importPreview.warnings" stripe>
								<el-table-column prop="instanceIri" label="实例IRI" show-overflow-tooltip />
								<el-table-column prop="message" label="警告信息" show-overflow-tooltip />
							</el-table>
						</el-card>

						<!-- 合并模式 + 确认按钮 -->
						<el-form label-width="120px" class="mt-4">
							<el-form-item label="全局合并模式">
								<el-radio-group v-model="globalMergeMode">
									<el-radio label="SKIP">跳过已存在</el-radio>
									<el-radio label="MERGE">合并追加</el-radio>
									<el-radio label="OVERWRITE">覆盖重写</el-radio>
								</el-radio-group>
							</el-form-item>
							<el-form-item>
								<el-button @click="resetImport">重新上传</el-button>
								<el-button type="primary" @click="confirmImport" :loading="importing"
									v-auth="'ontology_import_add'">确认导入</el-button>
							</el-form-item>
						</el-form>
					</template>
				</el-tab-pane>

				<!-- ==================== 审计日志 ==================== -->
				<el-tab-pane label="审计日志" name="logs">
					<el-form :inline="true" class="mt-4 mb-4">
						<el-form-item label="操作类型">
							<el-select v-model="logQuery.operationType" clearable placeholder="全部" style="width: 120px">
								<el-option label="导出" value="EXPORT" />
								<el-option label="导入" value="IMPORT" />
							</el-select>
						</el-form-item>
						<el-form-item>
							<el-button type="primary" @click="loadLogs">查询</el-button>
						</el-form-item>
					</el-form>

					<el-table :data="logData.records" stripe v-loading="logLoading">
						<el-table-column prop="operationType" label="类型" width="80">
							<template #default="{ row }">
								<el-tag :type="row.operationType === 'EXPORT' ? 'primary' : 'success'">
									{{ row.operationType === 'EXPORT' ? '导出' : '导入' }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="rdfFormat" label="格式" width="100" />
						<el-table-column prop="exportScope" label="范围" width="120" />
						<el-table-column prop="tripleCount" label="三元组数" width="100" />
						<el-table-column prop="instanceCount" label="实例数" width="80" />
						<el-table-column prop="contentSize" label="文件大小" width="100">
							<template #default="{ row }">{{ formatSize(row.contentSize) }}</template>
						</el-table-column>
						<el-table-column prop="durationMs" label="耗时" width="80">
							<template #default="{ row }">{{ row.durationMs }}ms</template>
						</el-table-column>
						<el-table-column prop="createBy" label="操作人" width="100" />
						<el-table-column prop="createTime" label="时间" width="180" />
					</el-table>

					<el-pagination
						class="mt-4"
						v-model:current-page="logData.current"
						v-model:page-size="logData.size"
						:total="logData.total"
						layout="total, prev, pager, next"
						@current-change="loadLogs" />
				</el-tab-pane>
			</el-tabs>

			<!-- 导出预览抽屉 -->
			<el-drawer v-model="previewVisible" title="导出预览" size="70%" direction="rtl">
				<template v-if="previewData">
					<el-descriptions :column="3" border class="mb-4">
						<el-descriptions-item label="格式">{{ previewData.format }}</el-descriptions-item>
						<el-descriptions-item label="范围">{{ previewData.scope }}</el-descriptions-item>
						<el-descriptions-item label="策略">{{ previewData.predicateStrategy }}</el-descriptions-item>
						<el-descriptions-item label="三元组数">{{ previewData.tripleCount }}</el-descriptions-item>
						<el-descriptions-item label="内容大小">{{ formatSize(previewData.contentSize) }}</el-descriptions-item>
						<el-descriptions-item label="校验状态">
							<el-tag :type="previewData.precheck?.passed ? 'success' : 'danger'">
								{{ previewData.precheck?.passed ? '通过' : '已拦截' }}
							</el-tag>
						</el-descriptions-item>
					</el-descriptions>
					<el-input type="textarea" :model-value="previewData.content" readonly :rows="30" />
				</template>
			</el-drawer>
		</div>
	</div>
</template>

<script lang="ts" name="ontologySerialization" setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Download, View, WarningFilled, InfoFilled, UploadFilled } from '@element-plus/icons-vue';
import { useMessage, useMessageBox } from '/@/hooks/message';
import {
	exportOntology,
	downloadOntology,
	previewExport,
	fetchFormats,
	fetchScopes,
	importPreview as importPreviewApi,
	importConfirm,
	fetchSerializationLogs
} from '/@/api/ontology/serialization';
import { fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchLatestReport } from '/@/api/ontology/validation';
import type {
	ExportRequest,
	FormatOption,
	ScopeOption,
	ImportPreviewVO,
	SerializationLogVO
} from '/@/types/ontology/serialization';
import type { EntityTypeTreeNode } from '/@/types/ontology/entity-type';

const router = useRouter();
const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const { confirm: msgConfirm } = useMessageBox();

// Tab 状态
const activeTab = ref('export');

// ==================== 导出 ====================
const exportForm = reactive<ExportRequest>({
	ontologyId: 935001,
	format: 'TURTLE',
	scope: 'FULL',
	predicateStrategy: 'PREFERRED_ALIAS',
	targetTypeId: undefined,
	force: false,
	download: false
});

const formats = ref<FormatOption[]>([]);
const scopes = ref<ScopeOption[]>([]);
const entityTypeTree = ref<EntityTypeTreeNode[]>([]);
const precheckStatus = ref<'passed' | 'blocked' | 'unknown'>('unknown');
const violationCount = ref(0);
const previewing = ref(false);
const exporting = ref(false);
const previewVisible = ref(false);
const previewData = ref<any>(null);

const canForceExport = ref(false);

const loadFormats = async () => {
	try {
		const { data } = await fetchFormats();
		formats.value = data || [];
	} catch (e) { /* ignore */ }
};

const loadScopes = async () => {
	try {
		const { data } = await fetchScopes();
		scopes.value = data || [];
	} catch (e) { /* ignore */ }
};

const loadEntityTypeTree = async () => {
	try {
			const { data } = await fetchEntityTypeTree('935001');
		entityTypeTree.value = data || [];
	} catch (e) { /* ignore */ }
};

const loadPrecheckStatus = async () => {
	try {
		const { data } = await fetchLatestReport(exportForm.ontologyId as number);
		if (data && data.conforms) {
			precheckStatus.value = 'passed';
			violationCount.value = 0;
		} else if (data) {
			precheckStatus.value = 'blocked';
			violationCount.value = data.violationCount || 0;
		} else {
			precheckStatus.value = 'unknown';
		}
	} catch (e) {
		precheckStatus.value = 'unknown';
	}
};

const goToValidation = () => {
	router.push('/ontology/validation/index');
};

const handlePreview = async () => {
	previewing.value = true;
	try {
		const { data } = await previewExport(exportForm);
		previewData.value = data;
		previewVisible.value = true;
	} catch (e: any) {
		msgError(e.msg || '预览失败');
	} finally {
		previewing.value = false;
	}
};

const handleExport = async () => {
	await doExport(false);
};

const handleForceExport = async () => {
	try {
		await msgConfirm('确认强制导出？当前存在校验违规，导出的本体可能不符合国标要求。');
		await doExport(true);
	} catch (e) { /* cancelled */ }
};

const doExport = async (force: boolean) => {
	exporting.value = true;
	try {
		const params = { ...exportForm, force, download: true };
		const response = await downloadOntology(params);
		const blob = new Blob([response.data || response], { type: getContentType(exportForm.format as string) });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		const ext = formats.value.find((f: FormatOption) => f.value === exportForm.format)?.extension || 'ttl';
		link.download = `ontology-${exportForm.ontologyId}-${Date.now()}.${ext}`;
		document.body.appendChild(link);
		link.click();
		document.body.removeChild(link);
		URL.revokeObjectURL(url);
		msgSuccess('导出成功');
		loadLogs();
	} catch (e: any) {
		msgError(e.msg || '导出失败');
	} finally {
		exporting.value = false;
	}
};

const getContentType = (format: string): string => {
	const map: Record<string, string> = {
		TURTLE: 'text/turtle',
		JSON_LD: 'application/ld+json',
		RDF_XML: 'application/rdf+xml',
		N_TRIPLES: 'application/n-triples'
	};
	return map[format] || 'text/plain';
};

// ==================== 导入 ====================
const uploading = ref(false);
const importing = ref(false);
const importPreview = ref<ImportPreviewVO | null>(null);
const globalMergeMode = ref('SKIP');

const handleFileUpload = async (options: any) => {
	const file = options.file as File;
	if (file.size > 10 * 1024 * 1024) {
		msgError('文件大小不能超过 10MB');
		return;
	}
	uploading.value = true;
	try {
		const { data } = await importPreviewApi(file, exportForm.ontologyId as number);
		importPreview.value = data;
		if (data.summary.errorCount > 0) {
			msgWarning(`预检发现 ${data.summary.errorCount} 条错误，请检查后确认导入`);
		}
	} catch (e: any) {
		msgError(e.msg || '文件解析失败');
	} finally {
		uploading.value = false;
	}
};

const resetImport = () => {
	importPreview.value = null;
};

const confirmImport = async () => {
	if (!importPreview.value) return;
	importing.value = true;
	try {
		const { data } = await importConfirm({
			previewId: importPreview.value.previewId,
			iriMergeMode: globalMergeMode.value,
			ontologyId: importPreview.value.ontologyId
		});
		msgSuccess(`导入完成：成功 ${data.importedCount} 个，跳过 ${data.skippedCount} 个，失败 ${data.failedCount} 个`);
		resetImport();
		loadLogs();
	} catch (e: any) {
		msgError(e.msg || '导入失败');
	} finally {
		importing.value = false;
	}
};

// ==================== 审计日志 ====================
const logLoading = ref(false);
const logData = reactive<{ records: SerializationLogVO[]; total: number; current: number; size: number }>({
	records: [],
	total: 0,
	current: 1,
	size: 20
});
const logQuery = reactive<{ operationType?: string }>({ operationType: undefined });

const loadLogs = async () => {
	logLoading.value = true;
	try {
		const { data } = await fetchSerializationLogs({
			ontologyId: exportForm.ontologyId,
			operationType: logQuery.operationType,
			page: logData.current,
			size: logData.size
		});
		logData.records = data?.records || [];
		logData.total = data?.total || 0;
	} catch (e) { /* ignore */ } finally {
		logLoading.value = false;
	}
};

// ==================== 工具方法 ====================
const formatSize = (bytes: number): string => {
	if (!bytes || bytes === 0) return '0 B';
	const units = ['B', 'KB', 'MB', 'GB'];
	let i = 0;
	let size = bytes;
	while (size >= 1024 && i < units.length - 1) {
		size /= 1024;
		i++;
	}
	return `${size.toFixed(1)} ${units[i]}`;
};

// ==================== 初始化 ====================
onMounted(async () => {
	await Promise.all([loadFormats(), loadScopes(), loadEntityTypeTree(), loadPrecheckStatus(), loadLogs()]);
});
</script>

<style scoped>
.ontology-serialization-page {
	height: 100%;
}
.serialization-tabs {
	height: 100%;
}
.export-form {
	max-width: 800px;
}
</style>
