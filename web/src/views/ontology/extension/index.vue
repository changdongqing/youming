<template>
	<div class="layout-padding ontology-extension-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-row :gutter="16">
				<!-- 左：模块列表 -->
				<el-col :span="14">
					<el-card shadow="never">
						<template #header>
							<div class="card-header">
								<el-input v-model="query.moduleCode" placeholder="模块代码" style="width: 140px" clearable
									@clear="handleSearch" @keyup.enter="handleSearch" />
								<el-input v-model="query.moduleName" placeholder="模块名称" style="width: 140px; margin-left: 8px" clearable
									@clear="handleSearch" @keyup.enter="handleSearch" />
								<el-button type="primary" icon="Search" @click="handleSearch" style="margin-left: 8px">搜索</el-button>
								<el-button v-auth="'ontology_extension_add'" type="success" icon="Plus"
									@click="openForm(null)" style="margin-left: 8px">新建模块</el-button>
							</div>
						</template>

						<el-table :data="moduleList" v-loading="loading" border style="width: 100%">
							<el-table-column prop="moduleCode" label="模块代码" width="120" />
							<el-table-column prop="moduleName" label="模块名称" width="140" />
							<el-table-column label="命名空间" width="120">
								<template #default="{ row }">
									<el-tag size="small" type="warning">{{ row.namespacePrefix || '—' }}</el-tag>
								</template>
							</el-table-column>
							<el-table-column prop="version" label="版本" width="80" />
							<el-table-column prop="sortOrder" label="排序" width="60" />
							<el-table-column prop="createTime" label="创建时间" width="150" />
							<el-table-column label="操作" width="260" fixed="right">
								<template #default="{ row }">
									<el-button link type="primary" @click="handleDetail(row)">详情</el-button>
									<el-button link type="primary" @click="openForm(row)">编辑</el-button>
									<el-button link type="warning" @click="handleValidate(row)">校验</el-button>
									<el-button link type="info" @click="handleImpact(row)">影响</el-button>
									<el-button link type="success" @click="handleExport(row)">导出</el-button>
									<el-button v-auth="'ontology_extension_del'" link type="danger" @click="handleDelete(row)">删除</el-button>
								</template>
							</el-table-column>
						</el-table>

						<pagination :total="total" v-model:current="query.pageNum" v-model:size="query.pageSize" @pagination="getList" />
					</el-card>
				</el-col>

				<!-- 右：模块详情 + 资源面板 -->
				<el-col :span="10">
					<el-card shadow="never" v-if="selectedModule">
						<template #header>
							<div class="card-header">
								<span>{{ selectedModule.moduleName }}（{{ selectedModule.moduleCode }}）</span>
								<el-button link type="primary" @click="openAssociate(selectedModule)" style="margin-left: auto">
									<el-icon><Plus /></el-icon> 关联资源
								</el-button>
							</div>
						</template>
						<el-descriptions :column="1" border size="small">
							<el-descriptions-item label="模块代码">{{ selectedModule.moduleCode }}</el-descriptions-item>
							<el-descriptions-item label="模块名称">{{ selectedModule.moduleName }}</el-descriptions-item>
							<el-descriptions-item label="命名空间">
								<el-tag size="small" type="warning">{{ selectedModule.namespacePrefix }}</el-tag>
								<span style="margin-left: 8px; color: #999; font-size: 12px">{{ selectedModule.namespaceUri }}</span>
							</el-descriptions-item>
							<el-descriptions-item label="版本">{{ selectedModule.version || '—' }}</el-descriptions-item>
							<el-descriptions-item label="描述">{{ selectedModule.description || '—' }}</el-descriptions-item>
							<el-descriptions-item label="创建时间">{{ selectedModule.createTime }}</el-descriptions-item>
						</el-descriptions>

						<ResourceTable :module-id="selectedModule.id" :refresh-key="resourceRefreshKey" @remove="handleRemoveResource" />
					</el-card>
					<el-empty v-else description="请选择左侧模块查看详情" />
				</el-col>
			</el-row>

			<ModuleForm v-model:visible="formVisible" :module="editModule" @success="handleSearch" />
			<ResourceAssociate v-model:visible="associateVisible" :module="selectedModule" @success="handleAssociateSuccess" />
			<ValidationReport v-model:visible="validationVisible" :report="validationReport" />
			<ImpactDialog v-model:visible="impactVisible" :summary="impactSummary" />

			<!-- 导出格式选择对话框 -->
			<el-dialog v-model="exportDialogVisible" title="选择导出格式" width="380px">
				<el-form label-width="80px">
					<el-form-item label="RDF格式">
						<el-select v-model="exportFormat" style="width: 100%">
							<el-option label="Turtle (.ttl)" value="TURTLE" />
							<el-option label="JSON-LD (.jsonld)" value="JSON-LD" />
							<el-option label="RDF/XML (.rdf)" value="RDF-XML" />
							<el-option label="N-Triples (.nt)" value="N-TRIPLES" />
						</el-select>
					</el-form-item>
				</el-form>
				<template #footer>
					<el-button @click="exportDialogVisible = false">取消</el-button>
					<el-button type="primary" :loading="exporting" @click="confirmExport">确认导出</el-button>
				</template>
			</el-dialog>
		</div>
	</div>
