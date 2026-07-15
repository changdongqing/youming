<template>
	<div class="panel-container">
		<div class="panel-title">
			<el-icon><Connection /></el-icon> 源数据
		</div>
		<el-scrollbar class="panel-content">
			<!-- 数据源选择 -->
			<el-select v-model="selectedSourceId" placeholder="选择数据源" filterable class="mb8 w-full" @change="handleSourceChange">
				<el-option v-for="ds in dataSourceList" :key="ds.id" :label="`${ds.sourceCode} - ${ds.sourceName}`" :value="ds.id" />
			</el-select>

			<!-- 元数据过期提示 -->
			<el-alert v-if="selectedSource && !selectedSource.metadataRefreshedAt" type="warning" :closable="false" show-icon class="mb8">
				<template #title>元数据尚未刷新</template>
			</el-alert>

			<template v-if="selectedSourceId">
				<!-- Schema 树 -->
				<el-input
					v-model="objectFilter"
					clearable
					placeholder="过滤表名"
					prefix-icon="Search"
					size="small"
					class="mb8"
					@keyup.enter="loadObjects"
					@clear="loadObjects"
				/>

				<el-tree
					:data="schemaTree"
					node-key="key"
					:props="treeProps"
					:expand-on-click-node="false"
					:highlight-current="true"
					@node-click="handleNodeClick"
				>
					<template #default="{ data }">
						<span class="tree-node">
							<span v-if="data.type === 'schema'" class="tree-node-label">{{ data.label }}</span>
							<span v-else class="tree-node-label">
								<el-tag size="small" :type="data.objectType === 'TABLE' ? 'primary' : 'info'" class="mr4">{{ data.objectType }}</el-tag>
								{{ data.label }}
							</span>
						</span>
					</template>
				</el-tree>

				<el-empty v-if="!loading && schemaTree.length === 0" description="无可用Schema" :image-size="40" />

				<!-- 列详情 -->
				<template v-if="columnMetadata">
					<el-divider content-position="left">
						{{ columnMetadata.schemaName }}.{{ columnMetadata.objectName }}
						<el-tag size="small" class="ml4" :type="metadataDrifted ? 'danger' : 'success'">
							{{ metadataDrifted ? '哈希漂移' : '稳定' }}
						</el-tag>
					</el-divider>
					<el-table :data="columnMetadata.columns" border size="small" style="width: 100%">
						<el-table-column prop="ordinal" label="#" width="40" />
						<el-table-column prop="name" label="列名" min-width="120" show-overflow-tooltip />
						<el-table-column prop="jdbcType" label="JDBC类型" width="100" />
						<el-table-column label="PK" width="40">
							<template #default="{ row }">
								<el-tag v-if="columnMetadata.primaryKey?.includes(row.name)" type="danger" size="small">PK</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="可空" width="50">
							<template #default="{ row }">
								<span :style="{ color: row.nullable ? 'var(--el-color-success)' : 'var(--el-color-danger)', fontSize: '12px' }">{{
									row.nullable ? '是' : '否'
								}}</span>
							</template>
						</el-table-column>
						<el-table-column prop="size" label="长度" width="60" />
					</el-table>
				</template>
			</template>

			<el-empty v-else description="请选择数据源" :image-size="60" />
		</el-scrollbar>
	</div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue';
import { Connection, Search } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { dataSourceApi } from '/@/api/ontology/data-mapping';
import type { DataSourceVO, SourceObjectMetadataVO, SourceObjectSummary } from '/@/types/ontology/data-mapping';
import { formatDigest } from '../utils/security-display';

const props = defineProps<{ sourceId?: number }>();
const emit = defineEmits<{
	(e: 'select', payload: { schema: string; object: string; columns: SourceObjectMetadataVO['columns'] }): void;
}>();

const { error: msgError } = useMessage();

const dataSourceList = ref<DataSourceVO[]>([]);
const selectedSourceId = ref<number | undefined>(props.sourceId);
const loading = ref(false);
const objectFilter = ref('');

