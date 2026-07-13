<!--
  数据属性面板。
  选中实体类型节点时展示其适用数据属性列表。
-->
<script lang="ts" name="DataPropertyPanel" setup>
import type { GraphNode } from '/@/types/ontology/visualization';

defineProps<{
	properties: any[];
	entityType?: GraphNode;
}>();
</script>

<template>
	<div class="data-property-panel">
		<h4 style="margin: 0 0 8px">数据属性 ({{ properties.length }})</h4>
		<el-table :data="properties" size="small" max-height="300" border>
			<el-table-column prop="name" label="名称" width="120" />
			<el-table-column prop="baseType" label="类型" width="80" />
			<el-table-column prop="isBuiltin" label="内置" width="50">
				<template #default="{ row }">
					<el-tag v-if="row.isBuiltin === '1'" type="primary" size="small">是</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="unitSymbol" label="单位" width="60" />
		</el-table>
	</div>
</template>

<style scoped>
.data-property-panel {
	margin-bottom: 12px;
}
</style>
