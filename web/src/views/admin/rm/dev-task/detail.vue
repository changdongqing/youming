<template>
	<el-dialog :close-on-click-modal="false" title="任务详情" draggable v-model="visible" width="800px">
		<div v-loading="loading">
			<el-descriptions :column="2" border>
				<el-descriptions-item label="编号">{{ detail.taskCode }}</el-descriptions-item>
				<el-descriptions-item label="名称">{{ detail.taskName }}</el-descriptions-item>
				<el-descriptions-item label="状态"><dict-tag :options="rm_dev_task_status" :value="detail.status" /></el-descriptions-item>
				<el-descriptions-item label="评审">{{ detail.reviewConclusion }}</el-descriptions-item>
				<el-descriptions-item label="计划开始">{{ detail.planStartDate }}</el-descriptions-item>
				<el-descriptions-item label="计划完成">{{ detail.planEndDate }}</el-descriptions-item>
				<el-descriptions-item label="实际开始">{{ detail.actualStartDate }}</el-descriptions-item>
				<el-descriptions-item label="实际完成">{{ detail.actualEndDate }}</el-descriptions-item>
				<el-descriptions-item label="所属需求">{{ detail.requirementCode }}</el-descriptions-item>
				<el-descriptions-item label="负责人">{{ detail.assigneeName }}</el-descriptions-item>
				<el-descriptions-item label="任务描述" :span="2">{{ detail.taskDesc }}</el-descriptions-item>
				<el-descriptions-item label="详细设计" :span="2"><div v-html="detail.detailDesign"></div></el-descriptions-item>
			</el-descriptions>
		</div>
	</el-dialog>
</template>

<script lang="ts" name="rmDevTaskDetail" setup>
import { useMessage } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';
import { getObj } from '/@/api/rm/dev-task';

const { rm_dev_task_status } = useDict('rm_dev_task_status');
const visible = ref(false);
const loading = ref(false);
const detail = ref<any>({});

const openDetail = async (id: string) => {
	visible.value = true;
	loading.value = true;
	try { const { data } = await getObj(id); detail.value = data; }
	catch (err: any) { useMessage().error(err.msg); }
	finally { loading.value = false; }
};

defineExpose({ openDetail });
</script>
