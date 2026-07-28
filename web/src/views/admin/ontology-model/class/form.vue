<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible" width="720px">
		<el-form :model="form" :rules="dataRules" label-width="110px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.localName')" prop="localName">
						<el-input :disabled="form.id !== ''" :placeholder="t('modelClass.inputLocalNameTip')" v-model="form.localName" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.classIri')">
						<el-input readonly v-model="form.classIri" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.label')" prop="label">
						<el-input :placeholder="t('modelClass.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.labelCn')" prop="labelCn">
						<el-input :placeholder="t('modelClass.inputLabelCnTip')" v-model="form.labelCn" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20" v-if="form.id === ''">
					<el-form-item :label="t('modelClass.selectTemplate')">
						<el-tree-select
							:placeholder="t('modelClass.selectTemplateTip')"
							v-model="form.templateCode"
							:data="templateTreeData"
							:props="{ label: 'label', value: 'templateCode', children: 'children' }"
							check-strictly
							clearable
							filterable
							style="width: 100%"
							@node-click="onTemplateChange"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('modelClass.description')" prop="description">
						<el-input :placeholder="t('modelClass.inputDescriptionTip')" type="textarea" v-model="form.description" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.icon')" prop="icon">
						<el-input v-model="form.icon" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('modelClass.color')" prop="color">
						<el-color-picker v-model="form.color" />
					</el-form-item>
				</el-col>
				<el-col :span="24" v-if="inherited">
					<inherited-preview :inherited="inherited" />
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

<script lang="ts" name="ModelClassDialog" setup>
import { useMessage } from '/@/hooks/message';
import { addObj, getObj, putObj, supplyClassTemplateTree, supplyInherited } from '/@/api/ontology-model/class';
import { getObj as getProject } from '/@/api/ontology-model/project';
import { useI18n } from 'vue-i18n';

const InheritedPreview = defineAsyncComponent(() => import('./inherited-preview.vue'));
const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const templateTreeData = ref<any[]>([]);
const inherited = ref<any>(null);
const namespaceBase = ref('');

const form = reactive({
	id: '',
	projectId: '',
	localName: '',
	classIri: '',
	label: '',
	labelCn: '',
	description: '',
	templateCode: '',
	classificationCode: '',
	icon: '',
	color: '',
});

const dataRules = computed(() => ({
	localName: [{ required: true, message: t('modelClass.inputLocalNameTip'), trigger: 'blur' }],
	label: [{ required: true, message: t('modelClass.inputLabelTip'), trigger: 'blur' }],
}));

// 监听 localName 变化，自动拼 IRI = namespaceBase + localName
watch(
	() => form.localName,
	(val) => {
		if (val && namespaceBase.value) {
			form.classIri = namespaceBase.value + val;
		}
	}
);

const openDialog = async (projectId: string, id?: string) => {
	visible.value = true;
	form.id = '';
	form.projectId = projectId || '';
	form.localName = '';
	form.classIri = '';
	form.label = '';
	form.labelCn = '';
	form.description = '';
	form.templateCode = '';
	form.classificationCode = '';
	form.icon = '';
	form.color = '';
	inherited.value = null;
	namespaceBase.value = '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	// 拉项目命名空间基址（用于 IRI 自动拼接）
	if (projectId) {
		try {
			const { data } = await getProject(projectId);
			namespaceBase.value = data.namespaceBase;
		} catch (err: any) {
			useMessage().error(err.msg);
		}
	}

	// 新建时拉分类模板树（编辑时模板锁定，不拉）
	if (!id) {
		await loadTemplateTree();
	} else {
		form.id = id;
		await getClassData(id);
	}
};

const loadTemplateTree = async () => {
	try {
		const { data } = await supplyClassTemplateTree('equipment');
		templateTreeData.value = data ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const onTemplateChange = async (nodeData: any) => {
	const code = nodeData?.templateCode;
	if (!code) {
		inherited.value = null;
		return;
	}
	try {
		const { data } = await supplyInherited(code);
		inherited.value = data;
	} catch (err: any) {
		useMessage().error(err.msg);
		inherited.value = null;
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

const getClassData = async (id: string) => {
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
