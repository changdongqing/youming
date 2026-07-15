<template>
	<div class="designer-header">
		<div class="header-left">
			<el-button link @click="$emit('back')">
				<el-icon><ArrowLeft /></el-icon> 返回
			</el-button>
			<el-divider direction="vertical" />
			<span class="header-title">{{ project?.mappingName }}</span>
			<el-tag v-if="version" :type="versionStatusTagType(version.versionStatus)" size="small" class="ml8">
				{{ versionStatusLabel(version.versionStatus) }}
			</el-tag>
			<span class="header-info ml12" v-if="version">
				版本: <el-tag size="small">{{ version.versionNumber }}</el-tag>
			</span>
			<span class="header-info ml8" v-if="project"> 本体工程ID: {{ project.ontologyId }} </span>
			<span class="header-info ml8" v-if="version?.configHash">
				配置哈希: <el-text size="small" type="info">{{ version.configHash.slice(0, 8) }}...</el-text>
			</span>
		</div>
		<div class="header-right">
			<el-button type="primary" @click="$emit('save')" :disabled="!canSave" v-auth="'ontology_mapping_edit'">
				<el-icon><Check /></el-icon> 保存
			</el-button>
			<el-button @click="$emit('validate')" :disabled="!canValidate" v-auth="'ontology_mapping_validate'">
				<el-icon><Checked /></el-icon> 校验
			</el-button>
			<el-button type="success" @click="$emit('publish')" :disabled="!canPublish" v-auth="'ontology_mapping_publish'">
				<el-icon><Upload /></el-icon> 发布
			</el-button>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { ArrowLeft, Check, Checked, Upload } from '@element-plus/icons-vue';
import type { MappingProjectVO, MappingVersionVO } from '/@/types/ontology/data-mapping';
import { versionStatusLabel, versionStatusTagType } from '../utils/mapping-status';
import { canSave as canSaveFn, canValidate as canValidateFn, canPublish as canPublishFn } from '../utils/designer-state';

const props = defineProps<{
	project: MappingProjectVO | null;
	version: MappingVersionVO | null;
	isDirty: boolean;
}>();

defineEmits<{
	(e: 'back'): void;
	(e: 'save'): void;
	(e: 'validate'): void;
	(e: 'publish'): void;
}>();

const canSave = computed(() => (props.version ? canSaveFn(props.version.versionStatus, props.isDirty) : false));
const canValidate = computed(() => (props.version ? canValidateFn(props.version.versionStatus) : false));
const canPublish = computed(() => (props.version ? canPublishFn(props.version.versionStatus, props.isDirty) : false));
</script>

<style scoped>
.designer-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 8px 12px;
	border-bottom: 1px solid var(--el-border-color-light);
	background-color: var(--el-bg-color);
}
.header-left {
	display: flex;
	align-items: center;
}
.header-title {
	font-size: 16px;
	font-weight: 600;
}
.header-info {
	font-size: 13px;
	color: var(--el-text-color-secondary);
}
.header-right {
	display: flex;
	gap: 8px;
}
.ml8 {
	margin-left: 8px;
}
.ml12 {
	margin-left: 12px;
}
</style>
