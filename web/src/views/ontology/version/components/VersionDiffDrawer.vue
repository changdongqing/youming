<template>
	<el-drawer v-model="show" title="版本差异对比" size="800px">
		<div v-loading="loading">
			<div v-if="diff" class="diff-container">
				<el-descriptions :column="2" border size="small" style="margin-bottom:16px">
					<el-descriptions-item label="兼容性">
						<el-tag :type="compatibilityTagType(diff.compatibility)" size="small">{{ diff.compatibility }}</el-tag>
					</el-descriptions-item>
					<el-descriptions-item label="新增">{{ diff.added.length }} 项</el-descriptions-item>
					<el-descriptions-item label="删除">{{ diff.removed.length }} 项</el-descriptions-item>
					<el-descriptions-item label="修改">{{ diff.modified.length }} 项</el-descriptions-item>
				</el-descriptions>

				<div v-if="diff.breakingReasons.length > 0" style="margin-bottom:16px">
					<el-alert type="error" show-icon :closable="false">
						<template #title>BREAKING 原因</template>
						<ul style="margin:0;padding-left:16px">
							<li v-for="(r, i) in diff.breakingReasons" :key="i">{{ r }}</li>
						</ul>
					</el-alert>
				</div>

				<el-tabs>
					<el-tab-pane :label="`新增 (${diff.added.length})`">
						<el-table :data="diff.added" border size="small">
							<el-table-column prop="resourceType" label="类型" width="160" />
							<el-table-column prop="iri" label="IRI" />
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`删除 (${diff.removed.length})`">
						<el-table :data="diff.removed" border size="small">
							<el-table-column prop="resourceType" label="类型" width="160" />
							<el-table-column prop="iri" label="IRI" />
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`修改 (${diff.modified.length})`">
						<el-table :data="diff.modified" border size="small">
							<el-table-column prop="resourceType" label="类型" width="160" />
							<el-table-column prop="iri" label="IRI" />
							<el-table-column type="expand">
								<template #default="{ row }">
									<el-table :data="row.changes" border size="small">
										<el-table-column prop="field" label="字段" width="160" />
										<el-table-column prop="oldValue" label="旧值" />
										<el-table-column prop="newValue" label="新值" />
									</el-table>
								</template>
							</el-table-column>
						</el-table>
					</el-tab-pane>
				</el-tabs>
			</div>
			<el-empty v-else description="请选择对比版本" />
		</div>

		<template #footer>
			<div style="display:flex;align-items:center;gap:8px">
				<el-select v-model="targetVersionId" placeholder="选择对比目标版本" style="width:200px">
					<el-option v-for="v in versionList" :key="v.id" :label="v.versionNumber" :value="v.id"
						:disabled="v.id === versionId" />
				</el-select>
				<el-button type="primary" @click="loadDiff" :disabled="!targetVersionId">对比</el-button>
			</div>
		</template>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import { fetchVersionDiff, fetchVersionPage } from '/@/api/ontology/version';
import type { VersionDiffVO, VersionDetailVO } from '/@/types/ontology/version';

const props = defineProps<{ visible: boolean; versionId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>();

const loading = ref(false);
const diff = ref<VersionDiffVO | null>(null);
const targetVersionId = ref<number>(0);
const versionList = ref<VersionDetailVO[]>([]);

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const loadVersionList = async () => {
	const { data } = await fetchVersionPage({ ontologyId: 935001, pageNum: 1, pageSize: 100 });
	versionList.value = data.records;
};

const loadDiff = async () => {
	if (!props.versionId || !targetVersionId.value) return;
	loading.value = true;
	try {
		const { data } = await fetchVersionDiff(props.versionId, targetVersionId.value);
		diff.value = data;
	} catch (e) {
		// ignore
	} finally {
		loading.value = false;
	}
};

const compatibilityTagType = (c: string) => c === 'BREAKING' ? 'danger' : c === 'BACKWARD_COMPATIBLE' ? 'success' : 'info';

watch(() => props.visible, (v) => {
	if (v) {
		diff.value = null;
		targetVersionId.value = 0;
		loadVersionList();
	}
});
</script>
