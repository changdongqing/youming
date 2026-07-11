<template>
	<div ref="pageRef" class="layout-padding ontology-instance-page">
		<splitpanes @resized="handleResized">
			<pane :size="leftPaneSize" :min="15" :max="50">
				<div v-loading="treeLoading" class="layout-padding-auto layout-padding-view tree-panel">
					<el-row class="mb8" justify="space-between">
						<el-button :type="selectedTypeId ? 'info' : 'primary'" @click="showAllInstances">全部实例</el-button>
						<el-button icon="refresh" :loading="refreshing" @click="refreshAll">刷新</el-button>
					</el-row>
					<el-input v-model="treeKeyword" clearable placeholder="搜索实体类型" prefix-icon="Search" class="mb8" />
					<el-scrollbar class="tree-scrollbar">
						<el-tree
							:key="treeKeyword.trim() || '__normal__'"
							:data="filteredTree"
							node-key="key"
							:props="treeProps"
							:expand-on-click-node="false"
							:highlight-current="true"
							:default-expand-all="Boolean(treeKeyword.trim())"
							@node-click="handleNodeClick"
						>
							<template #default="{ data }">
								<span class="tree-node">
									<span class="tree-node-label">{{ data.label }}（{{ data.name }}）</span>
									<el-tag v-if="data.isAbstract === '1'" size="small" type="info" class="ml6">抽象</el-tag>
								</span>
							</template>
						</el-tree>
						<el-empty v-if="!treeLoading && filteredTree.length === 0" description="暂无匹配的实体类型" :image-size="80" />
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div v-loading="tableLoading" class="layout-padding-auto layout-padding-view table-panel">
					<div class="mb8">
						<el-text v-if="selectedTypeId" type="info">
							当前类型：{{ selectedTypeLabel }}（{{ selectedTypeName }}）
							<el-switch v-model="query.includeSubtypes" active-text="含子类" @change="loadList" />
						</el-text>
						<el-text v-else type="info">全部实例</el-text>
					</div>
					<el-row class="mb8" :gutter="8">
						<el-col :span="5">
							<el-input v-model="query.keyword" clearable placeholder="IRI本地名或标签" @keyup.enter="loadList" />
						</el-col>
						<el-col :span="4">
							<el-select v-model="query.isBuiltin" clearable placeholder="内置/扩展">
								<el-option label="内置" value="1" />
								<el-option label="扩展" value="0" />
							</el-select>
						</el-col>
						<el-col :span="3">
							<el-button icon="search" type="primary" @click="loadList">查询</el-button>
						</el-col>
						<el-col :span="12" style="text-align: right">
							<el-button
								v-if="selectedTypeId && selectedIsAbstract"
								icon="folder-add"
								type="primary"
								disabled
							>
								<el-tooltip content="抽象类型不可实例化" placement="top">新增实例</el-tooltip>
							</el-button>
							<el-button
								v-else
								icon="folder-add"
								type="primary"
								v-auth="'ontology_instance_add'"
								@click="openCreateDialog"
							>新增实例</el-button>
						</el-col>
					</el-row>

					<el-table :data="pagedData" border style="width: 100%">
						<el-table-column type="index" label="#" width="50" />
						<el-table-column prop="label" label="标签" min-width="120" show-overflow-tooltip>
							<template #default="{ row }">
								{{ row.label || row.iriLocalName }}
								<el-tag v-if="row.isBuiltin === '1'" size="small" class="ml6">内置</el-tag>
							</template>
						</el-table-column>
						<el-table-column prop="iriLocalName" label="IRI本地名" min-width="140" show-overflow-tooltip />
						<el-table-column label="类型" min-width="100">
							<template #default="{ row }">
								{{ row.rdfTypeLabel || row.rdfTypeName || row.rdfTypeId }}
							</template>
						</el-table-column>
						<el-table-column label="命名空间" min-width="80">
							<template #default="{ row }">{{ row.namespacePrefix }}</template>
						</el-table-column>
						<el-table-column label="数据值" width="70" align="center">
							<template #default="{ row }">{{ row.dataValueCount }}</template>
						</el-table-column>
						<el-table-column label="出向断言" width="80" align="center">
							<template #default="{ row }">{{ row.outgoingRelationCount }}</template>
						</el-table-column>
						<el-table-column label="入向引用" width="80" align="center">
							<template #default="{ row }">{{ row.incomingRelationCount }}</template>
						</el-table-column>
						<el-table-column label="操作" width="120" fixed="right">
							<template #default="{ row }">
								<el-button link type="primary" @click="openDetail(row)">详情</el-button>
								<el-button link type="primary" v-auth="'ontology_instance_edit'" @click="openEditDialog(row)">编辑</el-button>
								<el-tooltip :content="row.isBuiltin === '1' ? '内置实例不可删除' : '删除实例'" placement="top">
									<span>
										<el-button
											link
											type="danger"
											:disabled="row.isBuiltin === '1'"
											v-auth="'ontology_instance_del'"
											@click="handleDelete(row)"
										>删除</el-button>
									</span>
								</el-tooltip>
							</template>
						</el-table-column>
					</el-table>
					<pagination :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @pagination="loadList" />
				</div>
			</pane>
		</splitpanes>

		<!-- 详情抽屉 -->
		<el-drawer v-model="detailVisible" title="实例详情" size="60%" destroy-on-close>
			<template v-if="detailData">
				<el-descriptions :column="2" border>
					<el-descriptions-item label="IRI">{{ detailData.iri }}</el-descriptions-item>
					<el-descriptions-item label="本地名">{{ detailData.iriLocalName }}</el-descriptions-item>
					<el-descriptions-item label="标签">{{ detailData.label }}</el-descriptions-item>
					<el-descriptions-item label="类型">{{ detailData.rdfTypeLabel || detailData.rdfTypeName }}</el-descriptions-item>
					<el-descriptions-item label="命名空间">{{ detailData.namespacePrefix }}</el-descriptions-item>
					<el-descriptions-item label="来源">{{ detailData.sourceType }}</el-descriptions-item>
					<el-descriptions-item label="声明模式">{{ detailData.declarationMode }}</el-descriptions-item>
					<el-descriptions-item label="排序">{{ detailData.sortOrder }}</el-descriptions-item>
					<el-descriptions-item v-if="detailData.remarks" label="备注">{{ detailData.remarks }}</el-descriptions-item>
				</el-descriptions>

				<el-divider content-position="left">数据属性值</el-divider>
				<el-table :data="detailData.dataValues" border style="width: 100%">
					<el-table-column prop="dataPropertyLabel" label="属性" min-width="100" show-overflow-tooltip>
						<template #default="{ row }">{{ row.dataPropertyLabel || row.dataPropertyName }}</template>
					</el-table-column>
					<el-table-column prop="literalValue" label="值" min-width="200" show-overflow-tooltip />
					<el-table-column prop="literalType" label="类型" width="90" />
					<el-table-column label="单位" width="80">
						<template #default="{ row }">{{ row.unitSymbol || '-' }}</template>
					</el-table-column>
					<el-table-column prop="sortOrder" label="序" width="50" />
				</el-table>

				<el-divider content-position="left">出向断言</el-divider>
				<el-table :data="detailData.outgoingRelations" border style="width: 100%">
					<el-table-column label="谓词" min-width="100">
						<template #default="{ row }">{{ row.objectPropertyLabel || row.objectPropertyName }}</template>
					</el-table-column>
					<el-table-column label="客体类型" width="100">
						<template #default="{ row }">
							<el-tag v-if="row.objectKind === 'ENTITY_TYPE'" size="small" type="warning">Schema资源</el-tag>
							<el-tag v-else size="small">实例</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="客体" min-width="200" show-overflow-tooltip>
						<template #default="{ row }">{{ row.objectLabel || row.objectIri }}</template>
					</el-table-column>
					<el-table-column prop="sortOrder" label="序" width="50" />
				</el-table>

				<el-divider content-position="left">入向引用</el-divider>
				<el-table :data="detailData.inboundRelations" border style="width: 100%">
					<el-table-column label="主体" min-width="200" show-overflow-tooltip>
						<template #default="{ row }">{{ row.subjectLabel || row.subjectIri }}</template>
					</el-table-column>
					<el-table-column label="谓词" min-width="100">
						<template #default="{ row }">{{ row.objectPropertyLabel || row.objectPropertyName }}</template>
					</el-table-column>
				</el-table>
			</template>
		</el-drawer>

		<!-- 新增/编辑对话框 -->
		<el-dialog v-model="dialog.visible" :title="dialog.title" width="780px" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-divider content-position="left">基本信息</el-divider>
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaceOptions" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="实体类型" prop="rdfTypeId">
					<el-select v-model="form.rdfTypeId" :disabled="isBuiltinEdit || !!form.id" filterable placeholder="选择实体类型" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="IRI本地名" prop="iriLocalName">
					<el-input v-model="form.iriLocalName" :disabled="isBuiltinEdit" :placeholder="form.id ? '' : '留空则自动生成'" />
				</el-form-item>
				<el-form-item label="IRI预览" prop="iri">
					<el-input :model-value="iriPreview" readonly placeholder="由后端根据命名空间和IRI本地名生成" />
				</el-form-item>
				<el-form-item label="标签" prop="label">
					<el-input v-model="form.label" placeholder="UI显示标签" maxlength="255" show-word-limit />
				</el-form-item>
				<el-divider content-position="left">治理</el-divider>
				<el-form-item label="排序" prop="sortOrder">
					<el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="备注" prop="remarks">
					<el-input v-model="form.remarks" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="dialog.loading" @click="submit">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyInstance" setup>
