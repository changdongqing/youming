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
		<el-drawer v-model="detailVisible" title="实例详情" size="64%" destroy-on-close>
			<template v-if="detailData">
				<el-descriptions :column="2" border>
					<el-descriptions-item label="IRI">{{ detailData.iri }}</el-descriptions-item>
					<el-descriptions-item label="本地名">{{ detailData.iriLocalName }}</el-descriptions-item>
					<el-descriptions-item label="标签">{{ detailData.label }}</el-descriptions-item>
					<el-descriptions-item label="类型">{{ detailData.rdfTypeLabel || detailData.rdfTypeName }}</el-descriptions-item>
					<el-descriptions-item label="命名空间">{{ detailData.namespacePrefix }}</el-descriptions-item>
					<el-descriptions-item label="来源">{{ sourceTypeLabel(detailData.sourceType) }}</el-descriptions-item>
					<el-descriptions-item label="声明模式">{{ declarationModeLabel(detailData.declarationMode) }}</el-descriptions-item>
					<el-descriptions-item label="排序">{{ detailData.sortOrder }}</el-descriptions-item>
					<el-descriptions-item v-if="detailData.remarks" label="备注">{{ detailData.remarks }}</el-descriptions-item>
				</el-descriptions>

				<el-divider content-position="left">
					<span>数据属性值</span>
					<el-button
						v-if="detailData.isBuiltin === '0'"
						v-auth="'ontology_instance_edit'"
						link type="primary" class="ml6" @click="openDataValueEditor"
					>编辑数据值</el-button>
				</el-divider>
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

				<el-divider content-position="left">
					<span>出向断言</span>
					<el-button
						v-if="detailData.isBuiltin === '0'"
						v-auth="'ontology_instance_edit'"
						link type="primary" class="ml6" @click="openRelationEditor"
					>管理断言</el-button>
				</el-divider>
				<el-table :data="detailData.outgoingRelations" border style="width: 100%">
					<el-table-column label="谓词" min-width="100">
						<template #default="{ row }">{{ row.objectPropertyLabel || row.objectPropertyName }}</template>
					</el-table-column>
					<el-table-column label="客体类型" width="110">
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

				<el-divider content-position="left">约束校验</el-divider>
				<validation-panel v-if="detailData?.id" :instance-id="detailData.id" />
			</template>
		</el-drawer>

		<!-- 新增/编辑对话框 -->
		<el-dialog v-model="dialog.visible" :title="dialog.title" width="820px" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-divider content-position="left">基本信息</el-divider>
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaceOptions" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="实体类型" prop="rdfTypeId">
					<el-select v-model="form.rdfTypeId" :disabled="isBuiltinEdit || !!form.id" filterable placeholder="选择实体类型" style="width: 100%" @change="onRdfTypeChange">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="IRI本地名" prop="iriLocalName">
					<el-input v-model="form.iriLocalName" :disabled="isBuiltinEdit" :placeholder="form.id ? '' : '留空则自动生成'" maxlength="128" />
				</el-form-item>
				<el-form-item label="IRI预览" prop="iri">
					<el-input :model-value="iriPreview" readonly placeholder="由后端根据命名空间和IRI本地名生成（仅供参考）" />
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

				<!-- Schema 驱动数据属性值表单（仅扩展实例新增时可用） -->
				<template v-if="!isBuiltinEdit && !form.id && formMeta">
					<el-divider content-position="left">
						<span>数据属性值</span>
						<span class="form-hint">（可选，创建后也可在详情中编辑）</span>
					</el-divider>
					<el-form-item
						v-for="propMeta in formMeta.applicableDataProperties"
						:key="propMeta.dataPropertyId"
						:label="propMeta.dataPropertyLabel || propMeta.dataPropertyName"
					>
						<template #label>
							<span>{{ propMeta.dataPropertyLabel || propMeta.dataPropertyName }}</span>
							<el-tag v-if="propMeta.inherited" size="small" type="info" class="ml6">继承</el-tag>
							<el-tag v-if="propMeta.isUnique === '1'" size="small" type="warning" class="ml6">唯一</el-tag>
						</template>
						<instance-value-field
							:prop-meta="propMeta"
							:enum-options="enumOptionsMap[propMeta.dataPropertyId] || []"
							:unit-options="unitOptionsFor(propMeta.unitCategoryId)"
							:values="dataValueDrafts[propMeta.dataPropertyId] || []"
							@update:values="updateDataValueDraft(propMeta.dataPropertyId, $event)"
						/>
					</el-form-item>
					<el-empty v-if="formMeta.applicableDataProperties.length === 0" description="该实体类型暂无可赋值的数据属性" :image-size="60" />
				</template>
			</el-form>
			<template #footer>
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="dialog.loading" @click="submit">确定</el-button>
			</template>
		</el-dialog>

		<!-- 数据值整体编辑对话框（详情抽屉入口） -->
		<el-dialog v-model="dataValueDialog.visible" title="编辑数据属性值" width="820px" destroy-on-close>
			<div v-loading="dataValueDialog.loading">
				<el-alert v-if="dataValueDialog.detail" type="info" :closable="false" show-icon class="mb8">
					<template #default>整体替换「{{ dataValueDialog.detail.label || dataValueDialog.detail.iriLocalName }}」的全部数据属性值。</template>
				</el-alert>
				<el-form label-width="140px" v-if="dataValueDialog.formMeta">
					<el-form-item
						v-for="propMeta in dataValueDialog.formMeta.applicableDataProperties"
						:key="propMeta.dataPropertyId"
						:label="propMeta.dataPropertyLabel || propMeta.dataPropertyName"
					>
						<template #label>
							<span>{{ propMeta.dataPropertyLabel || propMeta.dataPropertyName }}</span>
							<el-tag v-if="propMeta.inherited" size="small" type="info" class="ml6">继承</el-tag>
						</template>
						<instance-value-field
							:prop-meta="propMeta"
							:enum-options="enumOptionsMap[propMeta.dataPropertyId] || []"
							:unit-options="unitOptionsFor(propMeta.unitCategoryId)"
							:values="dataValueDrafts[propMeta.dataPropertyId] || []"
							@update:values="updateDataValueDraft(propMeta.dataPropertyId, $event)"
						/>
					</el-form-item>
					<el-empty v-if="dataValueDialog.formMeta.applicableDataProperties.length === 0" description="该实体类型暂无可赋值的数据属性" :image-size="60" />
				</el-form>
			</div>
			<template #footer>
				<el-button @click="dataValueDialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="dataValueDialog.saving" @click="saveDataValues">保存</el-button>
			</template>
		</el-dialog>

		<!-- 关系编辑器对话框（详情抽屉入口） -->
		<el-dialog v-model="relationDialog.visible" title="管理对象属性断言" width="820px" destroy-on-close>
			<div v-loading="relationDialog.loading">
				<el-alert v-if="relationDialog.formMeta" type="info" :closable="false" show-icon class="mb8">
					<template #default>为「{{ relationDialog.detail?.label || relationDialog.detail?.iriLocalName }}」添加出向断言。功能性属性仅单选，Schema 资源客体只读。</template>
				</el-alert>
				<div v-if="relationDialog.formMeta">
					<el-form
						v-for="propMeta in relationDialog.formMeta.applicableObjectProperties"
						:key="propMeta.objectPropertyId"
						label-width="160px"
						class="relation-prop-form"
					>
						<el-form-item>
							<template #label>
								<span>{{ propMeta.objectPropertyLabel || propMeta.objectPropertyName }}</span>
								<el-tag v-if="propMeta.isFunctional === '1'" size="small" type="warning" class="ml6">功能性</el-tag>
								<el-tag v-if="propMeta.inherited" size="small" type="info" class="ml6">继承</el-tag>
							</template>
							<instance-relation-field
								:subject-id="relationDialog.detail?.id"
								:prop-meta="propMeta"
								:existing-relations="existingRelationsByProp(propMeta.objectPropertyId)"
								@add="handleAddRelation(propMeta.objectPropertyId, $event)"
								@remove="handleRemoveRelation"
							/>
						</el-form-item>
					</el-form>
					<el-empty v-if="relationDialog.formMeta.applicableObjectProperties.length === 0" description="该实体类型暂无可声明的对象属性" :image-size="60" />
				</div>
			</div>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyInstance" setup>
