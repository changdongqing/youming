<template>
	<el-drawer v-model="visible" title="映射预览" size="60%" destroy-on-close>
		<div v-loading="loading">
			<!-- 预览参数 -->
			<el-form :inline="true" class="mb8">
				<el-form-item label="实体映射">
					<el-select v-model="previewParams.entityMappingCodes" multiple filterable placeholder="为空则预览全部" style="width: 300px">
						<el-option v-for="em in entityMappings" :key="em.mappingCode" :label="em.mappingName" :value="em.mappingCode" />
					</el-select>
				</el-form-item>
				<el-form-item label="样本数">
					<el-input-number v-model="previewParams.sampleSize" :min="1" :max="100" controls-position="right" />
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="loadPreview" :loading="loading">预览</el-button>
				</el-form-item>
			</el-form>

			<el-alert type="info" :closable="false" show-icon class="mb8">
				<template #title>样本通过不代表全量通过。源记录仅显示键掩码。</template>
			</el-alert>

			<template v-if="previewResult">
				<!-- 实体预览结果 -->
				<el-divider content-position="left">实体预览（{{ previewResult.entityResults.length }}条）</el-divider>
				<el-table :data="previewResult.entityResults" border size="small" style="width: 100%">
					<el-table-column prop="mappingCode" label="映射编码" width="120" show-overflow-tooltip />
					<el-table-column prop="sourceRecordKeyHash" label="记录键哈希" width="120" show-overflow-tooltip>
						<template #default="{ row }">{{ formatDigest(row.sourceRecordKeyHash) }}</template>
					</el-table-column>
					<el-table-column prop="generatedIri" label="生成IRI" min-width="200" show-overflow-tooltip />
					<el-table-column prop="label" label="标签" width="140" show-overflow-tooltip />
					<el-table-column label="操作" width="100">
						<template #default="{ row }">
							<el-tag :type="actionTagType(row.action)" size="small">{{ row.action }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="问题" min-width="120">
						<template #default="{ row }">
							<el-text v-if="row.issues?.length" type="danger" size="small">{{ row.issues.join('; ') }}</el-text>
							<span v-else>-</span>
						</template>
					</el-table-column>
				</el-table>

				<!-- 关系预览结果 -->
				<template v-if="previewResult.relationResults?.length">
					<el-divider content-position="left">关系预览（{{ previewResult.relationResults.length }}条）</el-divider>
					<el-table :data="previewResult.relationResults" border size="small" style="width: 100%">
						<el-table-column prop="mappingCode" label="映射编码" width="120" show-overflow-tooltip />
						<el-table-column label="主体键哈希" width="120" show-overflow-tooltip>
							<template #default="{ row }">{{ formatDigest(row.subjectRecordKeyHash) }}</template>
						</el-table-column>
						<el-table-column label="客体键哈希" width="120" show-overflow-tooltip>
							<template #default="{ row }">{{ formatDigest(row.objectRecordKeyHash) }}</template>
						</el-table-column>
						<el-table-column label="操作" width="100">
							<template #default="{ row }">
								<el-tag :type="relationActionTagType(row.action)" size="small">{{ row.action }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="问题" min-width="120">
							<template #default="{ row }">
								<el-text v-if="row.issues?.length" type="danger" size="small">{{ row.issues.join('; ') }}</el-text>
								<span v-else>-</span>
							</template>
						</el-table-column>
					</el-table>
				</template>

				<el-alert v-if="previewResult.truncated" type="warning" :closable="false" show-icon class="mt8">
					<template #title>结果已截断（达到样本上限）</template>
				</el-alert>
			</template>
		</div>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { useMessage } from '/@/hooks/message';
import { mappingValidationApi } from '/@/api/ontology/data-mapping';
import type { MappingPreviewResult, EntityMappingVO, PreviewAction, RelationPreviewAction } from '/@/types/ontology/data-mapping';
import { formatDigest } from '../utils/security-display';

const props = defineProps<{ versionId: number; entityMappings?: EntityMappingVO[] }>();
const { error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const previewResult = ref<MappingPreviewResult | null>(null);
const entityMappings = ref<EntityMappingVO[]>([]);

const previewParams = reactive({
	entityMappingCodes: [] as string[],
	sampleSize: 20,
});

const open = (mappings?: EntityMappingVO[]) => {
	visible.value = true;
	previewResult.value = null;
	entityMappings.value = mappings || props.entityMappings || [];
	previewParams.entityMappingCodes = [];
	previewParams.sampleSize = 20;
};

const loadPreview = async () => {
	loading.value = true;
	previewResult.value = null;
	try {
		const { data } = await mappingValidationApi.preview(props.versionId, {
			mappingVersionId: props.versionId,
			entityMappingCodes: previewParams.entityMappingCodes.length > 0 ? previewParams.entityMappingCodes : undefined,
			sampleSize: previewParams.sampleSize,
		});
		previewResult.value = data;
	} catch (e: any) {
		msgError(e.message || '预览失败');
	} finally {
		loading.value = false;
	}
};

const actionTagType = (action: PreviewAction): 'success' | 'primary' | 'info' | 'danger' => {
	switch (action) {
		case 'WOULD_CREATE':
			return 'success';
		case 'WOULD_UPDATE':
			return 'primary';
		case 'UNCHANGED':
			return 'info';
		case 'FAIL':
			return 'danger';
		default:
			return 'info';
	}
};

const relationActionTagType = (action: RelationPreviewAction): 'success' | 'info' | 'warning' => {
	switch (action) {
		case 'WOULD_CREATE':
			return 'success';
		case 'UNCHANGED':
			return 'info';
		case 'PENDING':
			return 'warning';
		default:
			return 'info';
	}
};

defineExpose({ open });
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mt8 {
	margin-top: 8px;
}
</style>
