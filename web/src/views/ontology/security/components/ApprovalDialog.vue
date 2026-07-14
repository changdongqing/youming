<template>
	<div>
		<el-form :inline="true" class="mb8">
			<el-form-item label="审批编号">
				<el-input v-model="searchNo" placeholder="审批编号" clearable @keyup.enter="handleSearch" />
			</el-form-item>
			<el-button type="primary" @click="handleSearch">查询</el-button>
			<el-button type="success" @click="createDialog.visible = true">发起审批</el-button>
		</el-form>

		<!-- 审批详情 -->
		<el-card v-if="currentApproval" class="mb8">
			<template #header>审批详情</template>
			<el-descriptions :column="2" border>
				<el-descriptions-item label="编号">{{ currentApproval.requestNo }}</el-descriptions-item>
				<el-descriptions-item label="操作类型">{{ currentApproval.operationType }}</el-descriptions-item>
				<el-descriptions-item label="目标">{{ currentApproval.targetRef }}</el-descriptions-item>
				<el-descriptions-item label="状态">
					<el-tag :type="statusTag(currentApproval.status)">{{ currentApproval.status }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="需要审批数">{{ currentApproval.requiredApprovals }}</el-descriptions-item>
				<el-descriptions-item label="过期时间">{{ currentApproval.expiresAt }}</el-descriptions-item>
			</el-descriptions>

			<div v-if="currentApproval.status === 'PENDING' || currentApproval.status === 'APPROVED'" class="mt8">
				<el-input v-model="actionForm.comment" placeholder="审批意见" class="mb8" />
				<el-button type="success" @click="handleAction('APPROVE')">同意</el-button>
				<el-button type="danger" @click="handleAction('REJECT')">拒绝</el-button>
			</div>
		</el-card>

		<!-- 发起审批对话框 -->
		<el-dialog v-model="createDialog.visible" title="发起高风险操作审批" width="600px">
			<el-form :model="createForm" label-width="100px">
				<el-form-item label="操作类型">
					<el-input v-model="createForm.operationType" placeholder="如 VERSION_RESTORE" />
				</el-form-item>
				<el-form-item label="目标引用">
					<el-input v-model="createForm.targetRef" type="textarea" :rows="2" placeholder="操作目标" />
				</el-form-item>
				<el-form-item label="操作参数">
					<el-input v-model="createForm.payload" type="textarea" :rows="4" placeholder="操作参数JSON" />
				</el-form-item>
				<el-form-item label="审批人数">
					<el-radio-group v-model="createForm.requiredApprovals">
						<el-radio :label="1">单人审批</el-radio>
						<el-radio :label="2">双人审批</el-radio>
					</el-radio-group>
				</el-form-item>
			</el-form>
			<el-alert v-if="createForm.requiredApprovals === 2" type="info" :closable="false" class="mt8">
				双人审批模式下，发起人不能作为审批人。
			</el-alert>
			<template #footer>
				<el-button @click="createDialog.visible = false">取消</el-button>
				<el-button type="primary" @click="handleCreate">发起</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { createApproval, approveAction, getApproval } from '/@/api/ontology/security';
import type { ApprovalVO } from '/@/types/ontology/security';

const searchNo = ref('');
const currentApproval = ref<ApprovalVO | null>(null);

const createDialog = reactive({ visible: false });
const createForm = reactive({ operationType: '', targetRef: '', payload: '', requiredApprovals: 1 });
const actionForm = reactive({ comment: '' });

const statusTag = (status: string) => {
	const map: Record<string, string> = {
		PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
		EXPIRED: 'info', CONSUMED: 'success', CANCELLED: 'info',
	};
	return map[status] || 'info';
};

const handleSearch = async () => {
	if (!searchNo.value) {
		ElMessage.warning('请输入审批编号');
		return;
	}
	const res = await getApproval(searchNo.value);
	currentApproval.value = res.data || null;
};

const handleCreate = async () => {
	if (!createForm.operationType || !createForm.targetRef) {
		ElMessage.warning('请填写操作类型和目标');
		return;
	}
	const res = await createApproval(createForm);
	currentApproval.value = res.data;
	createDialog.visible = false;
	ElMessage.success('审批已发起');
	searchNo.value = res.data?.requestNo || '';
};

const handleAction = async (decision: string) => {
	if (!currentApproval.value) return;
	const res = await approveAction(currentApproval.value.requestNo, {
		decision,
		comment: actionForm.comment,
	});
	currentApproval.value = res.data;
	actionForm.comment = '';
	ElMessage.success(decision === 'APPROVE' ? '已同意' : '已拒绝');
};
</script>
