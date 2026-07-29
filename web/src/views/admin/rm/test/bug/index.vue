<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item label="来源" prop="source"><el-select v-model="state.queryForm.source" clearable style="width:120px"><el-option v-for="i in rm_bug_source" :key="i.value" :label="i.label" :value="i.value" /></el-select></el-form-item>
					<el-form-item label="状态" prop="status"><el-select v-model="state.queryForm.status" clearable style="width:120px"><el-option v-for="i in rm_bug_status" :key="i.value" :label="i.label" :value="i.value" /></el-select></el-form-item>
					<el-form-item><el-button @click="getDataList" icon="search" type="primary">查询</el-button><el-button @click="resetQuery" icon="Refresh">重置</el-button></el-form-item>
				</el-form>
			</el-row>
			<el-table :data="state.dataList" v-loading="state.dataListLoading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
				<el-table-column type="index" width="60" />
				<el-table-column prop="bugCode" label="编号" width="140" />
				<el-table-column prop="title" label="标题" show-overflow-tooltip />
				<el-table-column prop="source" label="来源" width="100"><template #default="scope"><dict-tag :options="rm_bug_source" :value="scope.row.source" /></template></el-table-column>
				<el-table-column prop="severity" label="等级" width="80"><template #default="scope"><dict-tag :options="rm_bug_severity" :value="scope.row.severity" /></template></el-table-column>
				<el-table-column prop="status" label="状态" width="80"><template #default="scope"><dict-tag :options="rm_bug_status" :value="scope.row.status" /></template></el-table-column>
				<el-table-column prop="createDate" label="提报日期" width="120" />
				<el-table-column label="操作" width="100" fixed="right"><template #default="scope">
					<el-button v-auth="'rm_bug_handle'" v-if="scope.row.status === 'NEW' || scope.row.status === 'IN_PROGRESS'" @click="handleProcess(scope.row)" text type="primary">处理</el-button>
				</template></el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
	</div>
</template>

<script lang="ts" name="rmBug" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList, handle as handleBug } from '/@/api/rm/bug';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';
const queryRef = ref(); const showSearch = ref(true);
const { rm_bug_source, rm_bug_status, rm_bug_severity } = useDict('rm_bug_source', 'rm_bug_status', 'rm_bug_severity');
const state: BasicTableProps = reactive<BasicTableProps>({ createdIsNeed: true, queryForm: { source:'', status:'' }, pageList, descs: ['create_date'] });
const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);
const resetQuery = () => { queryRef.value?.resetFields(); getDataList(); };
const handleProcess = async (row: any) => {
	try { await useMessageBox().confirm('确认标记为已解决？'); await handleBug({ bugId: row.id, targetStatus: 'RESOLVED' }); useMessage().success('已标记为已解决'); getDataList(); } catch {}
};
</script>
