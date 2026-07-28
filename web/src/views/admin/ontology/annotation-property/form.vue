<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible">
		<el-form :model="form" :rules="dataRules" label-width="100px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('annotationProperty.localName')" prop="localName">
						<el-input :disabled="form.id !== ''" :placeholder="t('annotationProperty.inputLocalNameTip')" v-model="form.localName" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('annotationProperty.label')" prop="label">
						<el-input :placeholder="t('annotationProperty.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('annotationProperty.rangeXsd')" prop="rangeXsd">
						<el-select :placeholder="t('annotationProperty.selectRangeXsdTip')" v-model="form.rangeXsd" filterable allow-create clearable>
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in rangeXsdOptions" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('annotationProperty.appliesTo')" prop="appliesTo">
						<el-select :placeholder="t('annotationProperty.selectAppliesToTip')" v-model="form.appliesTo" clearable>
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in appliesToOptions" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('annotationProperty.sortOrder')" prop="sortOrder">
						<el-input-number :min="0" :placeholder="t('annotationProperty.inputSortOrderTip')" v-model="form.sortOrder" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('annotationProperty.description')" prop="description">
						<el-input :placeholder="t('annotationProperty.inputDescriptionTip')" type="textarea" v-model="form.description" />
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

<script lang="ts" name="AnnotationPropertyDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/ontology/annotation-property';
import { useI18n } from 'vue-i18n';
import { useAnnotationPropertyOptions } from './composables';

const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const { appliesToOptions, rangeXsdOptions } = useAnnotationPropertyOptions();

const form = reactive({
	id: '',
	localName: '',
	label: '',
	rangeXsd: '',
	appliesTo: '',
	description: '',
	sortOrder: 0,
});

const dataRules = computed(() => ({
	localName: [{ required: true, message: t('annotationProperty.inputLocalNameTip'), trigger: 'blur' }],
	label: [{ required: true, message: t('annotationProperty.inputLabelTip'), trigger: 'blur' }],
}));

const openDialog = async (id: string) => {
	visible.value = true;
	form.id = '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	if (id) {
		form.id = id;
		await getAnnotationPropertyData(id);
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

const getAnnotationPropertyData = async (id: string) => {
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
