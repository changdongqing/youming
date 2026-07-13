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

		<!-- 关联结果中若有违规/警告，展示提示 -->
		<el-alert
			v-if="lastResult && !lastResult.validationReport.conforms"
			type="error"
			:closable="false"
			style="margin-top: 12px"
			:title="`关联完成：成功 ${lastResult.associatedCount} 个，跳过 ${lastResult.skippedCount} 个（含 VIOLATION 违规，被阻断的资源未关联）`"
		/>
		<el-alert
			v-else-if="lastResult && lastResult.validationReport.warningCount > 0"
			type="warning"
			:closable="false"
			style="margin-top: 12px"
			:title="`关联完成：成功 ${lastResult.associatedCount} 个，跳过 ${lastResult.skippedCount} 个（含 ${lastResult.validationReport.warningCount} 条 WARNING 提示）`"
		/>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" :loading="submitting" :disabled="selected.length === 0" @click="handleAssociate">
				关联选中（{{ selected.length }}）
			</el-button>
		</template>
	</el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useMessage } from '/@/hooks/message';
import { associateExtensionResources } from '/@/api/ontology/extension';
import { fetchEntityTypeList } from '/@/api/ontology/entity-type';
import { fetchDataPropertyList } from '/@/api/ontology/data-property';
import { fetchObjectPropertyList } from '/@/api/ontology/object-property';
import { fetchAxiomRuleList } from '/@/api/ontology/axiom-rule';
import { fetchUnitList } from '/@/api/ontology/unit';
import type { ExtensionModule, ResourceType, ExtensionAssociateResult } from '/@/types/ontology/extension';

const props = defineProps<{ visible: boolean; module: ExtensionModule | null }>();
const emit = defineEmits<{ 'update:visible': [value: boolean]; success: [] }>();
const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();

const visible = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});
const loading = ref(false);
const submitting = ref(false);
const resourceType = ref<ResourceType>('ENTITY_TYPE');
const availableResources = ref<Array<{ id: string; name: string; iri: string }>>([]);
const selected = ref<Array<{ id: string; name: string; iri: string }>>([]);
const lastResult = ref<ExtensionAssociateResult | null>(null);

const loadResources = async () => {
	if (!props.module) return;
	loading.value = true;
	selected.value = [];
	try {
		const nsId = props.module.namespaceId;
		switch (resourceType.value) {
			case 'ENTITY_TYPE':
			case 'DATA_PROPERTY':
			case 'OBJECT_PROPERTY': {
				// 实体类型/数据属性/对象属性均支持 namespaceId + isBuiltin 过滤
				const fetcher = resourceType.value === 'ENTITY_TYPE'
					? fetchEntityTypeList
					: resourceType.value === 'DATA_PROPERTY'
						? fetchDataPropertyList
						: fetchObjectPropertyList;
				const res = await fetcher({ isBuiltin: '0', namespaceId: nsId } as any);
				availableResources.value = (res.data || []).map((r: any) => ({ id: String(r.id), name: r.name, iri: r.iri }));
				break;
			}
			case 'AXIOM_RULE': {
				// 公理规则无 namespaceId 字段，仅按 isBuiltin 过滤
				const res = await fetchAxiomRuleList({ isBuiltin: '0' } as any);
				availableResources.value = (res.data || []).map((r: any) => ({
					id: String(r.id),
					name: r.name,
					iri: r.ruleCode, // 公理规则用 ruleCode 作为标识快照
				}));
				break;
			}
			case 'UNIT': {
				// 单位支持 namespaceId 过滤（query 为 any 类型）
				const res = await fetchUnitList({ isBuiltin: '0', namespaceId: nsId });
				availableResources.value = (res.data || []).map((r: any) => ({
					id: String(r.id),
					name: r.unitName,
					iri: r.unitCode, // 单位用 unitCode 作为标识快照
				}));
				break;
			}
			default:
				availableResources.value = [];
		}
	} catch {
		availableResources.value = [];
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
		const res = await associateExtensionResources(props.module.id, {
			resources: selected.value.map((r) => ({
				resourceType: resourceType.value,
				resourceId: r.id,
			})),
		});
		const result = res.data as ExtensionAssociateResult;
		lastResult.value = result;
		if (!result.validationReport.conforms) {
			msgWarning(`成功关联 ${result.associatedCount} 个，跳过 ${result.skippedCount} 个（含违规，已阻断）`);
		} else if (result.validationReport.warningCount > 0) {
			msgWarning(`成功关联 ${result.associatedCount} 个，跳过 ${result.skippedCount} 个（含 ${result.validationReport.warningCount} 条警告）`);
		} else {
			msgSuccess(`已关联 ${result.associatedCount} 个资源`);
		}
		emit('success');
		if (result.associatedCount > 0) {
			// 有成功关联才关闭抽屉；若全部被阻断则保留抽屉供用户查看校验提示
			visible.value = false;
		}
	} catch {
		msgError('关联失败');
	} finally {
		submitting.value = false;
	}
};

// 抽屉打开时加载资源；切换 module 时重新加载
watch(() => props.module, (val) => {
	if (val) {
		lastResult.value = null;
		loadResources();
	}
}, { immediate: true });
</script>
