<template>
	<el-dialog v-model="visible" title="新建关系映射" width="800px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-steps :active="currentStep" finish-status="success" class="mb16">
			<el-step title="关系模式" />
			<el-step title="源对象" />
			<el-step title="主体映射" />
			<el-step title="客体映射" />
			<el-step title="对象属性" />
			<el-step title="策略" />
			<el-step title="预览" />
		</el-steps>

		<div v-loading="loading">
			<!-- 步骤1：关系模式 -->
			<div v-show="currentStep === 0">
				<el-form label-width="120px">
					<el-form-item label="关系模式" required>
						<el-radio-group v-model="form.relationMode">
							<el-radio value="FOREIGN_KEY">外键关联</el-radio>
							<el-radio value="SELF_REFERENCE">自引用</el-radio>
							<el-radio value="JOIN_TABLE">关联表</el-radio>
						</el-radio-group>
					</el-form-item>
					<el-form-item label="映射编码" required>
						<el-input v-model="form.mappingCode" placeholder="字母开头，3-64位" />
					</el-form-item>
					<el-form-item label="映射名称" required>
						<el-input v-model="form.mappingName" placeholder="关系映射名称" />
					</el-form-item>
				</el-form>
				<el-alert v-if="form.relationMode === 'SELF_REFERENCE'" type="info" :closable="false" show-icon>
					<template #title>自引用模式：主体和客体映射相同</template>
				</el-alert>
				<el-alert v-if="form.relationMode === 'JOIN_TABLE'" type="warning" :closable="false" show-icon>
					<template #title>关联表模式：需要配置 relationKeyColumns</template>
				</el-alert>
			</div>

			<!-- 步骤2：源对象 -->
			<div v-show="currentStep === 1">
				<el-form label-width="120px">
					<el-form-item label="数据源ID" required>
						<el-input-number v-model="form.sourceId" :min="1" controls-position="right" style="width: 100%" />
					</el-form-item>
					<el-form-item label="源Schema" required>
						<el-input v-model="form.sourceSchema" placeholder="如 public" />
					</el-form-item>
					<el-form-item label="源对象" required>
						<el-input v-model="form.sourceObject" placeholder="如 sys_user_role" />
					</el-form-item>
				</el-form>
			</div>

			<!-- 步骤3：主体映射和键 -->
			<div v-show="currentStep === 2">
				<el-form label-width="120px">
					<el-form-item label="主体实体映射ID" required>
						<el-input-number v-model="form.subjectEntityMappingId" :min="1" controls-position="right" style="width: 100%" />
					</el-form-item>
					<el-form-item label="主体键映射" required>
						<el-input
							v-model="form.subjectKeyMapping"
							type="textarea"
							:rows="2"
							placeholder='JSON，如 [{"column":"user_id","targetKeyColumn":"user_id","normalizer":"LONG"}]'
						/>
					</el-form-item>
				</el-form>
				<el-form-item v-if="form.relationMode === 'SELF_REFERENCE'" label="说明">
					<el-text type="info">自引用模式下，主体和客体映射相同，将自动填充客体</el-text>
				</el-form-item>
			</div>

			<!-- 步骤4：客体映射和键 -->
			<div v-show="currentStep === 3">
				<el-form label-width="120px">
					<el-form-item label="客体实体映射ID" required>
						<el-input-number
							v-model="form.objectEntityMappingId"
							:min="1"
							controls-position="right"
							style="width: 100%"
							:disabled="form.relationMode === 'SELF_REFERENCE'"
						/>
					</el-form-item>
					<el-form-item label="客体键映射" required>
						<el-input
							v-model="form.objectKeyMapping"
							type="textarea"
							:rows="2"
							placeholder="JSON"
							:disabled="form.relationMode === 'SELF_REFERENCE'"
						/>
					</el-form-item>
					<el-form-item label="关系键列" v-if="form.relationMode === 'JOIN_TABLE'" required>
						<el-input v-model="form.relationKeyColumns" type="textarea" :rows="2" placeholder="JSON，关联表必须配置" />
					</el-form-item>
					<el-form-item label="关系键列" v-else>
						<el-input v-model="form.relationKeyColumns" type="textarea" :rows="2" placeholder="JSON（可选）" />
					</el-form-item>
				</el-form>
			</div>

			<!-- 步骤5：对象属性 -->
			<div v-show="currentStep === 4">
				<el-form label-width="120px">
					<el-form-item label="对象属性ID" required>
						<el-input-number v-model="form.objectPropertyId" :min="1" controls-position="right" style="width: 100%" />
					</el-form-item>
				</el-form>
				<el-alert type="info" :closable="false" show-icon>
					<template #title>对象属性选择器应显示 domain/range 兼容结果。即使前端显示兼容，提交仍由后端校验。</template>
				</el-alert>
			</div>

			<!-- 步骤6：策略 -->
			<div v-show="currentStep === 5">
				<el-form label-width="120px">
					<el-form-item label="缺失目标策略">
						<el-select v-model="form.missingTargetPolicy" style="width: 100%">
							<el-option label="挂起待解析" value="PENDING" />
							<el-option label="跳过" value="SKIP" />
							<el-option label="记录失败" value="FAIL_RECORD" />
						</el-select>
					</el-form-item>
					<el-form-item label="删除策略">
						<el-select v-model="form.deleteStrategy" style="width: 100%">
							<el-option label="移除断言" value="REMOVE_ASSERTION" />
							<el-option label="保留断言" value="KEEP_ASSERTION" />
							<el-option label="阻断审查" value="BLOCK_AND_REVIEW" />
						</el-select>
					</el-form-item>
					<el-form-item label="所有权策略">
						<el-select v-model="form.ownershipPolicy" style="width: 100%">
							<el-option label="源优先" value="SOURCE_WINS" />
							<el-option label="人工优先" value="MANUAL_WINS" />
							<el-option label="拒绝冲突" value="REJECT_CONFLICT" />
						</el-select>
					</el-form-item>
					<el-form-item label="过滤条件">
						<FilterDslBuilder v-model="form.filterDsl" />
					</el-form-item>
					<el-form-item label="同步顺序">
						<el-input-number v-model="form.syncOrder" :min="0" controls-position="right" />
					</el-form-item>
					<el-form-item label="启用">
						<el-switch v-model="form.enabled" active-value="1" inactive-value="0" />
					</el-form-item>
				</el-form>
			</div>

			<!-- 步骤7：预览 -->
			<div v-show="currentStep === 6">
				<el-descriptions :column="1" border>
					<el-descriptions-item label="关系模式">{{ form.relationMode }}</el-descriptions-item>
					<el-descriptions-item label="映射编码">{{ form.mappingCode }}</el-descriptions-item>
					<el-descriptions-item label="映射名称">{{ form.mappingName }}</el-descriptions-item>
					<el-descriptions-item label="源对象">{{ form.sourceSchema }}.{{ form.sourceObject }}</el-descriptions-item>
					<el-descriptions-item label="主体映射ID">{{ form.subjectEntityMappingId }}</el-descriptions-item>
					<el-descriptions-item label="客体映射ID">{{ form.objectEntityMappingId }}</el-descriptions-item>
					<el-descriptions-item label="对象属性ID">{{ form.objectPropertyId }}</el-descriptions-item>
				</el-descriptions>
			</div>
		</div>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button @click="handlePrev" v-if="currentStep > 0">上一步</el-button>
			<el-button type="primary" @click="handleNext" v-if="currentStep < 6">下一步</el-button>
			<el-button type="success" @click="handleSubmit" v-if="currentStep === 6" :loading="submitting">创建</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingConfigApi } from '/@/api/ontology/data-mapping';
