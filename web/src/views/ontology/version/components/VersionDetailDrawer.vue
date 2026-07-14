<template>
	<el-drawer v-model="show" title="版本详情" size="640px" :close-on-click-modal="true">
		<div v-loading="loading">
			<el-descriptions v-if="detail" :column="1" border size="small">
				<el-descriptions-item label="版本号">{{ detail.versionNumber }}</el-descriptions-item>
				<el-descriptions-item label="版本IRI">{{ detail.versionIri }}</el-descriptions-item>
				<el-descriptions-item label="兼容性">
					<el-tag :type="compatibilityTagType(detail.compatibility)" size="small">{{ detail.compatibility }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="状态">
					<el-tag :type="statusTagType(detail.releaseStatus)" size="small">{{ detail.releaseStatus }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="是否当前版本">
					<el-tag :type="detail.isCurrent ? 'success' : 'info'" size="small">{{ detail.isCurrent ? '是' : '否' }}</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="前序版本">{{ detail.priorVersionNumber || '-' }}</el-descriptions-item>
				<el-descriptions-item label="恢复来源版本">{{ detail.restoreSourceVersionNumber || '-' }}</el-descriptions-item>
				<el-descriptions-item label="发布说明">{{ detail.releaseNotes || '-' }}</el-descriptions-item>
				<el-descriptions-item label="快照哈希">
					<span style="font-family:monospace;font-size:12px;word-break:break-all">{{ detail.snapshotHash }}</span>
				</el-descriptions-item>
				<el-descriptions-item label="工作区修订号">{{ detail.workspaceRevision }}</el-descriptions-item>
				<el-descriptions-item label="发布人">{{ detail.publishedBy || '-' }}</el-descriptions-item>
				<el-descriptions-item label="发布时间">{{ detail.publishedAt || '-' }}</el-descriptions-item>
				<el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
				<el-descriptions-item label="差异摘要" v-if="detail.diffSummary">
					<pre style="max-height:200px;overflow:auto;font-size:12px">{{ formatJson(detail.diffSummary) }}</pre>
				</el-descriptions-item>
			</el-descriptions>
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import { fetchVersionById } from '/@/api/ontology/version';
import type { VersionDetailVO } from '/@/types/ontology/version';

const props = defineProps<{ visible: boolean; versionId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>();

const loading = ref(false);
const detail = ref<VersionDetailVO | null>(null);

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const loadDetail = async () => {
	if (!props.versionId) return;
	loading.value = true;
	try {
		const { data } = await fetchVersionById(props.versionId);
		detail.value = data;
	} catch (e) {
		// ignore
	} finally {
		loading.value = false;
	}
};

const compatibilityTagType = (c: string) => c === 'BREAKING' ? 'danger' : c === 'BACKWARD_COMPATIBLE' ? 'success' : 'info';
const statusTagType = (s: string) => s === 'PUBLISHED' ? 'success' : s === 'PREPARED' ? 'warning' : s === 'FAILED' ? 'danger' : 'info';
const formatJson = (str: string) => { try { return JSON.stringify(JSON.parse(str), null, 2); } catch { return str; } };

watch(() => props.visible, (v) => { if (v) loadDetail(); });
</script>
