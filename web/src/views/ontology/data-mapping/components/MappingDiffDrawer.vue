<template>
	<el-drawer v-model="visible" title="版本差异对比" size="60%" destroy-on-close>
		<div v-loading="loading">
			<!-- 版本选择 -->
			<el-form :inline="true" class="mb8">
				<el-form-item label="基准版本">
					<el-select v-model="baseVersionId" placeholder="选择基准版本" filterable style="width: 180px" @change="loadDiff">
						<el-option v-for="v in versionList" :key="v.id" :label="v.versionNumber" :value="v.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="对比版本">
					<el-select v-model="compareVersionId" placeholder="选择对比版本" filterable style="width: 180px" @change="loadDiff">
						<el-option v-for="v in versionList" :key="v.id" :label="v.versionNumber" :value="v.id" />
					</el-select>
				</el-form-item>
			</el-form>

			<template v-if="diffResult">
				<!-- 变更分类 -->
				<el-descriptions :column="2" border class="mb8">
					<el-descriptions-item label="基准版本">{{ diffResult.baseVersionNumber }}</el-descriptions-item>
					<el-descriptions-item label="对比版本">{{ diffResult.compareVersionNumber }}</el-descriptions-item>
					<el-descriptions-item label="变更分类" :span="2">
						<el-tag :type="changeClassificationTagType(diffResult.changeClassification)" size="small">
							{{ changeClassificationLabel(diffResult.changeClassification) }}
						</el-tag>
					</el-descriptions-item>
				</el-descriptions>

				<!-- 变更项列表 -->
				<el-table :data="diffResult.changes" border size="small" style="width: 100%">
					<el-table-column prop="field" label="变更路径" min-width="200" show-overflow-tooltip />
					<el-table-column prop="oldValue" label="基准值" min-width="160" show-overflow-tooltip>
						<template #default="{ row }">{{ row.oldValue || '(空)' }}</template>
					</el-table-column>
					<el-table-column prop="newValue" label="对比值" min-width="160" show-overflow-tooltip>
						<template #default="{ row }">{{ row.newValue || '(空)' }}</template>
					</el-table-column>
					<el-table-column label="分类" width="100">
						<template #default="{ row }">
							<el-tag :type="changeClassificationTagType(row.classification)" size="small">
								{{ changeClassificationLabel(row.classification) }}
							</el-tag>
						</template>
					</el-table-column>
				</el-table>
			</template>
			<el-empty v-else description="请选择两个版本进行对比" :image-size="60" />
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingProjectApi } from '/@/api/ontology/data-mapping';
import type { MappingVersionVO, MappingVersionDiffVO } from '/@/types/ontology/data-mapping';
import { changeClassificationLabel, changeClassificationTagType } from '../utils/mapping-status';

const props = defineProps<{ projectId: number }>();
const { error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const versionList = ref<MappingVersionVO[]>([]);
const baseVersionId = ref<number | undefined>();
const compareVersionId = ref<number | undefined>();
const diffResult = ref<MappingVersionDiffVO | null>(null);

const open = async () => {
	visible.value = true;
	diffResult.value = null;
	baseVersionId.value = undefined;
	compareVersionId.value = undefined;
	await loadVersions();
};

const loadVersions = async () => {
	loading.value = true;
	try {
		const { data } = await mappingProjectApi.listVersions(props.projectId, { current: 1, size: 50 });
		versionList.value = data?.records || [];
	} catch (e: any) {
		msgError(e.message || '获取版本列表失败');
	} finally {
		loading.value = false;
	}
};

const loadDiff = async () => {
	if (!baseVersionId.value || !compareVersionId.value) return;
	if (baseVersionId.value === compareVersionId.value) return;
	loading.value = true;
	try {
		const { data } = await mappingProjectApi.diffVersions(baseVersionId.value, compareVersionId.value);
		diffResult.value = data;
	} catch (e: any) {
		msgError(e.message || '获取版本差异失败');
	} finally {
		loading.value = false;
	}
};

defineExpose({ open });
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
</style>
