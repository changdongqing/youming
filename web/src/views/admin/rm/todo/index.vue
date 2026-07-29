<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<!-- 待办统计卡片 -->
			<el-row :gutter="10" class="mb10">
				<el-col :span="4" v-for="(item, type) in todoCount.byType" :key="type">
					<el-card shadow="hover" :class="{ 'todo-active': state.queryForm.todoType === type }" @click="filterByType(type as string)">
						<div style="text-align:center"><div style="font-size:24px;font-weight:bold">{{ item }}</div><div style="color:#999">{{ todoTypeLabel(type as string) }}</div></div>
					</el-card>
				</el-col>
			</el-row>
			<el-table :data="state.dataList" v-loading="state.dataListLoading" border>
				<el-table-column prop="title" label="待办标题" show-overflow-tooltip />
				<el-table-column prop="todoType" label="类型" width="100"><template #default="scope">{{ todoTypeLabel(scope.row.todoType) }}</template></el-table-column>
				<el-table-column prop="billCode" label="单据编号" width="150" />
				<el-table-column prop="billType" label="单据类型" width="120" />
				<el-table-column prop="dueTime" label="截止时间" width="160"><template #default="scope"><span v-if="scope.row.overdue" style="color:#f56c6c">{{ scope.row.dueTime }} <el-tag type="danger" size="small">超期</el-tag></span><span v-else>{{ scope.row.dueTime }}</span></template></el-table-column>
				<el-table-column label="操作" width="100" fixed="right"><template #default="scope"><el-button @click="goHandle(scope.row)" text type="primary">去处理</el-button></template></el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
	</div>
</template>

<script lang="ts" name="rmTodo" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { myTodo, todoCount } from '/@/api/rm/todo';
import { useMessage } from '/@/hooks/message';

const todoCount = ref<any>({ byType: {}, total: 0 });
const state: BasicTableProps = reactive<BasicTableProps>({ createdIsNeed: true, queryForm: { todoType: '', billType: '' }, pageList: myTodo, descs: ['create_time'] });
const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);

const todoTypeLabel = (type: string) => ({ APPROVE:'待审批', DESIGN:'待设计', REVIEW:'待评审', DEV:'待开发', TEST:'待测试', ACCEPT:'待验收' }[type] || type);
const filterByType = (type: string) => { state.queryForm.todoType = type; getDataList(); };
const goHandle = (row: any) => { if (row.url) window.location.hash = '#' + row.url; };

const loadCount = async () => { try { const { data } = await todoCount(); todoCount.value = data; } catch {} };
onMounted(() => { loadCount(); getDataList(); });
</script>
