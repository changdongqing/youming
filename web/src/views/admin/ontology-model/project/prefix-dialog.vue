<template>
	<el-dialog :close-on-click-modal="false" :title="t('modelProject.prefixDialogTitle') + ' - ' + projectName" draggable v-model="visible" width="780px">
		<el-row>
			<div class="mb8" style="width: 100%">
				<el-button v-auth="'ont_project_manage'" @click="onOpenPrefixForm()" class="ml10" icon="folder-add" type="primary">
					{{ t('modelProject.addPrefix') }}
				</el-button>
			</div>
		</el-row>
		<el-table :data="prefixList" style="width: 100%" v-loading="loading" border>
			<el-table-column :label="t('modelProject.index')" type="index" width="60" />
			<el-table-column :label="t('modelProject.prefix')" prop="prefix" show-overflow-tooltip />
			<el-table-column :label="t('modelProject.namespace')" prop="namespace" show-overflow-tooltip />
			<el-table-column :label="t('modelProject.isDefault')" prop="isDefault" width="100">
				<template #default="scope">
					<el-tag size="small" type="success" v-if="scope.row.isDefault === '1'">{{ t('modelProject.yes') }}</el-tag>
					<el-tag size="small" type="info" v-else>{{ t('modelProject.no') }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column :label="t('common.action')" width="140">
				<template #default="scope">
					<el-button icon="edit-pen" @click="onOpenPrefixForm(scope.row)" text type="primary" v-auth="'ont_project_manage'">
						{{ t('common.editBtn') }}
					</el-button>
					<el-button icon="delete" @click="handleDelete(scope.row)" text type="primary" v-auth="'ont_project_manage'">
						{{ t('common.delBtn') }}
					</el-button>
				</template>
			</el-table-column>
		</el-table>

		<!-- 内嵌前缀表单弹窗 -->
		<el-dialog :close-on-click-modal="false" :title="prefixForm.id ? t('common.editBtn') : t('modelProject.addPrefix')" append-to-body draggable v-model="prefixVisible" width="520px">
			<el-form :model="prefixForm" :rules="prefixRules" label-width="100px" ref="prefixFormRef">
				<el-form-item :label="t('modelProject.prefix')" prop="prefix">
					<el-input :disabled="prefixForm.id !== ''" :placeholder="t('modelProject.inputPrefixTip')" v-model="prefixForm.prefix" />
				</el-form-item>
				<el-form-item :label="t('modelProject.namespace')" prop="namespace">
					<el-input :placeholder="t('modelProject.inputNamespaceTip')" v-model="prefixForm.namespace" />
				</el-form-item>
				<el-form-item :label="t('modelProject.isDefault')" prop="isDefault">
					<el-switch active-value="1" inactive-value="0" v-model="prefixForm.isDefault" />
				</el-form-item>
			</el-form>
			<template #footer>
				<span class="dialog-footer">
					<el-button @click="prefixVisible = false">{{ t('common.cancelButtonText') }}</el-button>
					<el-button @click="onSubmitPrefix" type="primary">{{ t('common.confirmButtonText') }}</el-button>
				</span>
			</template>
		</el-dialog>
	</el-dialog>
</template>

<script lang="ts" name="ModelPrefixDialog" setup>
import { useMessage, useMessageBox } from '/@/hooks/message';
import { addPrefix, delPrefix, listPrefix, putPrefix } from '/@/api/ontology-model/project';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const visible = ref(false);
const loading = ref(false);
const projectId = ref('');
const projectName = ref('');
const prefixList = ref<any[]>([]);

const prefixVisible = ref(false);
const prefixFormRef = ref();
const prefixForm = reactive({
	id: '',
	prefix: '',
	namespace: '',
	isDefault: '0',
});

const prefixRules = computed(() => ({
	prefix: [{ required: true, message: t('modelProject.inputPrefixTip'), trigger: 'blur' }],
	namespace: [{ required: true, message: t('modelProject.inputNamespaceTip'), trigger: 'blur' }],
}));

const openDialog = async (id: string, name: string) => {
	visible.value = true;
	projectId.value = id;
	projectName.value = name;
	await getPrefixList();
};

const getPrefixList = async () => {
	loading.value = true;
	try {
		const { data } = await listPrefix(projectId.value);
		prefixList.value = data ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const onOpenPrefixForm = (row?: any) => {
	prefixVisible.value = true;
	prefixForm.id = '';
	prefixForm.prefix = '';
	prefixForm.namespace = '';
	prefixForm.isDefault = '0';
	nextTick(() => {
		prefixFormRef.value?.resetFields();
	});
	if (row) {
		prefixForm.id = row.id;
		prefixForm.prefix = row.prefix;
		prefixForm.namespace = row.namespace;
		prefixForm.isDefault = row.isDefault ?? '0';
	}
};

const onSubmitPrefix = async () => {
	const valid = await prefixFormRef.value.validate().catch(() => {});
	if (!valid) return;
	try {
		prefixForm.id ? await putPrefix(projectId.value, prefixForm) : await addPrefix(projectId.value, prefixForm);
		useMessage().success(t(prefixForm.id ? 'common.editSuccessText' : 'common.addSuccessText'));
		prefixVisible.value = false;
		getPrefixList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm(t('modelProject.deletePrefixTip'));
	} catch {
		return;
	}
	try {
		await delPrefix(projectId.value, row.id);
		useMessage().success(t('common.delSuccessText'));
		getPrefixList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

defineExpose({
	openDialog,
});
</script>