import {
	addInstanceObj,
	delInstanceObj,
	fetchInstanceById,
	fetchInstancePage,
	putInstanceObj,
	fetchInstanceFormMeta,
	putInstanceDataValues,
	fetchInstanceOptions,
	addInstanceRelation,
	delInstanceRelation,
} from '/@/api/ontology/instance';
import { fetchEntityTypeList, fetchEntityTypeTree } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { fetchDataPropertyById } from '/@/api/ontology/data-property';
import { fetchUnitList } from '/@/api/ontology/unit';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { filterEntityTypeTree } from '/@/views/ontology/entity-type/tree-utils';
import InstanceValueField from './instance-value-field.vue';
import InstanceRelationField from './instance-relation-field.vue';
import ValidationPanel from '/@/views/ontology/validation/components/ValidationPanel.vue';
import type {
	InstanceDetail,
	InstanceForm,
	InstanceQuery,
	InstanceSummary,
	InstanceCreateRequest,
	InstanceUpdateRequest,
	InstanceFormMeta,
	InstanceDataValueDTO,
	InstanceDataValueVO,
	InstanceObjectRelationVO,
	DataPropertyMeta,
	OntologyId,
} from '/@/types/ontology/instance';
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

// ==================== Schema 驱动表单状态 ====================
const formMeta = ref<InstanceFormMeta | null>(null);
/** 枚举值缓存：dataPropertyId -> 枚举选项列表 */
const enumOptionsMap = reactive<Record<string, { value: string; canonical?: string }[]>>({});
/** 单位缓存：unitCategoryId -> 单位选项列表 */
const unitOptionsCache = reactive<Record<string, { id: OntologyId; symbol: string; name?: string }[]>>({});
/** 数据值草稿：dataPropertyId -> 该属性的多值列表（创建/编辑共用） */
const dataValueDrafts = reactive<Record<string, InstanceDataValueDTO[]>>({});

