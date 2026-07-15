<template>
	<el-dialog v-model="visible" title="配置调度" width="520px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-form :model="form" label-width="120px" v-loading="loading">
			<el-form-item label="启用调度">
				<el-switch v-model="form.scheduleEnabled" />
			</el-form-item>
			<el-form-item label="Cron表达式">
				<el-input v-model="form.scheduleCron" placeholder="如 0 0 2 * * ?" />
			</el-form-item>
			<el-form-item label="运行类型">
				<el-select v-model="form.scheduleRunType" placeholder="选择调度运行类型" style="width: 100%">
					<el-option label="全量" value="FULL" />
					<el-option label="增量" value="INCREMENTAL" />
				</el-select>
			</el-form-item>
		</el-form>
		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingJobApi } from '/@/api/ontology/data-mapping';
import type { MappingProjectVO } from '/@/types/ontology/data-mapping';

const props = defineProps<{ project?: MappingProjectVO | null }>();
const emit = defineEmits<{ (e: 'success'): void }>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const saving = ref(false);

const form = reactive({
	scheduleEnabled: false,
	scheduleCron: '',
	scheduleRunType: 'INCREMENTAL' as 'FULL' | 'INCREMENTAL',
});

const open = (project: MappingProjectVO) => {
	visible.value = true;
	form.scheduleEnabled = project.scheduleEnabled === '1';
	form.scheduleCron = project.scheduleCron || '';
	form.scheduleRunType = (project.scheduleRunType as 'FULL' | 'INCREMENTAL') || 'INCREMENTAL';
};

const handleSave = async () => {
	if (!props.project) return;
	saving.value = true;
	try {
		await mappingJobApi.updateSchedule(props.project.id, form.scheduleEnabled, form.scheduleCron || undefined, form.scheduleRunType || undefined);
		msgSuccess('调度配置成功');
		visible.value = false;
		emit('success');
	} catch (e: any) {
		msgError(e.message || '调度配置失败');
	} finally {
		saving.value = false;
	}
};

defineExpose({ open });
</script>
