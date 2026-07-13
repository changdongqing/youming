<template>
	<div>
		<el-row class="mb8" justify="space-between">
			<el-form :model="query" inline @keyup.enter="load">
				<el-form-item label="状态">
					<el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
						<el-option label="PENDING" value="PENDING" />
						<el-option label="PROCESSING" value="PROCESSING" />
						<el-option label="PUBLISHED" value="PUBLISHED" />
						<el-option label="FAILED" value="FAILED" />
					</el-select>
				</el-form-item>
				<el-form-item label="事件类型">
					<el-input v-model="query.eventType" clearable placeholder="如 ONTOLOGY_INSTANCE_CHANGED" style="width: 220px" />
				</el-form-item>
				<el-form-item label="事件ID">
					<el-input v-model="query.eventId" clearable placeholder="UUID" style="width: 200px" />
				</el-form-item>
				<el-form-item>
					<el-button icon="search" type="primary" @click="load">查询</el-button>
					<el-button icon="refresh" @click="resetQuery">重置</el-button>
				</el-form-item>
			</el-form>
		</el-row>

		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column label="事件ID" prop="eventId" width="200" show-overflow-tooltip />
			<el-table-column label="事件类型" prop="eventType" width="200" show-overflow-tooltip />
			<el-table-column label="版本" prop="eventVersion" width="60" />
			<el-table-column label="聚合" min-width="180" show-overflow-tooltip>
				<template #default="{ row }">
					{{ row.aggregateType }} / {{ row.aggregateId }}
				</template>
			</el-table-column>
			<el-table-column label="状态" prop="status" width="100">
				<template #default="{ row }">
					<el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="尝试" prop="deliveryAttempt" width="60" />
			<el-table-column label="Stream ID" prop="streamRecordId" width="140" show-overflow-tooltip />
			<el-table-column label="发生时间" prop="occurredAt" width="160" />
			<el-table-column label="错误信息" prop="lastErrorMessage" min-width="160" show-overflow-tooltip />
			<el-table-column label="操作" fixed="right" width="100">
				<template #default="{ row }">
					<el-button
						v-if="row.status === 'FAILED'"
						v-auth="'ontology_event_admin'"
						text
						type="primary"
						@click="handleRetry(row)"
					>重试</el-button>
				</template>
			</el-table-column>
		</el-table>

		<pagination
			:total="pagination.total"
			v-model:current="pagination.current"
			v-model:size="pagination.size"
			@current-change="load"
			@size-change="load"
		/>
	</div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { fetchOutboxPage, retryOutboxObj } from '/@/api/ontology/event';
import type { OutboxQuery, OutboxVO } from '/@/types/ontology/event';
import { useMessage, useMessageBox } from '/@/hooks/message';

const loading = ref(false);
const tableData = ref<OutboxVO[]>([]);
const query = reactive<OutboxQuery>({ status: '', eventType: '', eventId: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });

const load = async () => {
	loading.value = true;
	try {
		const res = await fetchOutboxPage({ ...query, current: pagination.current, size: pagination.size });
		tableData.value = res.data?.records || [];
		pagination.total = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const resetQuery = () => {
	query.status = '';
	query.eventType = '';
	query.eventId = '';
	pagination.current = 1;
	load();
};

const handleRetry = async (row: OutboxVO) => {
	await useMessageBox().confirm(`确认重试投递事件 ${row.eventId}？`);
	try {
		await retryOutboxObj(row.id);
		useMessage().success('已重置为 PENDING，等待 Relay 投递');
		load();
	} catch (err: any) {
		useMessage().error(err.msg || '重试失败');
	}
};

const statusTagType = (status: string) => {
	const map: Record<string, string> = {
		PENDING: 'warning',
		PROCESSING: 'primary',
		PUBLISHED: 'success',
		FAILED: 'danger',
	};
	return map[status] || 'info';
};

defineExpose({ load });

onMounted(() => load());
</script>
