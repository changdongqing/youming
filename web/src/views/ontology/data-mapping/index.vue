<template>
	<div class="layout-padding ontology-data-mapping-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-tabs v-model="activeTab" class="mt8">
				<el-tab-pane label="数据源" name="sources">
					<DataSourceTable v-if="activeTab === 'sources'" />
				</el-tab-pane>
				<el-tab-pane label="映射工程" name="projects">
					<MappingProjectTable v-if="activeTab === 'projects'" />
				</el-tab-pane>
				<el-tab-pane label="同步作业" name="jobs">
					<JobTable v-if="activeTab === 'jobs'" />
				</el-tab-pane>
				<el-tab-pane label="待解析关系" name="pending">
					<PendingRelationTable v-if="activeTab === 'pending'" />
				</el-tab-pane>
			</el-tabs>
		</div>
	</div>
</template>

<script lang="ts" name="ontologyDataMapping" setup>
import { ref } from 'vue';
import DataSourceTable from './components/DataSourceTable.vue';
import MappingProjectTable from './components/MappingProjectTable.vue';
import JobTable from './components/JobTable.vue';
import PendingRelationTable from './components/PendingRelationTable.vue';

const activeTab = ref('sources');
</script>

<style scoped>
.ontology-data-mapping-page {
	height: 100%;
}
</style>
