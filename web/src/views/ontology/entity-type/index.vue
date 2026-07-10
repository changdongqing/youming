<template>
	<div ref="pageRef" class="layout-padding ontology-entity-type-page">
		<splitpanes @resized="handleResized">
			<pane :size="leftPaneSize" :min="15" :max="50">
				<div class="layout-padding-auto layout-padding-view tree-panel">
					<el-row class="mb8" justify="space-between">
						<el-button icon="folder-add" type="primary" v-auth="'ontology_entity_type_add'" @click="openDialog()">新增类型</el-button>
						<el-button icon="refresh" @click="loadTree">刷新</el-button>
					</el-row>
					<el-input v-model="treeKeyword" clearable placeholder="搜索名称/标签" prefix-icon="Search" class="mb8" />
					<el-scrollbar>
						<el-tree
							ref="treeRef"
							:data="filteredTree"
							node-key="id"
							:props="treeProps"
							:expand-on-click-node="false"
							:highlight-current="true"
							:default-expand-all="false"
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
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div class="layout-padding-auto layout-padding-view detail-panel">
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
							<el-descriptions-item label="IRI">{{ selectedDetail.entityType.iri }}</el-descriptions-item>
							<el-descriptions-item label="英文名称">{{ selectedDetail.entityType.name }}</el-descriptions-item>
							<el-descriptions-item label="标签">{{ selectedLabel }}</el-descriptions-item>
							<el-descriptions-item label="定义">{{ selectedDetail.entityType.definition || '—' }}</el-descriptions-item>
							<el-descriptions-item label="属性集">
								<span class="text-muted">数据属性模块上线后填充</span>
							</el-descriptions-item>
							<el-descriptions-item label="父类">
								<span v-if="selectedDetail.parents && selectedDetail.parents.length">
									<el-tag v-for="p in selectedDetail.parents" :key="p.id" class="mr6" @click="handleNodeClick(p as any)">{{ p.name }}</el-tag>
								</span>
								<span v-else class="text-muted">—（根类型）</span>
							</el-descriptions-item>
							<el-descriptions-item label="子类">
								<span v-if="selectedDetail.children && selectedDetail.children.length">
									<el-tag v-for="c in selectedDetail.children" :key="c.id" type="info" class="mr6" @click="handleNodeClick(c as any)">{{ c.name }}</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="等价类">
								<span v-if="selectedDetail.equivalents && selectedDetail.equivalents.length">
									<el-tag v-for="e in selectedDetail.equivalents" :key="e.id" type="success" class="mr6">{{ e.name }}</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="不相交类">
								<span v-if="selectedDetail.disjoints && selectedDetail.disjoints.length">
									<el-tag v-for="d in selectedDetail.disjoints" :key="d.id" type="danger" class="mr6">{{ d.name }}</el-tag>
								</span>
								<span v-else class="text-muted">—</span>
							</el-descriptions-item>
							<el-descriptions-item label="命名空间">{{ selectedDetail.namespace?.prefix }}（{{ selectedDetail.namespace?.uri }}）</el-descriptions-item>
							<el-descriptions-item label="排序">{{ selectedDetail.entityType.sortOrder }}</el-descriptions-item>
						</el-descriptions>
					</template>
				</div>
			</pane>
		</splitpanes>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="640px" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="form.namespaceId" :disabled="isBuiltinEdit" placeholder="请选择命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaces" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="英文名称" prop="name">
					<el-input v-model="form.name" :disabled="isBuiltinEdit" placeholder="如 Standard" />
				</el-form-item>
				<el-form-item label="IRI" prop="iri">
					<el-input v-model="form.iri" :disabled="isBuiltinEdit" placeholder="可由命名空间+名称自动生成">
						<template #append>
							<el-button :disabled="isBuiltinEdit" @click="generateIri">自动生成</el-button>
						</template>
					</el-input>
				</el-form-item>
				<el-form-item label="中文标签" prop="label">
					<el-input v-model="form.label" placeholder="如 标准实体" />
				</el-form-item>
				<el-form-item label="定义" prop="definition">
					<el-input v-model="form.definition" type="textarea" maxlength="512" show-word-limit />
				</el-form-item>
				<el-form-item label="是否抽象类" prop="isAbstract">
					<el-switch v-model="form.isAbstract" :disabled="isBuiltinEdit" active-text="是" active-value="1" inactive-text="否" inactive-value="0" />
				</el-form-item>
				<el-form-item label="父类" prop="parentIds">
					<el-select v-model="form.parentIds" :disabled="isBuiltinEdit" multiple filterable placeholder="可多选，支持多继承" style="width: 100%">
						<el-option v-for="item in allTypes" :key="item.id" :label="item.name + '（' + item.iri + '）'" :value="item.id" />
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
				<el-button type="primary" :loading="dialog.loading" @click="submit">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyEntityType" setup>
import { addEntityTypeObj, delEntityTypeObj, fetchEntityTypeById, fetchEntityTypeList, fetchEntityTypeTree, putEntityTypeObj } from '/@/api/ontology/entity-type';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';

