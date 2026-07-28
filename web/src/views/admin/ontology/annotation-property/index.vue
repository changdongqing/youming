<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item :label="t('annotationProperty.appliesTo')" prop="appliesTo">
						<el-select :placeholder="t('annotationProperty.selectAppliesToTip')" style="max-width: 200px" v-model="state.queryForm.appliesTo" clearable @change="getDataList">
							<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in appliesToOptions" />
						</el-select>
					</el-form-item>
					<el-form-item :label="t('annotationProperty.localName')" prop="keyword">
						<el-input :placeholder="t('annotationProperty.inputLocalNameTip')" style="max-width: 180px" v-model="state.queryForm.keyword" @keyup.enter="getDataList" />
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">{{ t('common.queryBtn') }}</el-button>
						<el-button @click="resetQuery" icon="Refresh">{{ t('common.resetBtn') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button v-auth="'ont_ap_manage'" @click="formDialogRef.openDialog()" class="ml10" icon="folder-add" type="primary">
						{{ t('common.addBtn') }}
					</el-button>
					<el-button v-auth="'ont_ap_view'" @click="handleExport" class="ml10" icon="Download" type="success">
						{{ t('annotationProperty.export') }}
					</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-table :data="pagedList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column :label="t('annotationProperty.index')" type="index" width="60" />
				<el-table-column :label="t('annotationProperty.localName')" prop="localName" show-overflow-tooltip>
					<template #default="scope">
						<span>ont:{{ scope.row.localName }}</span>
					</template>
				</el-table-column>
				<el-table-column :label="t('annotationProperty.label')" prop="label" show-overflow-tooltip />
				<el-table-column :label="t('annotationProperty.rangeXsd')" prop="rangeXsd" width="140" show-overflow-tooltip />
				<el-table-column :label="t('annotationProperty.appliesTo')" prop="appliesTo" width="160">
					<template #default="scope">
						<el-tag size="small" v-if="scope.row.appliesTo">{{ appliesToLabel(scope.row.appliesTo) }}</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('annotationProperty.description')" prop="description" show-overflow-tooltip />
				<el-table-column :label="t('annotationProperty.source')" prop="source" width="80">
					<template #default="scope">
						<el-tag :type="scope.row.source === 'builtin' ? 'warning' : 'info'" size="small">
							{{ scope.row.source === 'builtin' ? t('annotationProperty.builtin') : t('annotationProperty.custom') }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('common.action')" width="160">
					<template #default="scope">
						<el-tooltip :content="t('annotationProperty.builtinEditDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
							<span>
								<el-button
									icon="edit-pen"
									@click="formDialogRef.openDialog(scope.row.id)"
									:disabled="scope.row.source === 'builtin'"
									text
									type="primary"
									v-auth="'ont_ap_manage'"
								>
									{{ t('common.editBtn') }}
								</el-button>
							</span>
						</el-tooltip>
						<el-tooltip :content="t('annotationProperty.builtinDeleteDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
							<span style="margin-left: 12px">
								<el-button
									icon="delete"
									@click="handleDelete([scope.row.id])"
									:disabled="scope.row.source === 'builtin'"
									text
									type="primary"
									v-auth="'ont_ap_manage'"
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

<script lang="ts" name="ontologyAnnotationProperty" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, exportObj, listObj } from '/@/api/ontology/annotation-property';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { useAnnotationPropertyOptions } from './composables';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const { t } = useI18n();

const formDialogRef = ref();
const queryRef = ref();
const showSearch = ref(true);

const { appliesToOptions, appliesToLabel } = useAnnotationPropertyOptions();

// 注册表体量小（≤100），后端 /list 返回全量，前端做 appliesTo 过滤 + 关键字搜索 + 客户端分页
const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: false,
	queryForm: {
		appliesTo: '',
		keyword: '',
	},
	pageList: listObj,
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 客户端关键字过滤（appliesTo 过滤走后端 list 参数，关键字走前端本地）
const filteredList = computed(() => {
	const kw = (state.queryForm.keyword || '').trim().toLowerCase();
	if (!kw) return state.dataList;
	return state.dataList.filter(
		(r: any) => r.localName?.toLowerCase().includes(kw) || r.label?.toLowerCase().includes(kw)
	);
});

// 客户端分页切片
const pagedList = computed(() => {
	const current = state.pagination?.current ?? 1;
	const size = state.pagination?.size ?? 10;
	const start = (current - 1) * size;
	return filteredList.value.slice(start, start + size);
});

// 分页 total 跟随过滤结果
watchEffect(() => {
	if (state.pagination) {
		state.pagination.total = filteredList.value.length;
	}
});

const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

const handleDelete = async (ids: string[]) => {
	try {
		await useMessageBox().confirm(t('annotationProperty.deleteTip'));
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

const handleExport = async () => {
	try {
		const { data } = await exportObj({ appliesTo: state.queryForm.appliesTo || undefined });
		const blob = new Blob([data], { type: 'text/markdown;charset=utf-8' });
		const url = URL.createObjectURL(blob);
		const a = document.createElement('a');
		a.href = url;
		a.download = 'annotation-property.md';
		a.click();
		URL.revokeObjectURL(url);
		useMessage().success(t('annotationProperty.exportSuccess'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>