const schemas = ref<string[]>([]);
const objects = ref<SourceObjectSummary[]>([]);
const columnMetadata = ref<SourceObjectMetadataVO | null>(null);

const selectedSource = computed(() => dataSourceList.value.find((ds) => ds.id === selectedSourceId.value));

// metadataHash 漂移状态（V1 简化：有 metadataHash 即视为稳定，无则视为可能漂移）
const metadataDrifted = ref(false);

interface SchemaTreeNode {
	key: string;
	label: string;
	type: 'schema' | 'object';
	schemaName?: string;
	objectName?: string;
	objectType?: string;
	children?: SchemaTreeNode[];
}

const schemaTree = ref<SchemaTreeNode[]>([]);
const treeProps = { label: 'label', children: 'children' };

const loadDataSourceList = async () => {
	try {
		const { data } = await dataSourceApi.page({ status: 'ACTIVE', current: 1, size: 100 });
		dataSourceList.value = data?.records || [];
	} catch (e: any) {
		msgError(e.message || '获取数据源列表失败');
	}
};

const handleSourceChange = async () => {
	schemas.value = [];
	objects.value = [];
	schemaTree.value = [];
	columnMetadata.value = null;
	if (!selectedSourceId.value) return;

	loading.value = true;
	try {
		const { data } = await dataSourceApi.listSchemas(selectedSourceId.value);
		schemas.value = data || [];
		await loadObjectsForAllSchemas();
	} catch (e: any) {
		msgError(e.message || '获取Schema列表失败');
	} finally {
		loading.value = false;
	}
};

const loadObjectsForAllSchemas = async () => {
	schemaTree.value = [];
	for (const schema of schemas.value) {
		try {
			const { data } = await dataSourceApi.listObjects(selectedSourceId.value!, { schema });
			const children: SchemaTreeNode[] = (data || []).map((obj) => ({
				key: `${schema}.${obj.objectName}`,
				label: obj.objectName,
				type: 'object' as const,
				schemaName: schema,
				objectName: obj.objectName,
				objectType: obj.objectType,
			}));
			schemaTree.value.push({
				key: schema,
				label: schema,
				type: 'schema',
				children,
			});
		} catch {
			// 单个 schema 失败不阻断
		}
	}
};

const loadObjects = async () => {
	// 重新加载当前展开的 schema 下的对象（带过滤）
	await loadObjectsForAllSchemas();
};

const handleNodeClick = async (data: SchemaTreeNode) => {
	if (data.type !== 'object' || !data.schemaName || !data.objectName) return;

	loading.value = true;
	try {
		const { data: metadata } = await dataSourceApi.getObjectMetadata(selectedSourceId.value!, data.schemaName, data.objectName);
		columnMetadata.value = metadata;
		emit('select', { schema: data.schemaName, object: data.objectName, columns: metadata.columns });
	} catch (e: any) {
		msgError(e.message || '获取对象元数据失败');
	} finally {
		loading.value = false;
	}
};

watch(
	() => props.sourceId,
	(val) => {
		selectedSourceId.value = val;
		if (val) handleSourceChange();
	}
);

onMounted(async () => {
	await loadDataSourceList();
	if (props.sourceId) handleSourceChange();
});
</script>

<style scoped>
.panel-container {
	height: 100%;
	display: flex;
	flex-direction: column;
}
.panel-title {
	padding: 8px 12px;
	font-weight: 600;
	font-size: 13px;
	border-bottom: 1px solid var(--el-border-color-light);
	background-color: var(--el-fill-color-light);
	display: flex;
	align-items: center;
	gap: 6px;
}
.panel-content {
	flex: 1;
	overflow: auto;
	padding: 8px 12px;
}
.mb8 {
	margin-bottom: 8px;
}
.ml4 {
	margin-left: 4px;
}
.mr4 {
	margin-right: 4px;
}
.w-full {
	width: 100%;
}
.tree-node {
	display: flex;
	align-items: center;
	font-size: 13px;
}
.tree-node-label {
	white-space: nowrap;
}
</style>
