<template>
	<div class="layout-padding">
		<splitpanes>
			<pane>
				<splitpanes>
					<pane size="32">
						<div class="layout-padding-auto layout-padding-view">
							<el-row>
								<div class="mb8" style="width: 100%">
									<el-select
										v-model="state.treeRoot"
										:placeholder="t('classTemplate.treeRoot')"
										class="ml10"
										style="width: 140px"
										@change="handleTreeRootChange"
									>
										<el-option key="equipment" label="equipment" value="equipment" />
									</el-select>
									<el-button
										v-auth="'ont_class_tpl_manage'"
										@click="formDialogRef.openDialog(null, state.treeRoot)"
										class="ml10"
										icon="folder-add"
										type="primary"
									>
										{{ t('classTemplate.addRootBtn') }}
									</el-button>
									<el-button
										v-auth="'ont_class_tpl_manage'"
										@click="ruleDrawerRef.open()"
										plain
										class="ml10"
										icon="set-up"
										type="primary"
									>
										{{ t('classTemplate.ruleTitle') }}
									</el-button>
								</div>
							</el-row>
							<el-scrollbar>
								<query-tree
									ref="classTreeRef"
									:query="state.queryList"
									:props="{ label: 'label', children: 'children', value: 'id' }"
									:placeholder="t('classTemplate.treeRoot')"
									@node-click="handleNodeClick"
								>
									<template #default="{ data }">
										<span class="custom-tree-node">
											<span class="node-icon" v-if="data.icon">{{ data.icon }}</span>
											<span class="label">{{ data.classificationCode }} {{ data.label }}</span>
											<span class="do">
												<el-button-group>
													<el-tooltip
														:content="t('classTemplate.addChildBtn')"
														placement="top"
													>
														<el-button
															icon="CirclePlus"
															size="small"
															@click.stop="formDialogRef.openDialog(null, state.treeRoot, data.id)"
														/>
													</el-tooltip>
													<el-tooltip
														:content="t('classTemplate.builtinEditDisabledTip')"
														:disabled="data.source !== 'builtin'"
														placement="top"
													>
														<span style="margin-left: 4px">
															<el-button
																:disabled="data.source === 'builtin'"
																icon="edit"
																size="small"
																@click.stop="formDialogRef.openDialog(data.id, state.treeRoot)"
															/>
														</span>
													</el-tooltip>
													<el-tooltip
														:content="t('classTemplate.builtinDeleteDisabledTip')"
														:disabled="data.source !== 'builtin'"
														placement="top"
													>
														<span style="margin-left: 4px">
															<el-button
																:disabled="data.source === 'builtin'"
																icon="delete"
																size="small"
																@click.stop="handleDelete(data.id)"
															/>
														</span>
													</el-tooltip>
												</el-button-group>
											</span>
										</span>
									</template>
								</query-tree>
							</el-scrollbar>
						</div>
					</pane>
					<pane>
						<div class="layout-padding-auto layout-padding-view detail-pane">
							<el-empty v-if="!state.selectedId" :description="t('classTemplate.emptySelectTip')" />
							<template v-else>
								<div class="detail-header mb12">
									<span class="detail-title">{{ state.detail?.label }} ({{ state.detail?.classificationCode }})</span>
									<div class="detail-actions">
										<el-button
											v-if="state.detail?.deprecated === '0'"
											v-auth="'ont_class_tpl_manage'"
											:type="'warning'"
											:text="true"
											@click="handleDeprecate(state.detail, '1')"
										>
											{{ t('classTemplate.deprecatedLabel') }}
										</el-button>
										<el-button
											v-else
											v-auth="'ont_class_tpl_manage'"
											:type="'success'"
											:text="true"
											@click="handleDeprecate(state.detail, '0')"
										>
											{{ t('common.restoreBtn') }}
										</el-button>
									</div>
								</div>
								<!-- 编码信息 -->
								<el-descriptions :column="2" border class="mb12">
									<el-descriptions-item :label="t('classTemplate.templateCode')">
										{{ state.detail?.templateCode }}
									</el-descriptions-item>
									<el-descriptions-item :label="t('classTemplate.classificationCode')">
										{{ state.detail?.classificationCode }}
									</el-descriptions-item>
									<el-descriptions-item :label="t('classTemplate.parentId')">
										<template v-if="state.detail?.parentTemplateCode">
											{{ state.detail?.parentClassificationCode }} {{ state.detail?.parentTemplateCode }}
										</template>
										<span v-else class="muted">{{ t('classTemplate.noParent') }}</span>
									</el-descriptions-item>
									<el-descriptions-item :label="t('classTemplate.source')">
										<el-tag :type="state.detail?.source === 'builtin' ? 'warning' : 'info'" size="small">
											{{ state.detail?.source === 'builtin' ? t('classTemplate.builtin') : t('classTemplate.custom') }}
										</el-tag>
									</el-descriptions-item>
									<el-descriptions-item :label="t('classTemplate.deprecated')" :span="2">
										<el-tag :type="state.detail?.deprecated === '1' ? 'danger' : 'success'" size="small">
											{{ state.detail?.deprecated === '1' ? t('classTemplate.deprecatedLabel') : t('classTemplate.normal') }}
										</el-tag>
									</el-descriptions-item>
									<el-descriptions-item :label="t('classTemplate.description')" :span="2">
										{{ state.detail?.description || '-' }}
									</el-descriptions-item>
								</el-descriptions>
								<!-- 外观 -->
								<div class="section-title mb8">{{ t('classTemplate.appearanceTitle') }}</div>
								<div class="appearance-card mb12">
									<span class="appearance-icon">{{ state.detail?.icon || '∅' }}</span>
									<span class="appearance-color" :style="{ background: state.detail?.color || '#d9d9d9' }"></span>
									<span class="muted ml8">{{ state.detail?.icon }} / {{ state.detail?.color }}</span>
								</div>
								<!-- 结构骨架（继承视图） -->
								<div class="section-title mb8">{{ t('classTemplate.structureTitle') }}</div>
								<inherited-panel :loading="state.inheritedLoading" :view="state.inheritedView" />
							</template>
						</div>
					</pane>
				</splitpanes>
			</pane>
		</splitpanes>
		<form-dialog @refresh="handleRefreshTree" ref="formDialogRef" />
		<rule-drawer ref="ruleDrawerRef" />
	</div>
