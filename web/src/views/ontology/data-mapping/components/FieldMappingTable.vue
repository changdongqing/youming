<template>
	<div>
		<div class="mb8" v-if="!readonly">
			<el-button type="primary" size="small" @click="handleAdd" :disabled="!canEdit">
				<el-icon><Plus /></el-icon> 添加字段映射
			</el-button>
		</div>

		<el-table v-loading="loading" :data="tableData" border size="small" style="width: 100%" row-key="id">
			<el-table-column label="启用" width="55">
				<template #default="{ row }">
					<el-switch v-model="row.enabled" active-value="1" inactive-value="0" :disabled="readonly" @change="handleRowChange(row)" />
				</template>
			</el-table-column>
			<el-table-column prop="fieldMappingCode" label="映射编码" width="120" show-overflow-tooltip />
			<el-table-column label="来源" width="90">
				<template #default="{ row }">{{ sourceKindLabel(row.sourceKind) }}</template>
			</el-table-column>
			<el-table-column label="源列/常量" width="140">
				<template #default="{ row }">
					<el-input
						v-if="row.sourceKind === 'CONSTANT'"
						v-model="row.constantValue"
						placeholder="常量值"
						size="small"
						:disabled="readonly"
						@change="handleRowChange(row)"
					/>
					<el-text v-else size="small">{{ row.sourceColumn || '-' }}</el-text>
				</template>
			</el-table-column>
			<el-table-column label="转换器" width="120">
				<template #default="{ row }">
					<el-select v-model="row.transformer" placeholder="IDENTITY" size="small" :disabled="readonly" @change="handleRowChange(row)">
						<el-option v-for="t in transformers" :key="t.code" :label="t.description || t.code" :value="t.code" />
					</el-select>
				</template>
			</el-table-column>
			<el-table-column label="目标属性" width="140">
				<template #default="{ row }">
					<el-text size="small">{{ getPropertyName(row.targetDataPropertyId) }}</el-text>
				</template>
			</el-table-column>
			<el-table-column label="空值策略" width="110">
				<template #default="{ row }">
					<el-select v-model="row.nullHandling" size="small" :disabled="readonly" @change="handleRowChange(row)">
						<el-option label="跳过" value="SKIP_NULL" />
						<el-option label="默认值" value="USE_DEFAULT" />
						<el-option label="拒绝" value="REJECT_NULL" />
					</el-select>
				</template>
			</el-table-column>
			<el-table-column label="所有权" width="110">
				<template #default="{ row }">
					<el-select v-model="row.ownershipPolicy" size="small" :disabled="readonly" @change="handleRowChange(row)">
						<el-option label="源优先" value="SOURCE_WINS" />
						<el-option label="人工优先" value="MANUAL_WINS" />
						<el-option label="拒绝冲突" value="REJECT_CONFLICT" />
					</el-select>
				</template>
			</el-table-column>
			<el-table-column prop="sortOrder" label="排序" width="60" />
			<el-table-column label="操作" width="100" fixed="right">
				<template #default="{ row }">
					<el-button link type="primary" size="small" @click="handleEdit(row)" v-if="!readonly">编辑</el-button>
					<el-popconfirm title="确认删除？" @confirm="handleDelete(row)" v-if="!readonly">
						<template #reference>
							<el-button link type="danger" size="small">删除</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<!-- 字段映射编辑弹窗 -->
		<el-dialog
			v-model="editDialog.visible"
			:title="editDialog.isEdit ? '编辑字段映射' : '新增字段映射'"
			width="640px"
			:close-on-click-modal="false"
			draggable
			destroy-on-close
		>
			<el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="120px" v-loading="editDialog.loading">
				<el-form-item label="映射编码" prop="fieldMappingCode">
					<el-input v-model="editForm.fieldMappingCode" :disabled="editDialog.isEdit" placeholder="字母开头，3-64位" />
				</el-form-item>
				<el-form-item label="映射名称">
					<el-input v-model="editForm.fieldMappingName" placeholder="可选" />
				</el-form-item>
				<el-form-item label="目标属性" prop="targetDataPropertyId">
					<el-select
						v-model="editForm.targetDataPropertyId"
						placeholder="选择目标数据属性"
						filterable
						style="width: 100%"
						@change="handleTargetPropertyChange"
					>
						<el-option
							v-for="p in availableProperties"
							:key="p.dataProperty.id"
							:label="`${p.displayName} (${p.dataProperty.baseType})`"
							:value="Number(p.dataProperty.id)"
						/>
					</el-select>
				</el-form-item>
				<el-form-item label="来源类型" prop="sourceKind">
					<el-radio-group v-model="editForm.sourceKind">
						<el-radio value="COLUMN">源列</el-radio>
						<el-radio value="CONSTANT">常量</el-radio>
					</el-radio-group>
				</el-form-item>
				<el-form-item label="源列" prop="sourceColumn" v-if="editForm.sourceKind === 'COLUMN'">
					<el-select v-model="editForm.sourceColumn" placeholder="选择源列" filterable style="width: 100%">
						<el-option v-for="col in sourceColumns" :key="col" :label="col" :value="col" :disabled="isForbiddenColumn(col)" />
					</el-select>
				</el-form-item>
				<el-form-item label="常量值" prop="constantValue" v-if="editForm.sourceKind === 'CONSTANT'">
					<el-input v-model="editForm.constantValue" placeholder="常量值" />
				</el-form-item>
				<el-form-item label="常量类型" v-if="editForm.sourceKind === 'CONSTANT'">
					<el-select v-model="editForm.constantLiteralType" placeholder="选择类型" style="width: 100%">
						<el-option label="字符串" value="STRING" />
						<el-option label="整数" value="INTEGER" />
						<el-option label="小数" value="DECIMAL" />
						<el-option label="布尔" value="BOOLEAN" />
						<el-option label="日期" value="DATE" />
						<el-option label="URI" value="URI" />
					</el-select>
				</el-form-item>
				<el-form-item label="转换器">
					<el-select v-model="editForm.transformer" placeholder="IDENTITY" style="width: 100%">
						<el-option v-for="t in filteredTransformers" :key="t.code" :label="t.description || t.code" :value="t.code" />
					</el-select>
					<el-button size="small" link type="primary" @click="handleEnumConfig" v-if="editForm.transformer === 'CLOSED_ENUM'">配置枚举映射</el-button>
				</el-form-item>
				<el-form-item label="空值处理">
					<el-select v-model="editForm.nullHandling" style="width: 100%">
						<el-option label="跳过空值" value="SKIP_NULL" />
						<el-option label="使用默认值" value="USE_DEFAULT" />
						<el-option label="拒绝空值" value="REJECT_NULL" />
					</el-select>
				</el-form-item>
				<el-form-item label="默认值" v-if="editForm.nullHandling === 'USE_DEFAULT'">
					<el-input v-model="editForm.defaultValue" placeholder="默认值" />
				</el-form-item>
				<el-form-item label="所有权策略">
					<el-select v-model="editForm.ownershipPolicy" style="width: 100%">
						<el-option label="源优先" value="SOURCE_WINS" />
						<el-option label="人工优先" value="MANUAL_WINS" />
						<el-option label="拒绝冲突" value="REJECT_CONFLICT" />
					</el-select>
				</el-form-item>
				<el-form-item label="排序">
					<el-input-number v-model="editForm.sortOrder" :min="0" controls-position="right" />
				</el-form-item>
				<el-form-item label="启用">
					<el-switch v-model="editForm.enabled" active-value="1" inactive-value="0" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="editDialog.visible = false">取消</el-button>
				<el-button type="primary" @click="handleSaveField" :loading="editDialog.loading">保存</el-button>
			</template>
		</el-dialog>

		<EnumTransformDialog ref="enumDialogRef" @confirm="(params) => (editForm.transformerParams = params)" />
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { mappingConfigApi } from '/@/api/ontology/data-mapping';
import type { FieldMappingVO, FieldMappingCreateRequest, FieldMappingUpdateRequest, TransformerInfo } from '/@/types/ontology/data-mapping';
import { sourceKindLabel } from '../utils/mapping-status';
import { isForbiddenColumn } from '../utils/security-display';
import EnumTransformDialog from './EnumTransformDialog.vue';

