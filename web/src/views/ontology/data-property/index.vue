<template>
	<div ref="pageRef" class="layout-padding ontology-data-property-page">
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
							当前定义域：{{ selectedDomainLabel }}（{{ selectedDomainName }}）· 含继承属性
						</el-text>
						<el-text v-else type="info">全部数据属性</el-text>
					</div>
					<el-row class="mb8" :gutter="8">
						<el-col :span="5">
							<el-input v-model="query.name" clearable placeholder="属性名称" />
						</el-col>
						<el-col :span="4">
							<el-select v-model="query.baseType" clearable placeholder="值域类型">
								<el-option label="BOOLEAN" value="BOOLEAN" />
								<el-option label="DATE" value="DATE" />
								<el-option label="NUMERIC" value="NUMERIC" />
								<el-option label="TEXT" value="TEXT" />
								<el-option label="URI" value="URI" />
								<el-option label="UNIT_REF" value="UNIT_REF" />
								<el-option label="TEXT_OR_NUMERIC" value="TEXT_OR_NUMERIC" />
							</el-select>
						</el-col>
						<el-col :span="4">
							<el-select v-model="query.isBuiltin" clearable placeholder="核心/扩展">
								<el-option label="核心" value="1" />
								<el-option label="扩展" value="0" />
							</el-select>
						</el-col>
						<el-col :span="3">
							<el-button icon="search" type="primary" @click="loadList">查询</el-button>
						</el-col>
						<el-col :span="8" style="text-align: right">
							<el-button icon="folder-add" type="primary" v-auth="'ontology_data_property_add'" @click="openDialog()">新增属性</el-button>
						</el-col>
					</el-row>

					<el-table v-if="!selectedDomainTypeId" :data="pagedData" border style="width: 100%">
						<el-table-column type="index" label="#" width="50" />
						<el-table-column prop="displayName" label="显示名" min-width="120" show-overflow-tooltip />
						<el-table-column prop="dataProperty.name" label="标准Name" min-width="120" show-overflow-tooltip />
						<el-table-column prop="dataProperty.baseType" label="值域" width="130" />
						<el-table-column prop="dataProperty.valueMode" label="值模式" width="140" />
						<el-table-column label="定义域" min-width="120">
							<template #default="{ row }">{{ row.domainEntityTypeLabel || row.domainEntityTypeName || '—' }}</template>
						</el-table-column>
						<el-table-column label="来源" width="100">
							<template #default="{ row }">
								<el-tag v-if="row.dataProperty.sourceType === 'APPENDIX_C'" size="small">附录C</el-tag>
								<el-tag v-else-if="row.dataProperty.sourceType === 'CLAUSE_REQUIRED'" size="small" type="success">条款直采</el-tag>
								<el-tag v-else-if="row.dataProperty.sourceType === 'APPENDIX_D_COMPAT'" size="small" type="warning">附录D兼容</el-tag>
								<el-tag v-else size="small" type="info">扩展</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="核心/扩展" width="80">
							<template #default="{ row }">
								<el-tag v-if="row.dataProperty.isBuiltin === '1'" size="small">核心</el-tag>
								<el-tag v-else size="small" type="info">扩展</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="操作" width="120" fixed="right">
							<template #default="{ row }">
								<el-button link type="primary" v-auth="'ontology_data_property_edit'" @click="openDialog(row)">编辑</el-button>
								<el-tooltip :content="row.dataProperty.isBuiltin === '1' ? '核心数据属性不可删除' : '删除数据属性'" placement="top">
									<span>
										<el-button link type="danger" :disabled="row.dataProperty.isBuiltin === '1'" v-auth="'ontology_data_property_del'" @click="handleDelete(row)">删除</el-button>
									</span>
								</el-tooltip>
							</template>
						</el-table-column>
					</el-table>
					<pagination v-if="!selectedDomainTypeId" :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @pagination="loadList" />

					<el-table v-if="selectedDomainTypeId" :data="applicableData" border style="width: 100%">
						<el-table-column type="index" label="#" width="50" />
						<el-table-column prop="displayName" label="显示名" min-width="120" show-overflow-tooltip />
						<el-table-column prop="dataProperty.name" label="标准Name" min-width="120" show-overflow-tooltip />
						<el-table-column prop="dataProperty.baseType" label="值域" width="130" />
						<el-table-column prop="dataProperty.valueMode" label="值模式" width="140" />
						<el-table-column label="直接/继承" width="100">
							<template #default="{ row }">
								<el-tag v-if="!row.inherited" size="small" type="success">直接</el-tag>
								<el-tag v-else size="small" type="warning">继承</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="声明定义域" min-width="120">
							<template #default="{ row }">{{ row.declaredDomainLabel || row.declaredDomainName || '—' }}</template>
						</el-table-column>
						<el-table-column label="来源" width="100">
							<template #default="{ row }">
								<el-tag v-if="row.dataProperty.sourceType === 'APPENDIX_C'" size="small">附录C</el-tag>
								<el-tag v-else-if="row.dataProperty.sourceType === 'CLAUSE_REQUIRED'" size="small" type="success">条款直采</el-tag>
								<el-tag v-else-if="row.dataProperty.sourceType === 'APPENDIX_D_COMPAT'" size="small" type="warning">附录D兼容</el-tag>
								<el-tag v-else size="small" type="info">扩展</el-tag>
							</template>
						</el-table-column>
					</el-table>
				</div>
			</pane>
		</splitpanes>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="720px" destroy-on-close>
			<el-alert v-if="!isBuiltinEdit && extensionNamespaces.length === 0" title="请先创建扩展命名空间，再新增数据属性" type="warning" :closable="false" class="mb8" />
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-divider content-position="left">基本信息</el-divider>
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择扩展命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaceOptions" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="英文名称" prop="name">
					<el-input v-model="form.name" :disabled="isBuiltinEdit" placeholder="如 vehicleWeight" />
				</el-form-item>
				<el-form-item label="IRI本地名" prop="iriLocalName">
					<el-input v-model="form.iriLocalName" :disabled="isBuiltinEdit" placeholder="缺省等于英文名称" />
				</el-form-item>
				<el-form-item label="IRI预览" prop="iri">
					<el-input :model-value="isBuiltinEdit ? form.iri : iriPreview" readonly placeholder="由后端根据命名空间和IRI本地名生成" />
				</el-form-item>
				<el-form-item label="中文标签" prop="label">
					<el-input v-model="form.label" placeholder="如 车辆重量" maxlength="128" show-word-limit />
				</el-form-item>
				<el-form-item label="定义" prop="definition">
					<el-input v-model="form.definition" :disabled="isBuiltinEdit" type="textarea" maxlength="512" show-word-limit />
				</el-form-item>
				<el-form-item label="定义域" prop="domainEntityTypeId">
					<el-select v-model="form.domainEntityTypeId" :disabled="isBuiltinEdit" filterable placeholder="选择实体类型" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>

				<el-divider content-position="left">值域</el-divider>
				<el-form-item label="基础类型" prop="baseType">
					<el-select v-model="form.baseType" :disabled="isBuiltinEdit" placeholder="选择值域类型" style="width: 100%" @change="onBaseTypeChange">
						<el-option label="BOOLEAN" value="BOOLEAN" />
						<el-option label="DATE" value="DATE" />
						<el-option label="NUMERIC" value="NUMERIC" />
						<el-option label="TEXT" value="TEXT" />
						<el-option label="URI" value="URI" />
						<el-option label="UNIT_REF" value="UNIT_REF" />
						<el-option label="TEXT_OR_NUMERIC" value="TEXT_OR_NUMERIC" />
					</el-select>
				</el-form-item>
				<el-form-item label="值模式" prop="valueMode">
					<el-select v-model="form.valueMode" :disabled="isBuiltinEdit || form.baseType === 'UNIT_REF'" placeholder="选择值模式" style="width: 100%">
						<el-option label="自由值" value="FREE" />
						<el-option label="闭合枚举" value="CLOSED_ENUM" />
						<el-option label="开放枚举" value="OPEN_ENUM" />
						<el-option label="外部字典" value="EXTERNAL_DICTIONARY" />
						<el-option label="单位字典" value="UNIT_DICTIONARY" :disabled="form.baseType !== 'UNIT_REF'" />
					</el-select>
				</el-form-item>
				<el-form-item v-if="fieldVisibility.valueSourceRef" label="外部值源" prop="valueSourceRef">
					<el-input v-model="form.valueSourceRef" placeholder="如 ICS、CCS" />
				</el-form-item>
				<el-form-item v-if="fieldVisibility.enumValues" label="枚举值" prop="enumValues">
					<el-text v-if="form.valueMode === 'OPEN_ENUM'" type="info" size="small" class="mb8 block-text">列表为推荐值，实例层仍允许提交其他合法值</el-text>
					<div v-if="!isBuiltinEdit" style="width: 100%">
						<el-input v-model="enumInput" placeholder="输入枚举值后回车" style="width: 200px" @keyup.enter="addEnum" />
						<el-button type="primary" link @click="addEnum">添加</el-button>
					</div>
					<div class="mt4">
						<el-tag v-for="(item, index) in form.enumValues" :key="index" closable :disable-transitions="false" class="mr6 mb6" @close="removeEnum(index)">
							{{ item }}
						</el-tag>
					</div>
				</el-form-item>

				<el-divider content-position="left">约束</el-divider>
				<el-form-item v-if="fieldVisibility.regexPattern" label="正则约束" prop="regexPattern">
					<el-input v-model="form.regexPattern" :disabled="isBuiltinEdit" placeholder="如 ^[A-Z0-9]{18}$" />
				</el-form-item>
				<el-form-item label="格式提示" prop="formatHint">
					<el-input v-model="form.formatHint" placeholder="如 YYYYMMDD" />
				</el-form-item>
				<el-form-item v-if="fieldVisibility.isUnique" label="唯一性" prop="isUnique">
					<el-switch v-model="form.isUnique" :disabled="isBuiltinEdit" active-text="是" active-value="1" inactive-text="否" inactive-value="0" />
				</el-form-item>
				<el-form-item v-if="fieldVisibility.unitCategoryId" label="单位分类" prop="unitCategoryId">
					<el-select v-model="form.unitCategoryId" :disabled="isBuiltinEdit" clearable filterable placeholder="NULL表示不限分类" style="width: 100%">
						<el-option v-for="item in unitCategoryOptions" :key="item.id" :label="item.categoryName + '（' + item.categoryCode + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item v-if="fieldVisibility.unitRefMode" label="单位引用模式">
					<el-input model-value="DICTIONARY_SYMBOL" disabled />
				</el-form-item>
				<el-form-item v-if="fieldVisibility.preferredAlias" label="首选别名" prop="preferredAlias">
					<el-input v-model="form.preferredAlias" :disabled="isBuiltinEdit" placeholder="如 measurementUnit" />
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

