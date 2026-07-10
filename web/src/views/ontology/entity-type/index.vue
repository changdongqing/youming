<template>
	<div ref="pageRef" class="layout-padding ontology-entity-type-page">
		<splitpanes @resized="handleResized">
			<pane :size="leftPaneSize" :min="15" :max="50">
				<div v-loading="treeLoading" class="layout-padding-auto layout-padding-view tree-panel">
					<el-row class="mb8" justify="space-between">
						<el-button icon="folder-add" type="primary" v-auth="'ontology_entity_type_add'" @click="openDialog()">新增类型</el-button>
						<el-button icon="refresh" :loading="refreshing" @click="refreshAll">刷新</el-button>
					</el-row>
					<el-input v-model="treeKeyword" clearable placeholder="搜索名称/标签" prefix-icon="Search" class="mb8" />
					<el-scrollbar class="tree-scrollbar">
						<el-tree
							:key="treeKeyword.trim() || '__normal__'"
							ref="treeRef"
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
									<el-tag v-if="data.isBuiltin === '1'" size="small" class="ml6">核心</el-tag>
									<el-tag v-if="data.isAbstract === '1'" size="small" type="warning" class="ml6">抽象</el-tag>
								</span>
							</template>
						</el-tree>
						<el-empty v-if="!treeLoading && filteredTree.length === 0" description="暂无匹配的实体类型" :image-size="80" />
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div v-loading="detailLoading" class="layout-padding-auto layout-padding-view detail-panel">
					<div v-if="!selectedDetail" class="empty-tip">
						<el-empty description="请选择左侧实体类型查看详情" />
					</div>
					<template v-else>
						<el-row class="mb8" justify="space-between" align="middle">
							<div class="detail-title">
								{{ selectedLabel }}
								<el-tag v-if="selectedDetail.entityType.isBuiltin === '1'" size="small">核心</el-tag>
								<el-tag v-if="selectedDetail.entityType.isAbstract === '1'" size="small" type="warning">抽象类</el-tag>
							</div>
							<el-button-group>
								<el-button icon="edit-pen" v-auth="'ontology_entity_type_edit'" @click="openDialog(selectedDetail.entityType)">编辑</el-button>
								<el-tooltip :content="selectedDetail.entityType.isBuiltin === '1' ? '核心实体类型不可删除' : '删除实体类型'" placement="top">
									<span>
										<el-button
											:disabled="selectedDetail.entityType.isBuiltin === '1'"
											icon="delete"
											v-auth="'ontology_entity_type_del'"
											@click="handleDelete(selectedDetail.entityType)"
										>
											删除
										</el-button>
									</span>
								</el-tooltip>
							</el-button-group>
						</el-row>

						<el-descriptions :column="1" border>
							<el-descriptions-item label="IRI">
								<span>{{ selectedDetail.entityType.iri }}</span>
								<el-button link type="primary" class="ml6" @click="copyIri(selectedDetail.entityType.iri)">复制</el-button>
							</el-descriptions-item>
							<el-descriptions-item label="英文名称">{{ selectedDetail.entityType.name }}</el-descriptions-item>
							<el-descriptions-item label="标签">{{ selectedLabel }}</el-descriptions-item>
							<el-descriptions-item label="定义">{{ selectedDetail.entityType.definition || '—' }}</el-descriptions-item>
							<el-descriptions-item label="属性集">
								<span class="text-muted">数据属性模块上线后填充</span>
							</el-descriptions-item>
							<el-descriptions-item label="父类">
								<span v-if="selectedDetail.parents.length">
									<el-tag v-for="parent in selectedDetail.parents" :key="parent.id" class="mr6 relation-tag" @click="handleEntityLink(parent)">
										{{ parent.name }}
									</el-tag>
								</span>
								<span v-else class="text-muted">—（根类型）</span>
							</el-descriptions-item>
							<el-descriptions-item label="子类">
								<span v-if="selectedDetail.children.length">
									<el-tag
										v-for="child in selectedDetail.children"
										:key="child.id"
										type="info"
										class="mr6 relation-tag"
										@click="handleEntityLink(child)"
									>
										{{ child.name }}
									</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="等价类">
								<span v-if="selectedDetail.equivalents.length">
									<el-tag v-for="equivalent in selectedDetail.equivalents" :key="equivalent.id" type="success" class="mr6">
										{{ equivalent.name }}
									</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="不相交类">
								<span v-if="selectedDetail.disjoints.length">
									<el-tag v-for="disjoint in selectedDetail.disjoints" :key="disjoint.id" type="danger" class="mr6">
										{{ disjoint.name }}
									</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="命名空间">
								{{ selectedDetail.namespace ? `${selectedDetail.namespace.prefix}（${selectedDetail.namespace.uri}）` : '—' }}
							</el-descriptions-item>
							<el-descriptions-item label="排序">{{ selectedDetail.entityType.sortOrder }}</el-descriptions-item>
						</el-descriptions>
					</template>
				</div>
			</pane>
		</splitpanes>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="640px" destroy-on-close>
			<el-alert v-if="!isBuiltinEdit && extensionNamespaces.length === 0" title="请先创建扩展命名空间，再新增实体类型" type="warning" :closable="false" class="mb8" />
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择扩展命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaceOptions" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="英文名称" prop="name">
					<el-input v-model="form.name" :disabled="isBuiltinEdit" placeholder="如 Vehicle" />
				</el-form-item>
				<el-form-item label="IRI" prop="iri">
					<el-input v-model="form.iri" readonly placeholder="由后端根据命名空间和英文名称生成" />
				</el-form-item>
				<el-form-item label="中文标签" prop="label">
					<el-input v-model="form.label" placeholder="如 车辆" maxlength="128" show-word-limit />
				</el-form-item>
				<el-form-item label="定义" prop="definition">
					<el-input v-model="form.definition" type="textarea" maxlength="512" show-word-limit />
				</el-form-item>
				<el-form-item label="是否抽象类" prop="isAbstract">
					<el-switch v-model="form.isAbstract" :disabled="isBuiltinEdit" active-text="是" active-value="1" inactive-text="否" inactive-value="0" />
				</el-form-item>
				<el-form-item label="父类" prop="parentIds">
					<el-select v-model="form.parentIds" :disabled="isBuiltinEdit" multiple filterable placeholder="可多选，支持多继承" style="width: 100%">
						<el-option v-for="item in parentOptions" :key="item.id" :label="item.name + '（' + item.iri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="排序" prop="sortOrder">
					<el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="备注" prop="remarks">
					<el-input v-model="form.remarks" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="dialog.loading" :disabled="!isBuiltinEdit && extensionNamespaces.length === 0" @click="submit">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyEntityType" setup>
