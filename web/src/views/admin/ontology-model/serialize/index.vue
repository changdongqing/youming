<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10 mb8">
				<el-form :inline="true">
					<el-form-item :label="t('modelSerialize.selectProject')">
						<el-select v-model="state.projectId" :placeholder="t('modelSerialize.selectProject')" style="width: 200px" @change="onProjectChange">
							<el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
						</el-select>
					</el-form-item>
					<el-form-item :label="t('modelSerialize.format')">
						<el-select v-model="state.format" style="width: 160px">
							<el-option v-for="o in formatOptions" :key="o.value" :label="o.label" :value="o.value" />
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button @click="handlePreview" icon="view" type="primary">{{ t('modelSerialize.preview') }}</el-button>
						<el-button @click="handleDownload" icon="Download" type="success" :disabled="!state.projectId">{{ t('modelSerialize.download') }}</el-button>
						<el-button v-auth="'ont_serialize_manage'" @click="onOpenImport" icon="Upload" type="warning" :disabled="!state.projectId">{{ t('modelSerialize.importRdf') }}</el-button>
						<el-button @click="handleValidate" icon="Check" :disabled="!state.projectId">{{ t('modelSerialize.validateConsistency') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row class="ml10 mb8" v-if="state.classCount !== null">
				<el-tag class="mr8" type="primary">{{ t('modelSerialize.classCount') }}: {{ state.classCount }}</el-tag>
				<el-tag class="mr8" type="success">{{ t('modelSerialize.datatypePropertyCount') }}: {{ state.datatypePropertyCount }}</el-tag>
				<el-tag class="mr8" type="warning">{{ t('modelSerialize.objectPropertyCount') }}: {{ state.objectPropertyCount }}</el-tag>
				<el-tag class="mr8" type="info">{{ t('modelSerialize.subclassOfCount') }}: {{ state.subclassOfCount }}</el-tag>
			</el-row>
			<div class="ml10 mr10">
				<el-card shadow="never" v-if="state.content">
					<code-editor v-model="state.content" :mode="state.format === 'TTL' ? 'text/turtle' : 'application/xml'" theme="darcula" :read-only="true" :height="500" />
				</el-card>
				<el-empty v-else :description="t('modelSerialize.emptyContent')" />
			</div>
		</div>
		<import-dialog ref="importDialogRef" :project-id="state.projectId" @refresh="handlePreview" />
	</div>
</template>

<script lang="ts" name="ontologyModelSerialize" setup>
import { useMessage } from '/@/hooks/message';
import { preview, downloadUrl, validate } from '/@/api/ontology-model/serialize';
import { pageList as pageProject } from '/@/api/ontology-model/project';
import { useI18n } from 'vue-i18n';
import { useSerializeOptions } from './composables';

const CodeEditor = defineAsyncComponent(() => import('/@/components/CodeEditor/index.vue'));
const ImportDialog = defineAsyncComponent(() => import('./import-dialog.vue'));
const { t } = useI18n();

const importDialogRef = ref();
const { formatOptions } = useSerializeOptions();

const projectList = ref<any[]>([]);

const state = reactive({
	projectId: '',
	format: 'TTL',
	content: '',
	classCount: null as number | null,
	datatypePropertyCount: null as number | null,
	objectPropertyCount: null as number | null,
	subclassOfCount: null as number | null,
});

const loadProjects = async () => {
	try {
		const { data } = await pageProject({ size: 200 });
		projectList.value = data?.records ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const onProjectChange = () => {
	state.content = '';
	state.classCount = null;
};

const handlePreview = async () => {
	if (!state.projectId) return;
	try {
		const { data } = await preview(state.projectId, state.format);
		state.content = data.content;
		state.classCount = data.classCount;
		state.datatypePropertyCount = data.datatypePropertyCount;
		state.objectPropertyCount = data.objectPropertyCount;
		state.subclassOfCount = data.subclassOfCount;
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleDownload = () => {
	if (!state.projectId) return;
	window.open(downloadUrl(state.projectId, state.format));
};

const handleValidate = async () => {
	if (!state.projectId) return;
	try {
		const { data } = await validate(state.projectId);
		useMessage().success(data ? t('modelSerialize.validateSuccess') : t('modelSerialize.validateFail'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const onOpenImport = () => {
	importDialogRef.value.openDialog();
};

onMounted(() => {
	loadProjects();
});
</script>

<style scoped>
.mr8 {
	margin-right: 8px;
}
</style>
