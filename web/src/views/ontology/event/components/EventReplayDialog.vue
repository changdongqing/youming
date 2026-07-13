<template>
	<el-dialog v-model="dialogVisible" title="受控回放" destroy-on-close width="600px">
		<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
			<el-form-item label="事件ID" prop="eventId">
				<el-input v-model="form.eventId" placeholder="被回放的事件 UUID" />
			</el-form-item>
			<el-form-item label="目标消费者组" prop="targetConsumerGroup">
				<el-select v-model="form.targetConsumerGroup" placeholder="选择目标消费者组" style="width: 100%">
					<el-option label="ontology-sparql-cache-v1" value="ontology-sparql-cache-v1" />
					<el-option label="ontology-visualization-v1" value="ontology-visualization-v1" />
					<el-option label="ontology-version-workspace-v1" value="ontology-version-workspace-v1" />
					<el-option label="ontology-security-audit-v1" value="ontology-security-audit-v1" />
				</el-select>
			</el-form-item>
			<el-form-item label="源回放序号" prop="sourceReplayNo">
				<el-input-number v-model="form.sourceReplayNo" :min="0" controls-position="right" />
				<span class="form-tip">0 为原始投递</span>
			</el-form-item>
			<el-form-item label="回放原因" prop="reason">
				<el-input v-model="form.reason" type="textarea" :rows="3" placeholder="说明回放原因" />
			</el-form-item>
			<el-form-item label="审批号">
				<el-input v-model="form.approvalRequestNo" placeholder="模块36审批号（批量或高风险回放时填写）" />
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button @click="dialogVisible = false">取消</el-button>
			<el-button type="primary" :loading="submitting" @click="submit">确定回放</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { createReplay } from '/@/api/ontology/event';
import type { ReplayRequest } from '/@/types/ontology/event';
import { useMessage } from '/@/hooks/message';
import type { FormInstance, FormRules } from 'element-plus';

const props = defineProps<{ visible: boolean }>();
const emit = defineEmits<{ 'update:visible': [val: boolean]; success: [] }>();

const dialogVisible = computed({
	get: () => props.visible,
	set: (val) => emit('update:visible', val),
});

const formRef = ref<FormInstance>();
const submitting = ref(false);
const form = reactive<ReplayRequest>({
	eventId: '',
	targetConsumerGroup: '',
	reason: '',
	approvalRequestNo: '',
	sourceReplayNo: 0,
});

const rules: FormRules = {
	eventId: [{ required: true, message: '事件ID不能为空', trigger: 'blur' }],
	targetConsumerGroup: [{ required: true, message: '目标消费者组不能为空', trigger: 'change' }],
	reason: [{ required: true, message: '回放原因不能为空', trigger: 'blur' }],
	sourceReplayNo: [{ required: true, message: '源回放序号不能为空', trigger: 'blur' }],
};

const submit = async () => {
	if (!formRef.value) return;
	await formRef.value.validate();
	submitting.value = true;
	try {
		await createReplay({ ...form });
		useMessage().success('回放已触发');
		dialogVisible.value = false;
		emit('success');
	} catch (err: any) {
		useMessage().error(err.msg || '回放失败');
	} finally {
		submitting.value = false;
	}
};
</script>

<style scoped>
.form-tip {
	margin-left: 8px;
	font-size: 12px;
	color: var(--el-text-color-secondary);
}
</style>