import { addEntityTypeObj, delEntityTypeObj, fetchEntityTypeById, fetchEntityTypeList, fetchEntityTypeTree, putEntityTypeObj } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';
import type {
	EntityType,
	EntityTypeCreateRequest,
	EntityTypeDetail,
	EntityTypeForm,
	EntityTypeTreeNode,
	EntityTypeUpdateRequest,
	NamespaceOption,
	OntologyId,
} from '/@/types/ontology/entity-type';

const pageRef = ref<HTMLElement>();
const treeRef = ref();
const formRef = ref();
const LEFT_DEFAULT_PX = 360;
const leftPaneSize = ref(30);
const userResized = ref(false);
const treeLoading = ref(false);
const detailLoading = ref(false);
const refreshing = ref(false);
let detailRequestSequence = 0;
let resizeObserver: ResizeObserver | undefined;

const handleResized = () => {
	userResized.value = true;
};

const recalcLeftPane = (width: number) => {
	if (userResized.value || !width) return;
	leftPaneSize.value = Math.min(50, Math.max(15, (LEFT_DEFAULT_PX / width) * 100));
};

const treeData = ref<EntityTypeTreeNode[]>([]);
const allTypes = ref<EntityType[]>([]);
const extensionNamespaces = ref<NamespaceOption[]>([]);
const treeKeyword = ref('');
const selectedDetail = ref<EntityTypeDetail>();
const selectedId = ref<OntologyId>();

const treeProps = { label: 'label', children: 'children' };

const dialog = reactive({
	visible: false,
	title: '新增实体类型',
	loading: false,
});

const createEmptyForm = (): EntityTypeForm => ({
	name: '',
	iri: '',
	label: '',
	definition: '',
	isAbstract: '0',
	parentIds: [],
	sortOrder: 0,
	remarks: '',
});

