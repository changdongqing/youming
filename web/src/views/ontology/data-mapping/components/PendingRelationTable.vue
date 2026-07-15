<template>
	<div>
		<!-- ==================== 搜索表单 ==================== -->
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input
					v-model="query.mappingProjectId"
					placeholder="映射工程ID"
					clearable
					style="width: 140px"
					@keyup.enter="loadData"
					@clear="loadData"
				/>
			</el-form-item>
			<el-form-item label="关系映射码">
				<el-input
					v-model="query.relationMappingCode"
					placeholder="关系映射编码"
					clearable
					style="width: 160px"
					@keyup.enter="loadData"
					@clear="loadData"
				/>
			</el-form-item>
			<el-form-item label="状态">
				<el-select v-model="query.pendingStatus" placeholder="全部" clearable style="width: 120px" @change="loadData">
					<el-option label="待解析" value="PENDING" />
					<el-option label="已解析" value="RESOLVED" />
					<el-option label="已忽略" value="IGNORED" />
					<el-option label="已过期" value="EXPIRED" />
				</el-select>
			</el-form-item>
			<el-form-item>
				<el-button type="primary" @click="loadData">
					<el-icon><Search /></el-icon> 搜索
				</el-button>
				<el-button @click="handleReset">重置</el-button>
			</el-form-item>
		</el-form>

		<!-- ==================== 列表 ==================== -->
		<el-table v-loading="loading" :data="tableData" border style="width: 100%">
			<el-table-column prop="relationMappingCode" label="关系映射编码" width="160" show-overflow-tooltip />
			<el-table-column prop="subjectEntityMappingCode" label="主体映射" width="140" show-overflow-tooltip />
			<el-table-column prop="objectEntityMappingCode" label="客体映射" width="140" show-overflow-tooltip />
			<el-table-column prop="sourceRelationKeyDigest" label="关系键摘要" width="140" show-overflow-tooltip />
			<el-table-column prop="subjectRecordKeyDigest" label="主体键摘要" width="140" show-overflow-tooltip />
			<el-table-column prop="objectRecordKeyDigest" label="客体键摘要" width="140" show-overflow-tooltip />
			<el-table-column prop="pendingReason" label="待解析原因" min-width="160" show-overflow-tooltip />
			<el-table-column label="状态" width="90">
				<template #default="{ row }">
					<el-tag :type="pendingStatusTagType(row.pendingStatus)" size="small">
						{{ pendingStatusLabel(row.pendingStatus) }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="retryCount" label="重试次数" width="80" />
			<el-table-column prop="nextRetryAt" label="下次重试" width="170" />
			<el-table-column prop="lastErrorCode" label="最近错误码" width="120" show-overflow-tooltip />
			<el-table-column prop="lastErrorMessage" label="最近错误" min-width="160" show-overflow-tooltip />
			<el-table-column label="操作" width="140" fixed="right">
				<template #default="{ row }">
					<el-button link type="primary" @click="handleRetry(row)" v-if="row.pendingStatus === 'PENDING'" v-auth="'ontology_mapping_retry'"
						>重试</el-button
					>
					<el-button link type="warning" @click="handleIgnore(row)" v-if="row.pendingStatus === 'PENDING'" v-auth="'ontology_mapping_admin'"
						>忽略</el-button
					>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next, sizes"
			:page-sizes="[10, 20, 50]"
			@current-change="loadData"
			@size-change="loadData"
			class="mt8"
		/>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import { useMessage } from '/@/hooks/message';
import { pendingRelationApi } from '/@/api/ontology/data-mapping';
import type { PendingRelationVO, PendingRelationQuery } from '/@/types/ontology/data-mapping';
import { pendingStatusLabel, pendingStatusTagType } from '../utils/mapping-status';

const { success: msgSuccess, error: msgError } = useMessage();

const loading = ref(false);
const tableData = ref<PendingRelationVO[]>([]);
const total = ref(0);
const query = reactive<PendingRelationQuery>({
	mappingProjectId: '' as any,
	relationMappingCode: '',
	pendingStatus: undefined,
	current: 1,
	size: 10,
});

const loadData = async () => {
	loading.value = true;
	try {
		const params = { ...query };
		if (params.mappingProjectId) {
			params.mappingProjectId = Number(params.mappingProjectId);
		} else {
			delete params.mappingProjectId;
		}
		const { data } = await pendingRelationApi.listPendingRelations(params);
		tableData.value = data?.records || [];
		total.value = data?.total || 0;
	} catch (e: any) {
		msgError(e.message || '获取待解析关系列表失败');
	} finally {
		loading.value = false;
	}
};

const handleReset = () => {
	query.mappingProjectId = '' as any;
	query.relationMappingCode = '';
	query.pendingStatus = undefined;
	query.current = 1;
	loadData();
};

const handleRetry = async (row: PendingRelationVO) => {
	try {
		await ElMessageBox.confirm(`确认重试待解析关系？`, '重试确认', { type: 'info' });
		await pendingRelationApi.retryPendingRelation(row.id);
		msgSuccess('重试已触发');
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '重试失败');
	}
};

const handleIgnore = async (row: PendingRelationVO) => {
	try {
		await ElMessageBox.confirm(`确认忽略该待解析关系？忽略后将不再自动重试。`, '忽略确认', { type: 'warning' });
		await pendingRelationApi.ignorePendingRelation(row.id);
		msgSuccess('已忽略');
		loadData();
	} catch (e: any) {
		if (e !== 'cancel') msgError(e.message || '忽略失败');
	}
};

onMounted(loadData);
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mt8 {
	margin-top: 8px;
}
</style>
