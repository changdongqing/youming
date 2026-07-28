<template>
	<div class="layout-padding">
		<splitpanes>
			<pane size="26">
				<div class="layout-padding-auto layout-padding-view">
					<el-row>
						<div class="mb8" style="width: 100%">
							<span class="ml10" style="font-weight: 600">{{ t('modelClass.projectList') }}</span>
						</div>
					</el-row>
					<el-scrollbar>
						<el-tree
							:data="projectTree"
							:props="{ label: 'name', children: 'children' }"
							node-key="id"
							:expand-on-click-node="false"
							:highlight-current="true"
							@node-click="handleProjectClick"
						>
							<template #default="{ data }">
								<span class="project-node">
									<el-icon><Folder /></el-icon>
									<span class="label">{{ data.name }}</span>
								</span>
							</template>
						</el-tree>
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div class="layout-padding-auto layout-padding-view">
					<el-row class="ml10" v-show="showSearch">
						<el-form :inline="true" :model="state.queryForm" ref="queryRef">
							<el-form-item :label="t('modelClass.label')" prop="label">
								<el-input :placeholder="t('modelClass.inputLabelTip')" style="max-width: 160px" v-model="state.queryForm.label" @keyup.enter="getDataList" />
							</el-form-item>
							<el-form-item :label="t('modelClass.templateCode')" prop="templateCode">
								<el-select :placeholder="t('modelClass.selectTemplate')" style="max-width: 160px" v-model="state.queryForm.templateCode" clearable @change="getDataList">
									<el-option key="has" :label="t('modelClass.hasTemplate')" value="" />
								</el-select>
							</el-form-item>
							<el-form-item>
								<el-button @click="getDataList" icon="search" type="primary">{{ t('common.queryBtn') }}</el-button>
								<el-button @click="resetQuery" icon="Refresh">{{ t('common.resetBtn') }}</el-button>
							</el-form-item>
						</el-form>
					</el-row>
					<el-row>
						<div class="mb8" style="width: 100%">
							<el-button v-auth="'ont_class_model_manage'" :disabled="!state.queryForm.projectId" @click="onOpenDialog()" class="ml10" icon="folder-add" type="primary">
								{{ t('common.addBtn') }}
							</el-button>
							<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
						</div>
					</el-row>
					<el-table :data="state.dataList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('modelClass.index')" type="index" width="60" />
						<el-table-column :label="t('modelClass.localName')" prop="localName" show-overflow-tooltip />
						<el-table-column :label="t('modelClass.label')" prop="label" show-overflow-tooltip />
						<el-table-column :label="t('modelClass.classIri')" prop="classIri" show-overflow-tooltip />
						<el-table-column :label="t('modelClass.classificationCode')" prop="classificationCode" width="130">
							<template #default="scope">
								<el-tag size="small" type="success" v-if="scope.row.classificationCode">{{ scope.row.classificationCode }}</el-tag>
								<span v-else>-</span>
							</template>
						</el-table-column>
						<el-table-column :label="t('modelClass.templateCode')" prop="templateCode" width="110">
							<template #default="scope">
								<el-tag size="small" type="warning" v-if="scope.row.templateCode">{{ t('modelClass.hasTemplate') }}</el-tag>
								<el-tag size="small" type="info" v-else>{{ t('modelClass.noTemplate') }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('common.action')" width="200">
							<template #default="scope">
								<el-button icon="view" @click="onOpenDetail(scope.row)" text type="primary" v-auth="'ont_class_model_view'">
									{{ t('modelClass.detail') }}
								</el-button>
								<el-button icon="edit-pen" @click="onOpenDialog(scope.row.id)" text type="primary" v-auth="'ont_class_model_manage'">
									{{ t('common.editBtn') }}
								</el-button>
								<el-button icon="delete" @click="handleDelete(scope.row)" text type="primary" v-auth="'ont_class_model_manage'">
									{{ t('common.delBtn') }}
								</el-button>
							</template>
						</el-table-column>
					</el-table>
					<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
				</div>
			</pane>
		</splitpanes>
		<form-dialog @refresh="getDataList()" ref="formDialogRef" />
		<detail-dialog ref="detailDialogRef" />
	</div>
</template>

<script lang="ts" name="ontologyModelClass" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, pageList } from '/@/api/ontology-model/class';
import { pageList as pageProject } from '/@/api/ontology-model/project';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { Folder } from '@element-plus/icons-vue';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const DetailDialog = defineAsyncComponent(() => import('./detail.vue'));
const { t } = useI18n();

const formDialogRef = ref();
const detailDialogRef = ref();
const queryRef = ref();
const showSearch = ref(true);

const projectTree = ref<any[]>([]);
const currentProject = ref<any>(null);

const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: true,
	queryForm: {
		projectId: '',
		label: '',
		templateCode: '',
	},
	pageList: pageList,
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 拉取项目列表填充左树
const loadProjectTree = async () => {
	try {
		const { data } = await pageProject({ size: 200 });
		projectTree.value = (data?.records ?? []).map((p: any) => ({ ...p, children: null }));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleProjectClick = (project: any) => {
	currentProject.value = project;
	state.queryForm.projectId = project.id;
	getDataList();
};

const onOpenDialog = (id?: string) => {
	if (!state.queryForm.projectId) {
		useMessage().warning(t('modelClass.selectProject'));
		return;
	}
	formDialogRef.value.openDialog(state.queryForm.projectId, id);
};

const onOpenDetail = (row: any) => {
	detailDialogRef.value.openDialog(row.id);
};

const resetQuery = () => {
	queryRef.value.resetFields();
	state.queryForm.projectId = currentProject.value?.id ?? '';
	getDataList();
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm(t('modelClass.deleteTip') + '「' + (row.label || row.localName) + '」？');
	} catch {
		return;
	}
	try {
		await delObj(row.id);
		useMessage().success(t('common.delSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

onMounted(() => {
	loadProjectTree();
});
</script>

<style scoped>
.project-node {
	display: flex;
	align-items: center;
	gap: 4px;
}
.project-node .label {
	margin-left: 4px;
}
</style>
