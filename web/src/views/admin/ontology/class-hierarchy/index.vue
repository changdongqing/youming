<template>
	<div class="layout-padding">
		<el-alert :title="t('classHierarchy.boundaryTip')" type="warning" :closable="false" class="mb12" show-icon />
		<splitpanes>
			<pane size="38">
				<div class="layout-padding-auto layout-padding-view tree-pane">
					<div class="section-title mb8">{{ t('classHierarchy.treeTitle') }}</div>
					<el-scrollbar v-loading="treeLoading">
						<el-empty v-if="!treeData.length" :description="t('classHierarchy.emptyTreeTip')" />
						<el-tree
							v-else
							:data="treeData"
							:props="{ label: 'classIri', children: 'children' }"
							node-key="classIri"
							default-expand-all
							highlight-current
							@node-click="handleNodeClick"
						>
							<template #default="{ data }">
								<span class="hierarchy-node">
									<span>{{ shortIri(data.classIri) }}</span>
									<el-tag v-if="data.syncStatus === '1'" size="small" type="success">{{ t('classHierarchy.statusSynced') }}</el-tag>
									<el-tag v-else-if="data.syncStatus === '2'" size="small" type="info">{{ t('classHierarchy.statusInvalid') }}</el-tag>
								</span>
							</template>
						</el-tree>
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div class="layout-padding-auto layout-padding-view table-pane">
					<div class="section-title mb8">{{ t('classHierarchy.tableTitle') }}</div>
					<el-row class="ml10 mb8" v-show="showSearch">
						<el-form :inline="true" :model="state.queryForm" ref="queryRef">
							<el-form-item :label="t('classHierarchy.syncStatus')" prop="syncStatus">
								<el-select
									v-model="state.queryForm.syncStatus"
									:placeholder="t('classHierarchy.selectSyncStatusTip')"
									clearable
									style="max-width: 160px"
								>
									<el-option key="0" :label="t('classHierarchy.statusPending')" value="0" />
									<el-option key="1" :label="t('classHierarchy.statusSynced')" value="1" />
									<el-option key="2" :label="t('classHierarchy.statusInvalid')" value="2" />
								</el-select>
							</el-form-item>
							<el-form-item :label="t('classHierarchy.childClassIri')" prop="childClassIri">
								<el-input v-model="state.queryForm.childClassIri" style="max-width: 200px" />
							</el-form-item>
							<el-form-item>
								<el-button @click="getDataList" icon="search" type="primary">{{ t('common.queryBtn') }}</el-button>
								<el-button @click="resetQuery" icon="Refresh">{{ t('common.resetBtn') }}</el-button>
							</el-form-item>
						</el-form>
					</el-row>
					<el-row>
						<div class="mb8" style="width: 100%">
							<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
						</div>
					</el-row>
					<el-table :data="state.dataList" v-loading="state.loading" border style="width: 100%" :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('classHierarchy.childClassIri')" prop="childClassIri" show-overflow-tooltip />
						<el-table-column :label="t('classHierarchy.parentClassIri')" prop="parentClassIri" show-overflow-tooltip />
						<el-table-column :label="t('classHierarchy.sourceTemplateRef')" prop="sourceTemplateRef" width="160" show-overflow-tooltip />
						<el-table-column :label="t('classHierarchy.syncStatus')" prop="syncStatus" width="110">
							<template #default="scope">
								<el-tag :type="syncStatusType(scope.row.syncStatus)" size="small">
									{{ syncStatusLabel(scope.row.syncStatus) }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('classHierarchy.syncTime')" prop="syncTime" width="160" show-overflow-tooltip />
						<el-table-column :label="t('classHierarchy.createTime')" prop="createTime" width="160" show-overflow-tooltip />
					</el-table>
					<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
				</div>
			</pane>
		</splitpanes>
	</div>
</template>

<script lang="ts" name="ontologyClassHierarchy" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { tree as fetchTree, pageList } from '/@/api/ontology/class-hierarchy';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const queryRef = ref();
const showSearch = ref(true);
const treeLoading = ref(false);
const treeData = ref<any[]>([]);

const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {
		syncStatus: '',
		childClassIri: '',
	},
	pageList: pageList,
	descs: ['sync_time'],
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

const loadTree = async () => {
	treeLoading.value = true;
	try {
		const { data } = await fetchTree();
		treeData.value = data || [];
	} finally {
		treeLoading.value = false;
	}
};

const handleNodeClick = (data: any) => {
	// 点击树节点联动右侧表格（按子类 IRI 过滤）
	state.queryForm.childClassIri = shortIri(data.classIri);
	getDataList();
};

const resetQuery = () => {
	queryRef.value?.resetFields();
	state.queryForm.childClassIri = '';
	getDataList();
};

/** 截取 IRI 短名（http://.../#Name -> Name）。 */
const shortIri = (iri: string) => {
	if (!iri) return '';
	const idx = Math.max(iri.lastIndexOf('#'), iri.lastIndexOf('/'));
	return idx >= 0 ? iri.substring(idx + 1) : iri;
};

const syncStatusType = (s: string) => (s === '1' ? 'success' : s === '2' ? 'info' : 'warning');
const syncStatusLabel = (s: string) =>
	s === '1' ? t('classHierarchy.statusSynced') : s === '2' ? t('classHierarchy.statusInvalid') : t('classHierarchy.statusPending');

onMounted(() => {
	loadTree();
	getDataList();
});
</script>

<style scoped>
.mb8 {
	margin-bottom: 8px;
}
.mb12 {
	margin-bottom: 12px;
}
.tree-pane,
.table-pane {
	padding: 16px;
}
.section-title {
	font-size: 13px;
	font-weight: 600;
	color: #606266;
	border-left: 3px solid var(--el-color-primary);
	padding-left: 8px;
}
.hierarchy-node {
	display: flex;
	align-items: center;
	gap: 6px;
	font-size: 13px;
}
</style>
