<!--
  可视化首页统计概览。
  展示本体工程的全局数据概览：节点/边统计、校验状态、实例分布。
-->
<script lang="ts" name="StatsOverview" setup>
import type { VisualizationStats } from '/@/types/ontology/visualization';

const props = defineProps<{ stats: VisualizationStats }>();

/** 校验状态图标类型 */
function validationIcon(status: string): 'success' | 'warning' | 'info' {
	if (status === 'PASS') return 'success';
	if (status === 'FAIL') return 'warning';
	return 'info';
}
</script>

<template>
	<div class="stats-overview">
		<el-row :gutter="16">
			<el-col :span="6">
				<el-card shadow="hover">
					<el-statistic title="实体类型" :value="props.stats.entityTypeCount" />
				</el-card>
			</el-col>
			<el-col :span="6">
				<el-card shadow="hover">
					<el-statistic title="数据属性" :value="props.stats.dataPropertyCount" />
				</el-card>
			</el-col>
			<el-col :span="6">
				<el-card shadow="hover">
					<el-statistic title="对象属性" :value="props.stats.objectPropertyCount" />
				</el-card>
			</el-col>
			<el-col :span="6">
				<el-card shadow="hover">
					<el-statistic title="实例总数" :value="props.stats.instanceCount" />
				</el-card>
			</el-col>
		</el-row>
		<el-row :gutter="16" style="margin-top: 16px">
			<el-col :span="8">
				<el-card shadow="hover">
					<el-statistic title="公理规则" :value="props.stats.axiomRuleCount" />
				</el-card>
			</el-col>
			<el-col :span="8">
				<el-card shadow="hover">
					<el-statistic title="导出次数" :value="props.stats.exportCount" />
				</el-card>
			</el-col>
			<el-col :span="8">
				<el-card shadow="hover">
					<el-statistic title="扩展模块" :value="props.stats.extensionModuleCount" />
				</el-card>
			</el-col>
		</el-row>
		<el-row :gutter="16" style="margin-top: 16px">
			<el-col :span="12">
				<el-card shadow="hover">
					<template #header>校验状态</template>
					<el-result
						:icon="validationIcon(props.stats.latestValidationStatus)"
						:title="props.stats.latestValidationStatus || '未校验'"
						:sub-title="`违规数: ${props.stats.latestViolationCount}`"
					/>
				</el-card>
			</el-col>
			<el-col :span="12">
				<el-card shadow="hover">
					<template #header>实例分布 Top 10</template>
					<el-table :data="props.stats.instanceDistribution" size="small" max-height="300">
						<el-table-column prop="name" label="实体类型" />
						<el-table-column prop="value" label="实例数" width="80" align="right" />
					</el-table>
				</el-card>
			</el-col>
		</el-row>
	</div>
</template>

<style scoped>
.stats-overview {
	padding: 8px;
}
</style>