<script lang="ts" name="ontologyDataProperty" setup>
import { addDataPropertyObj, delDataPropertyObj, fetchDataPropertyById, fetchDataPropertyPage, fetchDataPropertiesByDomain, putDataPropertyObj } from '/@/api/ontology/data-property';
import { fetchEntityTypeList, fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { fetchCategoryList } from '/@/api/ontology/unit';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { filterEntityTypeTree } from '/@/views/ontology/entity-type/tree-utils';
import { autoResolveUnitRefMode, autoResolveValueMode, getFieldVisibility } from './value-rules';
import type { ApplicableDataProperty, DataPropertyCreateRequest, DataPropertyForm, DataPropertyQuery, DataPropertySummary, DataPropertyUpdateRequest, OntologyId } from '/@/types/ontology/data-property';
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
const unitCategoryOptions = ref<{ id: OntologyId; categoryCode: string; categoryName: string }[]>([]);
const selectedDomainTypeId = ref<OntologyId>();
const selectedDomainName = ref('');
const selectedDomainLabel = ref('');
const pagedData = ref<DataPropertySummary[]>([]);
const applicableData = ref<ApplicableDataProperty[]>([]);
const enumInput = ref('');

const pagination = reactive({ current: 1, size: 10, total: 0 });

const query = reactive({
	name: '',
	baseType: '',
	isBuiltin: '',
});

const treeProps = { label: 'label', children: 'children' };

const dialog = reactive({
	visible: false,
	title: '新增数据属性',
	loading: false,
});

const createEmptyForm = (): DataPropertyForm => ({
	name: '',
	iriLocalName: '',
	iri: '',
	label: '',
	definition: '',
	domainEntityTypeId: undefined,
	baseType: 'TEXT',
	valueMode: 'FREE',
	valueSourceRef: '',
	regexPattern: '',
	formatHint: '',
	isUnique: '0',
	unitCategoryId: undefined,
	unitRefMode: '',
	preferredAlias: '',
	enumValues: [],
	sortOrder: 0,
	remarks: '',
});

const form = reactive<DataPropertyForm>(createEmptyForm());
const isBuiltinEdit = computed(() => Boolean(form.id && form.isBuiltin === '1'));
const namespaceOptions = computed(() => extensionNamespaces.value);
const fieldVisibility = computed(() => getFieldVisibility(form.baseType, form.valueMode));

// IRI 预览：根据选中的命名空间URI和IRI本地名实时计算（设计§9.2）
const iriPreview = computed(() => {
	const ns = extensionNamespaces.value.find((n: { id: OntologyId; prefix: string; uri: string }) => n.id === form.namespaceId);
	if (!ns) return '';
	const localName = form.iriLocalName || form.name;
	if (!localName) return '';
	return ns.uri + localName;
});

const rules = {
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
	name: [
		{ required: true, message: '请输入英文名称', trigger: 'blur' },
		{ pattern: /^[a-z][a-zA-Z0-9]*$/, message: '必须以小写字母开头，仅支持英文字母和数字', trigger: 'blur' },
	],
	label: [{ required: true, message: '请输入中文标签', trigger: 'blur' }],
	domainEntityTypeId: [{ required: true, message: '请选择定义域实体类型', trigger: 'change' }],
	baseType: [{ required: true, message: '请选择值域类型', trigger: 'change' }],
	valueMode: [{ required: true, message: '请选择值模式', trigger: 'change' }],
	enumValues: [
		{
			validator: (_rule: any, _value: any, callback: any) => {
				if (form.valueMode === 'CLOSED_ENUM' && (!form.enumValues || form.enumValues.length === 0)) {
					callback(new Error('闭合枚举至少需要一个枚举值'));
				} else {
					callback();
				}
			},
			trigger: 'change',
		},
	],
};

const filteredTree = computed<EntityTypeTreeNode[]>(() => filterEntityTypeTree(treeData.value, treeKeyword.value));

const getErrorMessage = (error: unknown, fallback: string) => {
	if (error && typeof error === 'object' && 'msg' in error) return String((error as { msg?: unknown }).msg || fallback);
	return fallback;
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

const loadUnitCategories = async () => {
	const response = await fetchCategoryList();
	unitCategoryOptions.value = (response.data || []) as { id: OntologyId; categoryCode: string; categoryName: string }[];
};

const loadList = async () => {
	if (selectedDomainTypeId.value) {
		tableLoading.value = true;
		try {
			const response = await fetchDataPropertiesByDomain(selectedDomainTypeId.value);
			applicableData.value = (response.data || []) as ApplicableDataProperty[];
		} catch (error: unknown) {
			useMessage().error(getErrorMessage(error, '加载失败'));
		} finally {
			tableLoading.value = false;
		}
	} else {
		tableLoading.value = true;
		try {
			const params: DataPropertyQuery = {
				current: pagination.current,
				size: pagination.size,
				name: query.name || undefined,
				baseType: query.baseType || undefined,
				isBuiltin: (query.isBuiltin as '0' | '1') || undefined,
			};
			const response = await fetchDataPropertyPage(params);
			const pageData = response.data || {};
			pagedData.value = (pageData.records || []) as DataPropertySummary[];
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
		await Promise.all([loadTree(), loadEntityTypes(), loadNamespaces(), loadUnitCategories()]);
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

const onBaseTypeChange = () => {
	form.valueMode = autoResolveValueMode(form.baseType, form.valueMode) as any;
	form.unitRefMode = autoResolveUnitRefMode(form.baseType);
	if (form.baseType !== 'UNIT_REF') {
		form.unitRefMode = '';
		form.preferredAlias = '';
	}
};

const addEnum = () => {
	const val = enumInput.value.trim();
	if (val && !form.enumValues.includes(val)) {
		form.enumValues.push(val);
	}
	enumInput.value = '';
};

const removeEnum = (index: number) => {
	form.enumValues.splice(index, 1);
};

const resetForm = (row?: any) => {
	Object.assign(form, createEmptyForm());
	delete form.id;
	delete form.isBuiltin;
	enumInput.value = '';
	if (row) {
		const dp = row.dataProperty || row;
		form.id = dp.id;
		form.isBuiltin = dp.isBuiltin;
		form.namespaceId = dp.namespaceId;
		form.name = dp.name;
		form.iriLocalName = dp.iriLocalName;
		form.iri = dp.iri;
		form.definition = dp.definition || '';
		form.domainEntityTypeId = dp.domainEntityTypeId;
		form.baseType = dp.baseType;
		form.valueMode = dp.valueMode;
		form.valueSourceRef = dp.valueSourceRef || '';
		form.regexPattern = dp.regexPattern || '';
		form.formatHint = dp.formatHint || '';
		form.isUnique = dp.isUnique;
		form.unitCategoryId = dp.unitCategoryId;
		form.unitRefMode = dp.unitRefMode || '';
		form.preferredAlias = dp.preferredAlias || '';
		form.sortOrder = dp.sortOrder;
		form.remarks = dp.remarks || '';
		form.enumValues = [];
		if (row.enums) {
			form.enumValues = row.enums.map((e: any) => e.enumValue);
		}
		if (row.labels) {
			const zh = row.labels.find((l: any) => l.locale === 'zh');
			form.label = zh?.label || '';
		}
	}
};

const openDialog = async (row?: any) => {
	dialog.visible = true;
	dialog.title = row ? '编辑数据属性' : '新增数据属性';
	if (row && row.dataProperty) {
		const response = await fetchDataPropertyById(row.dataProperty.id);
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
				const payload: DataPropertyUpdateRequest = {
					id: form.id!,
					label: form.label,
					sortOrder: form.sortOrder,
					remarks: form.remarks,
				};
				await putDataPropertyObj(payload);
			} else if (form.id) {
				const payload: DataPropertyUpdateRequest = {
					id: form.id,
					namespaceId: form.namespaceId,
					name: form.name,
					iriLocalName: form.iriLocalName || undefined,
					iri: form.iri || undefined,
					label: form.label,
					definition: form.definition || undefined,
					domainEntityTypeId: form.domainEntityTypeId,
					baseType: form.baseType,
					valueMode: form.valueMode,
					valueSourceRef: form.valueSourceRef || undefined,
					regexPattern: form.regexPattern || undefined,
					formatHint: form.formatHint || undefined,
					isUnique: form.isUnique,
					unitCategoryId: form.unitCategoryId,
					unitRefMode: form.unitRefMode || undefined,
					preferredAlias: form.preferredAlias || undefined,
					enumValues: form.enumValues,
					sortOrder: form.sortOrder,
					remarks: form.remarks || undefined,
				};
				await putDataPropertyObj(payload);
			} else {
				const payload: DataPropertyCreateRequest = {
					namespaceId: form.namespaceId!,
					name: form.name,
					iriLocalName: form.iriLocalName || undefined,
					iri: form.iri || undefined,
					label: form.label,
					definition: form.definition || undefined,
					domainEntityTypeId: form.domainEntityTypeId!,
					baseType: form.baseType,
					valueMode: form.valueMode,
					valueSourceRef: form.valueSourceRef || undefined,
					regexPattern: form.regexPattern || undefined,
					formatHint: form.formatHint || undefined,
					isUnique: form.isUnique,
					unitCategoryId: form.unitCategoryId,
					unitRefMode: form.unitRefMode || undefined,
					preferredAlias: form.preferredAlias || undefined,
					enumValues: form.enumValues,
					sortOrder: form.sortOrder,
					remarks: form.remarks || undefined,
				};
				await addDataPropertyObj(payload);
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

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该数据属性？');
		await delDataPropertyObj(row.dataProperty.id);
		useMessage().success('删除成功');
		await loadList();
	} catch (error: unknown) {
		if (error !== 'cancel') {
			useMessage().error(getErrorMessage(error, '删除失败'));
		}
	}
};

onMounted(() => {
	refreshAll();
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
});
</script>

<style scoped>
.ontology-data-property-page {
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
.mt4 {
	margin-top: 4px;
}
.block-text {
	display: block;
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
