<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item label="关键词" prop="keyword"><el-input v-model="state.queryForm.keyword" placeholder="搜索用例标题" clearable @keyup.enter="getDataList" /></el-form-item>
					<el-form-item><el-button @click="getDataList" icon="search" type="primary">查询</el-button><el-button @click="resetQuery" icon="Refresh">重置</el-button></el-form-item>
				</el-form>
			</el-row>
			<el-row><div class="mb8" style="width:100%">
				<el-button v-auth="'rm_case_add'" @click="formRef.openDialog()" icon="folder-add" type="primary" class="ml10">新增用例</el-button>
				<right-toolbar @queryTable="getDataList" class="ml10" style="float:right;margin-right:20px" v-model:showSearch="showSearch" />
			</div></el-row>
			<el-table :data="state.dataList" v-loading="state.dataListLoading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column type="index" width="60" />
				<el-table-column prop="caseCode" label="编号" width="140" />
				<el-table-column prop="title" label="用例标题" show-overflow-tooltip />
				<el-table-column prop="caseType" label="类型" width="100"><template #default="scope"><dict-tag :options="rm_case_type" :value="scope.row.caseType" /></template></el-table-column>
				<el-table-column prop="status" label="状态" width="80"><template #default="scope"><dict-tag :options="rm_case_type" :value="scope.row.status" /></template></el-table-column>
				<el-table-column label="操作" width="150" fixed="right"><template #default="scope">
					<el-button v-auth="'rm_case_edit'" @click="formRef.openDialog(scope.row.id)" text type="primary">编辑</el-button>
					<el-button v-auth="'rm_case_del'" @click="handleDelete(scope.row.id)" text type="danger">删除</el-button>
				</template></el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList" ref="formRef" />
	</div>
</template>

<script lang="ts" name="rmTestCase" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList, delObj } from '/@/api/rm/test-case';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const formRef = ref(); const queryRef = ref(); const showSearch = ref(true);
const { rm_case_type } = useDict('rm_case_type');
const state: BasicTableProps = reactive<BasicTableProps>({ createdIsNeed: true, queryForm: { keyword: '' }, pageList, descs: ['create_time'] });
const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);
const resetQuery = () => { queryRef.value?.resetFields(); getDataList(); };
const handleDelete = async (id: string) => { try { await useMessageBox().confirm('确认删除？'); await delObj(id); useMessage().success('删除成功'); getDataList(); } catch {} };
</script>
