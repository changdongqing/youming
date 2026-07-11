<template>
	<div class="layout-padding ontology-axiom-rule-page">
		<el-row class="mb8" :gutter="8">
			<el-col :span="6">
				<el-input v-model="query.name" clearable placeholder="规则名称" />
			</el-col>
			<el-col :span="4">
				<el-select v-model="query.category" clearable placeholder="规则类别">
					<el-option label="实体类型规则" value="ENTITY_TYPE" />
					<el-option label="属性规则" value="PROPERTY" />
					<el-option label="关系规则" value="RELATION" />
				</el-select>
			</el-col>
			<el-col :span="4">
				<el-select v-model="query.status" clearable placeholder="状态">
					<el-option label="启用" value="ACTIVE" />
					<el-option label="草稿" value="DRAFT" />
					<el-option label="阻塞" value="BLOCKED" />
				</el-select>
			</el-col>
			<el-col :span="3">
				<el-button icon="search" type="primary" @click="loadList">查询</el-button>
			</el-col>
			<el-col :span="7" style="text-align: right">
				<el-button icon="folder-add" type="primary" v-auth="'ontology_axiom_rule_add'" @click="openDialog()">新增规则</el-button>
			</el-col>
		</el-row>

		<el-tabs v-model="activeTab" class="mb8">
			<el-tab-pane label="规则目录" name="rules">
				<el-table v-loading="tableLoading" :data="pagedData" border style="width: 100%">
					<el-table-column type="index" label="#" width="50" />
					<el-table-column prop="axiomRule.name" label="名称" min-width="150" show-overflow-tooltip />
					<el-table-column prop="axiomRule.ruleCode" label="代码" min-width="180" show-overflow-tooltip />
					<el-table-column label="类别" width="100">
						<template #default="{ row }">{{ getCategoryLabel(row.axiomRule.category) }}</template>
					</el-table-column>
					<el-table-column prop="validationModeLabel" label="执行机制" width="120" />
					<el-table-column label="状态" width="80">
						<template #default="{ row }">
							<el-tag :type="getStatusTagType(row.axiomRule.status)" size="small">{{ row.axiomRule.status }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="严重级别" width="80">
						<template #default="{ row }">
							<el-tag :type="getSeverityTagType(row.axiomRule.severity)" size="small">{{ row.axiomRule.severity }}</el-tag>
						</template>
					</el-table-column>
					<el-table-column prop="targetCount" label="目标数" width="70" />
					<el-table-column label="来源" width="90">
						<template #default="{ row }">
							<el-tag v-if="row.axiomRule.sourceType === 'GB_CLAUSE_8'" size="small">第8.2条</el-tag>
							<el-tag v-else-if="row.axiomRule.sourceType === 'PRD_DERIVED'" size="small" type="success">PRD派生</el-tag>
							<el-tag v-else size="small" type="info">扩展</el-tag>
						</template>
					</el-table-column>
					<el-table-column label="内置" width="60">
						<template #default="{ row }">
							<span v-if="row.axiomRule.isBuiltin === '1'">🔒</span>
						</template>
					</el-table-column>
					<el-table-column label="操作" width="160" fixed="right">
						<template #default="{ row }">
							<el-button link type="primary" v-auth="'ontology_axiom_rule_view'" @click="openDetail(row)">详情</el-button>
							<el-button link type="primary" v-auth="'ontology_axiom_rule_edit'" @click="openDialog(row)">编辑</el-button>
							<el-button v-if="canDelete(row.axiomRule)" link type="danger" v-auth="'ontology_axiom_rule_del'" @click="handleDelete(row)">删除</el-button>
						</template>
					</el-table-column>
				</el-table>
				<pagination :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @pagination="loadList" />
			</el-tab-pane>

			<el-tab-pane label="不相交关系" name="disjoint">
				<el-row class="mb8">
					<el-button type="primary" v-auth="'ontology_axiom_rule_edit'" @click="openRelationDialog('disjoint')">新增不相交关系</el-button>
				</el-row>
				<el-table :data="disjointData" border style="width: 100%">
					<el-table-column type="index" label="#" width="50" />
					<el-table-column prop="typeAName" label="类型A" min-width="150" />
					<el-table-column prop="typeBName" label="类型B" min-width="150" />
					<el-table-column label="操作" width="100">
						<template #default="{ row }">
							<el-button link type="danger" v-auth="'ontology_axiom_rule_edit'" @click="handleDeleteRelation('disjoint', row)">删除</el-button>
						</template>
					</el-table-column>
				</el-table>
			</el-tab-pane>

			<el-tab-pane label="等价关系" name="equivalent">
				<el-row class="mb8">
					<el-button type="primary" v-auth="'ontology_axiom_rule_edit'" @click="openRelationDialog('equivalent')">新增等价关系</el-button>
				</el-row>
				<el-table :data="equivalentData" border style="width: 100%">
					<el-table-column type="index" label="#" width="50" />
					<el-table-column prop="typeAName" label="类型A" min-width="150" />
					<el-table-column prop="typeBName" label="类型B" min-width="150" />
					<el-table-column label="操作" width="100">
						<template #default="{ row }">
							<el-button link type="danger" v-auth="'ontology_axiom_rule_edit'" @click="handleDeleteRelation('equivalent', row)">删除</el-button>
						</template>
					</el-table-column>
				</el-table>
			</el-tab-pane>
		</el-tabs>

		<!-- 规则详情抽屉 -->
		<el-drawer v-model="detailVisible" title="规则详情" size="60%">
			<template v-if="detailData">
				<el-descriptions :column="1" border>
					<el-descriptions-item label="名称">{{ detailData.axiomRule.name }}</el-descriptions-item>
					<el-descriptions-item label="代码">{{ detailData.axiomRule.ruleCode }}</el-descriptions-item>
					<el-descriptions-item label="类别">{{ getCategoryLabel(detailData.axiomRule.category) }}</el-descriptions-item>
					<el-descriptions-item label="子类型">{{ detailData.axiomRule.subType }}</el-descriptions-item>
					<el-descriptions-item label="描述">{{ detailData.axiomRule.description || '—' }}</el-descriptions-item>
					<el-descriptions-item label="状态">
						<el-tag :type="getStatusTagType(detailData.axiomRule.status)" size="small">{{ detailData.axiomRule.status }}</el-tag>
						<span v-if="detailData.axiomRule.blockedReason" class="ml6" style="color: #f56c6c;">{{ detailData.axiomRule.blockedReason }}</span>
					</el-descriptions-item>
					<el-descriptions-item label="严重级别">
						<el-tag :type="getSeverityTagType(detailData.axiomRule.severity)" size="small">{{ detailData.axiomRule.severity }}</el-tag>
					</el-descriptions-item>
					<el-descriptions-item label="执行机制">{{ detailData.axiomRule.validationMode }}</el-descriptions-item>
					<el-descriptions-item label="执行器">{{ detailData.axiomRule.executorCode || '—' }}</el-descriptions-item>
					<el-descriptions-item label="模板">{{ detailData.axiomRule.templateCode }} v{{ detailData.axiomRule.templateVersion }}</el-descriptions-item>
					<el-descriptions-item label="来源">{{ detailData.axiomRule.sourceReference || detailData.axiomRule.sourceType }}</el-descriptions-item>
				</el-descriptions>
				<el-divider content-position="left">目标绑定</el-divider>
				<el-table :data="detailData.targets" border size="small">
					<el-table-column prop="bindingRole" label="角色" width="180" />
					<el-table-column prop="targetType" label="类型" width="120" />
					<el-table-column prop="name" label="名称" min-width="150" />
					<el-table-column prop="iri" label="IRI" min-width="200" show-overflow-tooltip />
				</el-table>
				<template v-if="detailData.generatedOwlPreview">
					<el-divider content-position="left">OWL预览</el-divider>
					<el-input :model-value="detailData.generatedOwlPreview" type="textarea" :rows="5" readonly />
				</template>
				<template v-if="detailData.generatedShaclPreview">
					<el-divider content-position="left">SHACL预览</el-divider>
					<el-input :model-value="detailData.generatedShaclPreview" type="textarea" :rows="8" readonly />
				</template>
			</template>
		</el-drawer>

		<!-- 新增/编辑弹窗 -->
		<el-dialog v-model="dialog.visible" :title="dialog.title" width="780px" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-divider content-position="left">基本信息</el-divider>
				<el-form-item label="规则代码" prop="ruleCode">
					<el-input v-model="form.ruleCode" :disabled="isBuiltinEdit || !!form.id" placeholder="如 EXT_MY_RULE" />
				</el-form-item>
				<el-form-item label="规则名称" prop="name">
					<el-input v-model="form.name" :disabled="isBuiltinEdit" placeholder="如 自定义唯一性约束" />
				</el-form-item>
				<el-form-item label="规则模板" prop="templateCode">
					<el-select v-model="form.templateCode" :disabled="isBuiltinEdit || !!form.id" placeholder="选择模板" style="width: 100%" @change="onTemplateChange">
						<el-option v-for="t in templates" :key="t.templateCode" :label="t.templateCode + ' (' + t.subType + ')'" :value="t.templateCode" />
					</el-select>
				</el-form-item>
				<el-form-item label="严重级别" prop="severity">
					<el-select v-model="form.severity" :disabled="isBuiltinEdit" placeholder="选择严重级别" style="width: 100%">
						<el-option label="违规(VIOLATION)" value="VIOLATION" />
						<el-option label="警告(WARNING)" value="WARNING" />
						<el-option label="提示(INFO)" value="INFO" />
					</el-select>
				</el-form-item>
				<el-form-item label="描述" prop="description">
					<el-input v-model="form.description" :disabled="isBuiltinEdit" type="textarea" maxlength="512" show-word-limit />
				</el-form-item>

				<el-divider content-position="left">目标绑定</el-divider>
				<el-form-item v-for="(target, index) in form.targets" :key="index" :label="`目标${Number(index) + 1}`">
					<el-row :gutter="8" style="width: 100%">
						<el-col :span="6">
							<el-input v-model="target.bindingRole" :disabled="isBuiltinEdit" placeholder="角色" />
						</el-col>
						<el-col :span="6">
							<el-select v-model="target.targetType" :disabled="isBuiltinEdit" placeholder="类型">
								<el-option label="实体类型" value="ENTITY_TYPE" />
								<el-option label="数据属性" value="DATA_PROPERTY" />
								<el-option label="对象属性" value="OBJECT_PROPERTY" />
								<el-option label="单位分类" value="UNIT_CATEGORY" />
							</el-select>
						</el-col>
						<el-col :span="9">
							<el-input v-model="target.targetId" :disabled="isBuiltinEdit" placeholder="目标ID" />
						</el-col>
						<el-col :span="3">
							<el-button v-if="!isBuiltinEdit" link type="danger" @click="form.targets.splice(index, 1)">移除</el-button>
						</el-col>
					</el-row>
				</el-form-item>
				<el-form-item v-if="!isBuiltinEdit">
					<el-button type="primary" link @click="addTarget">添加目标</el-button>
				</el-form-item>

				<template v-if="isCustomTemplate(form.templateCode)">
					<el-divider content-position="left">自定义形式化（草稿不可启用）</el-divider>
					<el-form-item label="OWL公理">
						<el-input v-model="form.customOwlAxiom" type="textarea" :rows="4" />
					</el-form-item>
					<el-form-item label="SHACL约束">
						<el-input v-model="form.customShaclShape" type="textarea" :rows="4" />
					</el-form-item>
				</template>

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

		<!-- 关系新增弹窗 -->
		<el-dialog v-model="relationDialog.visible" :title="relationDialog.title" width="500px" destroy-on-close>
			<el-form label-width="100px">
				<el-form-item label="类型A">
					<el-select v-model="relationDialog.typeAId" filterable placeholder="选择实体类型" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="类型B">
					<el-select v-model="relationDialog.typeBId" filterable placeholder="选择实体类型" style="width: 100%">
						<el-option v-for="item in entityTypeOptions" :key="item.id" :label="item.name" :value="item.id" />
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="relationDialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="relationDialog.loading" @click="submitRelation">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyAxiomRule" setup>
import {
	addAxiomRuleObj,
	addDisjointObj,
	addEquivalentObj,
	delAxiomRuleObj,
	delDisjointObj,
	delEquivalentObj,
	fetchAxiomRuleById,
	fetchAxiomRulePage,
	fetchAxiomRuleTemplates,
	fetchDisjointList,
	fetchEquivalentList,
	putAxiomRuleObj,
} from '/@/api/ontology/axiom-rule';
import { fetchEntityTypeList } from '/@/api/ontology/entity-type';
import { useMessage, useMessageBox } from '/@/hooks/message';
import {
	canDelete,
	getSeverityTagType,
	getStatusTagType,
	isBuiltinRule,
	isCustomTemplate,
} from './rule-form';
import type {
	AxiomRuleDetail,
	AxiomRuleForm,
	AxiomRuleSummary,
	AxiomRuleTargetDTO,
	AxiomRuleTemplate,
	EntityTypeRelation,
	OntologyId,
} from '/@/types/ontology/axiom-rule';

const formRef = ref();
const tableLoading = ref(false);
const activeTab = ref('rules');
const pagedData = ref<AxiomRuleSummary[]>([]);
const templates = ref<AxiomRuleTemplate[]>([]);
const detailVisible = ref(false);
const detailData = ref<AxiomRuleDetail>();
const disjointData = ref<EntityTypeRelation[]>([]);
const equivalentData = ref<EntityTypeRelation[]>([]);
const entityTypeOptions = ref<{ id: OntologyId; name: string }[]>([]);

const pagination = reactive({ current: 1, size: 10, total: 0 });

const query = reactive({
	name: '',
	category: '',
	status: '',
});

const dialog = reactive({ visible: false, title: '新增规则', loading: false });
const relationDialog = reactive({ visible: false, title: '', loading: false, type: '', typeAId: '' as OntologyId | undefined, typeBId: '' as OntologyId | undefined });

const createEmptyForm = (): AxiomRuleForm => ({
	ruleCode: '',
	name: '',
	category: 'PROPERTY',
	subType: '',
	description: '',
	severity: 'VIOLATION',
	templateCode: '',
	config: '',
	targets: [],
	customOwlAxiom: '',
	customShaclShape: '',
	sortOrder: 0,
	remarks: '',
});

const form = reactive<AxiomRuleForm>(createEmptyForm());
const isBuiltinEdit = computed(() => isBuiltinRule({ isBuiltin: form.isBuiltin || '0' }));

const rules = {
	ruleCode: [{ required: true, message: '请输入规则代码', trigger: 'blur' }],
	name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
	templateCode: [{ required: true, message: '请选择模板', trigger: 'change' }],
	severity: [{ required: true, message: '请选择严重级别', trigger: 'change' }],
};

const getErrorMessage = (error: unknown, fallback: string) => {
	if (error && typeof error === 'object' && 'msg' in error) return String((error as { msg?: unknown }).msg || fallback);
	return fallback;
};

const getCategoryLabel = (category: string) => {
	switch (category) {
		case 'ENTITY_TYPE': return '实体类型';
		case 'PROPERTY': return '属性';
		case 'RELATION': return '关系';
		default: return category;
	}
};

const loadList = async () => {
	tableLoading.value = true;
	try {
		const params: any = {
			current: pagination.current,
			size: pagination.size,
			name: query.name || undefined,
			category: query.category || undefined,
			status: query.status || undefined,
		};
		const response = await fetchAxiomRulePage(params);
		const pageData = response.data || {};
		pagedData.value = (pageData.records || []) as AxiomRuleSummary[];
		pagination.total = pageData.total || 0;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '加载失败'));
	} finally {
		tableLoading.value = false;
	}
};