const props = defineProps<{
	entityMappingId: number;
	availableProperties?: any[];
	sourceColumns?: string[];
	readonly?: boolean;
}>();
const emit = defineEmits<{
	(e: 'change'): void;
}>();

const { success: msgSuccess, error: msgError } = useMessage();

const loading = ref(false);
const tableData = ref<FieldMappingVO[]>([]);
const transformers = ref<TransformerInfo[]>([]);
const enumDialogRef = ref();

const availableProperties = computed(() => props.availableProperties || []);
const sourceColumns = computed(() => props.sourceColumns || []);

const canEdit = computed(() => !props.readonly);

const getPropertyName = (id: number): string => {
	const prop = availableProperties.value.find((p) => Number(p.dataProperty.id) === id);
	return prop?.displayName || String(id);
};

const filteredTransformers = computed(() => {
	// 根据选中的目标属性类型过滤转换器（简化版：返回全部）
	return transformers.value;
});

const loadFieldMappings = async () => {
	if (!props.entityMappingId) return;
	loading.value = true;
	try {
		const { data } = await mappingConfigApi.listFieldMappings(props.entityMappingId);
		tableData.value = data || [];
	} catch (e: any) {
		msgError(e.message || '获取字段映射列表失败');
	} finally {
		loading.value = false;
	}
};

