<template>
	<el-drawer v-model="show" title="查询历史" size="60%" direction="rtl">
		<el-form :inline="true" class="mb-4">
			<el-form-item label="创建人" v-if="isAdmin">
				<el-input v-model="queryForm.createBy" placeholder="筛选用户" clearable style="width: 140px" />
			</el-form-item>
			<el-form-item>
				<el-button type="primary" @click="loadHistory">查询</el-button>
			</el-form-item>
		</el-form>

		<el-table :data="historyData.records" stripe v-loading="loading">
			<el-table-column prop="queryType" label="类型" width="70">
				<template #default="{ row }">
					<el-tag size="small">{{ row.queryType }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="queryPreview" label="查询预览" min-width="300" show-overflow-tooltip />
			<el-table-column prop="status" label="状态" width="80">
				<template #default="{ row }">
					<el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="rowCount" label="行数" width="60" />
			<el-table-column prop="durationMs" label="耗时" width="70">
				<template #default="{ row }">{{ row.durationMs }}ms</template>
			</el-table-column>
			<el-table-column prop="createBy" label="用户" width="80" />
			<el-table-column prop="createTime" label="时间" width="160" />
			<el-table-column label="操作" width="80" fixed="right">
				<template #default="{ row }">
					<el-button link type="danger" @click="handleDelete(row)">删除</el-button>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			class="mt-4"
			v-model:current-page="historyData.current"
			v-model:page-size="historyData.size"
			:total="historyData.total"
			layout="total, prev, pager, next"
			@current-change="loadHistory" />
	</el-drawer>
</template>

<script lang="ts" name="SparqlQueryHistoryDrawer" setup>
import { ref, reactive, computed, watch } from 'vue';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { fetchSparqlHistory, deleteSparqlHistory } from '/@/api/ontology/sparql';
import type { SparqlQueryLogVO } from '/@/types/ontology/sparql';

const props = defineProps<{
	modelValue: boolean;
	ontologyId: number;
}>();

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void;
}>();

const { success: msgSuccess, error: msgError } = useMessage();
const { confirm: msgConfirm } = useMessageBox();

// 管理员判断（简化：角色 1 = 管理员，此处由前端 store 判断或默认 false）
const isAdmin = ref(false);

const show = computed({
	get: () => props.modelValue,
	set: (val: boolean) => emit('update:modelValue', val)
});

const loading = ref(false);
const queryForm = reactive<{ createBy?: string }>({ createBy: undefined });
const historyData = reactive<{ records: SparqlQueryLogVO[]; total: number; current: number; size: number }>({
	records: [],
	total: 0,
	current: 1,
	size: 20
});

const statusTagType = (status: string): string => {
	const map: Record<string, string> = { SUCCESS: 'success', REJECTED: 'danger', TIMEOUT: 'warning', FAILED: 'danger' };
	return map[status] || 'info';
};

const statusLabel = (status: string): string => {
	const map: Record<string, string> = { SUCCESS: '成功', REJECTED: '拒绝', TIMEOUT: '超时', FAILED: '失败' };
	return map[status] || status;
};

const loadHistory = async () => {
	loading.value = true;
	try {
		const { data } = await fetchSparqlHistory({
			ontologyId: props.ontologyId,
			createBy: queryForm.createBy || undefined,
			page: historyData.current,
			size: historyData.size
		});
		historyData.records = data?.records || [];
		historyData.total = data?.total || 0;
	} catch (e: any) {
		msgError(e.msg || '加载历史失败');
	} finally {
		loading.value = false;
	}
};

const handleDelete = async (row: SparqlQueryLogVO) => {
	try {
		await msgConfirm('确认删除该查询历史？');
		await deleteSparqlHistory(row.id);
		msgSuccess('删除成功');
		loadHistory();
	} catch (e: any) {
		if (e !== 'cancel') {
			msgError(e.msg || '删除失败');
		}
	}
};

watch(show, (val: boolean) => {
	if (val) {
		historyData.current = 1;
		loadHistory();
	}
});
</script>

<style scoped>
.mt-4 {
	margin-top: 16px;
}
.mb-4 {
	margin-bottom: 16px;
}
</style>
