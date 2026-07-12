<template>
	<el-drawer v-model="visible" title="关联资源" size="600px" :close-on-click-modal="false">
		<el-alert type="info" :closable="false" style="margin-bottom: 16px">
			仅可选择扩展资源（is_builtin=0），且资源命名空间须与模块绑定的命名空间一致。
		</el-alert>

		<el-form :inline="true" style="margin-bottom: 12px">
			<el-form-item label="资源类型">
				<el-select v-model="resourceType" placeholder="选择类型" @change="loadResources" style="width: 160px">
					<el-option label="实体类型" value="ENTITY_TYPE" />
					<el-option label="数据属性" value="DATA_PROPERTY" />
					<el-option label="对象属性" value="OBJECT_PROPERTY" />
					<el-option label="公理规则" value="AXIOM_RULE" />
					<el-option label="单位条目" value="UNIT" />
				</el-select>
			</el-form-item>
		</el-form>

		<el-table :data="availableResources" v-loading="loading" border @selection-change="handleSelectionChange" style="width: 100%">
			<el-table-column type="selection" width="40" />
			<el-table-column prop="name" label="名称" width="160" show-overflow-tooltip />
			<el-table-column prop="iri" label="IRI" show-overflow-tooltip />
		</el-table>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" :loading="submitting" :disabled="selected.length === 0" @click="handleAssociate">
				关联选中（{{ selected.length }}）
			</el-button>
		</template>
	</el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useMessage } from '/@/hooks/message';
import { associateExtensionResources } from '/@/api/ontology/extension';
import { fetchEntityTypeList } from '/@/api/ontology/entity-type';
import type { ExtensionModule } from '/@/types/ontology/extension';

const props = defineProps<{ module: ExtensionModule | null }>();
const emit = defineEmits<{ 'update:visible': [value: boolean]; success: [] }>();
const { success: msgSuccess, error: msgError } = useMessage();

const visible = computed({ get: () => true, set: (v) => emit('update:visible', v) });
const loading = ref(false);
const submitting = ref(false);
const resourceType = ref('ENTITY_TYPE');
const availableResources = ref<Array<{ id: string; name: string; iri: string }>>([]);
const selected = ref<Array<{ id: string; name: string; iri: string }>>([]);

const loadResources = async () => {
	if (!props.module) return;
	loading.value = true;
	try {
		// 首期仅实现实体类型选择器，其他类型后续扩展
		if (resourceType.value === 'ENTITY_TYPE') {
			const res = await fetchEntityTypeList({ isBuiltin: '0', namespaceId: props.module.namespaceId });
			availableResources.value = (res.data || []).map((r: any) => ({ id: r.id, name: r.name, iri: r.iri }));
		} else {
			availableResources.value = [];
		}
	} finally {
		loading.value = false;
	}
};

const handleSelectionChange = (val: any[]) => {
	selected.value = val;
};

const handleAssociate = async () => {
	if (!props.module) return;
	submitting.value = true;
	try {
		await associateExtensionResources(props.module.id, {
			resources: selected.value.map((r) => ({
				resourceType: resourceType.value as any,
				resourceId: r.id,
			})),
		});
		msgSuccess(`已关联 ${selected.value.length} 个资源`);
		emit('success');
		visible.value = false;
	} catch {
		msgError('关联失败');
	} finally {
		submitting.value = false;
	}
};

loadResources();
</script>
