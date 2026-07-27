<template>
	<el-drawer :title="t('classTemplate.ruleTitle')" v-model="visible" size="420px">
		<el-alert :title="t('classTemplate.ruleOnlyNewTip')" type="info" :closable="false" class="mb12" show-icon />
		<el-form :model="form" label-width="110px" v-loading="loading">
			<el-form-item :label="t('classTemplate.treeRoot')">
				<el-input v-model="form.treeRoot" disabled />
			</el-form-item>
			<el-form-item :label="t('classTemplate.separator')">
				<el-input v-model="form.separator" :placeholder="'-'" style="max-width: 160px" />
			</el-form-item>
			<el-form-item :label="t('classTemplate.levelDigits')">
				<el-input-number v-model="form.levelDigits" :min="1" :max="6" controls-position="right" />
			</el-form-item>
			<el-form-item :label="t('classTemplate.baseNumber')">
				<el-input-number v-model="form.baseNumber" :min="0" controls-position="right" />
			</el-form-item>
			<el-form-item :label="t('classTemplate.zeroPad')">
				<el-radio-group v-model="form.zeroPad">
					<el-radio value="1" border>{{ t('common.yes') }}</el-radio>
					<el-radio value="0" border>{{ t('common.no') }}</el-radio>
				</el-radio-group>
			</el-form-item>
			<el-form-item :label="t('classTemplate.description')">
				<el-input v-model="form.description" type="textarea" />
			</el-form-item>
		</el-form>
		<template #footer>
			<span>
				<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
				<el-button :loading="loading" @click="onSubmit" type="primary">{{ t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-drawer>
</template>

<script lang="ts" name="ClassificationRuleDrawer" setup>
import { getRule, saveRule } from '/@/api/ontology/class-template';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const visible = ref(false);
const loading = ref(false);

const form = reactive({
	id: '' as any,
	treeRoot: 'equipment',
	separator: '-',
	levelDigits: 2,
	baseNumber: 30,
	zeroPad: '1',
	description: '',
});

const open = async (treeRoot = 'equipment') => {
	visible.value = true;
	form.treeRoot = treeRoot;
	await loadRule(treeRoot);
};

const loadRule = async (treeRoot: string) => {
	loading.value = true;
	try {
		const { data } = await getRule(treeRoot);
		if (data) {
			Object.assign(form, data);
		} else {
			// 无规则时用默认值
			form.id = '';
			form.separator = '-';
			form.levelDigits = 2;
			form.baseNumber = 30;
			form.zeroPad = '1';
			form.description = '';
		}
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const onSubmit = async () => {
	if (loading.value) return;
	loading.value = true;
	try {
		await saveRule(form);
		useMessage().success(t('common.optSuccessText'));
		visible.value = false;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({ open });
</script>

<style scoped>
.mb12 {
	margin-bottom: 12px;
}
</style>
