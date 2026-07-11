<!--
	实例对象属性断言编辑字段。
	根据对象属性的功能性显示单选/多选；远程分页搜索可选实例（设计 §9.3 关系编辑器）。
	已存在的断言以标签展示并可删除；Schema 资源（ENTITY_TYPE）客体只读。
-->
<template>
	<div class="instance-relation-field">
		<!-- 已有断言标签列表 -->
		<div v-if="existingRelations.length > 0" class="existing-relations">
			<el-tag
				v-for="rel in existingRelations"
				:key="rel.id"
				:type="isSchemaResource(rel) ? 'warning' : 'info'"
				closable
				disable-transitions
				class="relation-tag"
				@close="emit('remove', rel.id)"
			>
				{{ formatRelationLabel(rel) }}
				<template v-if="isSchemaResource(rel)">（Schema资源）</template>
			</el-tag>
		</div>

		<!-- 远程搜索选择器 -->
		<el-select
			v-model="selectedOptionId"
			filterable
			remote
			clearable
			reserve-keyword
			:placeholder="searchPlaceholder"
			:remote-method="searchOptions"
			:loading="searchLoading"
			class="relation-select"
			@change="onSelect"
		>
			<el-option
				v-for="opt in optionList"
				:key="opt.id"
				:label="(opt.label || opt.iriLocalName) + '（' + (opt.rdfTypeName || '') + '）'"
				:value="opt.id"
			/>
			<template #footer>
				<el-text type="info" size="small">
					共 {{ optionTotal }} 条结果{{ hasMore ? '（仅显示前20条，请输入关键词缩小范围）' : '' }}
				</el-text>
			</template>
		</el-select>
	</div>
</template>

<script lang="ts" setup>
import { fetchInstanceOptions } from '/@/api/ontology/instance';
import { isFunctionalSelect, formatRelationLabel, isSchemaResourceRelation, collectRangeTypeIds } from './relation-editor';
import type { InstanceObjectRelationVO, ObjectPropertyMeta, InstanceOption, OntologyId } from '/@/types/ontology/instance';

const props = defineProps<{
	subjectId?: OntologyId;
	propMeta: ObjectPropertyMeta;
	existingRelations: InstanceObjectRelationVO[];
}>();

const emit = defineEmits<{
	(e: 'add', objectInstanceId: OntologyId): void;
	(e: 'remove', relationId: OntologyId): void;
}>();

const selectedOptionId = ref<OntologyId | undefined>(undefined);
const optionList = ref<InstanceOption[]>([]);
const optionTotal = ref(0);
const searchLoading = ref(false);
const hasMore = computed(() => optionTotal.value > optionList.value.length);

const isFunctional = computed(() => isFunctionalSelect(props.propMeta));
const rangeTypeIds = computed(() => collectRangeTypeIds(props.propMeta));

const searchPlaceholder = computed(() => {
	if (isFunctional.value && props.existingRelations.length > 0) {
		return '功能性属性已有断言，如需更换请先删除现有断言';
	}
	return '搜索实例（IRI本地名或标签）';
});

const isSchemaResource = (rel: InstanceObjectRelationVO) => isSchemaResourceRelation(rel);

const searchOptions = async (keyword: string) => {
	searchLoading.value = true;
	try {
		const response = await fetchInstanceOptions({
			keyword: keyword || undefined,
			objectPropertyId: props.propMeta.objectPropertyId,
			current: 1,
			size: 20,
		});
		const pageData = (response.data || {}) as { records?: InstanceOption[]; total?: number };
		optionList.value = pageData.records || [];
		optionTotal.value = pageData.total || 0;
	} catch {
		optionList.value = [];
		optionTotal.value = 0;
	} finally {
		searchLoading.value = false;
	}
};

const onSelect = (optionId: OntologyId) => {
	if (!optionId) return;
	// 功能性属性已有断言时，阻止添加（提示先删除）
	if (isFunctional.value && props.existingRelations.length > 0) {
		selectedOptionId.value = undefined;
		return;
	}
	emit('add', optionId);
	selectedOptionId.value = undefined;
	// 清空搜索结果，避免重复选择
	optionList.value = [];
	optionTotal.value = 0;
};

// 初始加载一次选项（首屏展示部分可选实例）
void searchOptions('');
</script>

<style scoped>
.instance-relation-field {
	width: 100%;
}
.existing-relations {
	display: flex;
	flex-wrap: wrap;
	gap: 4px;
	margin-bottom: 8px;
}
.relation-tag {
	max-width: 100%;
}
.relation-select {
	width: 100%;
}
</style>
