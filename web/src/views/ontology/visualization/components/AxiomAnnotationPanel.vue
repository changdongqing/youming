<!--
  公理标注面板。
  选中实体类型节点时展示该节点的公理约束详情（父类/子类/等价/不相交）。
-->
<script lang="ts" name="AxiomAnnotationPanel" setup>
import type { GraphNode } from '/@/types/ontology/visualization';

defineProps<{
	entityTypeDetail?: any;
	node?: GraphNode;
}>();
</script>

<template>
	<div class="axiom-panel">
		<h4 style="margin: 0 0 8px">公理约束</h4>
		<el-descriptions v-if="entityTypeDetail" :column="1" size="small" border>
			<el-descriptions-item label="不相交类型">
				<template v-if="entityTypeDetail.disjoints?.length">
					<el-tag
						v-for="d in entityTypeDetail.disjoints"
						:key="d.id"
						type="danger"
						size="small"
						style="margin: 2px"
					>
						{{ d.name }}
					</el-tag>
				</template>
				<span v-else>无</span>
			</el-descriptions-item>
			<el-descriptions-item label="等价类型">
				<template v-if="entityTypeDetail.equivalents?.length">
					<el-tag
						v-for="e in entityTypeDetail.equivalents"
						:key="e.id"
						type="success"
						size="small"
						style="margin: 2px"
					>
						{{ e.name }}
					</el-tag>
				</template>
				<span v-else>无</span>
			</el-descriptions-item>
			<el-descriptions-item label="父类">
				<template v-if="entityTypeDetail.parents?.length">
					<el-tag
						v-for="p in entityTypeDetail.parents"
						:key="p.id"
						type="primary"
						size="small"
						style="margin: 2px"
					>
						{{ p.name }}
					</el-tag>
				</template>
				<span v-else>无（根类型）</span>
			</el-descriptions-item>
			<el-descriptions-item label="子类">
				<template v-if="entityTypeDetail.children?.length">
					<el-tag
						v-for="c in entityTypeDetail.children"
						:key="c.id"
						size="small"
						style="margin: 2px"
					>
						{{ c.name }}
					</el-tag>
				</template>
				<span v-else>无</span>
			</el-descriptions-item>
		</el-descriptions>
		<el-empty v-else description="选择节点查看公理约束" :image-size="40" />
	</div>
</template>

<style scoped>
.axiom-panel {
	margin-top: 12px;
}
</style>
