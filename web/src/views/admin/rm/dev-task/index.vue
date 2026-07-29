<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item label="状态" prop="status">
						<el-select v-model="state.queryForm.status" clearable placeholder="全部" style="width: 140px">
							<el-option v-for="item in rm_dev_task_status" :key="item.value" :label="item.label" :value="item.value" />
						</el-select>
					</el-form-item>
					<el-form-item label="关键词" prop="keyword">
						<el-input v-model="state.queryForm.keyword" placeholder="搜索任务名称" clearable @keyup.enter="getDataList" />
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">查询</el-button>
						<el-button @click="resetQuery" icon="Refresh">重置</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button v-auth="'rm_dev_add'" @click="formRef.openDialog()" icon="folder-add" type="primary" class="ml10">任务分解</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-table :data="state.dataList" style="width: 100%" v-loading="state.dataListLoading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column type="index" width="60" />
				<el-table-column prop="taskCode" label="编号" width="150" show-overflow-tooltip />
				<el-table-column prop="taskName" label="任务名称" show-overflow-tooltip />
				<el-table-column prop="status" label="状态" width="100">
					<template #default="scope"><dict-tag :options="rm_dev_task_status" :value="scope.row.status" /></template>
				</el-table-column>
				<el-table-column prop="planEndDate" label="计划完成" width="120" />
				<el-table-column prop="reviewConclusion" label="评审" width="80" />
				<el-table-column label="操作" width="160" fixed="right">
					<template #default="scope">
						<el-button v-auth="'rm_dev_edit'" v-if="scope.row.status === 'PENDING_DEV'" @click="handleStart(scope.row.id)" text type="primary">开始</el-button>
						<el-button v-auth="'rm_dev_edit'" v-if="scope.row.status === 'IN_DEV' && scope.row.reviewConclusion === 'PASS'" @click="handleSubmitTest(scope.row.id)" text type="warning">提测</el-button>
						<el-button @click="goDetail(scope.row.id)" text type="primary">详情</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList" ref="formRef" />
		<detail-dialog ref="detailRef" @refresh="getDataList" />
	</div>
</template>

<script lang="ts" name="rmDevTask" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList, startDev, submitTest } from '/@/api/rm/dev-task';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const DetailDialog = defineAsyncComponent(() => import('./detail.vue'));
const formRef = ref();
const detailRef = ref();
const queryRef = ref();
const showSearch = ref(true);
const { rm_dev_task_status } = useDict('rm_dev_task_status');

const state: BasicTableProps = reactive<BasicTableProps>({
	createdIsNeed: true,
	queryForm: { status: '', keyword: '' },
	pageList: pageList,
	descs: ['create_time'],
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);
const resetQuery = () => { queryRef.value?.resetFields(); getDataList(); };

const handleStart = async (id: string) => {
	try { await useMessageBox().confirm('确认开始开发？'); await startDev(id); useMessage().success('已开始开发'); getDataList(); } catch {}
};
const handleSubmitTest = async (id: string) => {
	try { await useMessageBox().confirm('确认提测？'); await submitTest(id); useMessage().success('已提测'); getDataList(); } catch {}
};
const goDetail = (id: string) => detailRef.value.openDetail(id);
</script>
