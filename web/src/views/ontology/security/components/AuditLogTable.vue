<template>
	<div>
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input v-model="query.ontologyId" placeholder="工程ID" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-form-item label="用户ID">
				<el-input v-model="query.userId" placeholder="用户ID" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-button type="primary" @click="loadData">查询</el-button>
		</el-form>

		<el-table v-loading="loading" :data="tableData" border>
			<el-table-column prop="occurredAt" label="时间" width="180" />
			<el-table-column prop="username" label="用户" width="100" />
			<el-table-column prop="accessType" label="访问类型" width="120" />
			<el-table-column prop="resourceType" label="资源类型" width="120" />
			<el-table-column prop="resourceRef" label="资源引用" show-overflow-tooltip />
			<el-table-column prop="decision" label="决策" width="80">
				<template #default="{ row }">
					<el-tag :type="decisionTag(row.decision)">{{ row.decision }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="outcome" label="结果" width="80">
				<template #default="{ row }">
					<el-tag :type="row.outcome === 'SUCCESS' ? 'success' : 'danger'">{{ row.outcome }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="maxSecurityLevel" label="最高级别" width="100" />
			<el-table-column prop="resultCount" label="结果数" width="80" />
			<el-table-column prop="remoteAddr" label="IP" width="120" />
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next"
			@current-change="loadData"
		/>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchAuditPage } from '/@/api/ontology/security';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const query = reactive({ ontologyId: '', userId: '', current: 1, size: 20 });

const decisionTag = (decision: string) => {
	if (decision === 'DENY') return 'danger';
	if (decision === 'MASK') return 'warning';
	return 'success';
};

const loadData = async () => {
	loading.value = true;
	try {
		const res = await fetchAuditPage(query);
		tableData.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

onMounted(loadData);
</script>
