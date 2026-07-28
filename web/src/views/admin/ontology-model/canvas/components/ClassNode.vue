<template>
	<div class="class-node" :style="{ borderColor: data.color || '#409eff' }">
		<div class="class-node-header">
			<span class="class-node-icon">{{ data.icon || '📦' }}</span>
			<span class="class-node-label">{{ data.label }}</span>
		</div>
		<div class="class-node-body">
			<span class="class-node-code" v-if="data.classificationCode">{{ data.classificationCode }}</span>
			<el-tag v-if="data.templateCode" size="small" type="warning">{{ t('modelCanvas.templateBadge') }}</el-tag>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

defineProps<{ data: any }>();
const { t } = useI18n();
</script>

<style scoped>
.class-node {
	width: 160px;
	border: 2px solid #409eff;
	border-radius: 8px;
	background: #fff;
	font-size: 12px;
	overflow: hidden;
}
.class-node-header {
	padding: 6px 10px;
	background: #f5f7fa;
	font-weight: bold;
	display: flex;
	align-items: center;
	gap: 4px;
}
.class-node-body {
	padding: 4px 10px;
	display: flex;
	align-items: center;
	gap: 4px;
}
.class-node-code {
	color: #909399;
	font-size: 11px;
}
</style>
