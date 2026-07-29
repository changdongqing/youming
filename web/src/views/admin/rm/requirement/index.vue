<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item label="来源" prop="source">
						<el-select v-model="state.queryForm.source" clearable placeholder="全部" style="width: 160px">
							<el-option v-for="item in rm_req_source" :key="item.value" :label="item.label" :value="item.value" />
						</el-select>
					</el-form-item>
					<el-form-item label="状态" prop="status">
						<el-select v-model="state.queryForm.status" clearable placeholder="全部" style="width: 160px">
							<el-option v-for="item in rm_req_status" :key="item.value" :label="item.label" :value="item.value" />
						</el-select>
					</el-form-item>
					<el-form-item label="关键词" prop="keyword">
						<el-input v-model="state.queryForm.keyword" placeholder="搜索标题" clearable @keyup.enter="getDataList" />
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">查询</el-button>
						<el-button @click="resetQuery" icon="Refresh">重置</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button v-auth="'rm_req_add'" @click="formRef.openDialog()" icon="folder-add" type="primary" class="ml10">新增需求</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-table :data="state.dataList" style="width: 100%" v-loading="state.dataListLoading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column type="index" width="60" />
				<el-table-column prop="reqCode" label="编号" width="160" show-overflow-tooltip />
				<el-table-column prop="title" label="需求标题" show-overflow-tooltip />
				<el-table-column prop="source" label="来源" width="130">
					<template #default="scope">
						<dict-tag :options="rm_req_source" :value="scope.row.source" />
					</template>
				</el-table-column>
				<el-table-column prop="status" label="状态" width="120">
					<template #default="scope">
						<dict-tag :options="rm_req_status" :value="scope.row.status" />
					</template>
				</el-table-column>
				<el-table-column prop="expectCompleteDate" label="期望完成" width="120" />
				<el-table-column prop="createTime" label="创建时间" width="160" />
				<el-table-column label="操作" width="100" fixed="right">
					<template #default="scope">
						<el-button @click="goDetail(scope.row.id)" text type="primary">详情</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList" ref="formRef" />
	</div>
</template>

<script lang="ts" name="rmRequirement" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList } from '/@/api/rm/requirement';
import { useDict } from '/@/hooks/dict';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const formRef = ref();
const showSearch = ref(true);
const { rm_req_source, rm_req_status } = useDict('rm_req_source', 'rm_req_status');

const state: BasicTableProps = reactive<BasicTableProps>({
	createdIsNeed: true,
	queryForm: { source: '', status: '', keyword: '' },
	pageList: pageList,
	descs: ['create_time'],
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);

const resetQuery = () => {
	queryRef.value?.resetFields();
	getDataList();
};

const queryRef = ref();
const goDetail = (id: string) => {
	// 通过详情弹窗打开
	formRef.value.openDetail(id);
};
</script>
