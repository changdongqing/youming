<template>
	<el-dialog :close-on-click-modal="false" title="测试任务单详情" draggable v-model="visible" width="1000px" top="5vh">
		<div v-loading="loading">
			<el-descriptions :column="3" border class="mb20">
				<el-descriptions-item label="编号">{{ detail.taskCode }}</el-descriptions-item>
				<el-descriptions-item label="关联需求">{{ detail.requirementCode }}</el-descriptions-item>
				<el-descriptions-item label="关联任务">{{ detail.devTaskCode }}</el-descriptions-item>
				<el-descriptions-item label="结论"><el-tag :type="detail.conclusion==='PASS'?'success':'danger'">{{ detail.conclusion }}</el-tag></el-descriptions-item>
				<el-descriptions-item label="Bug总数">{{ detail.bugCount }}</el-descriptions-item>
				<el-descriptions-item label="未解决Bug">{{ detail.unresolvedBugCount }}</el-descriptions-item>
			</el-descriptions>
			<div class="mb20" v-if="detail.status === 'IN_TEST' || detail.status === 'PENDING_TEST'">
				<el-button type="success" v-auth="'rm_tst_execute'" :disabled="detail.unresolvedBugCount > 0" @click="handlePass">测试通过</el-button>
				<el-button type="danger" v-auth="'rm_tst_execute'" @click="handleReject">测试驳回</el-button>
			</div>
			<el-card header="测试执行清单" class="mb20">
				<el-table :data="detail.executions" border>
					<el-table-column prop="caseId" label="用例ID" width="120" />
					<el-table-column prop="result" label="结果" width="80"><template #default="scope"><el-tag :type="resultType(scope.row.result)">{{ scope.row.result }}</el-tag></template></el-table-column>
					<el-table-column prop="remark" label="备注" show-overflow-tooltip />
					<el-table-column prop="executeTime" label="执行时间" width="160" />
				</el-table>
			</el-card>
			<el-card header="关联Bug">
				<el-table :data="detail.bugs" border>
					<el-table-column prop="bugCode" label="编号" width="140" />
					<el-table-column prop="title" label="标题" show-overflow-tooltip />
					<el-table-column prop="severity" label="等级" width="80" />
					<el-table-column prop="status" label="状态" width="80" />
					<el-table-column prop="createDate" label="提报日期" width="120" />
				</el-table>
			</el-card>
		</div>
	</el-dialog>
</template>

<script lang="ts" name="rmTestTaskDetail" setup>
import { useMessage, useMessageBox } from '/@/hooks/message';
import { getObj, passTest, rejectTest } from '/@/api/rm/test-task';
const visible = ref(false); const loading = ref(false); const detail = ref<any>({});
const resultType = (r: string) => ({ PASS:'success', FAIL:'danger', BLOCKED:'warning' }[r] || 'info');
const openDetail = async (id: string) => { visible.value = true; loading.value = true; try { const { data } = await getObj(id); detail.value = data; } catch(err:any){useMessage().error(err.msg);} finally { loading.value=false; } };
const handlePass = async () => { try { await useMessageBox().confirm('确认测试通过？'); await passTest(detail.value.id); useMessage().success('测试通过'); openDetail(detail.value.id); } catch {} };
const handleReject = async () => { try { const { value } = await useMessageBox().prompt('请输入驳回说明','测试驳回',{inputPattern:/.+/}); await rejectTest(detail.value.id, value); useMessage().success('测试驳回'); openDetail(detail.value.id); } catch {} };
defineExpose({ openDetail });
</script>