const loadTransformers = async () => {
	try {
		const { data } = await mappingConfigApi.listTransformers();
		transformers.value = data || [];
	} catch {
		// 静默失败
	}
};

// 行内变更标记
const handleRowChange = (_row: FieldMappingVO) => {
	emit('change');
};

// 编辑弹窗
const editDialogRef = ref();
const editFormRef = ref();
const editDialog = reactive({ visible: false, isEdit: false, loading: false });
const editForm = reactive({
	id: undefined as number | undefined,
	fieldMappingCode: '',
	fieldMappingName: '',
	targetDataPropertyId: undefined as number | undefined,
	sourceColumn: '',
	sourceKind: 'COLUMN' as 'COLUMN' | 'CONSTANT',
	constantValue: '',
	constantLiteralType: 'STRING',
	constantUnitId: undefined as number | undefined,
	transformer: 'IDENTITY',
	transformerParams: '',
	nullHandling: 'SKIP_NULL',
	defaultValue: '',
	defaultLiteralType: '',
	multiValueStrategy: 'SINGLE',
	unitId: undefined as number | undefined,
	ownershipPolicy: 'SOURCE_WINS',
	sortOrder: 0,
	enabled: '1',
	description: '',
});

const editRules = {
	fieldMappingCode: [
		{ required: true, message: '请输入映射编码', trigger: 'blur' },
		{ pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,63}$/, message: '字母开头，3-64位', trigger: 'blur' },
	],
	targetDataPropertyId: [{ required: true, message: '请选择目标属性', trigger: 'change' }],
	sourceKind: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
};

const handleAdd = () => {
	editDialog.isEdit = false;
	editDialog.visible = true;
	Object.assign(editForm, {
		id: undefined,
		fieldMappingCode: '',
		fieldMappingName: '',
		targetDataPropertyId: undefined,
		sourceColumn: '',
		sourceKind: 'COLUMN',
		constantValue: '',
		constantLiteralType: 'STRING',
		constantUnitId: undefined,
		transformer: 'IDENTITY',
		transformerParams: '',
		nullHandling: 'SKIP_NULL',
		defaultValue: '',
		defaultLiteralType: '',
		multiValueStrategy: 'SINGLE',
		unitId: undefined,
		ownershipPolicy: 'SOURCE_WINS',
		sortOrder: tableData.value.length,
		enabled: '1',
		description: '',
	});
};

