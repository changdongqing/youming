<template>
	<el-row :gutter="12" v-loading="loading">
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">PENDING</div>
				<div class="stat-value pending">{{ overview.outboxPending || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">PROCESSING</div>
				<div class="stat-value processing">{{ overview.outboxProcessing || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">PUBLISHED</div>
				<div class="stat-value published">{{ overview.outboxPublished || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">FAILED</div>
				<div class="stat-value failed">{{ overview.outboxFailed || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">Stream 长度</div>
				<div class="stat-value">{{ overview.streamLength || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">死信 OPEN</div>
				<div class="stat-value dead-letter">{{ overview.deadLetterOpen || 0 }}</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">Redis 状态</div>
				<div class="stat-value">
					<el-tag :type="overview.redisAvailable ? 'success' : 'danger'" size="small">
						{{ overview.redisAvailable ? '在线' : '不可用' }}
					</el-tag>
				</div>
			</el-card>
		</el-col>
		<el-col :span="3">
			<el-card shadow="hover" class="stat-card">
				<div class="stat-label">操作</div>
				<div class="stat-value">
					<el-button icon="refresh" size="small" @click="$emit('refresh')">刷新</el-button>
				</div>
			</el-card>
		</el-col>
	</el-row>
</template>

<script setup lang="ts">
import type { EventOverviewVO } from '/@/types/ontology/event';

defineProps<{ overview: EventOverviewVO; loading: boolean }>();
defineEmits<{ refresh: [] }>();
</script>

<style scoped>
.stat-card {
	text-align: center;
}
.stat-label {
	font-size: 12px;
	color: var(--el-text-color-secondary);
	margin-bottom: 4px;
}
.stat-value {
	font-size: 24px;
	font-weight: bold;
}
.stat-value.pending {
	color: var(--el-color-warning);
}
.stat-value.processing {
	color: var(--el-color-primary);
}
.stat-value.published {
	color: var(--el-color-success);
}
.stat-value.failed {
	color: var(--el-color-danger);
}
.stat-value.dead-letter {
	color: var(--el-color-danger);
}
</style>
