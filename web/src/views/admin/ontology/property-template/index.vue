<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item :label="t('propertyTemplate.kind')" prop="kind">
						<el-select :placeholder="t('propertyTemplate.selectKindTip')" style="max-width: 180px" v-model="state.queryForm.kind" clearable>
							<el-option key="datatype" :label="t('propertyTemplate.datatype')" value="datatype" />
							<el-option key="object" :label="t('propertyTemplate.object')" value="object" />
						</el-select>
					</el-form-item>
					<el-form-item :label="t('propertyTemplate.category')" prop="category">
						<el-select :placeholder="t('propertyTemplate.selectCategoryTip')" style="max-width: 180px" v-model="state.queryForm.category" clearable>
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in categoryOptions" />
						</el-select>
					</el-form-item>
					<el-form-item :label="t('propertyTemplate.templateCode')" prop="templateCode">
						<el-input :placeholder="t('propertyTemplate.inputTemplateCodeTip')" style="max-width: 180px" v-model="state.queryForm.templateCode" />
					</el-form-item>
					<el-form-item :label="t('propertyTemplate.deprecated')" prop="deprecated">
						<el-select :placeholder="t('propertyTemplate.selectDeprecatedTip')" style="max-width: 180px" v-model="state.queryForm.deprecated" clearable>
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in deprecatedOptions" />
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
					<el-button v-auth="'ont_prop_tpl_manage'" @click="formDialogRef.openDialog()" class="ml10" icon="folder-add" type="primary">
						{{ t('common.addBtn') }}
					</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-table :data="state.dataList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column :label="t('propertyTemplate.index')" type="index" width="60" />
				<el-table-column :label="t('propertyTemplate.templateCode')" prop="templateCode" show-overflow-tooltip />
				<el-table-column :label="t('propertyTemplate.kind')" prop="kind" width="100">
					<template #default="scope">
						<el-tag :type="scope.row.kind === 'datatype' ? 'primary' : 'success'" size="small">
							{{ scope.row.kind === 'datatype' ? t('propertyTemplate.datatype') : t('propertyTemplate.object') }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('propertyTemplate.label')" prop="label" show-overflow-tooltip />
				<el-table-column :label="t('propertyTemplate.category')" prop="category" width="100" show-overflow-tooltip />
				<el-table-column :label="t('propertyTemplate.type')" prop="type" width="100" show-overflow-tooltip />
				<el-table-column :label="t('propertyTemplate.source')" prop="source" width="80">
					<template #default="scope">
						<el-tag :type="scope.row.source === 'builtin' ? 'warning' : 'info'" size="small">
							{{ scope.row.source === 'builtin' ? t('propertyTemplate.builtin') : t('propertyTemplate.custom') }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('propertyTemplate.deprecated')" prop="deprecated" width="80">
					<template #default="scope">
						<el-tag :type="scope.row.deprecated === '1' ? 'danger' : 'success'" size="small">
							{{ scope.row.deprecated === '1' ? t('propertyTemplate.deprecatedLabel') : t('propertyTemplate.normal') }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('propertyTemplate.createTime')" prop="createTime" width="160" show-overflow-tooltip />
				<el-table-column :label="t('common.action')" width="200">
					<template #default="scope">
						<el-tooltip :content="t('propertyTemplate.builtinEditDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
							<span>
								<el-button
									icon="edit-pen"
									@click="formDialogRef.openDialog(scope.row.id)"
									:disabled="scope.row.source === 'builtin'"
									text
									type="primary"
									v-auth="'ont_prop_tpl_manage'"
								>
									{{ t('common.editBtn') }}
								</el-button>
							</span>
						</el-tooltip>
						<el-button
							@click="handleDeprecate(scope.row)"
							:text="true"
							:type="scope.row.deprecated === '1' ? 'success' : 'warning'"
							v-auth="'ont_prop_tpl_manage'"
						>
							{{ scope.row.deprecated === '1' ? t('common.restoreBtn') : t('propertyTemplate.deprecatedLabel') }}
						</el-button>
						<el-tooltip :content="t('propertyTemplate.builtinDeleteDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
							<span style="margin-left: 12px">
								<el-button
									icon="delete"
									@click="handleDelete([scope.row.id])"
									:disabled="scope.row.source === 'builtin'"
									text
									type="primary"
									v-auth="'ont_prop_tpl_manage'"
								>
									{{ t('common.delBtn') }}
								</el-button>
							</span>
						</el-tooltip>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList()" ref="formDialogRef" />
	</div>
</template>

<script lang="ts" name="ontologyPropertyTemplate" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, deprecateObj, pageList } from '/@/api/ontology/property-template';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { usePropertyTemplateOptions } from './composables';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const { t } = useI18n();

const formDialogRef = ref();
const queryRef = ref();
const showSearch = ref(true);

const { categoryOptions, deprecatedOptions } = usePropertyTemplateOptions();

const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {
		kind: '',
		category: '',
		templateCode: '',
		deprecated: '',
	},
	pageList: pageList,
	descs: ['create_time'],
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

const handleDelete = async (ids: string[]) => {
	try {
		await useMessageBox().confirm(t('propertyTemplate.deleteTip'));
	} catch {
		return;
	}
	try {
		await delObj(ids[0]);
		useMessage().success(t('common.delSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleDeprecate = async (row: any) => {
	const newDeprecated = row.deprecated === '1' ? '0' : '1';
	const tip = newDeprecated === '1' ? t('propertyTemplate.deprecateTip') : t('propertyTemplate.restoreTip');
	try {
		await useMessageBox().confirm(tip);
	} catch {
		return;
	}
	try {
		await deprecateObj(row.id, newDeprecated);
		useMessage().success(t('common.optSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>