import { addInstanceObj, delInstanceObj, fetchInstanceById, fetchInstancePage, putInstanceObj } from '/@/api/ontology/instance';
import { fetchEntityTypeList, fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { filterEntityTypeTree } from '/@/views/ontology/entity-type/tree-utils';
import type { InstanceDetail, InstanceForm, InstanceQuery, InstanceSummary, InstanceCreateRequest, InstanceUpdateRequest, OntologyId } from '/@/types/ontology/instance';
import type { EntityTypeTreeNode } from '/@/types/ontology/entity-type';

const pageRef = ref<HTMLElement>();
const formRef = ref();
const LEFT_DEFAULT_PX = 360;
const leftPaneSize = ref(30);
const userResized = ref(false);
let resizeObserver: ResizeObserver | undefined;

const treeLoading = ref(false);
const tableLoading = ref(false);
const refreshing = ref(false);
const treeData = ref<EntityTypeTreeNode[]>([]);
const treeKeyword = ref('');
const treeProps = { label: 'label', children: 'children' };

const selectedTypeId = ref<OntologyId | null>(null);
const selectedTypeName = ref('');
const selectedTypeLabel = ref('');
const selectedIsAbstract = ref(false);

const pagedData = ref<InstanceSummary[]>([]);
const pagination = reactive({ current: 1, size: 10, total: 0 });
const query = reactive<InstanceQuery>({ keyword: '', includeSubtypes: false });

const namespaceOptions = ref<{ id: OntologyId; prefix: string; uri: string }[]>([]);
const entityTypeOptions = ref<{ id: OntologyId; name: string }[]>([]);

const detailVisible = ref(false);
const detailData = ref<InstanceDetail | null>(null);

const dialog = reactive({ visible: false, title: '', loading: false });
const createEmptyForm = (): InstanceForm => ({
	iriLocalName: '',
	rdfTypeId: undefined,
	namespaceId: undefined,
	label: '',
	sortOrder: 0,
	remarks: '',
	dataValues: [],
	objectRelations: [],
});
const form = reactive<InstanceForm>(createEmptyForm());

const isBuiltinEdit = computed(() => Boolean(form.id && form.isBuiltin === '1'));

const iriPreview = computed(() => {
	const ns = namespaceOptions.value.find((n: { id: OntologyId; prefix: string; uri: string }) => n.id === form.namespaceId);
	if (!ns) return '';
	const localName = form.iriLocalName || '';
	if (!localName) return '';
	return ns.uri + localName;
});

const filteredTree = computed(() => filterEntityTypeTree(treeData.value, treeKeyword.value));

const getErrorMessage = (error: unknown, fallback: string) => {
	if (error && typeof error === 'object' && 'msg' in error) return String((error as { msg?: unknown }).msg || fallback);
	return fallback;
};

const handleResized = () => { userResized.value = true; };
const recalcLeftPane = (width: number) => {
	if (userResized.value || !width) return;
	leftPaneSize.value = Math.min(50, Math.max(15, (LEFT_DEFAULT_PX / width) * 100));
};

const showAllInstances = () => {
	selectedTypeId.value = null;
	selectedTypeName.value = '';
	selectedTypeLabel.value = '';
	selectedIsAbstract.value = false;
	query.rdfTypeId = undefined;
	pagination.current = 1;
	loadList();
};

const handleNodeClick = (data: EntityTypeTreeNode) => {
	if (data.isAbstract === '1') {
		selectedIsAbstract.value = true;
	} else {
		selectedIsAbstract.value = false;
	}
	selectedTypeId.value = data.id;
	selectedTypeName.value = data.name;
	selectedTypeLabel.value = data.label;
	query.rdfTypeId = data.id;
	pagination.current = 1;
	loadList();
};

const loadTree = async () => {
	treeLoading.value = true;
	try {
		const response = await fetchEntityTypeTree();
		treeData.value = (response.data || []) as EntityTypeTreeNode[];
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载实体类型树失败'));
	} finally {
		treeLoading.value = false;
	}
};

const loadList = async () => {
	tableLoading.value = true;
	try {
		const params: InstanceQuery = {
			current: pagination.current,
			size: pagination.size,
			keyword: query.keyword || undefined,
			rdfTypeId: query.rdfTypeId || undefined,
			includeSubtypes: query.includeSubtypes || undefined,
			isBuiltin: (query.isBuiltin as '0' | '1') || undefined,
		};
		const response = await fetchInstancePage(params);
		const pageData = response.data || {};
		pagedData.value = (pageData.records || []) as InstanceSummary[];
		pagination.total = pageData.total || 0;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载实例列表失败'));
	} finally {
		tableLoading.value = false;
	}
};

const loadNamespaceOptions = async () => {
	try {
		const response = await fetchNamespaceList({});
		const list = (response.data || []) as { id: OntologyId; prefix: string; uri: string; isBuiltin: string }[];
		namespaceOptions.value = list.map((n) => ({ id: n.id, prefix: n.prefix, uri: n.uri }));
	} catch {
		namespaceOptions.value = [];
	}
};

const loadEntityTypeOptions = async () => {
	try {
		const response = await fetchEntityTypeList({});
		entityTypeOptions.value = ((response.data || []) as { id: OntologyId; name: string }[]).map((e) => ({ id: e.id, name: e.name }));
	} catch {
		entityTypeOptions.value = [];
	}
};

const openDetail = async (row: InstanceSummary) => {
	try {
		const response = await fetchInstanceById(row.id);
		detailData.value = response.data as InstanceDetail;
		detailVisible.value = true;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载详情失败'));
	}
};

const openCreateDialog = () => {
	Object.assign(form, createEmptyForm());
	delete form.id;
	delete form.isBuiltin;
	if (selectedTypeId.value) {
		form.rdfTypeId = selectedTypeId.value;
	}
	dialog.title = '新增实例';
	dialog.visible = true;
};

const openEditDialog = async (row: InstanceSummary) => {
	try {
		const response = await fetchInstanceById(row.id);
		const detail = response.data as InstanceDetail;
		Object.assign(form, {
			id: detail.id,
			iriLocalName: detail.iriLocalName,
			rdfTypeId: detail.rdfTypeId,
			namespaceId: detail.namespaceId,
			label: detail.label || '',
			sortOrder: detail.sortOrder,
			remarks: detail.remarks || '',
			isBuiltin: detail.isBuiltin,
		});
		dialog.title = '编辑实例';
		dialog.visible = true;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载实例详情失败'));
	}
};

const rules = {
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
	rdfTypeId: [{ required: true, message: '请选择实体类型', trigger: 'change' }],
	iriLocalName: [
		{ pattern: /^[A-Za-z][A-Za-z0-9_-]{0,127}$/, message: '必须以字母开头，仅支持英文字母、数字、下划线和短横线', trigger: 'blur' },
	],
	label: [{ required: false, message: '请输入标签', trigger: 'blur' }],
};

const submit = async () => {
	await formRef.value?.validate(async (valid: boolean) => {
		if (!valid) return;
		dialog.loading = true;
		try {
			if (isBuiltinEdit.value) {
				const payload: InstanceUpdateRequest = {
					id: form.id!,
					label: form.label,
					sortOrder: form.sortOrder,
					remarks: form.remarks,
				};
				await putInstanceObj(payload);
			} else if (form.id) {
				const payload: InstanceUpdateRequest = {
					id: form.id,
					namespaceId: form.namespaceId,
					iriLocalName: form.iriLocalName || undefined,
					rdfTypeId: form.rdfTypeId,
					label: form.label,
					sortOrder: form.sortOrder,
					remarks: form.remarks,
				};
				await putInstanceObj(payload);
			} else {
				const payload: InstanceCreateRequest = {
					namespaceId: form.namespaceId!,
					iriLocalName: form.iriLocalName || undefined,
					rdfTypeId: form.rdfTypeId!,
					label: form.label || undefined,
					sortOrder: form.sortOrder,
					remarks: form.remarks || undefined,
				};
				await addInstanceObj(payload);
			}
			useMessage().success('操作成功');
			dialog.visible = false;
			await loadList();
		} catch (error: unknown) {
			useMessage().error(getErrorMessage(error, '操作失败'));
		} finally {
			dialog.loading = false;
		}
	});
};

const handleDelete = (row: InstanceSummary) => {
	if (row.isBuiltin === '1') {
		useMessage().warning('内置实例不可删除');
		return;
	}
	useMessageBox()
		.confirm('确认删除该实例？删除后数据值和出向断言将一并清除。')
		.then(async () => {
			try {
				await delInstanceObj(row.id);
				useMessage().success('删除成功');
				await loadList();
			} catch (error: unknown) {
				useMessage().error(getErrorMessage(error, '删除失败'));
			}
		})
		.catch(() => {});
};

const refreshAll = async () => {
	refreshing.value = true;
	await Promise.all([loadTree(), loadList()]);
	refreshing.value = false;
};

onMounted(async () => {
	if (pageRef.value) {
		const width = pageRef.value.offsetWidth;
		recalcLeftPane(width);
		resizeObserver = new ResizeObserver((entries) => {
			for (const entry of entries) {
				recalcLeftPane(entry.contentRect.width);
			}
		});
		resizeObserver.observe(pageRef.value);
	}
	await Promise.all([loadTree(), loadList(), loadNamespaceOptions(), loadEntityTypeOptions()]);
});

onUnmounted(() => {
	resizeObserver?.disconnect();
});
</script>

<style scoped>
.tree-panel {
	display: flex;
	flex-direction: column;
	height: 100%;
}
.tree-scrollbar {
	flex: 1;
	overflow: auto;
}
.tree-node {
	display: flex;
	align-items: center;
	width: 100%;
}
.tree-node-label {
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
</style>
