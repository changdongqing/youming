<template>
	<el-card v-if="inherited" class="inherited-preview" shadow="never">
		<template #header>
			<div class="card-header">
				<span>{{ t('modelClass.inheritedPreview') }}</span>
				<el-tooltip :content="t('modelClass.inheritedPreviewTip')" placement="top">
					<el-icon class="ml4"><InfoFilled /></el-icon>
				</el-tooltip>
			</div>
		</template>
		<el-descriptions :column="2" border size="small" class="mb8">
			<el-descriptions-item :label="t('modelClass.classificationCode')">
				{{ inherited.classificationCode || '-' }}
			</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.parentChain')">
				{{ inherited.parentChain?.join(' ← ') || '-' }}
			</el-descriptions-item>
		</el-descriptions>
		<el-table :data="inherited.properties || []" border size="small" max-height="240">
			<el-table-column :label="t('modelClass.propertyTemplateCode')" prop="propertyTemplateCode" show-overflow-tooltip />
			<el-table-column :label="t('modelClass.refType')" prop="refType" width="90">
				<template #default="scope">
					<el-tag :type="refTypeTagType(scope.row.refType)" size="small">{{ refTypeLabel(scope.row.refType) }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column :label="t('modelClass.source')" prop="source" width="90">
				<template #default="scope">
					<el-tag :type="sourceTagType(scope.row.source)" size="small">{{ sourceLabel(scope.row.source) }}</el-tag>
				</template>
			</el-table-column>
		</el-table>
	</el-card>
</template>

<script lang="ts" name="InheritedPreview" setup>
import { InfoFilled } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { useModelClassOptions } from './composables';

defineProps<{
	inherited: any;
}>();

const { t } = useI18n();
const { refTypeLabel, refTypeTagType, sourceLabel, sourceTagType } = useModelClassOptions();
</script>

<style scoped>
.inherited-preview {
	margin-top: 12px;
}
.card-header {
	display: flex;
	align-items: center;
	gap: 4px;
	font-weight: 600;
}
.ml4 {
	margin-left: 4px;
}
</style>
