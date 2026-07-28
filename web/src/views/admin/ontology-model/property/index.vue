<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10 mb8">
				<el-form :inline="true" :model="state.queryForm" ref="queryRef">
					<el-form-item :label="t('modelProperty.selectProject')">
						<el-select v-model="state.queryForm.projectId" :placeholder="t('modelProperty.selectProject')" style="width: 180px" @change="loadClasses">
							<el-option v-for="p in projectList" :key="p.id" :label="p.name" :value="p.id" />
						</el-select>
					</el-form-item>
					<el-form-item :label="t('modelProperty.selectClass')">
						<el-select v-model="state.queryForm.classId" :placeholder="t('modelProperty.selectClass')" style="width: 200px" :disabled="!state.queryForm.projectId" @change="getDataList">
							<el-option v-for="c in classList" :key="c.id" :label="c.label || c.localName" :value="c.id" />
						</el-select>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button v-auth="'ont_prop_model_manage'" :disabled="!state.queryForm.classId" @click="onOpenDialog()" class="ml10" icon="folder-add" type="primary">
						{{ t('modelProperty.add') }}
					</el-button>
					<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
				</div>
			</el-row>
			<el-tabs v-model="activeTab" @tab-change="getDataList">
				<!-- 数据属性 Tab -->
				<el-tab-pane :label="t('modelProperty.datatypeProperty')" name="datatype">
					<el-table :data="state.dataList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('modelProperty.index')" type="index" width="60" />
						<el-table-column :label="t('modelProperty.localName')" prop="localName" show-overflow-tooltip />
						<el-table-column :label="t('modelProperty.label')" prop="label" show-overflow-tooltip />
						<el-table-column :label="t('modelProperty.xsdType')" prop="xsdType" width="120" />
						<el-table-column :label="t('modelProperty.unitRef')" prop="unitRef" width="100" show-overflow-tooltip />
						<el-table-column :label="t('modelProperty.minCardinality') + '..' + t('modelProperty.maxCardinality')" width="120">
							<template #default="scope">{{ formatCardinality(scope.row.minCardinality, scope.row.maxCardinality) }}</template>
						</el-table-column>
						<el-table-column :label="t('modelProperty.isIdentifier')" width="80">
							<template #default="scope">
								<el-tag size="small" type="success" v-if="scope.row.isIdentifier === '1'">{{ t('modelProperty.identifierYes') }}</el-tag>
								<el-tag size="small" type="info" v-else>{{ t('modelProperty.identifierNo') }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('common.action')" width="140">
							<template #default="scope">
								<el-button icon="edit-pen" @click="onOpenDialog(scope.row.id)" text type="primary" v-auth="'ont_prop_model_manage'">{{ t('common.editBtn') }}</el-button>
								<el-button icon="delete" @click="handleDelete(scope.row)" text type="primary" v-auth="'ont_prop_model_manage'">{{ t('common.delBtn') }}</el-button>
							</template>
						</el-table-column>
					</el-table>
				</el-tab-pane>
				<!-- 对象属性 Tab -->
				<el-tab-pane :label="t('modelProperty.objectProperty')" name="object">
					<el-table :data="state.dataList" style="width: 100%" v-loading="state.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('modelProperty.index')" type="index" width="60" />
						<el-table-column :label="t('modelProperty.localName')" prop="localName" show-overflow-tooltip />
						<el-table-column :label="t('modelProperty.label')" prop="label" show-overflow-tooltip />
						<el-table-column :label="t('modelProperty.rangeClass')" prop="rangeClassId" width="160">
							<template #default="scope">
								<el-tag size="small" type="warning" v-if="!scope.row.rangeClassId">{{ t('modelProperty.rangePending') }}</el-tag>
								<span v-else>{{ getClassName(scope.row.rangeClassId) }}</span>
							</template>
						</el-table-column>
						<el-table-column :label="t('modelProperty.minCardinality') + '..' + t('modelProperty.maxCardinality')" width="120">
							<template #default="scope">{{ formatCardinality(scope.row.minCardinality, scope.row.maxCardinality) }}</template>
						</el-table-column>
						<el-table-column :label="t('common.action')" width="200">
							<template #default="scope">
								<el-button icon="edit-pen" @click="onOpenDialog(scope.row.id)" text type="primary" v-auth="'ont_prop_model_manage'">{{ t('common.editBtn') }}</el-button>
								<el-button icon="delete" @click="handleDelete(scope.row)" text type="primary" v-auth="'ont_prop_model_manage'">{{ t('common.delBtn') }}</el-button>
								<el-button icon="Connection" @click="onSuggestInverse(scope.row)" text type="primary" v-auth="'ont_prop_model_view'" v-if="scope.row.rangeClassId">{{ t('modelProperty.suggestInverse') }}</el-button>
							</template>
						</el-table-column>
					</el-table>
				</el-tab-pane>
			</el-tabs>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>
		<form-dialog @refresh="getDataList()" ref="formDialogRef" :active-tab="activeTab" :class-id="state.queryForm.classId" :class-list="classList" />
		<inverse-dialog ref="inverseDialogRef" :class-list="classList" @refresh="getDataList" />
	</div>
</template>

<script lang="ts" name="ontologyModelProperty" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList as pageProject } from '/@/api/ontology-model/project';
import { pageList as pageClass } from '/@/api/ontology-model/class';
import { delObj as delDt, pageList as pageDt } from '/@/api/ontology-model/datatype-property';
import { delObj as delObj2, pageList as pageObj, suggestInverse } from '/@/api/ontology-model/object-property';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { useModelPropertyOptions } from './composables';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const InverseDialog = defineAsyncComponent(() => import('./inverse-dialog.vue'));
const { t } = useI18n();

const formDialogRef = ref();
const inverseDialogRef = ref();
const queryRef = ref();
const showSearch = ref(true);
const activeTab = ref('datatype');

const projectList = ref<any[]>([]);
const classList = ref<any[]>([]);

const { formatCardinality } = useModelPropertyOptions();

const state: BasicTableProps = reactive<BasicTableProps>({
	isPage: true,
	queryForm: {
		projectId: '',
		classId: '',
	},
	pageList: pageDt, // 动态切换（datatype/object）
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// Tab 切换时切换分页 API
watch(activeTab, (val) => {
	state.pageList = val === 'datatype' ? pageDt : pageObj;
});

const loadProjects = async () => {
	try {
		const { data } = await pageProject({ size: 200 });
		projectList.value = data?.records ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const loadClasses = async () => {
	state.queryForm.classId = '';
	classList.value = [];
	if (!state.queryForm.projectId) return;
	try {
		const { data } = await pageClass({ projectId: state.queryForm.projectId, size: 200 });
		classList.value = data?.records ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const getClassName = (classId: string) => {
	const cls = classList.value.find((c) => c.id === classId);
	return cls ? (cls.label || cls.localName) : classId;
};

const onOpenDialog = (id?: string) => {
	if (!state.queryForm.classId) {
		useMessage().warning(t('modelProperty.selectClass'));
		return;
	}
	formDialogRef.value.openDialog(state.queryForm.classId, id);
};

const onSuggestInverse = (row: any) => {
	inverseDialogRef.value.openDialog(row);
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm(t('modelProperty.deleteTip'));
	} catch {
		return;
	}
	try {
		activeTab.value === 'datatype' ? await delDt(row.id) : await delObj2(row.id);
		useMessage().success(t('common.delSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

onMounted(() => {
	loadProjects();
});
</script>
