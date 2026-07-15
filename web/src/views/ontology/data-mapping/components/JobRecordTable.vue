<template>
	<div>
		<el-form :inline="true" class="mb8">
			<el-form-item label="状态">
				<el-select v-model="recordQuery.recordStatus" placeholder="全部" clearable style="width: 120px" @change="loadRecords">
					<el-option label="失败" value="FAILED" />
					<el-option label="成功" value="SUCCESS" />
					<el-option label="跳过" value="SKIPPED" />
				</el-select>
			</el-form-item>
		</el-form>
		<el-table v-loading="loading" :data="recordList" border size="small">
			<el-table-column prop="mappingCode" label="映射编码" width="140" show-overflow-tooltip />
			<el-table-column label="阶段" width="80">
				<template #default="{ row }">{{ phaseLabel(row.phase) }}</template>
			</el-table-column>
			<el-table-column prop="sourceRecordKeyMasked" label="记录键掩码" min-width="160" show-overflow-tooltip />
			<el-table-column prop="recordAction" label="动作" width="100" />
			<el-table-column prop="recordStatus" label="状态" width="80" />
			<el-table-column prop="errorCode" label="错误码" width="120" show-overflow-tooltip />
			<el-table-column prop="errorMessage" label="错误消息" min-width="200" show-overflow-tooltip />
			<el-table-column prop="durationMs" label="耗时(ms)" width="90" />
			<el-table-column prop="createTime" label="时间" width="170" />
		</el-table>
		<el-pagination
			v-model:current-page="recordQuery.current"
			v-model:page-size="recordQuery.size"
			:total="total"
			layout="total, prev, pager, next"
			@current-change="loadRecords"
			class="mt8"
		/>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingJobApi } from '/@/api/ontology/data-mapping';
import type { MappingJobRecordVO } from '/@/types/ontology/data-mapping';
import { phaseLabel } from '../utils/mapping-status';

const props = defineProps<{ jobId: number }>();
const { error: msgError } = useMessage();

const loading = ref(false);
const recordList = ref<MappingJobRecordVO[]>([]);
const total = ref(0);
const recordQuery = reactive({ recordStatus: '', current: 1, size: 10 });

const loadRecords = async () => {
	loading.value = true;
	try {
		const { data } = await mappingJobApi.getJobRecords(props.jobId, {
			recordStatus: recordQuery.recordStatus || undefined,
			current: recordQuery.current,
			size: recordQuery.size,
		});
		recordList.value = data?.records || [];
		total.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取记录列表失败');
	} finally {
		loading.value = false;
	}
};

watch(
	() => props.jobId,
	() => {
		if (props.jobId) loadRecords();
	}
);

onMounted(() => {
	if (props.jobId) loadRecords();
});
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mt8 {
	margin-top: 8px;
}
</style>
