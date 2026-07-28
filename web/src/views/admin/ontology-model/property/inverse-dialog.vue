<template>
	<el-dialog :close-on-click-modal="false" :title="t('modelProperty.inverseDialogTitle')" draggable v-model="visible" width="560px">
		<el-alert :title="t('modelProperty.inverseDialogTip')" type="info" :closable="false" class="mb12" />
		<el-descriptions :column="1" border v-if="suggestion">
			<el-descriptions-item :label="t('modelProperty.inverseSuggestedName')">{{ suggestion.suggestedLocalName }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelProperty.inverseSuggestedLabel')">{{ suggestion.suggestedLabel }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelProperty.domainClass')">{{ getClassName(suggestion.suggestedDomainClassId) }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelProperty.rangeClass')">{{ getClassName(suggestion.suggestedRangeClassId) }}</el-descriptions-item>
		</el-descriptions>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
				<el-button @click="onCreateInverse" type="primary" :disabled="!suggestion">{{ t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="InverseSuggestDialog" setup>
import { useMessage } from '/@/hooks/message';
import { suggestInverse, addObj } from '/@/api/ontology-model/object-property';
import { useI18n } from 'vue-i18n';

const props = defineProps<{ classList: any[] }>();
const emit = defineEmits(['refresh']);
const { t } = useI18n();

const visible = ref(false);
const loading = ref(false);
const suggestion = ref<any>(null);
const originRow = ref<any>(null);

const getClassName = (classId: string) => {
	const cls = props.classList.find((c) => c.id === classId);
	return cls ? (cls.label || cls.localName) : classId;
};

const openDialog = async (row: any) => {
	visible.value = true;
	originRow.value = row;
	suggestion.value = null;
	loading.value = true;
	try {
		const { data } = await suggestInverse({
			localName: row.localName,
			domainClassId: row.domainClassId,
			rangeClassId: row.rangeClassId,
		});
		suggestion.value = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const onCreateInverse = async () => {
	if (!suggestion.value) return;
	try {
		await addObj({
			domainClassId: suggestion.value.suggestedDomainClassId,
			rangeClassId: suggestion.value.suggestedRangeClassId,
			localName: suggestion.value.suggestedLocalName,
			label: suggestion.value.suggestedLabel,
			minCardinality: 0,
			maxCardinality: -1,
		});
		useMessage().success(t('common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

defineExpose({ openDialog });
</script>

<style scoped>
.mb12 {
	margin-bottom: 12px;
}
</style>
