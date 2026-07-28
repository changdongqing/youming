<template>
	<el-dialog :close-on-click-modal="false" :title="t('modelSerialize.importRdf')" draggable v-model="visible" width="640px">
		<el-form label-width="120px">
			<el-form-item :label="t('modelSerialize.conflictStrategy')">
				<el-radio-group v-model="conflictStrategy">
					<el-radio v-for="o in conflictStrategyOptions" :key="o.value" :label="o.value">{{ o.label }}</el-radio>
				</el-radio-group>
			</el-form-item>
			<el-form-item>
				<el-upload
					drag
					:auto-upload="false"
					:limit="1"
					accept=".ttl,.owl.xml,.rdf,.xml"
					:on-change="onFileChange"
					:on-exceed="onExceed"
				>
					<el-icon class="el-icon--upload"><UploadFilled /></el-icon>
					<div class="el-upload__text">{{ t('modelSerialize.uploadTip') }}</div>
				</el-upload>
			</el-form-item>
		</el-form>

		<!-- 导入结果报告 -->
		<el-descriptions v-if="result" :column="2" border size="small" class="mb12" :title="t('modelSerialize.importResult')">
			<el-descriptions-item :label="t('modelSerialize.classCount')">{{ result.classCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.datatypePropertyCount')">{{ result.datatypePropertyCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.objectPropertyCount')">{{ result.objectPropertyCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.subclassOfCount')">{{ result.subclassOfCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.recognizedTemplate')">{{ result.recognizedTemplateCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.orphanTemplate')">{{ result.orphanTemplateCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.recognizedUnit')">{{ result.recognizedUnitCount }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelSerialize.skippedConflict')">{{ result.skippedConflictCount }}</el-descriptions-item>
		</el-descriptions>
		<el-alert v-if="result && result.warnings && result.warnings.length" :title="t('modelSerialize.warnings')" type="warning" :closable="false" class="mb12">
			<div v-for="(w, i) in result.warnings" :key="i">{{ w }}</div>
		</el-alert>
		<el-alert v-if="result && result.error" :title="result.error" type="error" :closable="false" class="mb12" />

		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="!file || loading" :loading="loading">{{ t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="SerializeImportDialog" setup>
import { UploadFilled } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { importRdf } from '/@/api/ontology-model/serialize';
import { useI18n } from 'vue-i18n';
import { useSerializeOptions } from './composables';

const props = defineProps<{ projectId: string }>();
const emit = defineEmits(['refresh']);
const { t } = useI18n();

const visible = ref(false);
const loading = ref(false);
const file = ref<File | null>(null);
const result = ref<any>(null);
const conflictStrategy = ref('SKIP');

const { conflictStrategyOptions } = useSerializeOptions();

const openDialog = () => {
	visible.value = true;
	file.value = null;
	result.value = null;
	conflictStrategy.value = 'SKIP';
};

const onFileChange = (uploadFile: any) => {
	file.value = uploadFile.raw;
};

const onExceed = () => {
	useMessage().warning('只能上传一个文件');
};

const onSubmit = async () => {
	if (!file.value || !props.projectId) return;
	loading.value = true;
	try {
		const { data } = await importRdf(props.projectId, file.value, conflictStrategy.value);
		result.value = data;
		if (!data.error) {
			useMessage().success(t('modelSerialize.importSuccess'));
			emit('refresh');
		}
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({ openDialog });
</script>

<style scoped>
.mb12 {
	margin-bottom: 12px;
}
</style>
