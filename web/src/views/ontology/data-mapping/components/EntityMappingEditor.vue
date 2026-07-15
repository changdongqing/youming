<template>
	<div class="entity-mapping-editor" v-loading="loading">
		<template v-if="entityMapping">
			<el-tabs v-model="activeTab">
				<el-tab-pane label="基本配置" name="basic">
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
						<el-row :gutter="16">
							<el-col :span="8">
								<el-form-item label="数据源">
									<el-input :model-value="entityMapping.sourceId" disabled />
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="源Schema">
									<el-input :model-value="entityMapping.sourceSchema" disabled />
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="源对象">
									<el-input :model-value="entityMapping.sourceObject" disabled />
								</el-form-item>
							</el-col>
						</el-row>
						<el-row :gutter="16">
							<el-col :span="12">
								<el-form-item label="目标实体类型">
									<el-input-number v-model="form.targetEntityTypeId" :min="1" controls-position="right" style="width: 100%" />
								</el-form-item>
							</el-col>
							<el-col :span="12">
								<el-form-item label="命名空间ID">
									<el-input-number v-model="form.targetNamespaceId" :min="1" controls-position="right" style="width: 100%" />
								</el-form-item>
							</el-col>
						</el-row>

						<el-divider content-position="left">键与IRI</el-divider>
						<el-form-item label="键列配置">
							<el-input
								v-model="form.keyColumns"
								type="textarea"
								:rows="2"
								placeholder='JSON，如 [{"column":"user_id","order":1,"normalizer":"LONG"}]'
							/>
						</el-form-item>
						<el-form-item label="IRI模板" prop="iriTemplate">
							<el-input v-model="form.iriTemplate" placeholder="如 user-account/{user_id|url}" />
							<div class="iri-vars" v-if="keyColumnNames.length">
								<span class="iri-vars-label">可插入变量：</span>
								<el-button v-for="col in keyColumnNames" :key="col" size="small" link type="primary" @click="insertVariable(col)">{{
									col
								}}</el-button>
							</div>
						</el-form-item>
						<el-form-item label="IRI样例">
							<div class="iri-preview">
								<el-input
									v-model="iriSampleValues"
									placeholder="输入键值样例（逗号分隔）"
									size="small"
									style="width: 300px"
									@keyup.enter="renderIriPreview"
								/>
								<el-button size="small" type="primary" @click="renderIriPreview" :loading="iriPreviewLoading">预览</el-button>
								<el-text v-if="iriPreview" type="success" size="small" class="ml8">{{ iriPreview }}</el-text>
								<el-text v-if="iriPreviewError" type="danger" size="small" class="ml8">{{ iriPreviewError }}</el-text>
							</div>
						</el-form-item>
						<el-form-item label="标签模板">
							<el-input v-model="form.labelTemplate" placeholder="如 {name}（{username}）" />
						</el-form-item>

						<el-divider content-position="left">过滤与增量</el-divider>
						<el-form-item label="过滤条件">
							<FilterDslBuilder v-model="form.filterDsl" :readonly="readonly" @change="markDirty" />
						</el-form-item>
						<el-row :gutter="16">
							<el-col :span="8">
								<el-form-item label="增量列">
									<el-input v-model="form.incrementalColumn" placeholder="增量列名" />
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="增量类型">
									<el-select v-model="form.incrementalType" placeholder="选择" clearable style="width: 100%">
										<el-option label="时间戳" value="TIMESTAMP" />
										<el-option label="数值" value="NUMERIC" />
									</el-select>
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="同步顺序">
									<el-input-number v-model="form.syncOrder" :min="0" controls-position="right" style="width: 100%" />
								</el-form-item>
							</el-col>
						</el-row>

						<el-divider content-position="left">删除策略</el-divider>
						<el-row :gutter="16">
							<el-col :span="8">
								<el-form-item label="删除标记列">
									<el-input v-model="form.sourceDeleteFlagColumn" placeholder="删除标记列名" />
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="删除值">
									<el-input v-model="form.sourceDeleteValues" placeholder='JSON，如 ["1","true"]' />
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="删除策略">
									<el-select v-model="form.deleteStrategy" style="width: 100%">
										<el-option label="忽略" value="IGNORE" />
										<el-option label="标记失活" value="MARK_INACTIVE" />
										<el-option label="软删除" value="SOFT_DELETE" />
										<el-option label="阻断审查" value="BLOCK_AND_REVIEW" />
									</el-select>
								</el-form-item>
							</el-col>
						</el-row>
						<el-row :gutter="16">
							<el-col :span="8">
								<el-form-item label="冲突策略">
									<el-select v-model="form.conflictPolicy" style="width: 100%">
										<el-option label="源优先" value="SOURCE_WINS" />
										<el-option label="人工优先" value="MANUAL_WINS" />
										<el-option label="拒绝冲突" value="REJECT_CONFLICT" />
									</el-select>
								</el-form-item>
							</el-col>
							<el-col :span="8">
								<el-form-item label="启用">
									<el-switch v-model="form.enabled" active-value="1" inactive-value="0" />
								</el-form-item>
							</el-col>
						</el-row>

						<el-form-item>
							<el-button type="primary" @click="handleSave" :loading="saving" :disabled="readonly">保存</el-button>
						</el-form-item>
					</el-form>
				</el-tab-pane>
				<el-tab-pane label="字段映射" name="fields">
					<FieldMappingTable
						:entity-mapping-id="entityMapping.id"
						:available-properties="availableProperties"
						:source-columns="sourceColumns"
						:readonly="readonly"
						@change="markDirty"
					/>
				</el-tab-pane>
			</el-tabs>
		</template>
		<el-empty v-else description="请选择实体映射" :image-size="60" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingConfigApi } from '/@/api/ontology/data-mapping';
