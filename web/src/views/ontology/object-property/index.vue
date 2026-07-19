<template>
	<div ref="pageRef" class="layout-padding ontology-object-property-page">
		<splitpanes @resized="handleResized">
			<pane :size="leftPaneSize" :min="15" :max="50">
				<div v-loading="treeLoading" class="layout-padding-auto layout-padding-view tree-panel">
					<el-row class="mb8" justify="space-between">
						<el-button :type="selectedDomainTypeId ? 'info' : 'primary'" @click="showAllProperties">全部属性</el-button>
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
									<el-tag v-if="data.isBuiltin === '1'" size="small" class="ml6">核心</el-tag>
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
						<el-text v-if="selectedDomainTypeId" type="info">
							当前主体类型：{{ selectedDomainLabel }}（{{ selectedDomainName }}）· 含继承属性
						</el-text>
						<el-text v-else type="info">全部对象属性</el-text>
					</div>
					<el-row class="mb8" :gutter="8">
						<el-col :span="5">
							<el-input v-model="query.name" clearable placeholder="属性名称" />
						</el-col>
						<el-col :span="4">
							<el-select v-model="query.isBuiltin" clearable placeholder="核心/扩展">
								<el-option label="核心" value="1" />
								<el-option label="扩展" value="0" />
							</el-select>
						</el-col>
						<el-col :span="4">
							<el-select v-model="query.inferenceSupport" clearable placeholder="能力契约">
								<el-option label="功能属性校验" value="FUNCTIONAL_CHECK" />
								<el-option label="不相交检测" value="DISJOINT_CHECK" />
								<el-option label="子类推理" value="SUBCLASS_INFERENCE" />
							</el-select>
						</el-col>
						<el-col :span="3">
							<el-button icon="search" type="primary" @click="loadList">查询</el-button>
						</el-col>
						<el-col :span="8" style="text-align: right">
							<el-button icon="folder-add" type="primary" v-auth="'ontology_object_property_add'" @click="openDialog()">新增属性</el-button>
						</el-col>
					</el-row>

					<el-table v-if="!selectedDomainTypeId" :data="pagedData" border :max-height="tableMaxHeight" style="width: 100%">
						<el-table-column type="index" label="#" width="50" />
						<el-table-column prop="label" label="标签" min-width="100" show-overflow-tooltip />
						<el-table-column prop="objectProperty.name" label="标准Name" min-width="120" show-overflow-tooltip />
						<el-table-column label="定义域" min-width="150">
							<template #default="{ row }">
								<el-tag v-for="d in row.domains" :key="d.id" size="small" class="mr6 mb6">{{ d.label || d.name }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="值域" min-width="150">
							<template #default="{ row }">
								<el-tag v-for="r in row.ranges" :key="r.id" size="small" type="success" class="mr6 mb6">{{ r.label || r.name }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="特性" width="120">
							<template #default="{ row }">
								<el-tag v-if="row.objectProperty.isFunctional === '1'" size="small" type="warning" class="mr6">功能</el-tag>
								<el-tag v-if="row.objectProperty.isInverseFunctional === '1'" size="small" type="warning" class="mr6">反功能</el-tag>
								<el-tag v-if="row.objectProperty.isTransitive === '1'" size="small" type="warning" class="mr6">传递</el-tag>
								<el-tag v-if="row.objectProperty.isSymmetric === '1'" size="small" type="warning" class="mr6">对称</el-tag>
								<el-tag v-if="row.objectProperty.inverseOfId" size="small" type="info" class="mr6">↔</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="来源" width="100">
							<template #default="{ row }">
								<el-tag v-if="row.objectProperty.sourceType === 'GB_TABLE1'" size="small">表1核心</el-tag>
								<el-tag v-else-if="row.objectProperty.sourceType === 'GB_TABLE1_DERIVED'" size="small" type="success">表1派生</el-tag>
								<el-tag v-else size="small" type="info">扩展</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="能力契约" width="120">
							<template #default="{ row }">
								<template v-if="row.inferenceSupport && row.inferenceSupport.length > 0">
									<el-tag v-for="cap in row.inferenceSupport" :key="cap" size="small" type="success" class="mr6 mb6">{{ inferenceCapabilityLabel(cap) }}</el-tag>
								</template>
								<el-tag v-else size="small" type="info">非推理托管</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="操作" width="120" fixed="right">
							<template #default="{ row }">
								<el-button link type="primary" v-auth="'ontology_object_property_edit'" @click="openDialog(row)">编辑</el-button>
								<el-tooltip :content="row.objectProperty.isBuiltin === '1' ? '核心对象属性不可删除' : '删除对象属性'" placement="top">
									<span>
										<el-button link type="danger" :disabled="row.objectProperty.isBuiltin === '1'" v-auth="'ontology_object_property_del'" @click="handleDelete(row)">删除</el-button>
									</span>
								</el-tooltip>
							</template>
						</el-table-column>
					</el-table>
					<pagination v-if="!selectedDomainTypeId" :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @pagination="loadList" />

					<el-table v-if="selectedDomainTypeId" :data="applicableData" border style="width: 100%">
						<el-table-column type="index" label="#" width="50" />
						<el-table-column prop="label" label="标签" min-width="100" show-overflow-tooltip />
						<el-table-column prop="objectProperty.name" label="标准Name" min-width="120" show-overflow-tooltip />
						<el-table-column label="定义域" min-width="150">
							<template #default="{ row }">
								<el-tag v-for="d in row.domains" :key="d.id" size="small" class="mr6 mb6">{{ d.label || d.name }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="值域" min-width="150">
							<template #default="{ row }">
								<el-tag v-for="r in row.ranges" :key="r.id" size="small" type="success" class="mr6 mb6">{{ r.label || r.name }}</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="直接/继承" width="100">
							<template #default="{ row }">
								<el-tag v-if="!row.inherited" size="small" type="success">直接</el-tag>
								<el-tag v-else size="small" type="warning">继承</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="来源" width="100">
							<template #default="{ row }">
								<el-tag v-if="row.objectProperty.sourceType === 'GB_TABLE1'" size="small">表1核心</el-tag>
								<el-tag v-else-if="row.objectProperty.sourceType === 'GB_TABLE1_DERIVED'" size="small" type="success">表1派生</el-tag>
								<el-tag v-else size="small" type="info">扩展</el-tag>
							</template>
						</el-table-column>
					</el-table>
				</div>
			</pane>
		</splitpanes>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="780px" destroy-on-close>
			<el-alert v-if="!isBuiltinEdit && extensionNamespaces.length === 0" title="请先创建扩展命名空间，再新增对象属性" type="warning" :closable="false" class="mb8" />
			<el-alert v-if="symmetricWarning" :title="symmetricWarning" type="info" :closable="false" class="mb8" />
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-divider content-position="left">基本信息</el-divider>
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择扩展命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaceOptions" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="英文名称" prop="name">
					<el-input v-model="form.name" :disabled="isBuiltinEdit" placeholder="如 relatedTo" />
				</el-form-item>
				<el-form-item label="IRI本地名" prop="iriLocalName">
					<el-input v-model="form.iriLocalName" :disabled="isBuiltinEdit" placeholder="缺省等于英文名称" />
				</el-form-item>
				<el-form-item label="IRI预览" prop="iri">
					<el-input :model-value="isBuiltinEdit ? form.iri : iriPreview" readonly placeholder="由后端根据命名空间和IRI本地名生成" />
				</el-form-item>
				<el-form-item label="中文标签" prop="label">
					<el-input v-model="form.label" placeholder="如 关联到" maxlength="128" show-word-limit />
				</el-form-item>
				<el-form-item label="定义" prop="definition">
					<el-input v-model="form.definition" :disabled="isBuiltinEdit" type="textarea" maxlength="512" show-word-limit />
				</el-form-item>

				<el-divider content-position="left">定义域/值域</el-divider>
				<el-form-item label="定义域" prop="domainEntityTypeIds">
					<el-select v-model="form.domainEntityTypeIds" :disabled="isBuiltinEdit" multiple filterable placeholder="选择实体类型（可多选）" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="值域" prop="rangeEntityTypeIds">
					<el-select v-model="form.rangeEntityTypeIds" :disabled="isBuiltinEdit" multiple filterable placeholder="选择实体类型（可多选）" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>

				<el-divider content-position="left">语义特性</el-divider>
				<el-form-item label="功能性" prop="isFunctional">
					<el-switch v-model="form.isFunctional" :disabled="isBuiltinEdit || form.isTransitive === '1'" active-text="是" active-value="1" inactive-text="否" inactive-value="0" @change="onFunctionalToggle" />
				</el-form-item>
				<el-form-item label="反功能性" prop="isInverseFunctional">
					<el-switch v-model="form.isInverseFunctional" :disabled="isBuiltinEdit || form.isTransitive === '1'" active-text="是" active-value="1" inactive-text="否" inactive-value="0" @change="onFunctionalToggle" />
				</el-form-item>
				<el-form-item label="传递性" prop="isTransitive">
					<el-switch v-model="form.isTransitive" :disabled="isBuiltinEdit || form.isFunctional === '1' || form.isInverseFunctional === '1'" active-text="是" active-value="1" inactive-text="否" inactive-value="0" @change="onTransitiveToggle" />
				</el-form-item>
				<el-form-item label="对称性" prop="isSymmetric">
					<el-switch v-model="form.isSymmetric" :disabled="isBuiltinEdit" active-text="是" active-value="1" inactive-text="否" inactive-value="0" />
				</el-form-item>
				<el-form-item v-if="!isBuiltinEdit" label="逆属性" prop="inverseOfId">
					<el-select v-model="form.inverseOfId" clearable filterable placeholder="选择逆属性（可选）" style="width: 100%">
						<el-option v-for="item in inverseOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>

				<el-divider content-position="left">推理能力契约</el-divider>
				<el-form-item label="能力契约">
					<el-checkbox-group v-model="form.inferenceSupport" :disabled="isBuiltinEdit">
						<el-checkbox label="FUNCTIONAL_CHECK" :disabled="form.isFunctional !== '1'">功能属性校验</el-checkbox>
						<el-checkbox label="DISJOINT_CHECK">不相交检测</el-checkbox>
						<el-checkbox label="SUBCLASS_INFERENCE">子类推理</el-checkbox>
					</el-checkbox-group>
					<el-text v-if="form.isFunctional !== '1'" type="info" size="small">勾选功能性后可选"功能属性校验"</el-text>
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
				<el-button type="primary" :loading="dialog.loading" :disabled="!isBuiltinEdit && extensionNamespaces.length === 0" @click="submit">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyObjectProperty" setup>
import { addObjectPropertyObj, delObjectPropertyObj, fetchObjectPropertyById, fetchObjectPropertyPage, fetchObjectPropertiesByDomain, putObjectPropertyObj } from '/@/api/ontology/object-property';
import { fetchEntityTypeList, fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { filterEntityTypeTree } from '/@/views/ontology/entity-type/tree-utils';
import { computeSymmetricWarning } from './semantic-rules';
import type { ApplicableObjectProperty, ObjectPropertyForm, ObjectPropertyQuery, ObjectPropertySummary, ObjectPropertyCreateRequest, ObjectPropertyUpdateRequest, OntologyId } from '/@/types/ontology/object-property';
import type { EntityTypeTreeNode } from '/@/types/ontology/entity-type';

const pageRef = ref<HTMLElement>();
const formRef = ref();
const LEFT_DEFAULT_PX = 360;
const leftPaneSize = ref(30);
const userResized = ref(false);
const treeLoading = ref(false);
const tableLoading = ref(false);
const refreshing = ref(false);
let resizeObserver: ResizeObserver | undefined;

const handleResized = () => {
	userResized.value = true;
};

const recalcLeftPane = (width: number) => {
	if (userResized.value || !width) return;
	leftPaneSize.value = Math.min(50, Math.max(15, (LEFT_DEFAULT_PX / width) * 100));
};

const treeData = ref<EntityTypeTreeNode[]>([]);
const treeKeyword = ref('');
const extensionNamespaces = ref<{ id: OntologyId; prefix: string; uri: string }[]>([]);
const entityTypeOptions = ref<{ id: OntologyId; name: string }[]>([]);
const allObjectProps = ref<{ id: OntologyId; name: string; isBuiltin: string; inverseOfId?: string }[]>([]);
const selectedDomainTypeId = ref<OntologyId>();
const selectedDomainName = ref('');
const selectedDomainLabel = ref('');
const pagedData = ref<ObjectPropertySummary[]>([]);
const applicableData = ref<ApplicableObjectProperty[]>([]);

const pagination = reactive({ current: 1, size: 10, total: 0 });

const tableMaxHeight = ref(window.innerHeight - 320);

const query = reactive({
	name: '',
	isBuiltin: '',
	inferenceSupport: '',
});

const treeProps = { label: 'label', children: 'children' };

const dialog = reactive({
	visible: false,
	title: '新增对象属性',
	loading: false,
});

const createEmptyForm = (): ObjectPropertyForm => ({
	name: '',
	iriLocalName: '',
	iri: '',
	label: '',
	definition: '',
	domainEntityTypeIds: [],
	rangeEntityTypeIds: [],
	isFunctional: '0',
	isInverseFunctional: '0',
	isTransitive: '0',
	isSymmetric: '0',
	inverseOfId: undefined,
	sortOrder: 0,
	remarks: '',
	inferenceSupport: [],
});

const form = reactive<ObjectPropertyForm>(createEmptyForm());
const isBuiltinEdit = computed(() => Boolean(form.id && form.isBuiltin === '1'));
const namespaceOptions = computed(() => extensionNamespaces.value);

const iriPreview = computed(() => {
	const ns = extensionNamespaces.value.find((n: { id: OntologyId; prefix: string; uri: string }) => n.id === form.namespaceId);
	if (!ns) return '';
	const localName = form.iriLocalName || form.name;
	if (!localName) return '';
	return ns.uri + localName;
});

const symmetricWarning = computed(() =>
	computeSymmetricWarning(form.isSymmetric === '1', form.domainEntityTypeIds, form.rangeEntityTypeIds)
);

const inverseOptions = computed(() => {
	return allObjectProps.value.filter((item: { id: OntologyId; name: string; isBuiltin: string; inverseOfId?: string }) => {
		if (form.id && item.id === form.id) return false;
		if (item.isBuiltin === '1') return false;
		if (item.inverseOfId) return false;
		return true;
	});
});

const rules = {
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
	name: [
		{ required: true, message: '请输入英文名称', trigger: 'blur' },
		{ pattern: /^[a-z][a-zA-Z0-9]*$/, message: '必须以小写字母开头，仅支持英文字母和数字', trigger: 'blur' },
	],
	label: [{ required: true, message: '请输入中文标签', trigger: 'blur' }],
	domainEntityTypeIds: [{ required: true, type: 'array', min: 1, message: '至少选择一个定义域实体类型', trigger: 'change' }],
	rangeEntityTypeIds: [{ required: true, type: 'array', min: 1, message: '至少选择一个值域实体类型', trigger: 'change' }],
};

const filteredTree = computed<EntityTypeTreeNode[]>(() => filterEntityTypeTree(treeData.value, treeKeyword.value));

const getErrorMessage = (error: unknown, fallback: string) => {
	if (error && typeof error === 'object' && 'msg' in error) return String((error as { msg?: unknown }).msg || fallback);
	return fallback;
};

const inferenceCapabilityLabel = (cap: string): string => {
	switch (cap) {
		case 'FUNCTIONAL_CHECK': return '功能校验';
		case 'DISJOINT_CHECK': return '不相交检测';
		case 'SUBCLASS_INFERENCE': return '子类推理';
		default: return cap;
	}
};

const loadTree = async () => {
	const response = await fetchEntityTypeTree();
	treeData.value = (response.data || []) as EntityTypeTreeNode[];
};

const loadEntityTypes = async () => {
	const response = await fetchEntityTypeList();
	entityTypeOptions.value = (response.data || []) as { id: OntologyId; name: string }[];
};

const loadNamespaces = async () => {
	const response = await fetchNamespaceList({ isBuiltin: '0' });
	extensionNamespaces.value = (response.data || []) as { id: OntologyId; prefix: string; uri: string }[];
};

const loadAllObjectProps = async () => {
	const response = await fetchObjectPropertyPage({ size: 999 });
	const pageData = response.data || {};
	allObjectProps.value = ((pageData.records || []) as ObjectPropertySummary[]).map((s) => ({
		id: s.objectProperty.id,
		name: s.objectProperty.name,
		isBuiltin: s.objectProperty.isBuiltin,
		inverseOfId: s.objectProperty.inverseOfId,
	}));
};

const loadList = async () => {
	if (selectedDomainTypeId.value) {
		tableLoading.value = true;
		try {
			const response = await fetchObjectPropertiesByDomain(selectedDomainTypeId.value);
			applicableData.value = (response.data || []) as ApplicableObjectProperty[];
		} catch (error: unknown) {
			useMessage().error(getErrorMessage(error, '加载失败'));
		} finally {
			tableLoading.value = false;
		}
	} else {
		tableLoading.value = true;
		try {
			const params: ObjectPropertyQuery = {
				current: pagination.current,
				size: pagination.size,
				name: query.name || undefined,
				isBuiltin: (query.isBuiltin as '0' | '1') || undefined,
				inferenceSupport: query.inferenceSupport || undefined,
			};
			const response = await fetchObjectPropertyPage(params);
			const pageData = response.data || {};
			pagedData.value = (pageData.records || []) as ObjectPropertySummary[];
			pagination.total = pageData.total || 0;
		} catch (error: unknown) {
			useMessage().error(getErrorMessage(error, '加载失败'));
		} finally {
			tableLoading.value = false;
		}
	}
};

const refreshAll = async () => {
	refreshing.value = true;
	try {
		await Promise.all([loadTree(), loadEntityTypes(), loadNamespaces(), loadAllObjectProps()]);
		await loadList();
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '刷新失败'));
	} finally {
		refreshing.value = false;
	}
};

const showAllProperties = () => {
	selectedDomainTypeId.value = undefined;
	selectedDomainName.value = '';
	selectedDomainLabel.value = '';
	pagination.current = 1;
	loadList();
};

const handleNodeClick = async (data: EntityTypeTreeNode) => {
	selectedDomainTypeId.value = data.id;
	selectedDomainName.value = data.name;
	selectedDomainLabel.value = data.label;
	await loadList();
};

const onTransitiveToggle = (val: string | number | boolean) => {
	if (val === '1') {
		form.isFunctional = '0';
		form.isInverseFunctional = '0';
	}
};

const onFunctionalToggle = (val: string | number | boolean) => {
	if (val === '1') {
		form.isTransitive = '0';
	}
};

const resetForm = (detail?: any) => {
	Object.assign(form, createEmptyForm());
	delete form.id;
	delete form.isBuiltin;
	if (detail) {
		const op = detail.objectProperty || detail;
		form.id = op.id;
		form.isBuiltin = op.isBuiltin;
		form.namespaceId = op.namespaceId;
		form.name = op.name;
		form.iriLocalName = op.iriLocalName;
		form.iri = op.iri;
		form.definition = op.definition || '';
		form.isFunctional = op.isFunctional;
		form.isInverseFunctional = op.isInverseFunctional;
		form.isTransitive = op.isTransitive;
		form.isSymmetric = op.isSymmetric;
		form.inverseOfId = op.inverseOfId;
		form.sortOrder = op.sortOrder;
		form.remarks = op.remarks || '';
		form.inferenceSupport = detail.inferenceSupport || op.inferenceSupport || [];
		if (detail.domains) {
			form.domainEntityTypeIds = detail.domains.map((d: any) => d.id);
		}
		if (detail.ranges) {
			form.rangeEntityTypeIds = detail.ranges.map((r: any) => r.id);
		}
		if (detail.labels) {
			const zh = detail.labels.find((l: any) => l.locale === 'zh');
			form.label = zh?.label || '';
		}
	}
};

const openDialog = async (row?: any) => {
	dialog.visible = true;
	dialog.title = row ? '编辑对象属性' : '新增对象属性';
	if (row && row.objectProperty) {
		const response = await fetchObjectPropertyById(row.objectProperty.id);
		resetForm(response.data);
	} else {
		resetForm();
	}
};

const submit = async () => {
	await formRef.value?.validate(async (valid: boolean) => {
		if (!valid) return;
		dialog.loading = true;
		try {
			if (isBuiltinEdit.value) {
				const payload: ObjectPropertyUpdateRequest = {
					id: form.id!,
					label: form.label,
					sortOrder: form.sortOrder,
					remarks: form.remarks,
				};
				await putObjectPropertyObj(payload);
				} else if (form.id) {
					const payload: ObjectPropertyUpdateRequest = {
						id: form.id,
						namespaceId: form.namespaceId,
						name: form.name,
						iriLocalName: form.iriLocalName || undefined,
						iri: form.iri || undefined,
						label: form.label,
						definition: form.definition || undefined,
						domainEntityTypeIds: form.domainEntityTypeIds,
						rangeEntityTypeIds: form.rangeEntityTypeIds,
						isFunctional: form.isFunctional,
						isInverseFunctional: form.isInverseFunctional,
						isTransitive: form.isTransitive,
						isSymmetric: form.isSymmetric,
						inverseOfId: form.inverseOfId || undefined,
						sortOrder: form.sortOrder,
						remarks: form.remarks || undefined,
						inferenceSupport: form.inferenceSupport,
					};
					await putObjectPropertyObj(payload);
				} else {
					const payload: ObjectPropertyCreateRequest = {
						namespaceId: form.namespaceId!,
						name: form.name,
						iriLocalName: form.iriLocalName || undefined,
						iri: form.iri || undefined,
						label: form.label,
						definition: form.definition || undefined,
						domainEntityTypeIds: form.domainEntityTypeIds,
						rangeEntityTypeIds: form.rangeEntityTypeIds,
						isFunctional: form.isFunctional,
						isInverseFunctional: form.isInverseFunctional,
						isTransitive: form.isTransitive,
						isSymmetric: form.isSymmetric,
						inverseOfId: form.inverseOfId || undefined,
						sortOrder: form.sortOrder,
						remarks: form.remarks || undefined,
						inferenceSupport: form.inferenceSupport,
					};
					await addObjectPropertyObj(payload);
			}
			useMessage().success('操作成功');
			dialog.visible = false;
			await Promise.all([loadList(), loadAllObjectProps()]);
		} catch (error: unknown) {
			useMessage().error(getErrorMessage(error, '操作失败'));
		} finally {
			dialog.loading = false;
		}
	});
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该对象属性？');
		await delObjectPropertyObj(row.objectProperty.id);
		useMessage().success('删除成功');
		await Promise.all([loadList(), loadAllObjectProps()]);
	} catch (error: unknown) {
		if (error !== 'cancel') {
			useMessage().error(getErrorMessage(error, '删除失败'));
		}
	}
};

onMounted(() => {
	refreshAll();
	tableMaxHeight.value = window.innerHeight - 320;
	window.addEventListener('resize', () => {
		tableMaxHeight.value = window.innerHeight - 320;
	});
	if (pageRef.value) {
		recalcLeftPane(pageRef.value.clientWidth);
		resizeObserver = new ResizeObserver((entries) => {
			for (const entry of entries) {
				recalcLeftPane(entry.contentRect.width);
			}
		});
		resizeObserver.observe(pageRef.value);
	}
});

onUnmounted(() => {
	resizeObserver?.disconnect();
	window.removeEventListener('resize', () => {});
});
</script>

<style scoped>
.ontology-object-property-page {
	height: 100%;
}
.tree-panel,
.table-panel {
	height: 100%;
	overflow: auto;
}
.tree-scrollbar {
	height: calc(100% - 100px);
}
.mr6 {
	margin-right: 6px;
}
.mb6 {
	margin-bottom: 6px;
}
.mb8 {
	margin-bottom: 8px;
}
.ml6 {
	margin-left: 6px;
}
.tree-node {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
}
.tree-node-label {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}
</style>
