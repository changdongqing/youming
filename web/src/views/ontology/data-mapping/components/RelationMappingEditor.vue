<template>
	<div class="relation-mapping-editor" v-loading="loading">
		<template v-if="relationMapping">
			<el-form ref="formRef" :model="form" :rules="rules" label-width="140px" :disabled="readonly">
				<el-row :gutter="16">
					<el-col :span="12">
						<el-form-item label="映射编码">
							<el-input v-model="form.mappingCode" disabled />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="映射名称" prop="mappingName">
							<el-input v-model="form.mappingName" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-form-item label="关系模式">
					<el-tag type="info">{{ relationMapping.relationMode }}</el-tag>
					<el-text type="warning" size="small" class="ml8">关系模式不可修改，需删除后重建</el-text>
				</el-form-item>
				<el-row :gutter="16">
					<el-col :span="8">
						<el-form-item label="数据源">
							<el-input :model-value="relationMapping.sourceId" disabled />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="源Schema">
							<el-input :model-value="relationMapping.sourceSchema" disabled />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="源对象">
							<el-input :model-value="relationMapping.sourceObject" disabled />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="16">
					<el-col :span="12">
						<el-form-item label="主体实体映射ID">
							<el-input-number v-model="form.subjectEntityMappingId" :min="1" controls-position="right" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="12">
						<el-form-item label="客体实体映射ID">
							<el-input-number v-model="form.objectEntityMappingId" :min="1" controls-position="right" style="width: 100%" />
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="16">
					<el-col :span="8">
						<el-form-item label="对象属性ID" prop="objectPropertyId">
							<el-input-number v-model="form.objectPropertyId" :min="1" controls-position="right" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="缺失目标策略">
							<el-select v-model="form.missingTargetPolicy" style="width: 100%">
								<el-option label="挂起待解析" value="PENDING" />
								<el-option label="跳过" value="SKIP" />
								<el-option label="记录失败" value="FAIL_RECORD" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="删除策略">
							<el-select v-model="form.deleteStrategy" style="width: 100%">
								<el-option label="移除断言" value="REMOVE_ASSERTION" />
								<el-option label="保留断言" value="KEEP_ASSERTION" />
								<el-option label="阻断审查" value="BLOCK_AND_REVIEW" />
							</el-select>
						</el-form-item>
					</el-col>
				</el-row>
				<el-row :gutter="16">
					<el-col :span="8">
						<el-form-item label="所有权策略">
							<el-select v-model="form.ownershipPolicy" style="width: 100%">
								<el-option label="源优先" value="SOURCE_WINS" />
								<el-option label="人工优先" value="MANUAL_WINS" />
								<el-option label="拒绝冲突" value="REJECT_CONFLICT" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="同步顺序">
							<el-input-number v-model="form.syncOrder" :min="0" controls-position="right" style="width: 100%" />
						</el-form-item>
					</el-col>
					<el-col :span="8">
						<el-form-item label="启用">
							<el-switch v-model="form.enabled" active-value="1" inactive-value="0" />
						</el-form-item>
					</el-col>
				</el-row>

				<el-divider content-position="left">键映射</el-divider>
				<el-form-item label="主体键映射">
					<el-input v-model="form.subjectKeyMapping" type="textarea" :rows="2" />
				</el-form-item>
				<el-form-item label="客体键映射">
					<el-input v-model="form.objectKeyMapping" type="textarea" :rows="2" />
				</el-form-item>
				<el-form-item label="关系键列">
					<el-input v-model="form.relationKeyColumns" type="textarea" :rows="2" />
				</el-form-item>

				<el-divider content-position="left">过滤条件</el-divider>
				<el-form-item label="过滤DSL">
					<FilterDslBuilder v-model="form.filterDsl" :readonly="readonly" @change="markDirty" />
				</el-form-item>

				<el-form-item>
					<el-button type="primary" @click="handleSave" :loading="saving" :disabled="readonly">保存</el-button>
				</el-form-item>
			</el-form>
		</template>
		<el-empty v-else description="请选择关系映射" :image-size="60" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingConfigApi } from '/@/api/ontology/data-mapping';
import type { RelationMappingVO, RelationMappingUpdateRequest } from '/@/types/ontology/data-mapping';
import { isRevisionConflict, getConflictMessage } from '../utils/designer-state';
import { useMappingDesignerStore } from '/@/stores/ontology/useMappingDesignerStore';
import FilterDslBuilder from './FilterDslBuilder.vue';

