<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible" width="640px">
		<el-form :model="form" :rules="dataRules" label-width="120px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('modelProject.projectCode')" prop="projectCode">
						<el-input :disabled="form.id !== ''" :placeholder="t('modelProject.inputProjectCodeTip')" v-model="form.projectCode" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelProject.name')" prop="name">
						<el-input :placeholder="t('modelProject.inputNameTip')" v-model="form.name" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelProject.status')" prop="status">
						<el-select :placeholder="t('modelProject.selectStatusTip')" v-model="form.status" clearable>
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in statusOptions" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('modelProject.namespaceBase')" prop="namespaceBase">
						<el-input :placeholder="t('modelProject.inputNamespaceBaseTip')" v-model="form.namespaceBase" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelProject.defaultFormat')" prop="defaultFormat">
						<el-select :placeholder="t('modelProject.selectDefaultFormatTip')" v-model="form.defaultFormat">
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in formatOptions" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('modelProject.description')" prop="description">
						<el-input :placeholder="t('modelProject.inputDescriptionTip')" type="textarea" v-model="form.description" />
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="loading">{{ t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="ModelProjectDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/ontology-model/project';
import { useI18n } from 'vue-i18n';
import { useModelProjectOptions } from './composables';

const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const { statusOptions, formatOptions } = useModelProjectOptions();

// serializationStrategy 不在表单暴露（v1 强制 B，由后端 saveProject/updateProject 自动设置）
const form = reactive({
	id: '',
	projectCode: '',
	name: '',
	description: '',
	namespaceBase: '',
	defaultFormat: 'TTL',
	serializationStrategy: 'B',
	status: 'draft',
});

const dataRules = computed(() => ({
	projectCode: [{ required: true, message: t('modelProject.inputProjectCodeTip'), trigger: 'blur' }],
	name: [{ required: true, message: t('modelProject.inputNameTip'), trigger: 'blur' }],
	namespaceBase: [{ required: true, message: t('modelProject.inputNamespaceBaseTip'), trigger: 'blur' }],
	defaultFormat: [{ required: true, message: t('modelProject.selectDefaultFormatTip'), trigger: 'change' }],
	status: [{ required: true, message: t('modelProject.selectStatusTip'), trigger: 'change' }],
}));

const openDialog = async (id: string) => {
	visible.value = true;
	form.id = '';

	// 默认值（新增）
	form.projectCode = '';
	form.name = '';
	form.description = '';
	form.namespaceBase = '';
	form.defaultFormat = 'TTL';
	form.serializationStrategy = 'B';
	form.status = 'draft';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	if (id) {
		form.id = id;
		await getProjectData(id);
	}
};

const onSubmit = async () => {
	if (loading.value) return;
	loading.value = true;

	try {
		const valid = await dataFormRef.value.validate().catch(() => {});
		if (!valid) {
			loading.value = false;
			return false;
		}

		form.id ? await putObj(form) : await addObj(form);
		useMessage().success(t(form.id ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const getProjectData = async (id: string) => {
	try {
		const { data } = await getObj(id);
		Object.assign(form, data);
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

defineExpose({
	openDialog,
});
</script>