/** 枚举标签映射 */
const sourceTypeLabel = (t: string) => (t === 'APPENDIX_D' ? '附录D' : t === 'EXTENSION' ? '扩展' : t);
const declarationModeLabel = (m: string) => (m === 'EXPLICIT' ? '显式声明' : m === 'REFERENCE_ONLY' ? '仅引用' : m);

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

/** 加载实体类型的动态表单元数据 */
const loadFormMeta = async (entityTypeId: OntologyId): Promise<InstanceFormMeta | null> => {
	try {
		const response = await fetchInstanceFormMeta(entityTypeId);
		return (response.data || null) as InstanceFormMeta | null;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载表单元数据失败'));
		return null;
	}
};

/** 确保枚举值已加载到缓存（CLOSED/OPEN_ENUM 属性按需懒加载） */
const ensureEnumOptions = async (propMeta: DataPropertyMeta) => {
	if (propMeta.valueMode !== 'CLOSED_ENUM' && propMeta.valueMode !== 'OPEN_ENUM') return;
	if (enumOptionsMap[propMeta.dataPropertyId]) return;
	try {
		const response = await fetchDataPropertyById(propMeta.dataPropertyId);
		const detail = response.data as { enums?: { enumValue: string; canonicalValue?: string }[] } | undefined;
		const enums = detail?.enums || [];
		enumOptionsMap[propMeta.dataPropertyId] = enums.map((e) => ({ value: e.enumValue, canonical: e.canonicalValue }));
	} catch {
		enumOptionsMap[propMeta.dataPropertyId] = [];
	}
};

