<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible" width="720px">
		<el-form :model="form" :rules="dataRules" label-width="130px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.quantityKind')" prop="quantityKindId">
						<el-select :placeholder="t('unit.selectQuantityKindTip')" v-model="form.quantityKindId" :disabled="form.id !== ''" style="width: 100%">
							<el-option :key="item.id" :label="item.labelCn ? item.labelCn + ' (' + item.label + ')' : item.label" :value="item.id" v-for="item in quantityKinds" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.qudtIri')" prop="qudtIri">
						<el-input :disabled="form.id !== ''" :placeholder="t('unit.inputQudtIriTip')" v-model="form.qudtIri" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.symbol')" prop="symbol">
						<el-input :placeholder="t('unit.inputSymbolTip')" v-model="form.symbol" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.ucumCode')" prop="ucumCode">
						<el-input :placeholder="t('unit.inputUcumCodeTip')" v-model="form.ucumCode" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.label')" prop="label">
						<el-input :placeholder="t('unit.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.labelCn')" prop="labelCn">
						<el-input :placeholder="t('unit.inputLabelCnTip')" v-model="form.labelCn" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.conversionMultiplier')" prop="conversionMultiplier">
						<el-input :placeholder="t('unit.inputMultiplierTip')" v-model="form.conversionMultiplier" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.conversionMultiplierSn')" prop="conversionMultiplierSn">
						<el-input :placeholder="t('unit.inputMultiplierSnTip')" v-model="form.conversionMultiplierSn" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.conversionOffset')" prop="conversionOffset">
						<el-input :placeholder="t('unit.inputOffsetTip')" v-model="form.conversionOffset" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('unit.conversionOffsetSn')" prop="conversionOffsetSn">
						<el-input :placeholder="t('unit.inputOffsetTip')" v-model="form.conversionOffsetSn" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('unit.scalingOf')" prop="scalingOf">
						<el-select :placeholder="t('unit.inputScalingOfTip')" v-model="form.scalingOf" clearable style="width: 100%">
							<el-option :key="u.qudtIri" :label="(u.labelCn || u.label) + ' [' + u.symbol + ']'" :value="u.qudtIri" v-for="u in baseUnitOptions" />
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

<script lang="ts" name="UnitDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj, listQuantityKind, pageList } from '/@/api/ontology/unit';
import { useI18n } from 'vue-i18n';

const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const quantityKinds = ref<any[]>([]);
const baseUnitOptions = ref<any[]>([]);

const form = reactive({
	id: '',
	quantityKindId: '' as string | number,
	qudtIri: '',
	symbol: '',
	label: '',
	labelCn: '',
	conversionMultiplier: '',
	conversionMultiplierSn: '',
	conversionOffset: '',
	conversionOffsetSn: '',
	scalingOf: '',
	ucumCode: '',
});

const dataRules = computed(() => ({
	quantityKindId: [{ required: true, message: t('unit.selectQuantityKindTip'), trigger: 'change' }],
	qudtIri: [{ required: true, message: t('unit.inputQudtIriTip'), trigger: 'blur' }],
	label: [{ required: true, message: t('unit.inputLabelTip'), trigger: 'blur' }],
}));

/**
 * 打开对话框。
 * @param id 编辑时传入；新增时为 null
 * @param defaultQkId 新增时默认量纲 id（来自左侧选中）
 */
const openDialog = async (id: string | null, defaultQkId?: string) => {
	visible.value = true;
	form.id = '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	await loadQuantityKinds();

	if (id) {
		form.id = id;
		await getUnitData(id);
	} else if (defaultQkId) {
		form.quantityKindId = defaultQkId;
		await loadBaseUnits(defaultQkId);
	}
};

/**
 * 加载量纲下拉。
 */
const loadQuantityKinds = async () => {
	try {
		const { data } = await listQuantityKind();
		quantityKinds.value = data || [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

/**
 * 加载某量纲下的单位，作为基准单位下拉（scalingOf 候选）。
 */
const loadBaseUnits = async (qkId: string | number) => {
	baseUnitOptions.value = [];
	if (!qkId) return;
	try {
		const { data } = await pageList({ quantityKindId: qkId, size: 999 });
		baseUnitOptions.value = data.records || [];
	} catch {
		baseUnitOptions.value = [];
	}
};

/**
 * 量纲变化时刷新基准单位下拉。
 */
watch(
	() => form.quantityKindId,
	(val) => {
		if (val && form.id === '') {
			loadBaseUnits(val);
			form.scalingOf = '';
		}
	}
);

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

const getUnitData = async (id: string) => {
	try {
		const { data } = await getObj(id);
		Object.assign(form, data);
		if (form.quantityKindId) {
			await loadBaseUnits(form.quantityKindId);
		}
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

defineExpose({
	openDialog,
});
</script>