</template>

<script lang="ts" name="ontologyClassTemplate" setup>
import { tree as fetchTree, getObj, inherited, delObj, deprecateObj } from '/@/api/ontology/class-template';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const RuleDrawer = defineAsyncComponent(() => import('./rule-drawer.vue'));
const InheritedPanel = defineAsyncComponent(() => import('./inherited-panel.vue'));
const QueryTree = defineAsyncComponent(() => import('/@/components/QueryTree/index.vue'));

const { t } = useI18n();

const formDialogRef = ref();
const ruleDrawerRef = ref();
const classTreeRef = ref();

const state = reactive({
	treeRoot: 'equipment',
	selectedId: '' as string,
	detail: null as any,
	inheritedView: null as any,
	inheritedLoading: false,
	/**
	 * QueryTree 的 query：返回 Promise，参数是搜索关键词。包装成返回 { data } 的请求。
	 */
	queryList: (_name?: string) => fetchTree(state.treeRoot),
});

/**
 * 切换分类树时刷新左侧树。
 */
const handleTreeRootChange = async () => {
	state.selectedId = '';
	state.detail = null;
	state.inheritedView = null;
	await classTreeRef.value?.getdeptTree();
};

/**
 * 点击树节点，加载右侧详情 + 继承视图。
	 * @param data 节点数据
	 */
const handleNodeClick = async (data: any) => {
	state.selectedId = data.id;
	await loadDetail(data.id);
	await loadInherited(data.id);
};

const loadDetail = async (id: string) => {
	try {
		const { data } = await getObj(id);
		state.detail = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const loadInherited = async (id: string) => {
	state.inheritedLoading = true;
	try {
		const { data } = await inherited(id);
		state.inheritedView = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		state.inheritedLoading = false;
	}
};

/**
 * 新增/编辑/删除后刷新左侧树，并保持当前选中。
 */
const handleRefreshTree = async () => {
	await classTreeRef.value?.getdeptTree();
	if (state.selectedId) {
		await loadDetail(state.selectedId);
		await loadInherited(state.selectedId);
	}
};

const handleDelete = async (id: string) => {
	try {
		await useMessageBox().confirm(t('classTemplate.deleteTip'));
	} catch {
		return;
	}
	try {
		await delObj(id);
		useMessage().success(t('common.delSuccessText'));
		state.selectedId = '';
		state.detail = null;
		state.inheritedView = null;
		await classTreeRef.value?.getdeptTree();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const handleDeprecate = async (row: any, deprecated: string) => {
	const tip = deprecated === '1' ? t('classTemplate.deprecateTip') : t('classTemplate.restoreTip');
	try {
		await useMessageBox().confirm(tip);
	} catch {
		return;
	}
	try {
		await deprecateObj(row.id, deprecated);
		useMessage().success(t('common.optSuccessText'));
		await loadDetail(row.id);
		await classTreeRef.value?.getdeptTree();
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>

<style scoped>
.detail-pane {
	padding: 16px;
}
.detail-header {
	display: flex;
	align-items: center;
	justify-content: space-between;
}
.detail-title {
	font-size: 16px;
	font-weight: 600;
}
.section-title {
	font-size: 13px;
	font-weight: 600;
	color: #606266;
	border-left: 3px solid var(--el-color-primary);
	padding-left: 8px;
}
.appearance-card {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 8px 12px;
	background: #fafafa;
	border-radius: 4px;
}
.appearance-icon {
	font-size: 22px;
}
.appearance-color {
	display: inline-block;
	width: 22px;
	height: 22px;
	border-radius: 4px;
	border: 1px solid #e8e8e8;
}
.muted {
	color: #999;
}
.menu:deep(.el-tree-node__label) {
	display: flex;
	flex: 1;
	height: 100%;
}
.custom-tree-node {
	display: flex;
	flex: 1;
	align-items: center;
	justify-content: space-between;
	font-size: 14px;
	padding-right: 8px;
	height: 100%;
}
.custom-tree-node .node-icon {
	margin-right: 4px;
}
.custom-tree-node .do {
	display: none;
}
.custom-tree-node:hover .do {
	display: inline-block;
}
.ml8 {
	margin-left: 8px;
}
.mb8 {
	margin-bottom: 8px;
}
.mb12 {
	margin-bottom: 12px;
}
</style>
