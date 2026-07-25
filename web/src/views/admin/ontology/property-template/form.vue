<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible">
		<el-form :model="form" :rules="dataRules" label-width="100px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('propertyTemplate.templateCode')" prop="templateCode">
						<el-input :disabled="form.id !== ''" :placeholder="t('propertyTemplate.inputTemplateCodeTip')" v-model="form.templateCode" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('propertyTemplate.kind')" prop="kind">
						<el-radio-group v-model="form.kind" @change="handleKindChange">
							<el-radio value="datatype" border>{{ t('propertyTemplate.datatype') }}</el-radio>
							<el-radio value="object" border>{{ t('propertyTemplate.object') }}</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('propertyTemplate.label')" prop="label">
						<el-input :placeholder="t('propertyTemplate.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('propertyTemplate.category')" prop="category">
						<el-select :placeholder="t('propertyTemplate.selectCategoryTip')" v-model="form.category">
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in categoryOptions" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('propertyTemplate.description')" prop="description">
						<el-input :placeholder="t('propertyTemplate.inputDescriptionTip')" type="textarea" v-model="form.description" />
					</el-form-item>
				</el-col>
				<!-- datatype 专属字段 -->
				<el-col :span="12" class="mb20" v-show="form.kind === 'datatype'">
					<el-form-item :label="t('propertyTemplate.type')" prop="type">
						<el-select :placeholder="t('propertyTemplate.selectTypeTip')" v-model="form.type">
							<el-option key="string" label="string" value="string" />
							<el-option key="integer" label="integer" value="integer" />
							<el-option key="decimal" label="decimal" value="decimal" />
							<el-option key="boolean" label="boolean" value="boolean" />
							<el-option key="datetime" label="datetime" value="datetime" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20" v-show="form.kind === 'datatype'">
					<el-form-item :label="t('propertyTemplate.isIdentifier')" prop="isIdentifier">
						<el-radio-group v-model="form.isIdentifier">
							<el-radio value="0" border>{{ t('common.no') }}</el-radio>
							<el-radio value="1" border>{{ t('common.yes') }}</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20" v-show="form.kind === 'datatype'">
					<el-form-item :label="t('propertyTemplate.unitRef')" prop="unitRef">
						<el-input :placeholder="t('propertyTemplate.inputUnitRefTip')" v-model="form.unitRef" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20" v-show="form.kind === 'datatype'">
					<el-form-item :label="t('propertyTemplate.values')" prop="enumValues">
						<el-input :placeholder="t('propertyTemplate.inputValuesTip')" v-model="form.enumValues" />
					</el-form-item>
				</el-col>
				<!-- object 专属字段 -->
				<el-col :span="12" class="mb20" v-show="form.kind === 'object'">
					<el-form-item :label="t('propertyTemplate.defaultCardinality')" prop="defaultCardinality">
						<el-select :placeholder="t('propertyTemplate.selectCardinalityTip')" v-model="form.defaultCardinality">
							<el-option key="one-to-one" label="one-to-one" value="one-to-one" />
							<el-option key="one-to-many" label="one-to-many" value="one-to-many" />
							<el-option key="many-to-one" label="many-to-one" value="many-to-one" />
							<el-option key="many-to-many" label="many-to-many" value="many-to-many" />
						</el-select>
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

<script lang="ts" name="PropertyTemplateDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj } from '/@/api/ontology/property-template';
import { useI18n } from 'vue-i18n';
import { usePropertyTemplateOptions } from './composables';

const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const { categoryOptions } = usePropertyTemplateOptions();

const form = reactive({
	id: '',
	templateCode: '',
	kind: 'datatype',
	label: '',
	description: '',
	category: '',
	type: '',
	isIdentifier: '0',
	unitRef: '',
	enumValues: '',
	defaultCardinality: '',
});

// type / defaultCardinality 的必填随 kind 动态切换：
// - kind=datatype 时 type 必填、defaultCardinality 不校验
// - kind=object 时 defaultCardinality 必填、type 不校验
const dataRules = computed(() => ({
	templateCode: [{ required: true, message: t('propertyTemplate.inputTemplateCodeTip'), trigger: 'blur' }],
	kind: [{ required: true, message: t('propertyTemplate.selectKindTip'), trigger: 'change' }],
	label: [{ required: true, message: t('propertyTemplate.inputLabelTip'), trigger: 'blur' }],
	type: form.kind === 'datatype' ? [{ required: true, message: t('propertyTemplate.selectTypeTip'), trigger: 'change' }] : [],
	defaultCardinality: form.kind === 'object' ? [{ required: true, message: t('propertyTemplate.selectCardinalityTip'), trigger: 'change' }] : [],
}));

const handleKindChange = () => {
	if (form.kind === 'object') {
		form.type = '';
		form.isIdentifier = '0';
		form.unitRef = '';
		form.enumValues = '';
	} else {
		form.defaultCardinality = '';
	}
};

const openDialog = async (id: string) => {
	visible.value = true;
	form.id = '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	if (id) {
		form.id = id;
		await getPropertyTemplateData(id);
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

const getPropertyTemplateData = async (id: string) => {
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
