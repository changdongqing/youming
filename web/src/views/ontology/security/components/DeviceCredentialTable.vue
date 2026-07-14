<template>
	<div>
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="设备编码">
				<el-input v-model="query.deviceCode" placeholder="设备编码" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-form-item label="状态">
				<el-select v-model="query.status" clearable placeholder="全部">
					<el-option label="活跃" value="ACTIVE" />
					<el-option label="锁定" value="LOCKED" />
					<el-option label="吊销" value="REVOKED" />
					<el-option label="过期" value="EXPIRED" />
				</el-select>
			</el-form-item>
			<el-button type="primary" @click="loadData">查询</el-button>
			<el-button type="success" @click="createDialog.visible = true">创建凭证</el-button>
		</el-form>

		<el-table v-loading="loading" :data="tableData" border>
			<el-table-column prop="deviceCode" label="设备编码" width="150" />
			<el-table-column prop="authType" label="认证类型" width="120" />
			<el-table-column prop="status" label="状态" width="80">
				<template #default="{ row }">
					<el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="expiresAt" label="过期时间" width="180" />
			<el-table-column prop="lastAuthenticatedAt" label="最后认证" width="180" />
			<el-table-column prop="failedCount" label="失败次数" width="90" />
			<el-table-column prop="lockedUntil" label="锁定截止" width="180" />
			<el-table-column label="操作" width="180">
				<template #default="{ row }">
					<el-button size="small" type="warning" @click="handleRotate(row.id)">轮换</el-button>
					<el-popconfirm title="确认吊销？" @confirm="handleRevoke(row.id)">
						<template #reference>
							<el-button size="small" type="danger">吊销</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next"
			@current-change="loadData"
		/>

		<!-- 创建凭证对话框 -->
		<el-dialog v-model="createDialog.visible" title="创建设备凭证" width="500px">
			<el-alert type="warning" :closable="false" class="mb8">
				Token 只在创建成功后显示一次，关闭弹窗后不可再次查看。
			</el-alert>
			<el-form :model="createForm" label-width="100px">
				<el-form-item label="设备编码">
					<el-input v-model="createForm.deviceCode" />
				</el-form-item>
				<el-form-item label="过期时间">
					<el-date-picker v-model="createForm.expiresAt" type="datetime" placeholder="留空表示永不过期" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="createDialog.visible = false">取消</el-button>
				<el-button type="primary" @click="handleCreate">创建</el-button>
			</template>
		</el-dialog>

		<!-- Token 展示对话框（只显示一次） -->
		<el-dialog v-model="tokenDialog.visible" title="设备 Token" width="600px" :close-on-click-modal="false">
			<el-alert type="error" :closable="false" class="mb8">
				此 Token 仅显示一次，请立即安全保存。关闭后将无法再次查看。
			</el-alert>
			<el-input :model-value="tokenDialog.token" readonly type="textarea" :rows="4" />
			<template #footer>
				<el-button type="primary" @click="tokenDialog.visible = false">我已保存</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import {
	fetchDeviceCredentialsPage,
	createDeviceCredential,
	rotateDeviceToken,
	revokeDeviceCredential,
} from '/@/api/ontology/security';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const query = reactive({ deviceCode: '', status: '', current: 1, size: 10 });

const createDialog = reactive({ visible: false });
const createForm = reactive({ deviceCode: '', expiresAt: '' });

const tokenDialog = reactive({ visible: false, token: '' });

const statusTag = (status: string) => {
	const map: Record<string, string> = { ACTIVE: 'success', LOCKED: 'warning', REVOKED: 'danger', EXPIRED: 'info' };
	return map[status] || 'info';
};

const statusLabel = (status: string) => {
	const map: Record<string, string> = { ACTIVE: '活跃', LOCKED: '锁定', REVOKED: '吊销', EXPIRED: '过期' };
	return map[status] || status;
};

const loadData = async () => {
	loading.value = true;
	try {
		const res = await fetchDeviceCredentialsPage(query);
		tableData.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const handleCreate = async () => {
	if (!createForm.deviceCode) {
		ElMessage.warning('请输入设备编码');
		return;
	}
	const res = await createDeviceCredential(createForm.deviceCode, createForm.expiresAt || undefined);
	tokenDialog.token = res.data?.token || '';
	tokenDialog.visible = true;
	createDialog.visible = false;
	createForm.deviceCode = '';
	createForm.expiresAt = '';
	ElMessage.success('凭证创建成功');
	loadData();
};

const handleRotate = async (id: string) => {
	const res = await rotateDeviceToken(id);
	tokenDialog.token = res.data?.token || '';
	tokenDialog.visible = true;
	ElMessage.success('Token 已轮换');
	loadData();
};

const handleRevoke = async (id: string) => {
	await revokeDeviceCredential(id);
	ElMessage.success('凭证已吊销');
	loadData();
};

onMounted(loadData);
</script>