const form = reactive<EntityTypeForm>(createEmptyForm());
const isBuiltinEdit = computed(() => Boolean(form.id && form.isBuiltin === '1'));
const namespaceOptions = computed(() => (isBuiltinEdit.value && selectedDetail.value?.namespace ? [selectedDetail.value.namespace] : extensionNamespaces.value));

const rules = {
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
	name: [
		{ required: true, message: '请输入英文名称', trigger: 'blur' },
		{ pattern: /^[A-Z][a-zA-Z0-9]*$/, message: '必须以大写字母开头，仅支持英文字母和数字', trigger: 'blur' },
	],
	label: [{ required: true, message: '请输入中文标签', trigger: 'blur' }],
};

const selectedLabel = computed(() => {
	if (!selectedDetail.value) return '';
	const zhLabel = selectedDetail.value.labels.find((label) => label.locale === 'zh');
	return zhLabel?.label || selectedDetail.value.labels[0]?.label || selectedDetail.value.entityType.name;
});

const filteredTree = computed<EntityTypeTreeNode[]>(() => {
	const keyword = treeKeyword.value.trim().toLowerCase();
	if (!keyword) return treeData.value;
	const filterNodes = (nodes: EntityTypeTreeNode[]): EntityTypeTreeNode[] =>
		nodes.flatMap((node) => {
			const children = filterNodes(node.children || []);
			const matched = node.label.toLowerCase().includes(keyword) || node.name.toLowerCase().includes(keyword);
			return matched || children.length > 0 ? [{ ...node, children }] : [];
		});
	return filterNodes(treeData.value);
});

const collectDescendantIds = (node: EntityTypeTreeNode, result: Set<OntologyId>) => {
	for (const child of node.children || []) {
		result.add(child.id);
		collectDescendantIds(child, result);
	}
};

const invalidParentIds = computed(() => {
	const invalid = new Set<OntologyId>();
	if (!form.id) return invalid;
	invalid.add(form.id);
	const visit = (nodes: EntityTypeTreeNode[]) => {
		for (const node of nodes) {
			if (node.id === form.id) collectDescendantIds(node, invalid);
			visit(node.children || []);
		}
	};
	visit(treeData.value);
	return invalid;
});

const parentOptions = computed(() => allTypes.value.filter((type) => !invalidParentIds.value.has(type.id)));

const getErrorMessage = (error: unknown, fallback: string) => {
	if (error && typeof error === 'object' && 'msg' in error) return String((error as { msg?: unknown }).msg || fallback);
	return fallback;
};

const loadTree = async () => {
	const response = await fetchEntityTypeTree();
	treeData.value = (response.data || []) as EntityTypeTreeNode[];
};

const loadAllTypes = async () => {
	const response = await fetchEntityTypeList();
	allTypes.value = (response.data || []) as EntityType[];
};

const loadNamespaces = async () => {
	const response = await fetchNamespaceList({ isBuiltin: '0' });
	extensionNamespaces.value = (response.data || []) as NamespaceOption[];
};

const refreshAll = async () => {
	refreshing.value = true;
	try {
		await Promise.all([loadTree(), loadAllTypes(), loadNamespaces()]);
		if (selectedId.value) await loadDetail(selectedId.value);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '刷新失败'));
	} finally {
		refreshing.value = false;
	}
};

const loadDetail = async (id: OntologyId) => {
	const requestSequence = ++detailRequestSequence;
	detailLoading.value = true;
	try {
		const response = await fetchEntityTypeById(id);
		if (requestSequence === detailRequestSequence) selectedDetail.value = response.data as EntityTypeDetail;
	} finally {
		if (requestSequence === detailRequestSequence) detailLoading.value = false;
	}
};

const handleNodeClick = async (data: EntityTypeTreeNode) => {
	selectedId.value = data.id;
	try {
		await loadDetail(data.id);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '详情加载失败'));
	}
};

const handleEntityLink = async (entityType: EntityType) => {
	selectedId.value = entityType.id;
	try {
		await loadDetail(entityType.id);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '详情加载失败'));
	}
};