/** 获取某单位分类下的单位选项（UNIT_REF 属性按需懒加载） */
const unitOptionsFor = (unitCategoryId?: OntologyId) => {
	if (!unitCategoryId) return [];
	if (!unitOptionsCache[unitCategoryId]) return [];
	return unitOptionsCache[unitCategoryId];
};

/** 预加载 UNIT_REF 属性涉及的单位分类 */
const ensureUnitOptions = async (metas: DataPropertyMeta[]) => {
	const categoryIds = Array.from(new Set(
		metas.filter((m) => m.baseType === 'UNIT_REF' && m.unitCategoryId).map((m) => m.unitCategoryId!)
	));
	for (const catId of categoryIds) {
		if (unitOptionsCache[catId]) continue;
		try {
			const response = await fetchUnitList({ categoryId: catId });
			const list = (response.data || []) as { id: OntologyId; unitSymbol: string; unitName?: string }[];
			unitOptionsCache[catId] = list.map((u) => ({ id: u.id, symbol: u.unitSymbol, name: u.unitName }));
		} catch {
			unitOptionsCache[catId] = [];
		}
	}
};

/** 初始化数据值草稿（从详情已有值填充，或创建空草稿） */
const initDataValueDrafts = (meta: InstanceFormMeta, existing?: InstanceDataValueVO[] | InstanceDataValueDTO[]) => {
	// 清空旧草稿
	Object.keys(dataValueDrafts).forEach((k) => delete dataValueDrafts[k]);
	for (const propMeta of meta.applicableDataProperties) {
		const existingValues = (existing || []).filter((v) => String((v as { dataPropertyId: OntologyId }).dataPropertyId) === propMeta.dataPropertyId);
		dataValueDrafts[propMeta.dataPropertyId] = existingValues.map((v) => ({
			dataPropertyId: propMeta.dataPropertyId,
			literalValue: (v as { literalValue: string }).literalValue,
			literalType: (v as { literalType: InstanceDataValueDTO['literalType'] }).literalType,
			unitId: (v as { unitId?: OntologyId }).unitId,
			literalSymbol: (v as { literalSymbol?: string }).literalSymbol,
			sortOrder: (v as { sortOrder: number }).sortOrder,
		}));
	}
};

/** 收集草稿为提交用的扁平 dataValues 数组 */
const collectDataValueDrafts = (): InstanceDataValueDTO[] => {
	const result: InstanceDataValueDTO[] = [];
	let order = 1;
	for (const key of Object.keys(dataValueDrafts)) {
		for (const dv of dataValueDrafts[key]) {
			result.push({ ...dv, sortOrder: dv.sortOrder ?? order });
			order++;
		}
	}
	return result;
};

const updateDataValueDraft = (propId: OntologyId, values: InstanceDataValueDTO[]) => {
	dataValueDrafts[propId] = values;
};

