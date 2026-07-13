<template>
	<div>
		<el-row class="mb8">
			<el-button icon="refresh" @click="load">刷新</el-button>
		</el-row>

		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column label="消费者组" prop="consumerGroup" min-width="220" show-overflow-tooltip />
			<el-table-column label="待处理 (PEL)" prop="pendingCount" width="120">
				<template #default="{ row }">
					<el-tag :type="row.pendingCount > 0 ? 'warning' : 'success'" size="small">
						{{ row.pendingCount }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column label="在线状态" prop="online" width="100">
				<template #default="{ row }">
					<el-tag :type="row.online ? 'success' : 'danger'" size="small">
						{{ row.online ? '在线' : '离线' }}
					</el-tag>
				</template>
			</el-table-column>
		</el-table>
	</div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { fetchConsumerGroups } from '/@/api/ontology/event';
import type { ConsumerGroupVO } from '/@/types/ontology/event';

const loading = ref(false);
const tableData = ref<ConsumerGroupVO[]>([]);

const load = async () => {
	loading.value = true;
	try {
		const res = await fetchConsumerGroups();
		tableData.value = res.data || [];
	} finally {
		loading.value = false;
	}
};

defineExpose({ load });

onMounted(() => load());
</script>
