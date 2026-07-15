<template>
	<div>
		<!-- ==================== 搜索表单 ==================== -->
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="编码">
				<el-input v-model="query.sourceCode" placeholder="数据源编码" clearable style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
			</el-form-item>
			<el-form-item label="名称">
				<el-input v-model="query.sourceName" placeholder="数据源名称" clearable style="width: 160px" @keyup.enter="loadData" @clear="loadData" />
			</el-form-item>
			<el-form-item label="状态">
				<el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
					<el-option label="启用" value="ACTIVE" />
					<el-option label="停用" value="INACTIVE" />
					<el-option label="已归档" value="ARCHIVED" />
				</el-select>
			</el-form-item>
			<el-form-item>
				<el-button type="primary" @click="loadData">
					<el-icon><Search /></el-icon> 搜索
				</el-button>
				<el-button @click="handleReset">重置</el-button>
				<el-button type="success" @click="handleAdd" v-auth="'ontology_mapping_source_manage'">
					<el-icon><Plus /></el-icon> 新增
				</el-button>
			</el-form-item>
		</el-form>

		<!-- ==================== 列表 ==================== -->
		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column prop="sourceCode" label="编码" width="140" show-overflow-tooltip />
			<el-table-column prop="sourceName" label="名称" min-width="140" show-overflow-tooltip />
			<el-table-column prop="sourceType" label="源类型" width="100" />
			<el-table-column prop="databaseType" label="数据库类型" width="120" />
			<el-table-column label="状态" width="90">
				<template #default="{ row }">
					<el-tag :type="dataSourceStatusTagType(row.status)" size="small">
						{{ dataSourceStatusLabel(row.status) }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="连接测试" width="100">
				<template #default="{ row }">
					<el-tag v-if="row.lastTestStatus" :type="connectionTestTagType(row.lastTestStatus)" size="small">
						{{ connectionTestStatusLabel(row.lastTestStatus) }}
					</el-tag>
					<span v-else>-</span>
				</template>
			</el-table-column>
			<el-table-column prop="metadataRefreshedAt" label="元数据刷新" width="170">
				<template #default="{ row }">{{ row.metadataRefreshedAt || '-' }}</template>
			</el-table-column>
			<el-table-column prop="updateTime" label="更新时间" width="170" />
			<el-table-column label="操作" width="300" fixed="right">
				<template #default="{ row }">
					<el-button link type="primary" @click="handleEdit(row)" v-auth="'ontology_mapping_source_manage'">编辑</el-button>
					<el-button link type="primary" @click="handleTestConnection(row)" v-auth="'ontology_mapping_source_manage'">测试</el-button>
					<el-button link type="primary" @click="handleToggleStatus(row)" v-auth="'ontology_mapping_source_manage'">
						{{ row.status === 'ACTIVE' ? '停用' : '启用' }}
					</el-button>
					<el-button link type="primary" @click="handleRefreshMetadata(row)" v-auth="'ontology_mapping_source_manage'">刷新</el-button>
					<el-button link type="primary" @click="handleBrowseMetadata(row)">浏览</el-button>
					<el-popconfirm title="确认删除该数据源？" @confirm="handleDelete(row)" v-if="row.status !== 'ACTIVE'">
						<template #reference>
							<el-button link type="danger" v-auth="'ontology_mapping_source_manage'">删除</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next, sizes"
			:page-sizes="[10, 20, 50]"
			@current-change="loadData"
			@size-change="loadData"
			class="mt8"
		/>

		<!-- ==================== 数据源表单 ==================== -->
		<DataSourceFormDialog ref="formDialogRef" @success="loadData" />

		<!-- ==================== 元数据浏览 ==================== -->
		<MetadataBrowser ref="metadataBrowserRef" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Plus } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { dataSourceApi } from '/@/api/ontology/data-mapping';
import type { DataSourceVO, DataSourceQuery } from '/@/types/ontology/data-mapping';
import { dataSourceStatusLabel, dataSourceStatusTagType, connectionTestStatusLabel, connectionTestTagType } from '../utils/mapping-status';
import DataSourceFormDialog from './DataSourceFormDialog.vue';
import MetadataBrowser from './MetadataBrowser.vue';

const { success: msgSuccess, error: msgError } = useMessage();

const loading = ref(false);
const tableData = ref<DataSourceVO[]>([]);
const total = ref(0);
const query = reactive<DataSourceQuery>({
	sourceCode: '',
	sourceName: '',
	status: undefined,
	current: 1,
	size: 10,
});

const formDialogRef = ref();
const metadataBrowserRef = ref();

const loadData = async () => {
	loading.value = true;
	try {
		const { data } = await dataSourceApi.page(query);
		tableData.value = data?.records || [];
		total.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取数据源列表失败');
	} finally {
		loading.value = false;
	}
};

const handleReset = () => {
	query.sourceCode = '';
	query.sourceName = '';
	query.status = undefined;
	query.current = 1;
	loadData();
};

const handleAdd = () => {
	formDialogRef.value?.openDialog();
};

const handleEdit = (row: DataSourceVO) => {
	formDialogRef.value?.openDialog(row.id);
};

const handleTestConnection = async (row: DataSourceVO) => {
	try {
		const { data } = await dataSourceApi.testConnection(row.id);
		if (data.success) {
			msgSuccess(`连接成功（${data.latencyMs}ms）`);
		} else {
			msgError(`连接失败：${data.errorMessage || data.errorCode || '未知错误'}`);
		}
		loadData();
	} catch (e: any) {
		msgError(e.message || '测试连接失败');
	}
};

const handleToggleStatus = async (row: DataSourceVO) => {
	const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
	try {
		await ElMessageBox.confirm(`确认${newStatus === 'ACTIVE' ? '启用' : '停用'}数据源「${row.sourceName}」？`, '状态变更', { type: 'warning' });
		await dataSourceApi.updateStatus(row.id, newStatus);
		msgSuccess('状态变更成功');
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '状态变更失败');
	}
};

const handleRefreshMetadata = async (row: DataSourceVO) => {
	try {
		await ElMessageBox.confirm(`确认刷新数据源「${row.sourceName}」的元数据？`, '刷新元数据', { type: 'info' });
		const { data } = await dataSourceApi.refreshMetadata(row.id);
		msgSuccess(`元数据刷新完成，更新 ${data} 个对象`);
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '刷新元数据失败');
	}
};

const handleBrowseMetadata = (row: DataSourceVO) => {
	metadataBrowserRef.value?.open(row);
};

const handleDelete = async (row: DataSourceVO) => {
	try {
		await dataSourceApi.remove(row.id);
		msgSuccess('删除成功');
		loadData();
	} catch (e: any) {
		msgError(e.message || '删除失败');
	}
};

onMounted(loadData);
</script>