const loadTemplates = async () => {
	const response = await fetchAxiomRuleTemplates();
	templates.value = (response.data || []) as AxiomRuleTemplate[];
};

const loadDisjoint = async () => {
	const response = await fetchDisjointList();
	disjointData.value = (response.data || []) as EntityTypeRelation[];
};

const loadEquivalent = async () => {
	const response = await fetchEquivalentList();
	equivalentData.value = (response.data || []) as EntityTypeRelation[];
};

const loadEntityTypes = async () => {
	const response = await fetchEntityTypeList();
	entityTypeOptions.value = (response.data || []) as { id: OntologyId; name: string }[];
};

const onTemplateChange = () => {
	const template = templates.value.find((t: AxiomRuleTemplate) => t.templateCode === form.templateCode);
	if (template) {
		form.category = template.category;
		form.subType = template.subType;
		form.targets = template.bindingRoles.map((r: { role: string; targetType: any; }) => ({
			bindingRole: r.role,
			bindingOrder: 0,
			targetType: r.targetType,
			targetId: '',
		}));
	}
};

const addTarget = () => {
	form.targets.push({ bindingRole: '', bindingOrder: 0, targetType: 'ENTITY_TYPE', targetId: '' });
};

const openDetail = async (row: any) => {
	const response = await fetchAxiomRuleById(row.axiomRule.id);
	detailData.value = response.data as AxiomRuleDetail;
	detailVisible.value = true;
};

