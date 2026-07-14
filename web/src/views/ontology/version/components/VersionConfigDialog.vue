<template>
	<el-dialog v-model="show" title="版本配置" width="560px" :close-on-click-modal="false">
		<el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
			<el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
				配置本体标识IRI和版本IRI基础路径，用于发布版本时拼接版本IRI。
				已有已发布版本时，本体IRI不可修改。
			</el-alert>
			<el-form-item label="本体IRI" prop="ontologyIri">
				<el-input v-model="form.ontologyIri" placeholder="如 http://example.org/standard-ontology"
					:disabled="hasPublishedVersion" style="width: 360px" />
			</el-form-item>
			<el-form-item label="版本IRI基础路径" prop="versionIriBase">
				<el-input v-model="form.versionIriBase" placeholder="如 http://example.org/standard-ontology/version/"
					style="width: 360px" />
				<span class="form-hint">必须以/结尾</span>
			</el-form-item>
		</el-form>

		<template #footer>
			<el-button @click="show = false">取消</el-button>
			<el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { updateVersionConfig, fetchVersionPage } from '/@/api/ontology/version';

const props = defineProps<{ visible: boolean; projectId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void; (e: 'success'): void }>();

const { success: msgSuccess, error: msgError } = useMessage();
const formRef = ref<FormInstance>();
const loading = ref(false);
const hasPublishedVersion = ref(false);

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const form = ref({ ontologyIri: '', versionIriBase: '' });

const rules: FormRules = {
	ontologyIri: [
		{ required: true, message: '请输入本体IRI', trigger: 'blur' },
		{ pattern: /^https?:\/\//, message: '必须为绝对IRI（http://或https://开头）', trigger: 'blur' },
	],
	versionIriBase: [
		{ required: true, message: '请输入版本IRI基础路径', trigger: 'blur' },
		{ pattern: /^https?:\/\/.*\/$/, message: '必须以/结尾', trigger: 'blur' },
	],
};

const checkPublishedVersions = async () => {
	if (!props.projectId) return;
	try {
		const { data } = await fetchVersionPage({
			ontologyId: props.projectId,
			releaseStatus: 'PUBLISHED',
			pageNum: 1,
			pageSize: 1,
		});
		hasPublishedVersion.value = data.total > 0;
	} catch {
		hasPublishedVersion.value = false;
	}
};

const handleSave = async () => {
	if (!formRef.value) return;
	await formRef.value.validate(async (valid) => {
		if (!valid) return;
		loading.value = true;
		try {
			await updateVersionConfig(props.projectId, form.value);
			msgSuccess('版本配置保存成功');
			show.value = false;
			emit('success');
		} catch (e: any) {
			msgError(e.message || '保存失败');
		} finally {
			loading.value = false;
		}
	});
};

watch(() => props.visible, (v) => {
	if (v) {
		form.value = { ontologyIri: '', versionIriBase: '' };
		checkPublishedVersions();
	}
});
</script>

<style scoped>
.form-hint { color: var(--el-text-color-secondary); font-size: 12px; margin-left: 8px; }
</style>
