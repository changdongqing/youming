<template>
	<el-dialog :close-on-click-modal="false" title="需求详情" draggable v-model="visible" width="1000px" top="5vh">
		<div v-loading="loading">
			<!-- 状态步骤条 -->
			<el-steps :active="stepIndex" finish-status="success" align-center class="mb20">
				<el-step title="草稿" />
				<el-step title="待审批" />
				<el-step title="待评审" />
				<el-step title="设计中" />
				<el-step title="设计评审" />
				<el-step title="排期" />
				<el-step title="开发" />
				<el-step title="测试" />
				<el-step title="待验收" />
				<el-step title="完成" />
			</el-steps>
			<!-- 动态操作按钮 -->
			<div class="mb20" v-if="actionButtons.length > 0">
				<el-button v-for="btn in actionButtons" :key="btn.action" :type="btn.type" v-auth="btn.permission" @click="btn.handler">{{ btn.label }}</el-button>
			</div>
			<el-tabs v-model="activeTab">
				<el-tab-pane label="基本信息" name="basic">
					<el-descriptions :column="2" border>
						<el-descriptions-item label="编号">{{ detail.reqCode }}</el-descriptions-item>
						<el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
						<el-descriptions-item label="来源"><dict-tag :options="rm_req_source" :value="detail.source" /></el-descriptions-item>
						<el-descriptions-item label="优先级"><dict-tag :options="rm_priority" :value="detail.priority" /></el-descriptions-item>
						<el-descriptions-item label="期望完成">{{ detail.expectCompleteDate }}</el-descriptions-item>
						<el-descriptions-item label="状态"><dict-tag :options="rm_req_status" :value="detail.status" /></el-descriptions-item>
						<el-descriptions-item label="发起人">{{ detail.initiatorName }}</el-descriptions-item>
						<el-descriptions-item label="部门">{{ detail.initiatorDeptName }}</el-descriptions-item>
						<el-descriptions-item label="需求描述" :span="2"><div v-html="detail.description"></div></el-descriptions-item>
					</el-descriptions>
				</el-tab-pane>
				<el-tab-pane label="审批记录" name="approval">
					<el-timeline>
						<el-timeline-item v-for="r in detail.approvalRecords" :key="r.id" :timestamp="r.approveTime" :type="r.conclusion === 'PASS' ? 'success' : 'danger'">
							<h4>{{ r.nodeCode }} - {{ r.conclusion }}</h4>
							<p>{{ r.opinion }}</p>
						</el-timeline-item>
					</el-timeline>
				</el-tab-pane>
				<el-tab-pane label="需求设计" name="design">
					<div v-if="detail.designContent" v-html="detail.designContent"></div>
					<el-empty v-else description="暂无设计" />
				</el-tab-pane>
				<el-tab-pane label="关联任务" name="devTasks">
					<el-table :data="detail.devTasks" border>
						<el-table-column prop="taskCode" label="编号" width="150" />
						<el-table-column prop="taskName" label="任务名称" show-overflow-tooltip />
						<el-table-column prop="status" label="状态" width="100" />
					</el-table>
				</el-tab-pane>
			</el-tabs>
		</div>
	</el-dialog>
</template>

<script lang="ts" name="rmRequirementDetail" setup>
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';
import { getObj, submitReq, approveReq, qualityConfirm, acceptReq } from '/@/api/rm/requirement';
import { useAccessDict } from '/@/hooks/dict';

const { rm_req_source, rm_req_status, rm_priority } = useDict('rm_req_source', 'rm_req_status', 'rm_priority');

const visible = ref(false);
const loading = ref(false);
const activeTab = ref('basic');
const detail = ref<any>({});

const statusStepMap: Record<string, number> = {
	DRAFT: 0, PENDING_APPROVAL: 1, PENDING_REVIEW: 2, DESIGNING: 3, DESIGN_REVIEW: 4,
	SCHEDULING: 5, DEVELOPING: 6, TESTING: 7, PENDING_ACCEPTANCE: 8, COMPLETED: 9, RELEASED: 9,
};
const stepIndex = computed(() => statusStepMap[detail.value.status] ?? 0);

const actionButtons = computed(() => {
	const s = detail.value.status;
	const btns: any[] = [];
	if (s === 'DRAFT') btns.push({ action: 'submit', label: '提交审批', type: 'primary', permission: 'rm_req_add', handler: handleSubmit });
	if (s === 'PENDING_APPROVAL') btns.push({ action: 'approve', label: '审批通过', type: 'primary', permission: 'rm_req_approve', handler: () => handleApprove('PASS') }, { action: 'reject', label: '驳回', type: 'danger', permission: 'rm_req_approve', handler: () => handleApprove('REJECT') });
	if (s === 'TESTING') btns.push({ action: 'quality', label: '质量确认', type: 'success', permission: 'rm_req_approve', handler: handleQuality });
	if (s === 'PENDING_ACCEPTANCE') btns.push({ action: 'accept', label: '验收通过', type: 'success', permission: 'rm_req_view', handler: () => handleAccept('PASS') });
	return btns;
});

const openDetail = async (id: string) => {
	visible.value = true;
	activeTab.value = 'basic';
	loading.value = true;
	try {
		const { data } = await getObj(id);
		detail.value = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const handleSubmit = async () => {
	try {
		await useMessageBox().confirm('确认提交审批？');
		await submitReq(detail.value.id);
		useMessage().success('提交成功');
		openDetail(detail.value.id);
	} catch {}
};

const handleApprove = async (conclusion: string) => {
	try {
		if (conclusion === 'REJECT') {
			const { value } = await useMessageBox().prompt('请输入驳回原因', '驳回', { inputPattern: /.+/ });
			await approveReq({ billId: detail.value.id, conclusion, opinion: value });
		} else {
			await approveReq({ billId: detail.value.id, conclusion });
		}
		useMessage().success('审批完成');
		openDetail(detail.value.id);
	} catch {}
};

const handleQuality = async () => {
	try {
		await useMessageBox().confirm('确认质量确认？');
		await qualityConfirm(detail.value.id);
		useMessage().success('质量确认完成');
		openDetail(detail.value.id);
	} catch {}
};

const handleAccept = async (conclusion: string) => {
	try {
		await useMessageBox().confirm('确认验收？');
		await acceptReq(detail.value.id, conclusion);
		useMessage().success('验收完成');
		openDetail(detail.value.id);
	} catch {}
};

defineExpose({ openDetail });
</script>
