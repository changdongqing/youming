<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item :label="t('modelProject.name')" prop="name">
						<el-input :placeholder="t('modelProject.inputNameTip')" style="max-width: 180px" v-model="state.queryForm.name" @keyup.enter="getDataList" />
					</el-form-item>
					<el-form-item :label="t('modelProject.status')" prop="status">
						<el-select :placeholder="t('modelProject.selectStatusTip')" style="max-width: 160px" v-model="state.queryForm.status" clearable @change="getDataList">
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in statusOptions" />
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">{{ t('common.queryBtn') }}</el-button>
						<el-button @click="resetQuery" icon="Refresh">{{ t('common.resetBtn') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button v-auth="'ont_project_manage'" @click="formDialogRef.openDialog()" class="ml10" icon="folder-add" type="primary">
						{{ t('common.addBtn') }}
					</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-table :data="state.dataList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column :label="t('modelProject.index')" type="index" width="60" />
				<el-table-column :label="t('modelProject.projectCode')" prop="projectCode" show-overflow-tooltip />
				<el-table-column :label="t('modelProject.name')" prop="name" show-overflow-tooltip />
				<el-table-column :label="t('modelProject.namespaceBase')" prop="namespaceBase" show-overflow-tooltip />
				<el-table-column :label="t('modelProject.defaultFormat')" prop="defaultFormat" width="120">
					<template #default="scope">
						<el-tag size="small">{{ scope.row.defaultFormat }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('modelProject.status')" prop="status" width="100">
					<template #default="scope">
						<el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('common.action')" width="220">
					<template #default="scope">
						<el-button icon="edit-pen" @click="formDialogRef.openDialog(scope.row.id)" text type="primary" v-auth="'ont_project_manage'">
							{{ t('common.editBtn') }}
						</el-button>
						<el-button icon="Connection" @click="onOpenPrefix(scope.row)" text type="primary" v-auth="'ont_project_view'">
							{{ t('modelProject.prefixManage') }}
						</el-button>
						<el-button icon="delete" @click="handleDelete(scope.row)" text type="primary" v-auth="'ont_project_manage'">
							{{ t('common.delBtn') }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList()" ref="formDialogRef" />
		<prefix-dialog ref="prefixDialogRef" />
	</div>
</template>

<script lang="ts" name="ontologyModelProject" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, pageList } from '/@/api/ontology-model/project';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { useModelProjectOptions } from './composables';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const PrefixDialog = defineAsyncComponent(() => import('./prefix-dialog.vue'));
const { t } = useI18n();

const formDialogRef = ref();
const prefixDialogRef = ref();
const queryRef = ref();
const showSearch = ref(true);

const { statusOptions, statusLabel, statusTagType } = useModelProjectOptions();

// 服务端分页（项目体量可增长，用后端分页 + 名称模糊/状态过滤）
const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: true,
	queryForm: {
		name: '',
		status: '',
	},
	pageList: pageList,
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

const onOpenPrefix = (row: any) => {
	prefixDialogRef.value.openDialog(row.id, row.name);
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm(t('modelProject.deleteTip') + '「' + row.name + '」？');
	} catch {
		return;
	}
	try {
		await delObj(row.id);
		useMessage().success(t('common.delSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>
