<template>
	<div class="panel-container">
		<div class="panel-title">
			<el-icon><Share /></el-icon> 本体模型
		</div>
		<el-scrollbar class="panel-content">
			<!-- 搜索 -->
			<el-input v-model="treeKeyword" clearable placeholder="搜索实体类型" prefix-icon="Search" size="small" class="mb8" />

			<!-- 实体类型树 -->
			<el-tree
				:key="treeKeyword.trim() || '__normal__'"
				:data="filteredTree"
				node-key="key"
				:props="treeProps"
				:expand-on-click-node="false"
				:highlight-current="true"
				:default-expand-all="Boolean(treeKeyword.trim())"
				@node-click="handleNodeClick"
			>
				<template #default="{ data }">
					<span class="tree-node">
						<span class="tree-node-label">{{ data.label }}（{{ data.name }}）</span>
						<el-tag v-if="data.isBuiltin === '1'" size="small" class="ml4">核心</el-tag>
						<el-tag v-if="data.isAbstract === '1'" size="small" type="warning" class="ml4">抽象</el-tag>
					</span>
				</template>
			</el-tree>

			<el-empty v-if="!treeLoading && filteredTree.length === 0" description="无实体类型" :image-size="40" />

			<!-- 选中实体的属性 -->
			<template v-if="selectedNodeType">
				<el-divider content-position="left">数据属性</el-divider>
				<el-table v-loading="propsLoading" :data="dataProperties" border size="small" style="width: 100%">
					<el-table-column prop="displayName" label="名称" min-width="120" show-overflow-tooltip>
						<template #default="{ row }">
							<el-icon v-if="needsLockIcon(row.dataProperty.valueSourceRef)" class="mr4"><Lock /></el-icon>
							{{ row.displayName }}
							<el-tag v-if="row.inherited" size="small" type="info" class="ml4">继承</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="类型" width="80">
						<template #default="{ row }">{{ row.dataProperty.baseType }}</template>
					</el-table-column>
					<el-table-column label="模式" width="90">
						<template #default="{ row }">{{ row.dataProperty.valueMode }}</template>
					</el-table-column>
					<el-table-column label="唯一" width="50">
						<template #default="{ row }">
							<el-tag v-if="row.dataProperty.isUnique === '1'" type="danger" size="small">是</el-tag>
						</template>
					</el-table-column>
				</el-table>

				<el-divider content-position="left">对象属性</el-divider>
				<el-table v-loading="propsLoading" :data="objectProperties" border size="small" style="width: 100%">
					<el-table-column prop="label" label="名称" min-width="120" show-overflow-tooltip>
						<template #default="{ row }">
							{{ row.label || row.objectProperty.name }}
							<el-tag v-if="row.inherited" size="small" type="info" class="ml4">继承</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="domain" min-width="100" show-overflow-tooltip>
						<template #default="{ row }">{{ row.domains?.map((d: any) => d.label || d.name).join(', ') }}</template>
					</el-table-column>
					<el-table-column label="range" min-width="100" show-overflow-tooltip>
						<template #default="{ row }">{{ row.ranges?.map((r: any) => r.label || r.name).join(', ') }}</template>
					</el-table-column>
				</el-table>
			</template>
		</el-scrollbar>
	</div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue';
import { Share, Search, Lock } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchDataPropertiesByDomain } from '/@/api/ontology/data-property';
import { fetchObjectPropertiesByDomain } from '/@/api/ontology/object-property';
import { filterEntityTypeTree } from '/@/views/ontology/entity-type/tree-utils';
import type { EntityTypeTreeNode } from '/@/types/ontology/entity-type';
import { needsLockIcon } from '../utils/security-display';

const props = defineProps<{ ontologyId?: string | number }>();
const emit = defineEmits<{ (e: 'select', entityTypeId: string): void }>();

const { error: msgError } = useMessage();

const treeLoading = ref(false);
const propsLoading = ref(false);
const treeData = ref<EntityTypeTreeNode[]>([]);
const treeKeyword = ref('');
const treeProps = { label: 'label', children: 'children' };

const filteredTree = computed(() => filterEntityTypeTree(treeData.value, treeKeyword.value));

const selectedNodeType = ref<EntityTypeTreeNode | null>(null);
const dataProperties = ref<any[]>([]);
const objectProperties = ref<any[]>([]);

const loadTree = async () => {
	treeLoading.value = true;
	try {
		const params = props.ontologyId ? { ontologyId: String(props.ontologyId) } : undefined;
		const { data } = await fetchEntityTypeTree(params);
		treeData.value = data || [];
	} catch (e: any) {
		msgError(e.message || '获取实体类型树失败');
	} finally {
		treeLoading.value = false;
	}
};

const handleNodeClick = async (node: EntityTypeTreeNode) => {
	selectedNodeType.value = node;
	emit('select', node.id);

	propsLoading.value = true;
	try {
		const [{ data: dp }, { data: op }] = await Promise.all([fetchDataPropertiesByDomain(node.id), fetchObjectPropertiesByDomain(node.id)]);
		dataProperties.value = dp || [];
		objectProperties.value = op || [];
	} catch (e: any) {
		msgError(e.message || '获取属性列表失败');
	} finally {
		propsLoading.value = false;
	}
};

watch(
	() => props.ontologyId,
	() => {
		loadTree();
	}
);

onMounted(() => {
	loadTree();
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
.tree-node {
	display: flex;
	align-items: center;
	font-size: 13px;
}
.tree-node-label {
	white-space: nowrap;
}
</style>
