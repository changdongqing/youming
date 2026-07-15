<template>
	<el-dialog
		v-model="visible"
		:title="`元数据浏览 - ${dataSource?.sourceName || ''}`"
		width="900px"
		:close-on-click-modal="false"
		draggable
		destroy-on-close
	>
		<div v-loading="loading">
			<!-- ==================== Schema选择 ==================== -->
			<el-form :inline="true" class="mb8">
				<el-form-item label="Schema">
					<el-select v-model="selectedSchema" placeholder="选择Schema" filterable style="width: 200px" @change="loadObjects">
						<el-option v-for="s in schemas" :key="s" :label="s" :value="s" />
					</el-select>
				</el-form-item>
				<el-form-item label="对象名">
					<el-input v-model="objectFilter" placeholder="过滤对象名" clearable style="width: 200px" @keyup.enter="loadObjects" @clear="loadObjects" />
				</el-form-item>
				<el-button type="primary" @click="loadObjects" :disabled="!selectedSchema">查询</el-button>
				<el-button @click="loadSchemas" :loading="loadingSchemas">刷新Schema</el-button>
			</el-form>

			<!-- ==================== 对象列表 ==================== -->
			<el-table :data="filteredObjects" border size="small" highlight-current-row @row-click="handleObjectClick" style="width: 100%">
				<el-table-column prop="schemaName" label="Schema" width="120" />
				<el-table-column prop="objectName" label="对象名" min-width="180" />
				<el-table-column prop="objectType" label="类型" width="80">
					<template #default="{ row }">
						<el-tag size="small" :type="row.objectType === 'TABLE' ? 'primary' : 'info'">{{ row.objectType }}</el-tag>
					</template>
				</el-table-column>
			</el-table>

			<!-- ==================== 列详情 ==================== -->
			<el-divider v-if="objectMetadata" content-position="left">
				{{ objectMetadata.schemaName }}.{{ objectMetadata.objectName }} 的列详情
				<el-tag size="small" class="ml4">hash: {{ objectMetadata.metadataHash?.slice(0, 8) }}...</el-tag>
			</el-divider>
			<el-table v-if="objectMetadata" :data="objectMetadata.columns" border size="small" style="width: 100%">
				<el-table-column prop="ordinal" label="#" width="50" />
				<el-table-column prop="name" label="列名" min-width="160" />
				<el-table-column prop="jdbcType" label="JDBC类型" width="120" />
				<el-table-column label="主键" width="60">
					<template #default="{ row }">
						<el-tag v-if="objectMetadata.primaryKey?.includes(row.name)" type="danger" size="small">PK</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="可空" width="60">
					<template #default="{ row }">
						<span :style="{ color: row.nullable ? 'var(--el-color-success)' : 'var(--el-color-danger)' }">{{ row.nullable ? '是' : '否' }}</span>
					</template>
				</el-table-column>
				<el-table-column prop="size" label="长度" width="80" />
			</el-table>

			<!-- 外键信息 -->
			<template v-if="objectMetadata && objectMetadata.foreignKeys?.length">
				<el-divider content-position="left">外键</el-divider>
				<el-table :data="objectMetadata.foreignKeys" border size="small">
					<el-table-column prop="name" label="外键名" width="160" />
					<el-table-column prop="columns" label="列" min-width="120">
						<template #default="{ row }">{{ row.columns?.join(', ') }}</template>
					</el-table-column>
					<el-table-column label="引用对象" min-width="160">
						<template #default="{ row }">{{ row.targetSchema }}.{{ row.targetObject }} ({{ row.targetColumns?.join(', ') }})</template>
					</el-table-column>
				</el-table>
			</template>
		</div>

		<template #footer>
			<el-button @click="visible = false">关闭</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue';
import { useMessage } from '/@/hooks/message';
import { dataSourceApi } from '/@/api/ontology/data-mapping';
import type { DataSourceVO, SourceObjectSummary, SourceObjectMetadataVO } from '/@/types/ontology/data-mapping';

const { error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const loadingSchemas = ref(false);
const dataSource = ref<DataSourceVO | null>(null);

const schemas = ref<string[]>([]);
const selectedSchema = ref('');
const objects = ref<SourceObjectSummary[]>([]);
const objectFilter = ref('');
const objectMetadata = ref<SourceObjectMetadataVO | null>(null);

const filteredObjects = computed(() => {
	if (!objectFilter.value) return objects.value;
	const f = objectFilter.value.toLowerCase();
	return objects.value.filter((o) => o.objectName.toLowerCase().includes(f));
});

const open = async (ds: DataSourceVO) => {
	dataSource.value = ds;
	visible.value = true;
	schemas.value = [];
	selectedSchema.value = '';
	objects.value = [];
	objectMetadata.value = null;
	await loadSchemas();
};

const loadSchemas = async () => {
	if (!dataSource.value) return;
	loadingSchemas.value = true;
	try {
		const { data } = await dataSourceApi.listSchemas(dataSource.value.id);
		schemas.value = data || [];
		// 自动选中第一个Schema
		if (schemas.value.length > 0 && !selectedSchema.value) {
			selectedSchema.value = schemas.value[0];
			await loadObjects();
		}
	} catch (e: any) {
		msgError(e.message || '获取Schema列表失败');
	} finally {
		loadingSchemas.value = false;
	}
};

const loadObjects = async () => {
	if (!dataSource.value || !selectedSchema.value) return;
	loading.value = true;
	objectMetadata.value = null;
	try {
		const { data } = await dataSourceApi.listObjects(dataSource.value.id, {
			schema: selectedSchema.value,
			objectName: objectFilter.value || undefined,
		});
		objects.value = data || [];
	} catch (e: any) {
		msgError(e.message || '获取对象列表失败');
	} finally {
		loading.value = false;
	}
};

const handleObjectClick = async (row: SourceObjectSummary) => {
	if (!dataSource.value) return;
	loading.value = true;
	try {
		const { data } = await dataSourceApi.getObjectMetadata(dataSource.value.id, row.schemaName, row.objectName);
		objectMetadata.value = data;
	} catch (e: any) {
		msgError(e.message || '获取对象元数据失败');
	} finally {
		loading.value = false;
	}
};

defineExpose({ open });
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.ml4 {
	margin-left: 4px;
}
</style>
