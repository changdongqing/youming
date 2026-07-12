<template>
	<el-dialog v-model="visible" title="变更影响分析" width="640px">
		<template v-if="summary">
			<el-alert type="warning" :closable="false"
				title="删除扩展模块将解除以下资源的关联关系（资源本身不会被删除）" style="margin-bottom: 16px" />
			<el-descriptions :column="2" border size="small">
				<el-descriptions-item label="扩展实体类型">{{ summary.resourceSummary?.entityType || 0 }} 个</el-descriptions-item>
				<el-descriptions-item label="扩展数据属性">{{ summary.resourceSummary?.dataProperty || 0 }} 个</el-descriptions-item>
				<el-descriptions-item label="扩展对象属性">{{ summary.resourceSummary?.objectProperty || 0 }} 个</el-descriptions-item>
				<el-descriptions-item label="扩展公理规则">{{ summary.resourceSummary?.axiomRule || 0 }} 个</el-descriptions-item>
				<el-descriptions-item label="扩展单位">{{ summary.resourceSummary?.unit || 0 }} 个</el-descriptions-item>
				<el-descriptions-item label="关联实例总数">{{ summary.instanceImpact?.totalInstances || 0 }} 个</el-descriptions-item>
			</el-descriptions>
			<template v-if="summary.instanceImpact?.instancesByType?.length">
				<div style="margin-top: 16px; margin-bottom: 8px; font-weight: bold">实例分布</div>
				<el-table :data="summary.instanceImpact.instancesByType" border size="small">
					<el-table-column prop="entityTypeIri" label="实体类型IRI" show-overflow-tooltip />
					<el-table-column prop="count" label="实例数" width="80" align="center" />
				</el-table>
			</template>
		</template>
		<el-empty v-else description="无影响分析数据" />
		<template #footer>
			<el-button @click="visible = false">关闭</el-button>
		</template>
	</el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ExtensionImpactSummary } from '/@/types/ontology/extension';

const props = defineProps<{ summary: ExtensionImpactSummary | null }>();
const emit = defineEmits<{ 'update:visible': [value: boolean] }>();

const visible = computed({ get: () => true, set: (v) => emit('update:visible', v) });
</script>
