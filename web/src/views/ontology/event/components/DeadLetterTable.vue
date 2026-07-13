<template>
	<div>
		<el-row class="mb8" justify="space-between">
			<el-form :model="query" inline @keyup.enter="load">
				<el-form-item label="状态">
					<el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
						<el-option label="OPEN" value="OPEN" />
						<el-option label="REPLAYED" value="REPLAYED" />
						<el-option label="RESOLVED" value="RESOLVED" />
						<el-option label="IGNORED" value="IGNORED" />
					</el-select>
				</el-form-item>
				<el-form-item label="消费者组">
					<el-input v-model="query.consumerGroup" clearable style="width: 200px" />
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
			<el-table-column label="消费者组" prop="consumerGroup" width="220" show-overflow-tooltip />
			<el-table-column label="回放序号" prop="replayNo" width="80" />
			<el-table-column label="失败分类" prop="failureCategory" width="120">
				<template #default="{ row }">
					<el-tag :type="row.failureCategory === 'NON_RETRYABLE' ? 'danger' : 'warning'" size="small">
						{{ row.failureCategory }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="尝试次数" prop="deliveryCount" width="80" />
			<el-table-column label="状态" prop="status" width="100">
				<template #default="{ row }">
					<el-tag :type="dlqStatusTagType(row.status)" size="small">{{ row.status }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="错误信息" prop="errorMessage" min-width="180" show-overflow-tooltip />
			<el-table-column label="最近失败" prop="lastFailedAt" width="160" />
			<el-table-column label="操作" fixed="right" width="160">
				<template #default="{ row }">
					<el-button
						v-if="row.status === 'OPEN'"
						v-auth="'ontology_event_admin'"
						text
						type="primary"
						@click="handleReplay(row)"
					>回放</el-button>
					<el-button
						v-if="row.status === 'OPEN'"
						v-auth="'ontology_event_admin'"
						text
						type="success"
						@click="handleResolve(row, 'RESOLVED')"
					>已处理</el-button>
					<el-button
						v-if="row.status === 'OPEN'"
						v-auth="'ontology_event_admin'"
						text
						type="info"
						@click="handleResolve(row, 'IGNORED')"
					>忽略</el-button>
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
import { fetchDeadLetterPage, replayDeadLetterObj, resolveDeadLetterObj } from '/@/api/ontology/event';
import type { DeadLetterQuery, DeadLetterVO } from '/@/types/ontology/event';
import { useMessage, useMessageBox } from '/@/hooks/message';

const loading = ref(false);
const tableData = ref<DeadLetterVO[]>([]);
const query = reactive<DeadLetterQuery>({ status: '', consumerGroup: '' });
const pagination = reactive({ current: 1, size: 10, total: 0 });

const load = async () => {
	loading.value = true;
	try {
		const res = await fetchDeadLetterPage({ ...query, current: pagination.current, size: pagination.size });
		tableData.value = res.data?.records || [];
		pagination.total = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const resetQuery = () => {
	query.status = '';
	query.consumerGroup = '';
	pagination.current = 1;
	load();
};

const handleReplay = async (row: DeadLetterVO) => {
	await useMessageBox().confirm(`确认回放死信 ${row.eventId} 到 ${row.consumerGroup}？`);
	try {
		await replayDeadLetterObj(row.id);
		useMessage().success('已触发回放');
		load();
	} catch (err: any) {
		useMessage().error(err.msg || '回放失败');
	}
};

const handleResolve = async (row: DeadLetterVO, action: string) => {
	await useMessageBox().confirm(`确认将此死信标记为${action === 'RESOLVED' ? '已处理' : '已忽略'}？`);
	try {
		await resolveDeadLetterObj(row.id, { action });
		useMessage().success('操作成功');
		load();
	} catch (err: any) {
		useMessage().error(err.msg || '操作失败');
	}
};

const dlqStatusTagType = (status: string) => {
	const map: Record<string, string> = {
		OPEN: 'danger',
		REPLAYED: 'warning',
		RESOLVED: 'success',
		IGNORED: 'info',
	};
	return map[status] || 'info';
};

defineExpose({ load });

onMounted(() => load());
</script>