import type { RelationMappingCreateRequest } from '/@/types/ontology/data-mapping';
import FilterDslBuilder from './FilterDslBuilder.vue';

const props = defineProps<{ versionId: number }>();
const emit = defineEmits<{ (e: 'success'): void }>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const submitting = ref(false);
const currentStep = ref(0);

const form = reactive({
	mappingCode: '',
	mappingName: '',
	relationMode: 'FOREIGN_KEY' as 'FOREIGN_KEY' | 'SELF_REFERENCE' | 'JOIN_TABLE',
	objectPropertyId: undefined as number | undefined,
	subjectEntityMappingId: undefined as number | undefined,
	objectEntityMappingId: undefined as number | undefined,
	sourceId: undefined as number | undefined,
	sourceSchema: '',
	sourceObject: '',
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

const open = () => {
	visible.value = true;
	currentStep.value = 0;
	Object.assign(form, {
		mappingCode: '',
		mappingName: '',
		relationMode: 'FOREIGN_KEY',
		objectPropertyId: undefined,
		subjectEntityMappingId: undefined,
		objectEntityMappingId: undefined,
		sourceId: undefined,
		sourceSchema: '',
		sourceObject: '',
		subjectKeyMapping: '',
		objectKeyMapping: '',
		relationKeyColumns: '',
		filterDsl: '',
		missingTargetPolicy: 'PENDING',
		deleteStrategy: 'REMOVE_ASSERTION',
		ownershipPolicy: 'SOURCE_WINS',
		syncOrder: 1000,
		enabled: '1',
		description: '',
		revision: 0,
	});
};

const handlePrev = () => {
	if (currentStep.value > 0) currentStep.value--;
};

const handleNext = () => {
	// 自引用模式自动填充客体
	if (currentStep.value === 2 && form.relationMode === 'SELF_REFERENCE') {
		form.objectEntityMappingId = form.subjectEntityMappingId;
		form.objectKeyMapping = form.subjectKeyMapping;
	}
	currentStep.value++;
};

const handleSubmit = async () => {
	submitting.value = true;
	try {
		const payload: RelationMappingCreateRequest = {
			mappingCode: form.mappingCode,
			mappingName: form.mappingName,
			relationMode: form.relationMode,
			objectPropertyId: form.objectPropertyId!,
			subjectEntityMappingId: form.subjectEntityMappingId!,
			objectEntityMappingId: form.objectEntityMappingId!,
			sourceId: form.sourceId!,
			sourceSchema: form.sourceSchema,
			sourceObject: form.sourceObject,
			subjectKeyMapping: form.subjectKeyMapping,
			objectKeyMapping: form.objectKeyMapping,
			relationKeyColumns: form.relationKeyColumns,
			filterDsl: form.filterDsl || undefined,
			missingTargetPolicy: form.missingTargetPolicy,
			deleteStrategy: form.deleteStrategy,
			ownershipPolicy: form.ownershipPolicy,
			syncOrder: form.syncOrder,
			enabled: form.enabled,
			description: form.description || undefined,
			revision: form.revision,
		};
		await mappingConfigApi.createRelationMapping(props.versionId, payload);
		msgSuccess('关系映射创建成功');
		visible.value = false;
		emit('success');
	} catch (e: any) {
		msgError(e.message || '创建失败');
	} finally {
		submitting.value = false;
	}
};

defineExpose({ open });
</script>

<style scoped>
.mb16 {
	margin-bottom: 16px;
}
</style>