const pageRef = ref();
const treeRef = ref();
const formRef = ref();
const LEFT_DEFAULT_PX = 360;
const leftPaneSize = ref(30);
const userResized = ref(false);

const handleResized = () => {
	userResized.value = true;
};

const recalcLeftPane = (width: number) => {
	if (userResized.value || !width) return;
	leftPaneSize.value = Math.min(50, Math.max(15, (LEFT_DEFAULT_PX / width) * 100));
};

const treeData = ref<any[]>([]);
const allTypes = ref<any[]>([]);
const namespaces = ref<any[]>([]);
const treeKeyword = ref('');
const selectedDetail = ref<any>();
const selectedId = ref<number>();

const treeProps = { label: 'label' };

const dialog = reactive({
	visible: false,
	title: '新增实体类型',
	loading: false,
});

const form = reactive<any>({});
const isBuiltinEdit = computed(() => form.id && form.isBuiltin === '1');

const rules = {
	namespaceId: [{ required: true, message: '请选择命名空间', trigger: 'change' }],
	name: [{ required: true, message: '请输入英文名称', trigger: 'blur' }],
};

const selectedLabel = computed(() => {
	if (!selectedDetail.value) return '';
	const labels = selectedDetail.value.labels;
	if (labels && labels.length > 0) {
		const zh = labels.find((l: any) => l.locale === 'zh');
		return zh ? zh.label : labels[0].label;
	}
	return selectedDetail.value.entityType.name;
});

const filteredTree = computed(() => {
	const keyword = treeKeyword.value.trim().toLowerCase();
	if (!keyword) return treeData.value;
	const filterNodes = (nodes: any[]): any[] => {
		return nodes
			.map((node) => {
				const children = filterNodes(node.children || []);
				const match = node.label?.toLowerCase().includes(keyword) || node.name?.toLowerCase().includes(keyword);
				if (match || children.length > 0) {
					return { ...node, children };
				}
				return null;
			})
			.filter(Boolean);
	};
	return filterNodes(treeData.value);
});

const loadTree = async () => {
	const res = await fetchEntityTypeTree();
	treeData.value = res.data || [];
};

const loadAllTypes = async () => {
	const res = await fetchEntityTypeList();
	allTypes.value = res.data || [];
};

const loadNamespaces = async () => {
	const res = await fetchNamespaceList();
	namespaces.value = res.data || [];
};

const handleNodeClick = async (data: any) => {
	const id = data.id;
	selectedId.value = id;
	const res = await fetchEntityTypeById(id);
	selectedDetail.value = res.data;
};

const resetForm = (row?: any) => {
	Object.keys(form).forEach((key) => delete form[key]);
	if (row) {
		const detail = selectedDetail.value;
		const label = detail?.labels?.find((l: any) => l.locale === 'zh')?.label || '';
		Object.assign(form, {
			...row,
			label,
			parentIds: detail?.parentIds || [],
		});
	} else {
		const defaultNs = namespaces.value.find((n) => n.prefix === 'std');
		Object.assign(form, {
			namespaceId: defaultNs?.id,
			name: '',
			iri: '',
			label: '',
			definition: '',
			isAbstract: '0',
			parentIds: [],
			sortOrder: 0,
			remarks: '',
		});
	}
};

const openDialog = (row?: any) => {
	resetForm(row);
	dialog.title = row ? '编辑实体类型' : '新增实体类型';
	dialog.visible = true;
};

const generateIri = () => {
	if (!form.namespaceId || !form.name) {
		useMessage().warning('请先选择命名空间并输入英文名称');
		return;
	}
	const ns = namespaces.value.find((n) => n.id === form.namespaceId);
	if (ns) {
		form.iri = ns.uri + form.name;
	}
};

const submit = async () => {
	await formRef.value?.validate();
	dialog.loading = true;
	try {
		if (form.id) {
			await putEntityTypeObj(form);
		} else {
			await addEntityTypeObj(form);
		}
		useMessage().success('保存成功');
		dialog.visible = false;
		await loadTree();
		await loadAllTypes();
		if (selectedId.value) {
			await handleNodeClick({ id: selectedId.value });
		}
	} catch (err: any) {
		useMessage().error(err.msg || '保存失败');
	} finally {
		dialog.loading = false;
	}
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该实体类型吗？');
	} catch {
		return;
	}
	try {
		await delEntityTypeObj(row.id);
		useMessage().success('删除成功');
		selectedDetail.value = undefined;
		selectedId.value = undefined;
		await loadTree();
		await loadAllTypes();
	} catch (err: any) {
		useMessage().error(err.msg || '删除失败');
	}
};

onMounted(() => {
	loadTree();
	loadAllTypes();
	loadNamespaces();
	if (pageRef.value) {
		recalcLeftPane(pageRef.value.clientWidth);
		const ro = new ResizeObserver((entries) => {
			recalcLeftPane(entries[0].contentRect.width);
		});
		ro.observe(pageRef.value);
		onUnmounted(() => ro.disconnect());
	}
});
</script>

<style scoped>
.ontology-entity-type-page {
	height: 100%;
}
.tree-panel,
.detail-panel {
	height: 100%;
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