const resetForm = (detail?: any) => {
	Object.assign(form, createEmptyForm());
	delete form.id;
	delete form.isBuiltin;
	if (detail) {
		const r = detail.axiomRule || detail;
		form.id = r.id;
		form.isBuiltin = r.isBuiltin;
		form.ruleCode = r.ruleCode;
		form.name = r.name;
		form.category = r.category;
		form.subType = r.subType;
		form.description = r.description || '';
		form.severity = r.severity;
		form.templateCode = r.templateCode;
		form.config = r.configJson || '';
		form.sortOrder = r.sortOrder;
		form.remarks = r.remarks || '';
		form.customOwlAxiom = r.owlAxiom || '';
		form.customShaclShape = r.shaclShape || '';
		form.targets = (detail.targets || []).map((t: any) => ({
			bindingRole: t.bindingRole,
			bindingOrder: t.bindingOrder,
			targetType: t.targetType,
			targetId: t.targetId,
		}));
	}
};

const openDialog = async (row?: any) => {
	dialog.visible = true;
	dialog.title = row ? '编辑规则' : '新增规则';
	if (row && row.axiomRule) {
		const response = await fetchAxiomRuleById(row.axiomRule.id);
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
			const targets: AxiomRuleTargetDTO[] = form.targets.filter((t: AxiomRuleTargetDTO) => t.targetId);
			if (isBuiltinEdit.value) {
				await putAxiomRuleObj({ id: form.id!, sortOrder: form.sortOrder, remarks: form.remarks });
			} else if (form.id) {
				await putAxiomRuleObj({
					id: form.id,
					name: form.name,
					description: form.description || undefined,
					severity: form.severity,
					config: form.config || undefined,
					targets,
					customOwlAxiom: form.customOwlAxiom || undefined,
					customShaclShape: form.customShaclShape || undefined,
					sortOrder: form.sortOrder,
					remarks: form.remarks || undefined,
				});
			} else {
				await addAxiomRuleObj({
					ruleCode: form.ruleCode,
					name: form.name,
					category: form.category,
					subType: form.subType,
					description: form.description || undefined,
					severity: form.severity,
					templateCode: form.templateCode,
					config: form.config || undefined,
					targets,
					customOwlAxiom: form.customOwlAxiom || undefined,
					customShaclShape: form.customShaclShape || undefined,
					sortOrder: form.sortOrder,
					remarks: form.remarks || undefined,
				});
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
		await useMessageBox().confirm('确认删除该公理规则？');
		await delAxiomRuleObj(row.axiomRule.id);
		useMessage().success('删除成功');
		await loadList();
	} catch (error: unknown) {
		if (error !== 'cancel') useMessage().error(getErrorMessage(error, '删除失败'));
	}
};

const openRelationDialog = (type: string) => {
	relationDialog.visible = true;
	relationDialog.type = type;
	relationDialog.title = type === 'disjoint' ? '新增不相交关系' : '新增等价关系';
	relationDialog.typeAId = undefined;
	relationDialog.typeBId = undefined;
};

const submitRelation = async () => {
	if (!relationDialog.typeAId || !relationDialog.typeBId) {
		useMessage().warning('请选择两个实体类型');
		return;
	}
	relationDialog.loading = true;
	try {
		const obj = { entityTypeAId: relationDialog.typeAId, entityTypeBId: relationDialog.typeBId };
		if (relationDialog.type === 'disjoint') {
			await addDisjointObj(obj);
			await loadDisjoint();
		} else {
			await addEquivalentObj(obj);
			await loadEquivalent();
		}
		useMessage().success('操作成功');
		relationDialog.visible = false;
	} catch (error: unknown) {
		useMessage().error(getErrorMessage(error, '操作失败'));
	} finally {
		relationDialog.loading = false;
	}
};

const handleDeleteRelation = async (type: string, row: any) => {
	try {
		await useMessageBox().confirm('确认删除该关系？');
		if (type === 'disjoint') {
			await delDisjointObj(row.typeAId, row.typeBId);
			await loadDisjoint();
		} else {
			await delEquivalentObj(row.typeAId, row.typeBId);
			await loadEquivalent();
		}
		useMessage().success('删除成功');
	} catch (error: unknown) {
		if (error !== 'cancel') useMessage().error(getErrorMessage(error, '删除失败'));
	}
};

onMounted(async () => {
	await Promise.all([loadList(), loadTemplates(), loadDisjoint(), loadEquivalent(), loadEntityTypes()]);
});
</script>

<style scoped>
.ontology-axiom-rule-page {
	height: 100%;
}
.ml6 {
	margin-left: 6px;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