const handleEdit = (row: FieldMappingVO) => {
	editDialog.isEdit = true;
	editDialog.visible = true;
	Object.assign(editForm, {
		id: row.id,
		fieldMappingCode: row.fieldMappingCode,
		fieldMappingName: row.fieldMappingName || '',
		targetDataPropertyId: row.targetDataPropertyId,
		sourceColumn: row.sourceColumn || '',
		sourceKind: row.sourceKind,
		constantValue: row.constantValue || '',
		constantLiteralType: row.constantLiteralType || 'STRING',
		constantUnitId: row.constantUnitId,
		transformer: row.transformer || 'IDENTITY',
		transformerParams: row.transformerParams || '',
		nullHandling: row.nullHandling,
		defaultValue: row.defaultValue || '',
		defaultLiteralType: row.defaultLiteralType || '',
		multiValueStrategy: row.multiValueStrategy,
		unitId: row.unitId,
		ownershipPolicy: row.ownershipPolicy,
		sortOrder: row.sortOrder,
		enabled: row.enabled,
		description: row.description || '',
	});
};

const handleTargetPropertyChange = () => {
	// 选择目标属性后可过滤转换器（V1简化）
};

const handleEnumConfig = () => {
	enumDialogRef.value?.open(editForm.transformerParams);
};

const handleSaveField = async () => {
	try {
		await editFormRef.value?.validate();
	} catch {
		return;
	}
	editDialog.loading = true;
	try {
		if (editDialog.isEdit) {
			const payload: FieldMappingUpdateRequest = {
				id: editForm.id!,
				fieldMappingName: editForm.fieldMappingName || undefined,
				targetDataPropertyId: editForm.targetDataPropertyId,
				sourceColumn: editForm.sourceColumn || undefined,
				sourceKind: editForm.sourceKind,
				constantValue: editForm.constantValue || undefined,
				constantLiteralType: editForm.constantLiteralType || undefined,
				constantUnitId: editForm.constantUnitId,
				transformer: editForm.transformer || undefined,
				transformerParams: editForm.transformerParams || undefined,
				nullHandling: editForm.nullHandling,
				defaultValue: editForm.defaultValue || undefined,
				defaultLiteralType: editForm.defaultLiteralType || undefined,
				multiValueStrategy: editForm.multiValueStrategy,
				unitId: editForm.unitId,
				ownershipPolicy: editForm.ownershipPolicy,
				sortOrder: editForm.sortOrder,
				enabled: editForm.enabled,
				description: editForm.description || undefined,
			};
			await mappingConfigApi.updateFieldMapping(editForm.id!, payload);
			msgSuccess('字段映射更新成功');
		} else {
			const payload: FieldMappingCreateRequest = {
				fieldMappingCode: editForm.fieldMappingCode,
				fieldMappingName: editForm.fieldMappingName || undefined,
				targetDataPropertyId: editForm.targetDataPropertyId!,
				sourceColumn: editForm.sourceColumn || undefined,
				sourceKind: editForm.sourceKind,
				constantValue: editForm.constantValue || undefined,
				constantLiteralType: editForm.constantLiteralType || undefined,
				constantUnitId: editForm.constantUnitId,
				transformer: editForm.transformer || undefined,
				transformerParams: editForm.transformerParams || undefined,
				nullHandling: editForm.nullHandling,
				defaultValue: editForm.defaultValue || undefined,
				defaultLiteralType: editForm.defaultLiteralType || undefined,
				multiValueStrategy: editForm.multiValueStrategy,
				unitId: editForm.unitId,
				ownershipPolicy: editForm.ownershipPolicy,
				sortOrder: editForm.sortOrder,
				enabled: editForm.enabled,
				description: editForm.description || undefined,
			};
			await mappingConfigApi.createFieldMapping(props.entityMappingId, payload);
			msgSuccess('字段映射创建成功');
		}
		editDialog.visible = false;
		loadFieldMappings();
		emit('change');
	} catch (e: any) {
		msgError(e.message || '保存失败');
	} finally {
		editDialog.loading = false;
	}
};

const handleDelete = async (row: FieldMappingVO) => {
	try {
		await mappingConfigApi.deleteFieldMapping(row.id);
		msgSuccess('删除成功');
		loadFieldMappings();
		emit('change');
	} catch (e: any) {
		msgError(e.message || '删除失败');
	}
};

watch(
	() => props.entityMappingId,
	() => {
		if (props.entityMappingId) loadFieldMappings();
	}
);

onMounted(() => {
	loadTransformers();
	if (props.entityMappingId) loadFieldMappings();
});
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
</style>
