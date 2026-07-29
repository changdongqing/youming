<template>
	<el-dialog :close-on-click-modal="false" title="任务分解" draggable v-model="visible" width="900px">
		<el-form :model="state" label-width="100px">
			<el-form-item label="所属需求">
				<el-input v-model="state.requirementId" placeholder="需求ID" />
			</el-form-item>
			<el-row v-for="(task, i) in state.tasks" :key="i" :gutter="10">
				<el-col :span="8"><el-form-item :label="`任务${i+1}`"><el-input v-model="task.taskName" placeholder="任务名称" /></el-form-item></el-col>
				<el-col :span="5"><el-form-item label="计划开始"><el-date-picker v-model="task.planStartDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
				<el-col :span="5"><el-form-item label="计划完成"><el-date-picker v-model="task.planEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
				<el-col :span="4"><el-button v-if="state.tasks.length>1" @click="state.tasks.splice(i,1)" text type="danger"><el-icon><Delete /></el-icon></el-button></el-col>
			</el-row>
			<el-button @click="state.tasks.push({taskName:'',planStartDate:'',planEndDate:''})" type="primary" plain>添加任务</el-button>
		</el-form>
		<template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="onSubmit">确定</el-button></template>
	</el-dialog>
</template>

<script lang="ts" name="rmDevTaskForm" setup>
import { useMessage } from '/@/hooks/message';
import { createTasks } from '/@/api/rm/dev-task';

const emit = defineEmits(['refresh']);
const visible = ref(false);
const state = reactive({ requirementId: '', tasks: [{taskName:'',planStartDate:'',planEndDate:''}] });

const openDialog = (requirementId?: string) => {
	visible.value = true;
	state.requirementId = requirementId || '';
	state.tasks = [{taskName:'',planStartDate:'',planEndDate:''}];
};

const onSubmit = async () => {
	if (!state.requirementId) { useMessage().warning('请输入需求ID'); return; }
	if (state.tasks.some(t => !t.taskName)) { useMessage().warning('任务名称不能为空'); return; }
	try {
		await createTasks(state.requirementId, state.tasks);
		useMessage().success('任务分解成功');
		visible.value = false;
		emit('refresh');
	} catch (err: any) { useMessage().error(err.msg); }
};

defineExpose({ openDialog });
</script>
