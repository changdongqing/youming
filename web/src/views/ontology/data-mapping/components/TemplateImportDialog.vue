<template>
	<el-dialog v-model="visible" title="从模板创建映射工程" width="560px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading">
			<el-form-item label="映射模板" prop="templateCode">
				<el-select v-model="form.templateCode" placeholder="选择模板" style="width: 100%" @change="onTemplateChange">
					<el-option
						v-for="t in templates"
						:key="t.templateCode"
						:label="`${t.templateName} (${t.templateCode})`"
						:value="t.templateCode"
					>
						<span style="float: left">{{ t.templateName }}</span>
						<span style="float: right; color: #8492a6; font-size: 12px">
							{{ t.entityMappingCount }}实体/{{ t.relationMappingCount }}关系
						</span>
					</el-option>
				</el-select>
			</el-form-item>
			<el-form-item label="数据源" prop="dataSourceId">
				<el-select v-model="form.dataSourceId" placeholder="选择已配置的数据源" style="width: 100%" filterable>
					<el-option
						v-for="ds in dataSources"
						:key="ds.id"
						:label="`${ds.sourceName} (${ds.sourceCode})`"
						:value="ds.id"
					/>
				</el-select>
			</el-form-item>
			<el-form-item label="映射编码" prop="mappingCodeOverride">
				<el-input
					v-model="form.mappingCodeOverride"
					placeholder="留空则使用模板默认编码"
					clearable
				/>
				<div class="form-tip">以字母开头，仅含字母、数字、下划线，如 UPMS_ORGANIZATION</div>
			</el-form-item>
			<el-alert v-if="selectedTemplate" type="info" :closable="false" style="margin-top: 8px">
				{{ selectedTemplate.description }}
			</el-alert>
		</el-form>
		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" :loading="submitting" @click="handleSubmit">确认导入</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { dataSourceApi, mappingProjectApi } from '/@/api/ontology/data-mapping';
import type { MappingTemplateSummary, TemplateImportRequest, DataSourceVO } from '/@/types/ontology/data-mapping';

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const templates = ref<MappingTemplateSummary[]>([]);
const dataSources = ref<DataSourceVO[]>([]);

const form = reactive({
	templateCode: '',
	dataSourceId: undefined as number | undefined,
	mappingCodeOverride: ''
});

const rules: FormRules = {
	templateCode: [{ required: true, message: '请选择映射模板', trigger: 'change' }],
	dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }]
};

const selectedTemplate = computed(() =>
	templates.value.find((t: MappingTemplateSummary) => t.templateCode === form.templateCode)
);

const openDialog = async () => {
	resetForm();
	visible.value = true;
	await loadOptions();
};

const resetForm = () => {
	form.templateCode = '';
	form.dataSourceId = undefined;
	form.mappingCodeOverride = '';
	formRef.value?.resetFields();
};

const loadOptions = async () => {
	loading.value = true;
	try {
		const [tplRes, dsRes] = await Promise.all([
			mappingProjectApi.listTemplates(),
			dataSourceApi.page({ current: 1, size: 200 })
		]);
		templates.value = tplRes.data || [];
		dataSources.value = ((dsRes.data?.records || []) as DataSourceVO[]).filter(
			(ds) => ds.status === 'ACTIVE'
		);
	} catch (err: any) {
		msgError('加载选项失败: ' + (err?.message || err));
	} finally {
		loading.value = false;
	}
};

const onTemplateChange = () => {
	// 选中模板后自动填充默认编码提示
};

const handleSubmit = async () => {
	if (!formRef.value) return;
	await formRef.value.validate(async (valid: boolean) => {
		if (!valid) return;
		submitting.value = true;
		try {
			const payload: TemplateImportRequest = {
				templateCode: form.templateCode,
				dataSourceId: form.dataSourceId!,
				mappingCodeOverride: form.mappingCodeOverride || undefined
			};
			await mappingProjectApi.createProjectFromTemplate(payload);
			msgSuccess('模板导入成功，已创建映射工程及全部映射配置');
			emit('success');
			visible.value = false;
		} catch (err: any) {
			msgError('模板导入失败: ' + (err?.message || err));
		} finally {
			submitting.value = false;
		}
	});
};

const emit = defineEmits<{
	(e: 'success'): void;
}>();

defineExpose({ openDialog });
</script>

<style scoped>
.form-tip {
	font-size: 12px;
	color: #909399;
	line-height: 1.4;
	margin-top: 2px;
}
</style>