const resetForm = (row?: EntityType) => {
	Object.assign(form, createEmptyForm());
	delete form.id;
	delete form.namespaceId;
	delete form.isBuiltin;
	if (row && selectedDetail.value) {
		Object.assign(form, {
			id: row.id,
			namespaceId: row.namespaceId,
			name: row.name,
			iri: row.iri,
			label: selectedLabel.value,
			definition: row.definition || '',
			isAbstract: row.isAbstract,
			parentIds: [...selectedDetail.value.parentIds],
			sortOrder: row.sortOrder,
			remarks: row.remarks || '',
			isBuiltin: row.isBuiltin,
		});
		return;
	}
	const defaultNamespace = extensionNamespaces.value.find((namespace) => namespace.isDefault === '1') || extensionNamespaces.value[0];
	form.namespaceId = defaultNamespace?.id;
};

const openDialog = (row?: EntityType) => {
	if (!row && extensionNamespaces.value.length === 0) {
		useMessage().warning('请先创建扩展命名空间');
		return;
	}
	resetForm(row);
	dialog.title = row ? '编辑实体类型' : '新增实体类型';
	dialog.visible = true;
};

const syncIriPreview = () => {
	if (isBuiltinEdit.value) return;
	const namespace = extensionNamespaces.value.find((item) => item.id === form.namespaceId);
	form.iri = namespace && form.name ? namespace.uri + form.name : '';
};

watch(() => [form.namespaceId, form.name], syncIriPreview);

const buildPayload = (): EntityTypeCreateRequest => ({
	namespaceId: form.namespaceId as OntologyId,
	name: form.name,
	iri: form.iri || undefined,
	label: form.label,
	definition: form.definition || undefined,
	isAbstract: form.isAbstract,
	parentIds: [...form.parentIds],
	sortOrder: form.sortOrder,
	remarks: form.remarks || undefined,
});

const submit = async () => {
	await formRef.value?.validate();
	dialog.loading = true;
	try {
		const response = form.id
			? await putEntityTypeObj({ ...buildPayload(), id: form.id } as EntityTypeUpdateRequest)
			: await addEntityTypeObj(buildPayload());
		const saved = response.data as EntityType;
		useMessage().success('保存成功');
		dialog.visible = false;
		selectedId.value = saved.id;
		await Promise.all([loadTree(), loadAllTypes()]);
		await loadDetail(saved.id);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '保存失败'));
	} finally {
		dialog.loading = false;
	}
};

const handleDelete = async (row: EntityType) => {
	try {
		await useMessageBox().confirm(`确认删除实体类型“${row.name}”吗？`);
	} catch {
		return;
	}
	try {
		await delEntityTypeObj(row.id);
		useMessage().success('删除成功');
		selectedDetail.value = undefined;
		selectedId.value = undefined;
		await Promise.all([loadTree(), loadAllTypes()]);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '删除失败'));
	}
};

const copyIri = async (iri: string) => {
	await navigator.clipboard.writeText(iri);
	useMessage().success('IRI已复制');
};

onMounted(async () => {
	treeLoading.value = true;
	try {
		await Promise.all([loadTree(), loadAllTypes(), loadNamespaces()]);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '实体类型数据加载失败'));
	} finally {
		treeLoading.value = false;
	}
	if (pageRef.value) {
		recalcLeftPane(pageRef.value.clientWidth);
		resizeObserver = new ResizeObserver((entries) => recalcLeftPane(entries[0].contentRect.width));
		resizeObserver.observe(pageRef.value);
	}
});

onUnmounted(() => resizeObserver?.disconnect());
</script>

<style scoped>
.ontology-entity-type-page {
	height: 100%;
}
.tree-panel,
.detail-panel {
	height: 100%;
}
.tree-panel {
	display: flex;
	flex-direction: column;
}
.tree-scrollbar {
	flex: 1;
	min-height: 0;
}
.tree-node {
	display: flex;
	align-items: center;
	gap: 4px;
	font-size: 14px;
}
.tree-node-label {
	white-space: nowrap;
}
.detail-title {
	font-size: 16px;
	font-weight: 600;
	display: flex;
	align-items: center;
	gap: 8px;
}
.empty-tip {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 100%;
}
.relation-tag {
	cursor: pointer;
}
.text-muted {
	color: #999;
}
.mr6 {
	margin-right: 6px;
}
.ml6 {
	margin-left: 6px;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