import type { EntityMappingVO, EntityMappingUpdateRequest } from '/@/types/ontology/data-mapping';
import { extractIriVariables, renderLocalName } from '../utils/iri-template';
import type { KeyColumnConfig } from '../utils/iri-template';
import { isRevisionConflict, getConflictMessage } from '../utils/designer-state';
import { useMappingDesignerStore } from '/@/stores/ontology/useMappingDesignerStore';
import FilterDslBuilder from './FilterDslBuilder.vue';
import FieldMappingTable from './FieldMappingTable.vue';

const props = defineProps<{
	entityMappingId?: number;
	availableProperties?: any[];
	sourceColumns?: string[];
	readonly?: boolean;
}>();
const emit = defineEmits<{ (e: 'change'): void }>();

const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const store = useMappingDesignerStore();

const loading = ref(false);
const saving = ref(false);
const activeTab = ref('basic');
const entityMapping = ref<EntityMappingVO | null>(null);
const formRef = ref();

const form = reactive({
	mappingCode: '',
	mappingName: '',
	targetEntityTypeId: undefined as number | undefined,
	targetNamespaceId: undefined as number | undefined,
	keyColumns: '',
	iriTemplate: '',
	labelTemplate: '',
	filterDsl: '',
	incrementalColumn: '',
	incrementalType: '',
	sourceDeleteFlagColumn: '',
	sourceDeleteValues: '',
	deleteStrategy: 'MARK_INACTIVE',
	conflictPolicy: 'SOURCE_WINS',
	syncOrder: 0,
	enabled: '1',
	description: '',
	revision: 0,
});

const rules = {
	mappingName: [{ required: true, message: '请输入映射名称', trigger: 'blur' }],
	iriTemplate: [{ required: true, message: '请输入IRI模板', trigger: 'blur' }],
};

// 键列名列表（用于IRI模板变量插入）
const keyColumnNames = computed<string[]>(() => {
	if (!form.keyColumns) return [];
	try {
		const configs: KeyColumnConfig[] = JSON.parse(form.keyColumns);
		return configs.map((c) => c.column);
	} catch {
		return [];
	}
});

// IRI 预览
const iriSampleValues = ref('');
const iriPreview = ref('');
const iriPreviewError = ref('');
const iriPreviewLoading = ref(false);

const insertVariable = (column: string) => {
	form.iriTemplate = form.iriTemplate + `{${column}}`;
};

