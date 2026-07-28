<template>
	<div class="inherited-panel">
		<el-table v-loading="loading" :data="view?.properties || []" border style="width: 100%" empty-text="-">
			<el-table-column :label="t('classTemplate.index')" type="index" width="60" />
			<el-table-column :label="t('classTemplate.propertyRefs')" prop="propertyTemplateCode" show-overflow-tooltip />
			<el-table-column :label="t('classTemplate.refType')" prop="refType" width="110">
				<template #default="scope">
					<el-tag :type="scope.row.refType === 'property' ? undefined : 'success'" size="small">
						{{ scope.row.refType === 'property' ? t('classTemplate.refTypeProperty') : t('classTemplate.refTypeRelationship') }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column :label="t('classTemplate.sortOrder')" prop="sortOrder" width="80" />
			<el-table-column :label="t('classTemplate.source')" width="140">
				<template #default="scope">
					<el-tag
						v-if="sourceTagConfig[scope.row.source as keyof typeof sourceTagConfig]"
						:type="sourceTagConfig[scope.row.source as keyof typeof sourceTagConfig].type || undefined"
						size="small"
					>
						{{ sourceTagConfig[scope.row.source as keyof typeof sourceTagConfig].label }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column :label="t('classTemplate.parentTemplateCode')" prop="sourceClassTemplateCode" width="160" show-overflow-tooltip />
		</el-table>
		<div v-if="view?.parentChain?.length" class="parent-chain muted mt8">
			{{ t('classTemplate.parentId') }}: {{ view.parentChain.join(' ← ') }}
		</div>
	</div>
</template>

<script lang="ts" name="ClassTemplateInheritedPanel" setup>
import { useI18n } from 'vue-i18n';
import { useClassTemplateOptions } from './composables';

defineProps<{
	view: any;
	loading?: boolean;
}>();

const { t } = useI18n();
const { sourceTagConfig } = useClassTemplateOptions();
</script>

<style scoped>
.parent-chain {
	font-size: 12px;
}
.muted {
	color: #999;
}
.mt8 {
	margin-top: 8px;
}
</style>
