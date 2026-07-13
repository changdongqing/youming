<template>
	<el-drawer v-model="visible" :title="module ? '编辑扩展模块' : '新建扩展模块'" size="480px" :close-on-click-modal="false">
		<el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
			<el-form-item label="模块代码" prop="moduleCode">
				<el-input v-model="form.moduleCode" placeholder="如 medical、petroleum" :disabled="!!module" />
			</el-form-item>
			<el-form-item label="模块名称" prop="moduleName">
				<el-input v-model="form.moduleName" placeholder="如 医疗行业扩展" />
			</el-form-item>
			<el-form-item label="命名空间" prop="namespaceId">
				<el-select v-model="form.namespaceId" placeholder="选择扩展命名空间" filterable style="width: 100%">
					<el-option v-for="ns in namespaceOptions" :key="ns.id" :label="`${ns.prefix}（${ns.uri}）`" :value="ns.id" />
				</el-select>
				<div style="font-size: 12px; color: #999; margin-top: 4px">仅扩展命名空间可选</div>
			</el-form-item>
			<el-form-item label="版本号">
				<el-input v-model="form.version" placeholder="如 1.0.0" />
			</el-form-item>
			<el-form-item label="描述">
				<el-input v-model="form.description" type="textarea" :rows="3" placeholder="模块描述" />
			</el-form-item>
			<el-form-item label="排序">
				<el-input-number v-model="form.sortOrder" :min="0" :max="999" />
			</el-form-item>
			<el-form-item label="备注">
				<el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="备注信息" />
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" :loading="submitting" @click="handleSubmit">{{ module ? '保存' : '创建' }}</el-button>
		</template>
	</el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue';
import type { FormInstance } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { addExtensionModuleObj, putExtensionModuleObj } from '/@/api/ontology/extension';
import type { ExtensionModule } from '/@/types/ontology/extension';

const props = defineProps<{ visible: boolean; module: ExtensionModule | null }>();
const emit = defineEmits<{ 'update:visible': [value: boolean]; success: [] }>();
const { success: msgSuccess, error: msgError } = useMessage();

const visible = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});
const formRef = ref<FormInstance>();
const submitting = ref(false);
const namespaceOptions = ref<Array<{ id: string; prefix: string; uri: string }>>([]);

const form = reactive({
	moduleCode: '',
	moduleName: '',
	namespaceId: '' as string,
	version: '',
	description: '',
	sortOrder: 0,
	remarks: '',
});

const rules = {
	moduleCode: [
		{ required: true, message: '模块代码不能为空', trigger: 'blur' },
		{ pattern: /^[a-z][a-z0-9_-]*$/, message: '小写英文开头，仅含小写英文、数字、下划线、中划线', trigger: 'blur' },
	],
	moduleName: [{ required: true, message: '模块名称不能为空', trigger: 'blur' }],
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
};

const loadNamespaces = async () => {
	try {
		const res = await fetchNamespaceList({ isBuiltin: '0' });
		namespaceOptions.value = res.data || [];
	} catch {
		// ignore
	}
};

watch(() => props.module, (val) => {
	if (val) {
		form.moduleCode = val.moduleCode;
		form.moduleName = val.moduleName;
		form.namespaceId = val.namespaceId;
		form.version = val.version || '';
		form.description = val.description || '';
		form.sortOrder = val.sortOrder || 0;
		form.remarks = val.remarks || '';
	} else {
		Object.assign(form, { moduleCode: '', moduleName: '', namespaceId: '', version: '', description: '', sortOrder: 0, remarks: '' });
	}
	// 每次抽屉打开（module 变化）时重新加载命名空间列表，确保新增的扩展命名空间可见
	loadNamespaces();
}, { immediate: true });

const handleSubmit = async () => {
	if (!formRef.value) return;
	await formRef.value.validate(async (valid) => {
		if (!valid) return;
		submitting.value = true;
		try {
			if (props.module) {
				await putExtensionModuleObj({
					id: props.module.id,
					moduleName: form.moduleName,
					namespaceId: form.namespaceId,
					version: form.version,
					description: form.description,
					sortOrder: form.sortOrder,
					remarks: form.remarks,
				});
				msgSuccess('修改成功');
			} else {
				await addExtensionModuleObj({
					moduleCode: form.moduleCode,
					moduleName: form.moduleName,
					namespaceId: form.namespaceId,
					version: form.version,
					description: form.description,
				});
				msgSuccess('创建成功');
			}
			emit('success');
			visible.value = false;
		} catch {
			msgError('操作失败');
		} finally {
			submitting.value = false;
		}
	});
};
</script>