/** 实体类型变化时加载表单元数据 + 枚举/单位选项 */
const onRdfTypeChange = async (typeId?: OntologyId) => {
	formMeta.value = null;
	if (!typeId) return;
	const meta = await loadFormMeta(typeId);
	formMeta.value = meta;
	if (meta) {
		await Promise.all([
			...meta.applicableDataProperties.map((p) => ensureEnumOptions(p)),
			ensureUnitOptions(meta.applicableDataProperties),
		]);
		initDataValueDrafts(meta);
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
	formMeta.value = null;
	Object.keys(dataValueDrafts).forEach((k) => delete dataValueDrafts[k]);
	if (selectedTypeId.value) {
		form.rdfTypeId = selectedTypeId.value;
		void onRdfTypeChange(selectedTypeId.value);
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
					dataValues: collectDataValueDrafts().length > 0 ? collectDataValueDrafts() : undefined,
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
		.confirm('确认删除该实例？相关数据（数据值、出向断言）将被清除。')
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

// ==================== 数据值编辑器（详情入口） ====================
const dataValueDialog = reactive<{ visible: boolean; loading: boolean; saving: boolean; detail: InstanceDetail | null; formMeta: InstanceFormMeta | null }>({
	visible: false,
	loading: false,
	saving: false,
	detail: null,
	formMeta: null,
});

const openDataValueEditor = async () => {
	if (!detailData.value) return;
	dataValueDialog.detail = detailData.value;
	dataValueDialog.visible = true;
	dataValueDialog.loading = true;
	dataValueDialog.formMeta = null;
	try {
		const meta = await loadFormMeta(detailData.value.rdfTypeId);
		dataValueDialog.formMeta = meta;
		if (meta) {
			await Promise.all([
				...meta.applicableDataProperties.map((p) => ensureEnumOptions(p)),
				ensureUnitOptions(meta.applicableDataProperties),
			]);
			// InstanceDataValueVO 结构与草稿初始化兼容
			initDataValueDrafts(meta, detailData.value.dataValues as unknown as InstanceDataValueDTO[]);
		}
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载数据值编辑器失败'));
	} finally {
		dataValueDialog.loading = false;
	}
};

const saveDataValues = async () => {
	if (!dataValueDialog.detail) return;
	dataValueDialog.saving = true;
	try {
		const drafts = collectDataValueDrafts();
		await putInstanceDataValues(dataValueDialog.detail.id, drafts);
		useMessage().success('数据值已更新');
		dataValueDialog.visible = false;
		// 刷新详情
		await openDetail({ id: dataValueDialog.detail.id } as InstanceSummary);
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '保存数据值失败'));
	} finally {
		dataValueDialog.saving = false;
	}
};

// ==================== 关系编辑器（详情入口） ====================
const relationDialog = reactive<{ visible: boolean; loading: boolean; detail: InstanceDetail | null; formMeta: InstanceFormMeta | null }>({
	visible: false,
	loading: false,
	detail: null,
	formMeta: null,
});

/** 取某对象属性下已有的出向断言（用于已存在断言展示与删除） */
const existingRelationsByProp = (objectPropertyId: OntologyId): InstanceObjectRelationVO[] => {
	if (!relationDialog.detail) return [];
	return (relationDialog.detail.outgoingRelations || []).filter((r: InstanceObjectRelationVO) => r.objectPropertyId === objectPropertyId);
};

const openRelationEditor = async () => {
	if (!detailData.value) return;
	relationDialog.detail = detailData.value;
	relationDialog.visible = true;
	relationDialog.loading = true;
	relationDialog.formMeta = null;
	try {
		const meta = await loadFormMeta(detailData.value.rdfTypeId);
		relationDialog.formMeta = meta;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载关系编辑器失败'));
	} finally {
		relationDialog.loading = false;
	}
};

const handleAddRelation = async (objectPropertyId: OntologyId, objectInstanceId: OntologyId) => {
	if (!relationDialog.detail) return;
	try {
		await addInstanceRelation(relationDialog.detail.id, { objectPropertyId, objectInstanceId });
		useMessage().success('断言已添加');
		// 刷新详情数据（关系编辑器与详情抽屉共用 detailData）
		const response = await fetchInstanceById(relationDialog.detail.id);
		relationDialog.detail = response.data as InstanceDetail;
		if (detailData.value && detailData.value.id === relationDialog.detail.id) {
			detailData.value = relationDialog.detail;
		}
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '添加断言失败'));
	}
};

const handleRemoveRelation = async (relationId: OntologyId) => {
	if (!relationDialog.detail) return;
	try {
		await delInstanceRelation(relationDialog.detail.id, relationId);
		useMessage().success('断言已删除');
		const response = await fetchInstanceById(relationDialog.detail.id);
		relationDialog.detail = response.data as InstanceDetail;
		if (detailData.value && detailData.value.id === relationDialog.detail.id) {
			detailData.value = relationDialog.detail;
		}
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '删除断言失败'));
	}
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
.form-hint {
	font-size: 12px;
	color: var(--el-text-color-secondary);
	margin-left: 4px;
}
.relation-prop-form {
	margin-bottom: 8px;
}
.ml6 {
	margin-left: 6px;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
