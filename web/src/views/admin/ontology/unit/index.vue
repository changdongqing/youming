<template>
	<div class="layout-padding">
		<splitpanes>
			<pane size="26">
				<div class="layout-padding-auto layout-padding-view">
					<el-scrollbar>
						<query-tree
							ref="qkTreeRef"
							:query="state.queryList"
							:props="{ label: 'label', children: 'children', value: 'id' }"
							:placeholder="t('unit.quantityKind')"
							@node-click="handleNodeClick"
						>
							<template #default="{ data }">
								<span class="custom-tree-node">
									<span class="node-icon">{{ data.icon }}</span>
									<span class="label">{{ data.labelCn || data.label }}</span>
									<span class="count">({{ data.unitCount }})</span>
								</span>
							</template>
						</query-tree>
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div class="layout-padding-auto layout-padding-view">
					<!-- 顶部查询 + 操作栏 -->
					<el-row class="ml10" v-show="showSearch">
						<el-form :inline="true" :model="state.queryForm" ref="queryRef">
							<el-form-item :label="t('unit.qudtIri')" prop="qudtIri">
								<el-input :placeholder="t('unit.inputQudtIriTip')" style="max-width: 220px" v-model="state.queryForm.qudtIri" />
							</el-form-item>
							<el-form-item :label="t('unit.deprecated')" prop="deprecated">
								<el-select :placeholder="t('unit.selectDeprecatedTip')" style="max-width: 160px" v-model="state.queryForm.deprecated" clearable>
									<el-option :key="item.value" :label="item.label" :value="item.value" v-for="item in deprecatedOptions" />
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
							<el-button v-auth="'ont_unit_manage'" @click="formDialogRef.openDialog(null, state.selectedQkId)" class="ml10" icon="folder-add" type="primary" :disabled="!state.selectedQkId">
								{{ t('unit.addBtn') }}
							</el-button>
							<right-toolbar @queryTable="getDataList" class="ml10" style="float: right; margin-right: 20px" v-model:showSearch="showSearch" />
						</div>
					</el-row>
					<!-- 单位列表 -->
					<el-table :data="state.dataList" style="width: 100%" v-loading="state.dataListLoading" border :cell-style="tableStyle.cellStyle" :header-cell-style="tableStyle.headerCellStyle">
						<el-table-column :label="t('unit.index')" type="index" width="60" />
						<el-table-column :label="t('unit.symbol')" prop="symbol" width="80" show-overflow-tooltip />
						<el-table-column :label="t('unit.label')" prop="label" show-overflow-tooltip />
						<el-table-column :label="t('unit.labelCn')" prop="labelCn" width="100" show-overflow-tooltip />
						<el-table-column :label="t('unit.conversionMultiplier')" width="130" show-overflow-tooltip>
							<template #default="scope">
								<span>{{ scope.row.conversionMultiplierSn || scope.row.conversionMultiplier }}</span>
							</template>
						</el-table-column>
						<el-table-column :label="t('unit.scalingOf')" prop="scalingOf" width="160" show-overflow-tooltip>
							<template #default="scope">
								<span v-if="scope.row.scalingOf">{{ shortIri(scope.row.scalingOf) }}</span>
								<span v-else class="muted">—</span>
							</template>
						</el-table-column>
						<el-table-column :label="t('unit.ucumCode')" prop="ucumCode" width="100" show-overflow-tooltip />
						<el-table-column :label="t('unit.source')" prop="source" width="80">
							<template #default="scope">
								<el-tag :type="scope.row.source === 'builtin' ? 'warning' : 'info'" size="small">
									{{ scope.row.source === 'builtin' ? t('unit.builtin') : t('unit.custom') }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('unit.deprecated')" prop="deprecated" width="80">
							<template #default="scope">
								<el-tag :type="scope.row.deprecated === '1' ? 'danger' : 'success'" size="small">
									{{ scope.row.deprecated === '1' ? t('unit.deprecatedLabel') : t('unit.normal') }}
								</el-tag>
							</template>
						</el-table-column>
						<el-table-column :label="t('common.action')" width="200">
							<template #default="scope">
								<el-tooltip :content="t('unit.builtinEditDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
									<span>
										<el-button
											icon="edit-pen"
											@click="formDialogRef.openDialog(scope.row.id)"
											:disabled="scope.row.source === 'builtin'"
											text
											type="primary"
											v-auth="'ont_unit_manage'"
										>
											{{ t('common.editBtn') }}
										</el-button>
									</span>
								</el-tooltip>
								<el-button
									@click="handleDeprecate(scope.row)"
									:text="true"
									:type="scope.row.deprecated === '1' ? 'success' : 'warning'"
									v-auth="'ont_unit_manage'"
								>
									{{ scope.row.deprecated === '1' ? t('common.restoreBtn') : t('unit.deprecatedLabel') }}
								</el-button>
								<el-tooltip :content="t('unit.builtinDeleteDisabledTip')" :disabled="scope.row.source !== 'builtin'" placement="top">
									<span style="margin-left: 12px">
										<el-button
											icon="delete"
											@click="handleDelete([scope.row.id])"
											:disabled="scope.row.source === 'builtin'"
											text
											type="primary"
											v-auth="'ont_unit_manage'"
										>
											{{ t('common.delBtn') }}
										</el-button>
									</span>
								</el-tooltip>
							</template>
						</el-table-column>
					</el-table>
					<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
					<!-- 换算试算区 -->
					<convert-panel :quantity-kind-id="state.selectedQkId" :quantity-kind-label="state.selectedQkLabel" :units="state.currentUnits" />
				</div>
			</pane>
		</splitpanes>
		<form-dialog @refresh="getDataList" ref="formDialogRef" />
	</div>
</template>

<script lang="ts" name="ontologyUnit" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { listQuantityKind, pageList, delObj, deprecateObj } from '/@/api/ontology/unit';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';
import { useUnitOptions } from './composables';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const ConvertPanel = defineAsyncComponent(() => import('./convert-panel.vue'));
const QueryTree = defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'));

const { t } = useI18n();
const { deprecatedOptions } = useUnitOptions();

const formDialogRef = ref();
const queryRef = ref();
const qkTreeRef = ref();
const showSearch = ref(true);

const state: BasicTableProps = reactive<BasicTableProps>({
	createdIsNeed: false,
	queryForm: {
		quantityKindId: '',
		qudtIri: '',
		deprecated: '',
	},
	pageList: pageList,
	descs: ['create_time'],
	// 额外字段（非 BasicTableProps 标准字段，供本页用）
	...({
		selectedQkId: '',
		selectedQkLabel: '',
		currentUnits: [] as any[],
		queryList: async () => {
			// QueryTree 的 query：约定返回 Promise<axios 响应>，组件取 r.data 作树形数据。
			// 量纲是扁平 8 项，包一层"量纲"根节点形成单层树。
			const { data } = await listQuantityKind();
			const kinds = (data || []).map((k: any) => ({ ...k, icon: qkIcon(k.label) }));
			return { data: [{ id: 'root', label: t('unit.quantityKind'), labelCn: t('unit.quantityKind'), children: kinds }] };
		},
	} as any),
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state as BasicTableProps);

const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

/**
 * 点击量纲树节点，加载右侧单位列表。
 * 量纲节点含 unitCount，根节点不触发查询。
 */
const handleNodeClick = async (data: any) => {
	if (data.id === 'root') {
		return;
	}
	state.selectedQkId = data.id;
	state.selectedQkLabel = data.labelCn || data.label;
	state.queryForm.quantityKindId = data.id;
	await getDataList();
	// 供换算试算区用：拉当前量纲全量单位（含系数，size=999）
	await loadCurrentUnits(data.id);
};

const loadCurrentUnits = async (qkId: string) => {
	try {
		const { data } = await pageList({ quantityKindId: qkId, size: 999 });
		state.currentUnits = data.records || [];
	} catch {
		state.currentUnits = [];
	}
};

const handleDelete = async (ids: string[]) => {
	try {
		await useMessageBox().confirm(t('unit.deleteTip'));
	} catch {
		return;
	}
	try {
		await delObj(ids[0]);
		useMessage().success(t('common.delSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleDeprecate = async (row: any) => {
	const newDeprecated = row.deprecated === '1' ? '0' : '1';
	const tip = newDeprecated === '1' ? t('unit.deprecateTip') : t('unit.restoreTip');
	try {
		await useMessageBox().confirm(tip);
	} catch {
		return;
	}
	try {
		await deprecateObj(row.id, newDeprecated);
		useMessage().success(t('common.optSuccessText'));
		getDataList();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

/**
 * 量纲图标（emoji，按 label 映射）。
 */
const qkIcon = (label: string): string => {
	const map: Record<string, string> = {
		Length: '📏',
		Mass: '⚖️',
		Time: '⏱️',
		ThermodynamicTemperature: '🌡️',
		Area: '🟦',
		Volume: '📦',
		Currency: '💰',
		InformationEntropy: '💾',
	};
	return map[label] || '📐';
};

/**
 * IRI 短名（取末段，如 .../unit/KiloM -> KiloM）。
 */
const shortIri = (iri: string): string => {
	if (!iri) return '';
	return iri.split('/').pop() || iri;
};
</script>

<style scoped>
.custom-tree-node {
	display: flex;
	flex: 1;
	align-items: center;
	font-size: 14px;
	padding-right: 8px;
	height: 100%;
}
.custom-tree-node .node-icon {
	margin-right: 4px;
}
.custom-tree-node .count {
	margin-left: 6px;
	color: #999;
	font-size: 12px;
}
.muted {
	color: #999;
}
</style>
