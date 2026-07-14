<template>
	<div class="layout-padding ontology-security-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-tabs v-model="activeTab" class="mt8">
				<el-tab-pane label="工程 ACL" name="acl">
					<ProjectAclTable />
				</el-tab-pane>
				<el-tab-pane label="主体许可" name="clearance">
					<ClearanceTable />
				</el-tab-pane>
				<el-tab-pane label="数据策略" name="policy">
					<DataPolicyTable />
				</el-tab-pane>
				<el-tab-pane label="审计日志" name="audit">
					<AuditLogTable />
				</el-tab-pane>
				<el-tab-pane label="设备凭证" name="device">
					<DeviceCredentialTable />
				</el-tab-pane>
				<el-tab-pane label="审批管理" name="approval">
					<ApprovalDialog />
				</el-tab-pane>
			</el-tabs>
		</div>
	</div>
</template>

<script lang="ts" name="ontologySecurity" setup>
import { ref } from 'vue';
import ProjectAclTable from './components/ProjectAclTable.vue';
import ClearanceTable from './components/ClearanceTable.vue';
import DataPolicyTable from './components/DataPolicyTable.vue';
import AuditLogTable from './components/AuditLogTable.vue';
import DeviceCredentialTable from './components/DeviceCredentialTable.vue';
import ApprovalDialog from './components/ApprovalDialog.vue';

const activeTab = ref('acl');
</script>

<style scoped>
.ontology-security-page {
	height: 100%;
}
</style>
