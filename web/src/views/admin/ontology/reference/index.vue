<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-tabs v-model="activeTab" @tab-change="handleTabChange">
				<!-- QUDT 单位 -->
				<el-tab-pane name="qudt">
					<template #label>
						<span>{{ t('reference.tabQudt') }}</span>
					</template>
					<el-row class="mb8 ml10">
						<el-input :placeholder="t('reference.inputKeywordTip')" style="max-width: 200px" v-model="qudtState.queryForm.keyword" @keyup.enter="getQudtList" clearable />
						<el-button @click="getQudtList" icon="search" type="primary" class="ml10">{{ t('common.queryBtn') }}</el-button>
					</el-row>
					<el-table :data="qudtState.dataList" style="width: 100%" v-loading="qudtState.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('reference.iri')" prop="iri" show-overflow-tooltip />
						<el-table-column :label="t('reference.label')" prop="label" width="160" show-overflow-tooltip />
						<el-table-column :label="t('reference.symbol')" prop="symbol" width="80" />
						<el-table-column :label="t('reference.quantityKind')" prop="quantityKindIri" width="200" show-overflow-tooltip />
						<el-table-column :label="t('reference.conversionMultiplier')" prop="conversionMultiplier" width="120" />
						<el-table-column :label="t('common.action')" width="160">
							<template #default="scope">
								<el-button @click="handleImportUnit(scope.row.iri)" text type="primary" v-auth="'ont_unit_manage'">
									{{ t('reference.importUnit') }}
								</el-button>
							</template>
						</el-table-column>
					</el-table>
					<pagination @current-change="(v) => pageChange(qudtState, v)" @size-change="(v) => sizeChange(qudtState, v)" v-bind="qudtState.pagination" />
				</el-tab-pane>

				<!-- Brick 类 -->
				<el-tab-pane name="brick">
					<template #label>
						<span>{{ t('reference.tabBrick') }}</span>
					</template>
					<el-row class="mb8 ml10">
						<el-input :placeholder="t('reference.inputKeywordTip')" style="max-width: 200px" v-model="brickState.queryForm.keyword" @keyup.enter="getBrickList" clearable />
						<el-button @click="getBrickList" icon="search" type="primary" class="ml10">{{ t('common.queryBtn') }}</el-button>
					</el-row>
					<el-table :data="brickState.dataList" style="width: 100%" v-loading="brickState.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('reference.iri')" prop="iri" show-overflow-tooltip />
						<el-table-column :label="t('reference.label')" prop="label" width="180" show-overflow-tooltip />
						<el-table-column :label="t('reference.definition')" prop="definition" show-overflow-tooltip />
						<el-table-column :label="t('reference.parentClass')" prop="parentIri" width="200" show-overflow-tooltip />
						<el-table-column :label="t('reference.deprecated')" prop="deprecated" width="80">
							<template #default="scope">
								<el-tag v-if="scope.row.deprecated" size="small" type="danger">{{ t('reference.deprecated') }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('common.action')" width="160">
							<template #default="scope">
								<el-button @click="handleImportClass(scope.row.iri)" text type="primary" v-auth="'ont_class_tpl_manage'">
									{{ t('reference.importClass') }}
								</el-button>
							</template>
						</el-table-column>
					</el-table>
					<pagination @current-change="(v) => pageChange(brickState, v)" @size-change="(v) => sizeChange(brickState, v)" v-bind="brickState.pagination" />
				</el-tab-pane>

				<!-- CCO 注释属性 -->
				<el-tab-pane name="cco">
					<template #label>
						<span>{{ t('reference.tabCco') }}</span>
					</template>
					<el-row class="mb8 ml10">
						<el-input :placeholder="t('reference.inputKeywordTip')" style="max-width: 200px" v-model="ccoState.queryForm.keyword" @keyup.enter="getCcoList" clearable />
						<el-button @click="getCcoList" icon="search" type="primary" class="ml10">{{ t('common.queryBtn') }}</el-button>
					</el-row>
					<el-table :data="ccoState.dataList" style="width: 100%" v-loading="ccoState.loading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('reference.iri')" prop="iri" show-overflow-tooltip />
						<el-table-column :label="t('reference.label')" prop="label" width="200" show-overflow-tooltip />
						<el-table-column :label="t('reference.definition')" prop="definition" show-overflow-tooltip />
					</el-table>
					<pagination @current-change="(v) => pageChange(ccoState, v)" @size-change="(v) => sizeChange(ccoState, v)" v-bind="ccoState.pagination" />
				</el-tab-pane>
			</el-tabs>
		</div>
	</div>
</template>

<script lang="ts" name="ontologyReference" setup>
import { useTable } from '/@/hooks/table';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { importBrickClass, importQudtUnit, pageAnnotationProperties, pageClasses, pageUnits } from '/@/api/ontology/reference';

const { t } = useI18n();

const activeTab = ref('qudt');

// 三个 Tab 各自的表格状态（复用 useTable 的 tableStyle，分页手动管理）
const buildState = (api: any) =>
	reactive({
		queryForm: { keyword: '' },
		dataList: [] as any[],
		loading: false,
		pagination: { current: 1, size: 10, total: 0 },
		pageList: api,
	});

const qudtState = buildState(pageUnits);
const brickState = buildState(pageClasses);
const ccoState = buildState(pageAnnotationProperties);

const { tableStyle } = useTable(qudtState);

// ---------- 分页查询 ----------
const fetchData = async (state: any, api: any) => {
	state.loading = true;
	try {
		const { data } = await api({
			...state.queryForm,
			current: state.pagination.current,
			size: state.pagination.size,
		});
		state.dataList = data?.records ?? [];
		state.pagination.total = data?.total ?? 0;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		state.loading = false;
	}
};

const getQudtList = () => {
	qudtState.pagination.current = 1;
	fetchData(qudtState, pageUnits);
};
const getBrickList = () => {
	brickState.pagination.current = 1;
	fetchData(brickState, pageClasses);
};
const getCcoList = () => {
	ccoState.pagination.current = 1;
	fetchData(ccoState, pageAnnotationProperties);
};

const pageChange = (state: any, val: number) => {
	state.pagination.current = val;
	fetchData(state, state.pageList);
};
const sizeChange = (state: any, val: number) => {
	state.pagination.size = val;
	state.pagination.current = 1;
	fetchData(state, state.pageList);
};

const handleTabChange = (name: any) => {
	if (name === 'qudt' && qudtState.dataList.length === 0) getQudtList();
	if (name === 'brick' && brickState.dataList.length === 0) getBrickList();
	if (name === 'cco' && ccoState.dataList.length === 0) getCcoList();
};

// ---------- 导入 ----------
const handleImportUnit = async (iri: string) => {
	try {
		await useMessageBox().confirm(t('reference.importUnitTip'));
	} catch {
		return;
	}
	try {
		const { msg } = await importQudtUnit(iri);
		useMessage().success(msg || t('reference.importSuccess'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleImportClass = async (iri: string) => {
	try {
		await useMessageBox().confirm(t('reference.importClassTip'));
	} catch {
		return;
	}
	try {
		const { msg } = await importBrickClass(iri);
		useMessage().success(msg || t('reference.importSuccess'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

// 首次加载 QUDT Tab
onMounted(() => getQudtList());
</script>

<style lang="scss" scoped>
/**
 * 表格高度自适应：el-tabs/el-tab-pane 默认是普通块级容器，会阻断全局 layout-flex 高度链，
 * 导致 el-table 拿不到确定高度、行多时不出现滚动条、分页被挤出视口。
 * 此处补齐 flex 高度链：tabs → content → 激活的 pane 均为 flex 列容器并传递剩余高度，
 * 使 el-table 靠 flex:1 撑高（超出内部滚动），pagination 固定底部，与其它列表页表现一致。
 */
:deep(.el-tabs) {
	display: flex;
	flex-direction: column;
	flex: 1;
	overflow: hidden;
}
:deep(.el-tabs__header) {
	flex-shrink: 0;
}
:deep(.el-tabs__content) {
	flex: 1;
	overflow: hidden;
}
:deep(.el-tab-pane) {
	height: 100%;
	display: flex;
	flex-direction: column;
}
</style>