const renderIriPreview = async () => {
	if (!entityMapping.value || !form.iriTemplate) return;
	iriPreviewLoading.value = true;
	iriPreview.value = '';
	iriPreviewError.value = '';

	// 构建样例值映射
	const sampleValues: Record<string, string> = {};
	const values = iriSampleValues.value.split(',').map((v) => v.trim());
	const cols = keyColumnNames.value;
	cols.forEach((col, idx) => {
		if (values[idx]) sampleValues[col] = values[idx];
	});

	try {
		const { data } = await mappingConfigApi.iriPreview(entityMapping.value.id, sampleValues);
		if (data.success) {
			iriPreview.value = data.fullIri;
		} else {
			iriPreviewError.value = data.errorMessage || '预览失败';
		}
	} catch (e: any) {
		// 后端调用失败时使用前端渲染
		iriPreview.value = renderLocalName(form.iriTemplate, sampleValues) || '(无法渲染)';
	} finally {
		iriPreviewLoading.value = false;
	}
};

const loadEntityMapping = async () => {
	if (!props.entityMappingId) return;
	loading.value = true;
	try {
		const { data } = await mappingConfigApi.getEntityMappingDetail(props.entityMappingId);
		entityMapping.value = data;
		form.mappingCode = data.mappingCode;
		form.mappingName = data.mappingName;
		form.targetEntityTypeId = data.targetEntityTypeId;
		form.targetNamespaceId = data.targetNamespaceId;
		form.keyColumns = data.keyColumns;
		form.iriTemplate = data.iriTemplate;
		form.labelTemplate = data.labelTemplate || '';
		form.filterDsl = data.filterDsl || '';
		form.incrementalColumn = data.incrementalColumn || '';
		form.incrementalType = data.incrementalType || '';
		form.sourceDeleteFlagColumn = data.sourceDeleteFlagColumn || '';
		form.sourceDeleteValues = data.sourceDeleteValues || '';
		form.deleteStrategy = data.deleteStrategy;
		form.conflictPolicy = data.conflictPolicy;
		form.syncOrder = data.syncOrder;
		form.enabled = data.enabled;
		form.description = data.description || '';
		form.revision = data.revision;
		store.markFieldMappingsDirty(data.id, false);
	} catch (e: any) {
		msgError(e.message || '获取实体映射详情失败');
	} finally {
		loading.value = false;
	}
};

const markDirty = () => {
	if (entityMapping.value) {
		store.markFieldMappingsDirty(entityMapping.value.id, true);
	}
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
		const payload: EntityMappingUpdateRequest = {
			id: entityMapping.value!.id,
			mappingName: form.mappingName,
			targetEntityTypeId: form.targetEntityTypeId,
			targetNamespaceId: form.targetNamespaceId,
			keyColumns: form.keyColumns,
			iriTemplate: form.iriTemplate,
			labelTemplate: form.labelTemplate || undefined,
			filterDsl: form.filterDsl || undefined,
			incrementalColumn: form.incrementalColumn || undefined,
			incrementalType: (form.incrementalType as 'TIMESTAMP' | 'NUMERIC') || undefined,
			sourceDeleteFlagColumn: form.sourceDeleteFlagColumn || undefined,
			sourceDeleteValues: form.sourceDeleteValues || undefined,
			deleteStrategy: form.deleteStrategy,
			conflictPolicy: form.conflictPolicy,
			syncOrder: form.syncOrder,
			enabled: form.enabled,
			description: form.description || undefined,
			revision: form.revision,
		};
		const { data } = await mappingConfigApi.updateEntityMapping(entityMapping.value!.id, payload);
		form.revision = data.revision;
		entityMapping.value = data;
		store.markFieldMappingsDirty(data.id, false);
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
	() => props.entityMappingId,
	() => {
		if (props.entityMappingId) loadEntityMapping();
	}
);

onMounted(() => {
	if (props.entityMappingId) loadEntityMapping();
});
</script>

<style scoped>
.entity-mapping-editor {
	padding: 8px;
}
.iri-vars {
	margin-top: 4px;
}
.iri-vars-label {
	font-size: 12px;
	color: var(--el-text-color-secondary);
}
.iri-preview {
	display: flex;
	align-items: center;
	gap: 4px;
}
.ml8 {
	margin-left: 8px;
}
</style>
