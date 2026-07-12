<template>
	<div style="margin-top: 16px">
		<div style="font-weight: bold; margin-bottom: 8px">关联资源列表</div>
		<el-table :data="resources" v-loading="loading" border size="small" style="width: 100%">
			<el-table-column label="类型" width="120">
				<template #default="{ row }">
					<el-tag size="small">{{ typeLabel(row.resourceType) }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="resourceName" label="名称" width="140" show-overflow-tooltip />
			<el-table-column prop="resourceIri" label="IRI" show-overflow-tooltip />
			<el-table-column label="操作" width="80" fixed="right">
				<template #default="{ row }">
					<el-button link type="danger" size="small" @click="handleRemove(row)">解除</el-button>
				</template>
			</el-table-column>
		</el-table>
		<el-empty v-if="!loading && resources.length === 0" description="暂无关联资源" :image-size="60" />
	</div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchExtensionResources } from '/@/api/ontology/extension';
import type { ExtensionResource } from '/@/types/ontology/extension';

const props = defineProps<{ moduleId: string; refreshKey: number }>();
const emit = defineEmits<{ remove: [resourceId: string, resourceType: string] }>();

const loading = ref(false);
const resources = ref<ExtensionResource[]>([]);

const typeLabel = (type: string) => {
	const map: Record<string, string> = {
		ENTITY_TYPE: '实体类型',
		DATA_PROPERTY: '数据属性',
		OBJECT_PROPERTY: '对象属性',
		AXIOM_RULE: '公理规则',
		UNIT: '单位',
	};
	return map[type] || type;
};

const load = async () => {
	loading.value = true;
	try {
		const res = await fetchExtensionResources(props.moduleId);
		resources.value = res.data || [];
	} finally {
		loading.value = false;
	}
};

const handleRemove = (row: ExtensionResource) => {
	emit('remove', String(row.resourceId), row.resourceType);
};

watch(() => props.refreshKey, () => load(), { immediate: true });
</script>
