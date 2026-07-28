<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible" width="640px">
		<el-form :model="form" :rules="dataRules" label-width="110px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelProperty.localName')" prop="localName">
						<el-input :disabled="form.id !== ''" :placeholder="t('modelProperty.inputLocalNameTip')" v-model="form.localName" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelProperty.label')" prop="label">
						<el-input :placeholder="t('modelProperty.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<!-- 数据属性专属字段 -->
				<template v-if="activeTab === 'datatype'">
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.xsdType')" prop="xsdType">
							<el-select v-model="form.xsdType" :placeholder="t('modelProperty.xsdType')" @change="onXsdTypeChange">
								<el-option v-for="o in xsdTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.unitRef')" prop="unitRef">
							<el-input :disabled="!isNumericType(form.xsdType)" v-model="form.unitRef" />
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.minCardinality')" prop="minCardinality">
							<el-input-number :min="0" v-model="form.minCardinality" />
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.maxCardinality')" prop="maxCardinality">
							<el-input-number :min="-1" v-model="form.maxCardinality" />
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.isIdentifier')" prop="isIdentifier">
							<el-switch active-value="1" inactive-value="0" v-model="form.isIdentifier" />
						</el-form-item>
					</el-col>
				</template>
				<!-- 对象属性专属字段 -->
				<template v-else>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.rangeClass')" prop="rangeClassId">
							<el-select v-model="form.rangeClassId" :placeholder="t('modelProperty.selectRange')" clearable>
								<el-option v-for="c in classList" :key="c.id" :label="c.label || c.localName" :value="c.id" :disabled="c.id === classId" />
							</el-select>
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.minCardinality')" prop="minCardinality">
							<el-input-number :min="0" v-model="form.minCardinality" />
						</el-form-item>
					</el-col>
					<el-col :span="12" class="mb20">
						<el-form-item :label="t('modelProperty.maxCardinality')" prop="maxCardinality">
							<el-input-number :min="-1" v-model="form.maxCardinality" />
						</el-form-item>
					</el-col>
				</template>
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

<script lang="ts" name="ModelPropertyDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj as addDt, getObj as getDt, putObj as putDt } from '/@/api/ontology-model/datatype-property';
import { addObj as addObj2, getObj as getObj2, putObj as putObj2 } from '/@/api/ontology-model/object-property';
import { useI18n } from 'vue-i18n';
import { useModelPropertyOptions } from './composables';

const props = defineProps<{ activeTab: string; classId: string; classList: any[] }>();
const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const { xsdTypeOptions, isNumericType } = useModelPropertyOptions();

const form = reactive({
	id: '',
	classId: '',
	domainClassId: '',
	localName: '',
	label: '',
	xsdType: 'xsd:string',
	unitRef: '',
	minCardinality: 0,
	maxCardinality: -1,
	isIdentifier: '0',
	rangeClassId: '',
});

const dataRules = computed(() => ({
	localName: [{ required: true, message: t('modelProperty.inputLocalNameTip'), trigger: 'blur' }],
	label: [{ required: true, message: t('modelProperty.inputLabelTip'), trigger: 'blur' }],
	xsdType: [{ required: true, message: t('modelProperty.xsdType'), trigger: 'change' }],
	rangeClassId: [{ required: props.activeTab === 'object', message: t('modelProperty.selectRange'), trigger: 'change' }],
}));

const onXsdTypeChange = () => {
	if (!isNumericType(form.xsdType)) {
		form.unitRef = '';
	}
};

const openDialog = async (classId: string, id?: string) => {
	visible.value = true;
	form.id = '';
	form.classId = classId;
	form.domainClassId = classId;
	form.localName = '';
	form.label = '';
	form.xsdType = 'xsd:string';
	form.unitRef = '';
	form.minCardinality = 0;
	form.maxCardinality = -1;
	form.isIdentifier = '0';
	form.rangeClassId = '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	if (id) {
		form.id = id;
		await loadData(id);
	}
};

const loadData = async (id: string) => {
	try {
		const { data } = props.activeTab === 'datatype' ? await getDt(id) : await getObj2(id);
		Object.assign(form, data);
	} catch (err: any) {
		useMessage().error(err.msg);
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
		if (props.activeTab === 'datatype') {
			form.id ? await putDt(form) : await addDt(form);
		} else {
			form.id ? await putObj2(form) : await addObj2(form);
		}
		useMessage().success(t(form.id ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({ openDialog });
</script>