const props = defineProps<{ relationMappingId?: number; readonly?: boolean }>();
const emit = defineEmits<{ (e: 'change'): void }>();

const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const store = useMappingDesignerStore();

const loading = ref(false);
const saving = ref(false);
const relationMapping = ref<RelationMappingVO | null>(null);
const formRef = ref();

const form = reactive({
	mappingCode: '',
	mappingName: '',
	relationMode: '',
	objectPropertyId: undefined as number | undefined,
	subjectEntityMappingId: undefined as number | undefined,
	objectEntityMappingId: undefined as number | undefined,
	subjectKeyMapping: '',
	objectKeyMapping: '',
	relationKeyColumns: '',
	filterDsl: '',
	missingTargetPolicy: 'PENDING',
	deleteStrategy: 'REMOVE_ASSERTION',
	ownershipPolicy: 'SOURCE_WINS',
	syncOrder: 1000,
	enabled: '1' as '0' | '1',
	description: '',
	revision: 0,
});

const rules = {
	mappingName: [{ required: true, message: '请输入映射名称', trigger: 'blur' }],
	objectPropertyId: [{ required: true, message: '请输入对象属性ID', trigger: 'blur' }],
};

const loadRelationMapping = async () => {
	if (!props.relationMappingId) return;
	loading.value = true;
	try {
		const { data } = await mappingConfigApi.getRelationMappingDetail(props.relationMappingId);
		relationMapping.value = data;
		form.mappingCode = data.mappingCode;
		form.mappingName = data.mappingName;
		form.relationMode = data.relationMode;
		form.objectPropertyId = data.objectPropertyId;
		form.subjectEntityMappingId = data.subjectEntityMappingId;
		form.objectEntityMappingId = data.objectEntityMappingId;
		form.subjectKeyMapping = data.subjectKeyMapping;
		form.objectKeyMapping = data.objectKeyMapping;
		form.relationKeyColumns = data.relationKeyColumns;
		form.filterDsl = data.filterDsl || '';
		form.missingTargetPolicy = data.missingTargetPolicy;
		form.deleteStrategy = data.deleteStrategy;
		form.ownershipPolicy = data.ownershipPolicy;
		form.syncOrder = data.syncOrder;
		form.enabled = data.enabled;
		form.description = data.description || '';
		form.revision = data.revision;
	} catch (e: any) {
		msgError(e.message || '获取关系映射详情失败');
	} finally {
		loading.value = false;
	}
};

const markDirty = () => {
	emit('change');
};

const handleSave = async () => {
	try {
		await formRef.value?.validate();
	} catch {
		return;
	}
	saving.value = true;
	try {
		const payload: RelationMappingUpdateRequest = {
			id: relationMapping.value!.id,
			mappingName: form.mappingName,
			objectPropertyId: form.objectPropertyId,
			subjectEntityMappingId: form.subjectEntityMappingId,
			objectEntityMappingId: form.objectEntityMappingId,
			subjectKeyMapping: form.subjectKeyMapping || undefined,
			objectKeyMapping: form.objectKeyMapping || undefined,
			relationKeyColumns: form.relationKeyColumns || undefined,
			filterDsl: form.filterDsl || undefined,
			missingTargetPolicy: form.missingTargetPolicy,
			deleteStrategy: form.deleteStrategy,
			ownershipPolicy: form.ownershipPolicy,
			syncOrder: form.syncOrder,
			enabled: form.enabled,
			description: form.description || undefined,
			revision: form.revision,
		};
		const { data } = await mappingConfigApi.updateRelationMapping(relationMapping.value!.id, payload);
		form.revision = data.revision;
		relationMapping.value = data;
		store.markRelationMappingsDirty(false);
		msgSuccess('保存成功');
	} catch (e: any) {
		if (isRevisionConflict(e?.response?.status || e?.status)) {
			msgWarning(getConflictMessage());
		} else {
			msgError(e.message || '保存失败');
		}
	} finally {
		saving.value = false;
	}
};

watch(
	() => props.relationMappingId,
	() => {
		if (props.relationMappingId) loadRelationMapping();
	}
);

onMounted(() => {
	if (props.relationMappingId) loadRelationMapping();
});
</script>

<style scoped>
.relation-mapping-editor {
	padding: 8px;
}
.ml8 {
	margin-left: 8px;
}
</style>