</template>

<script setup lang="ts" name="ontologyExtension">
import { ref, reactive, onMounted } from 'vue';
import { useMessage, useMessageBox } from '/@/hooks/message';
import {
	fetchExtensionModulePage,
	delExtensionModuleObj,
	validateExtensionModule,
	fetchExtensionImpact,
	exportExtensionModule,
	removeExtensionResource,
} from '/@/api/ontology/extension';
import type { ExtensionModule, ExtensionValidationReport, ExtensionImpactSummary, RdfFormat } from '/@/types/ontology/extension';
import ModuleForm from './module-form.vue';
import ResourceAssociate from './resource-associate.vue';
import ValidationReport from './validation-report.vue';
import ImpactDialog from './impact-dialog.vue';
import ResourceTable from './components/ResourceTable.vue';

const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const { confirm: msgConfirm } = useMessageBox();

const loading = ref(false);
const moduleList = ref<ExtensionModule[]>([]);
const total = ref(0);
const query = reactive({
	moduleCode: '',
	moduleName: '',
	pageNum: 1,
	pageSize: 10,
});

const selectedModule = ref<ExtensionModule | null>(null);
const formVisible = ref(false);
const editModule = ref<ExtensionModule | null>(null);
const associateVisible = ref(false);
const validationVisible = ref(false);
const validationReport = ref<ExtensionValidationReport | null>(null);
const impactVisible = ref(false);
const impactSummary = ref<ExtensionImpactSummary | null>(null);
const resourceRefreshKey = ref(0);
// 导出格式选择
const exportDialogVisible = ref(false);
const exportFormat = ref<RdfFormat>('TURTLE');
const exporting = ref(false);
const exportTarget = ref<ExtensionModule | null>(null);

// 导出文件扩展名映射
const exportExtMap: Record<string, string> = {
	TURTLE: 'ttl',
	'JSON-LD': 'jsonld',
	'RDF-XML': 'rdf',
	'N-TRIPLES': 'nt',
};

const getList = async () => {
	loading.value = true;
	try {
		const res = await fetchExtensionModulePage(query);
		moduleList.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const handleSearch = () => {
	query.pageNum = 1;
	getList();
};

const handleDetail = (row: ExtensionModule) => {
	selectedModule.value = row;
};

const openForm = (row: ExtensionModule | null) => {
	editModule.value = row;
	formVisible.value = true;
};

const openAssociate = (row: ExtensionModule) => {
	selectedModule.value = row;
	associateVisible.value = true;
};

const handleAssociateSuccess = () => {
	resourceRefreshKey.value++;
	getList();
};

const handleValidate = async (row: ExtensionModule) => {
	try {
		const res = await validateExtensionModule(row.id);
		validationReport.value = res.data;
		validationVisible.value = true;
	} catch {
		msgError('校验失败');
	}
};

const handleImpact = async (row: ExtensionModule) => {
	try {
		const res = await fetchExtensionImpact(row.id);
		impactSummary.value = res.data;
		impactVisible.value = true;
	} catch {
		msgError('影响分析失败');
	}
};

const handleExport = (row: ExtensionModule) => {
	// 打开格式选择对话框，暂存目标模块
	exportTarget.value = row;
	exportFormat.value = 'TURTLE';
	exportDialogVisible.value = true;
};

const confirmExport = async () => {
	if (!exportTarget.value) return;
	const row = exportTarget.value;
	exporting.value = true;
	try {
		const response: any = await exportExtensionModule(row.id, exportFormat.value);
		// request 封装的 blob 响应，实际数据在 response 或 response.data
		const blob = new Blob([response.data || response], { type: 'application/octet-stream' });
		const url = window.URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = `${row.moduleCode}_extension.${exportExtMap[exportFormat.value] || 'txt'}`;
		a.click();
		window.URL.revokeObjectURL(url);
		msgSuccess('导出成功');
		exportDialogVisible.value = false;
	} catch {
		msgError('导出失败');
	} finally {
		exporting.value = false;
	}
};

const handleDelete = (row: ExtensionModule) => {
	msgConfirm(`确认删除扩展模块「${row.moduleName}」？关联资源将被解除（资源本身不删除）。`, '删除确认', {
		type: 'warning',
	}).then(async () => {
		try {
			await delExtensionModuleObj(row.id);
			msgSuccess('删除成功');
			if (selectedModule.value?.id === row.id) {
				selectedModule.value = null;
			}
			getList();
		} catch {
			msgError('删除失败');
		}
	}).catch(() => {});
};

const handleRemoveResource = (resourceId: string, resourceType: string) => {
	if (!selectedModule.value) return;
	msgConfirm('确认解除该资源的关联？资源本身不会被删除。').then(async () => {
		try {
			await removeExtensionResource(selectedModule.value!.id, resourceId, resourceType);
			msgSuccess('已解除关联');
			resourceRefreshKey.value++;
			getList();
		} catch {
			msgError('解除关联失败');
		}
	}).catch(() => {});
};

onMounted(() => {
	getList();
});
</script>

<style scoped>
.ontology-extension-page .card-header {
	display: flex;
	align-items: center;
}
</style>
