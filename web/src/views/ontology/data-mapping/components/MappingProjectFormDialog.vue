<template>
	<el-dialog v-model="visible" :title="title" width="640px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-form ref="formRef" :model="form" :rules="rules" label-width="140px" v-loading="loading">
			<el-form-item label="映射编码" prop="mappingCode">
				<el-input v-model="form.mappingCode" placeholder="字母开头，3-64位字母/数字/下划线" :disabled="isEdit" />
			</el-form-item>
			<el-form-item label="映射名称" prop="mappingName">
				<el-input v-model="form.mappingName" placeholder="映射名称" maxlength="128" show-word-limit />
			</el-form-item>
			<el-form-item label="本体工程ID" prop="ontologyId">
				<el-input-number v-model="form.ontologyId" :min="1" controls-position="right" :disabled="isEdit" style="width: 100%" />
			</el-form-item>
			<el-form-item label="默认命名空间ID" prop="defaultNamespaceId">
				<el-input-number v-model="form.defaultNamespaceId" :min="1" controls-position="right" :disabled="isEdit" style="width: 100%" />
			</el-form-item>
			<el-form-item label="版本兼容约束" prop="ontologyVersionConstraint">
				<el-input v-model="form.ontologyVersionConstraint" placeholder="如 >=1.0.0 <2.0.0" />
			</el-form-item>
			<el-divider content-position="left">调度配置（可选）</el-divider>
			<el-form-item label="Cron表达式">
				<el-input v-model="form.scheduleCron" placeholder="如 0 0 2 * * ?" />
			</el-form-item>
			<el-form-item label="调度运行类型">
				<el-select v-model="form.scheduleRunType" placeholder="选择调度运行类型" style="width: 100%">
					<el-option label="全量" value="FULL" />
					<el-option label="增量" value="INCREMENTAL" />
				</el-select>
			</el-form-item>
			<el-divider content-position="left">安全与说明</el-divider>
			<el-form-item label="安全级别">
				<el-select v-model="form.securityLevelCode" placeholder="默认 INTERNAL" style="width: 100%">
					<el-option label="公开" value="PUBLIC" />
					<el-option label="内部" value="INTERNAL" />
					<el-option label="受限" value="RESTRICTED" />
					<el-option label="机密" value="CONFIDENTIAL" />
				</el-select>
			</el-form-item>
			<el-form-item label="描述">
				<el-input v-model="form.description" type="textarea" :rows="2" />
			</el-form-item>
			<el-form-item label="备注">
				<el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="255" show-word-limit />
			</el-form-item>
			<el-form-item label="初始发布说明" v-if="!isEdit">
				<el-input v-model="form.releaseNotes" type="textarea" :rows="2" placeholder="初始版本发布说明（可选）" />
			</el-form-item>
		</el-form>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingProjectApi } from '/@/api/ontology/data-mapping';
import type { MappingProjectCreateRequest, MappingProjectUpdateRequest } from '/@/types/ontology/data-mapping';

const emit = defineEmits<{
	(e: 'success'): void;
}>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const isEdit = ref(false);
const formRef = ref();

const form = reactive({
	id: undefined as number | undefined,
	mappingCode: '',
	mappingName: '',
	ontologyId: undefined as number | undefined,
	defaultNamespaceId: undefined as number | undefined,
	ontologyVersionConstraint: '',
	securityLevelCode: 'INTERNAL',
	description: '',
	scheduleCron: '',
	scheduleRunType: 'INCREMENTAL' as 'FULL' | 'INCREMENTAL',
	remarks: '',
	releaseNotes: '',
	revision: 0,
});

const title = computed(() => (isEdit.value ? '编辑映射工程' : '新建映射工程'));

const rules = {
	mappingCode: [
		{ required: true, message: '请输入映射编码', trigger: 'blur' },
		{ pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,63}$/, message: '字母开头，3-64位字母/数字/下划线', trigger: 'blur' },
	],
	mappingName: [{ required: true, message: '请输入映射名称', trigger: 'blur' }],
	ontologyId: [{ required: true, message: '请输入本体工程ID', trigger: 'blur' }],
	defaultNamespaceId: [{ required: true, message: '请输入默认命名空间ID', trigger: 'blur' }],
	ontologyVersionConstraint: [{ required: true, message: '请输入版本兼容约束', trigger: 'blur' }],
};

const openDialog = async (id?: number) => {
	visible.value = true;
	isEdit.value = !!id;

	// 重置表单
	Object.assign(form, {
		id: undefined,
		mappingCode: '',
		mappingName: '',
		ontologyId: undefined,
		defaultNamespaceId: undefined,
		ontologyVersionConstraint: '',
		securityLevelCode: 'INTERNAL',
		description: '',
		scheduleCron: '',
		scheduleRunType: 'INCREMENTAL',
		remarks: '',
		releaseNotes: '',
		revision: 0,
	});

	if (id) {
		loading.value = true;
		try {
			const { data } = await mappingProjectApi.getProjectDetail(id);
			form.id = data.id;
			form.mappingCode = data.mappingCode;
			form.mappingName = data.mappingName;
			form.ontologyId = data.ontologyId;
			form.defaultNamespaceId = data.defaultNamespaceId;
			form.securityLevelCode = data.securityLevelCode || 'INTERNAL';
			form.description = data.description || '';
			form.scheduleCron = data.scheduleCron || '';
			form.scheduleRunType = (data.scheduleRunType as 'FULL' | 'INCREMENTAL') || 'INCREMENTAL';
			form.remarks = data.remarks || '';
			form.revision = data.revision;
		} catch (e: any) {
			msgError(e.message || '获取工程详情失败');
		} finally {
			loading.value = false;
		}
	}
};

const handleSubmit = async () => {
	try {
		await formRef.value?.validate();
	} catch {
		return;
	}
	loading.value = true;
	try {
		if (isEdit.value) {
			const payload: MappingProjectUpdateRequest = {
				id: form.id!,
				mappingName: form.mappingName,
				description: form.description,
				securityLevelCode: form.securityLevelCode,
				scheduleEnabled: form.scheduleCron ? '1' : '0',
				scheduleCron: form.scheduleCron || undefined,
				scheduleRunType: form.scheduleRunType || undefined,
				remarks: form.remarks,
				revision: form.revision,
			};
			await mappingProjectApi.updateProject(form.id!, payload);
			msgSuccess('工程更新成功');
		} else {
			const payload: MappingProjectCreateRequest = {
				mappingCode: form.mappingCode,
				mappingName: form.mappingName,
				ontologyId: form.ontologyId!,
				defaultNamespaceId: form.defaultNamespaceId!,
				description: form.description,
				ontologyVersionConstraint: form.ontologyVersionConstraint,
				securityLevelCode: form.securityLevelCode,
				scheduleCron: form.scheduleCron || undefined,
				scheduleRunType: form.scheduleRunType || undefined,
				remarks: form.remarks,
				releaseNotes: form.releaseNotes,
			};
			await mappingProjectApi.createProject(payload);
			msgSuccess('工程创建成功');
		}
		visible.value = false;
		emit('success');
	} catch (e: any) {
		msgError(e.message || '保存失败');
	} finally {
		loading.value = false;
	}
};

defineExpose({ openDialog });
</script>
