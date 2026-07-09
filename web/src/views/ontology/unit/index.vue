<template>
	<div ref="pageRef" class="layout-padding ontology-unit-page">
		<splitpanes @resized="handleResized">
			<pane :size="leftPaneSize" :min="10" :max="50">
				<div class="layout-padding-auto layout-padding-view category-panel">
					<el-row class="mb8" justify="space-between">
						<el-button icon="folder-add" type="primary" v-auth="'ontology_unit_add'" @click="openCategoryDialog()">新增分类</el-button>
						<el-button icon="refresh" @click="loadCategories">刷新</el-button>
					</el-row>
					<el-input v-model="categoryKeyword" clearable placeholder="搜索分类编码/名称" prefix-icon="Search" class="mb8" />
					<el-scrollbar>
						<el-tree
							ref="categoryTreeRef"
							:data="filteredCategories"
							node-key="id"
							:props="categoryTreeProps"
							:expand-on-click-node="false"
							:highlight-current="true"
							@node-click="handleCategoryClick"
						>
								<template #default="{ data }">
									<span class="custom-tree-node">
										<span class="custom-tree-node-info">
											<span class="label">{{ data.categoryName }}</span>
											<el-tag v-if="data.isBuiltin === '1'" size="small" class="ml6">内置</el-tag>
											<span class="code">{{ data.categoryCode }} / {{ data.baseUnitSymbol }}</span>
										</span>
									<span class="do">
										<el-button-group>
											<el-button icon="edit" size="small" v-auth="'ontology_unit_edit'" @click.stop="openCategoryDialog(data)" />
											<el-tooltip :content="data.isBuiltin === '1' ? '内置分类不可删除' : '删除分类'" placement="top">
												<span>
													<el-button
														:disabled="data.isBuiltin === '1'"
														icon="delete"
														size="small"
														v-auth="'ontology_unit_del'"
														@click.stop="handleDeleteCategory(data)"
													/>
												</span>
											</el-tooltip>
										</el-button-group>
									</span>
								</span>
							</template>
						</el-tree>
					</el-scrollbar>
				</div>
			</pane>
			<pane>
				<div class="layout-padding-auto layout-padding-view unit-panel">
					<el-row class="mb8" justify="space-between">
						<div class="selected-title">
							{{ selectedCategory?.categoryName || '请选择单位分类' }}
							<span v-if="selectedCategory" class="selected-code">{{ selectedCategory.categoryCode }}</span>
						</div>
						<el-button :disabled="!selectedCategory" icon="folder-add" type="primary" v-auth="'ontology_unit_add'" @click="openUnitDialog()">
							新增单位
						</el-button>
					</el-row>

					<el-form ref="queryRef" :model="unitQuery" inline @keyup.enter="loadUnits">
						<el-form-item label="单位编码" prop="unitCode">
							<el-input v-model="unitQuery.unitCode" clearable placeholder="如 kilowatt" style="width: 160px" />
						</el-form-item>
						<el-form-item label="符号" prop="unitSymbol">
							<el-input v-model="unitQuery.unitSymbol" clearable placeholder="如 kW" style="width: 120px" />
						</el-form-item>
						<el-form-item label="名称" prop="unitName">
							<el-input v-model="unitQuery.unitName" clearable placeholder="如 千瓦" style="width: 160px" />
						</el-form-item>
						<el-form-item>
							<el-button icon="search" type="primary" @click="loadUnits">查询</el-button>
							<el-button icon="refresh" @click="resetUnitQuery">重置</el-button>
						</el-form-item>
					</el-form>

					<el-table v-loading="unitLoading" :data="units" border style="width: 100%">
						<el-table-column label="序号" type="index" width="60" />
						<el-table-column label="编码" prop="unitCode" min-width="130" show-overflow-tooltip />
						<el-table-column label="符号" prop="unitSymbol" width="100" show-overflow-tooltip />
						<el-table-column label="名称" prop="unitName" min-width="120" show-overflow-tooltip />
						<el-table-column label="基准" prop="isBaseUnit" width="80">
							<template #default="scope">
								<el-tag v-if="scope.row.isBaseUnit === '1'" type="success">是</el-tag>
								<el-tag v-else type="info">否</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="内置" prop="isBuiltin" width="80">
							<template #default="scope">
								<el-tag v-if="scope.row.isBuiltin === '1'">内置</el-tag>
								<el-tag v-else type="warning">扩展</el-tag>
							</template>
						</el-table-column>
						<el-table-column label="换算系数" prop="factor" width="120" show-overflow-tooltip />
						<el-table-column label="偏移" prop="offsetValue" width="100" show-overflow-tooltip />
						<el-table-column label="命名空间" prop="namespaceId" min-width="120" show-overflow-tooltip>
							<template #default="scope">
								{{ getNamespaceLabel(scope.row.namespaceId) }}
							</template>
						</el-table-column>
						<el-table-column label="排序" prop="sortOrder" width="80" />
						<el-table-column label="操作" fixed="right" width="160">
							<template #default="scope">
								<el-button icon="edit-pen" text type="primary" v-auth="'ontology_unit_edit'" @click="openUnitDialog(scope.row)">编辑</el-button>
								<el-tooltip :content="scope.row.isBuiltin === '1' ? '内置单位不可删除' : '删除单位'" placement="top">
									<span>
										<el-button
											:disabled="scope.row.isBuiltin === '1'"
											icon="delete"
											text
											type="primary"
											v-auth="'ontology_unit_del'"
											@click="handleDeleteUnit(scope.row)"
										>
											删除
										</el-button>
									</span>
								</el-tooltip>
							</template>
						</el-table-column>
					</el-table>
					<pagination
						v-bind="pagination"
						@current-change="handleCurrentChange"
						@size-change="handleSizeChange"
					/>
				</div>
			</pane>
		</splitpanes>

		<el-dialog v-model="categoryDialog.visible" :title="categoryDialog.title" width="520px" destroy-on-close>
			<el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="110px">
				<el-form-item label="分类编码" prop="categoryCode">
					<el-input v-model="categoryForm.categoryCode" :disabled="isBuiltinCategoryEdit" placeholder="如 voltage" />
				</el-form-item>
				<el-form-item label="分类名称" prop="categoryName">
					<el-input v-model="categoryForm.categoryName" placeholder="如 电压" />
				</el-form-item>
				<el-form-item label="基准单位符号" prop="baseUnitSymbol">
					<el-input v-model="categoryForm.baseUnitSymbol" :disabled="isBuiltinCategoryEdit" placeholder="如 V" />
				</el-form-item>
				<el-form-item label="排序" prop="sortOrder">
					<el-input-number v-model="categoryForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="备注" prop="remarks">
					<el-input v-model="categoryForm.remarks" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="categoryDialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="categoryDialog.loading" @click="submitCategory">确定</el-button>
			</template>
		</el-dialog>

		<el-dialog v-model="unitDialog.visible" :title="unitDialog.title" width="640px" destroy-on-close>
			<el-form ref="unitFormRef" :model="unitForm" :rules="unitRules" label-width="120px">
				<el-form-item label="单位分类" prop="categoryId">
					<el-select v-model="unitForm.categoryId" :disabled="isBuiltinUnitEdit" placeholder="请选择单位分类" style="width: 100%">
						<el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="单位编码" prop="unitCode">
					<el-input v-model="unitForm.unitCode" :disabled="isBuiltinUnitEdit" placeholder="如 kilowatt" />
				</el-form-item>
				<el-form-item label="单位符号" prop="unitSymbol">
					<el-input v-model="unitForm.unitSymbol" :disabled="isBuiltinUnitEdit" placeholder="如 kW" />
				</el-form-item>
				<el-form-item label="单位名称" prop="unitName">
					<el-input v-model="unitForm.unitName" placeholder="如 千瓦" />
				</el-form-item>
				<el-form-item label="是否基准单位" prop="isBaseUnit">
					<el-switch v-model="unitForm.isBaseUnit" :disabled="isBuiltinUnitEdit" active-text="是" active-value="1" inactive-text="否" inactive-value="0" />
				</el-form-item>
				<el-form-item label="换算系数" prop="factor">
					<el-input-number v-model="unitForm.factor" :disabled="isBuiltinUnitEdit" :precision="12" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="换算偏移" prop="offsetValue">
					<el-input-number v-model="unitForm.offsetValue" :disabled="isBuiltinUnitEdit" :precision="12" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="命名空间" prop="namespaceId">
					<el-select v-model="unitForm.namespaceId" :disabled="isBuiltinUnitEdit" placeholder="请选择命名空间" filterable style="width: 100%">
						<el-option v-for="item in namespaces" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
					</el-select>
				</el-form-item>
				<el-form-item label="排序" prop="sortOrder">
					<el-input-number v-model="unitForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
				</el-form-item>
				<el-form-item label="备注" prop="remarks">
					<el-input v-model="unitForm.remarks" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="unitDialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="unitDialog.loading" @click="submitUnit">确定</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyUnit" setup>
import {
	addCategoryObj,
	addUnitObj,
	delCategoryObj,
	delUnitObj,
	fetchCategoryList,
	fetchUnitPage,
	putCategoryObj,
	putUnitObj,
} from '/@/api/ontology/unit';
import { fetchNamespaceList } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';

const categoryTreeRef = ref();
const categoryFormRef = ref();
const unitFormRef = ref();
const queryRef = ref();

const namespaces = ref<any[]>([]);

const getNamespaceLabel = (id: any) => {
	const ns = namespaces.value.find((item) => item.id === id);
	return ns ? ns.prefix : '';
};

const pageRef = ref();
const LEFT_DEFAULT_PX = 300;
const leftPaneSize = ref(28);
const userResized = ref(false);
let resizeObserver: ResizeObserver | null = null;

const recalcLeftPane = (width: number) => {
	if (userResized.value || !width) return;
	leftPaneSize.value = Math.min(50, Math.max(10, (LEFT_DEFAULT_PX / width) * 100));
};

const handleResized = () => {
	userResized.value = true;
};

const categories = ref<any[]>([]);
const selectedCategory = ref<any>();
const categoryKeyword = ref('');
const unitLoading = ref(false);
const units = ref<any[]>([]);

const categoryTreeProps = {
	label: 'categoryName',
};

const unitQuery = reactive({
	unitCode: '',
	unitSymbol: '',
	unitName: '',
});

const pagination = reactive({
	current: 1,
	size: 10,
	total: 0,
});

const categoryDialog = reactive({
	visible: false,
	title: '新增单位分类',
	loading: false,
});

const unitDialog = reactive({
	visible: false,
	title: '新增单位',
	loading: false,
});

const categoryForm = reactive<any>({});
const unitForm = reactive<any>({});

const categoryRules = {
	categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
	categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
	baseUnitSymbol: [{ required: true, message: '请输入基准单位符号', trigger: 'blur' }],
};

const validateNamespace = (_rule: any, value: any, callback: any) => {
	if (unitForm.isBuiltin === '1' || value) {
		callback();
		return;
	}
	callback(new Error('扩展单位必须选择命名空间'));
};

const unitRules = {
	categoryId: [{ required: true, message: '请选择单位分类', trigger: 'change' }],
	unitCode: [{ required: true, message: '请输入单位编码', trigger: 'blur' }],
	unitSymbol: [{ required: true, message: '请输入单位符号', trigger: 'blur' }],
	unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
	namespaceId: [{ validator: validateNamespace, trigger: 'change' }],
};

const filteredCategories = computed(() => {
	const keyword = categoryKeyword.value.trim().toLowerCase();
	if (!keyword) {
		return categories.value;
	}
	return categories.value.filter((item) => {
		return item.categoryName?.toLowerCase().includes(keyword) || item.categoryCode?.toLowerCase().includes(keyword);
	});
});

const isBuiltinCategoryEdit = computed(() => categoryForm.id && categoryForm.isBuiltin === '1');
const isBuiltinUnitEdit = computed(() => unitForm.id && unitForm.isBuiltin === '1');

const resetCategoryForm = (row?: any) => {
	Object.keys(categoryForm).forEach((key) => delete categoryForm[key]);
	Object.assign(categoryForm, row ? { ...row } : { categoryCode: '', categoryName: '', baseUnitSymbol: '', sortOrder: 0, remarks: '' });
};

	const resetUnitForm = (row?: any) => {
		Object.keys(unitForm).forEach((key) => delete unitForm[key]);
		const defaultNsId = namespaces.value.find((item) => item.prefix === 'std')?.id;
		Object.assign(
			unitForm,
			row
				? { ...row }
				: {
						categoryId: selectedCategory.value?.id,
						unitCode: '',
						unitSymbol: '',
						unitName: '',
						isBaseUnit: '0',
						factor: undefined,
						offsetValue: undefined,
						sortOrder: 0,
						namespaceId: defaultNsId,
						remarks: '',
					},
		);
	};

	const loadNamespaces = async () => {
		const res = await fetchNamespaceList();
		namespaces.value = res.data || [];
	};

	const loadCategories = async () => {
	const res = await fetchCategoryList();
	categories.value = res.data || [];
	if (!selectedCategory.value && categories.value.length > 0) {
		selectedCategory.value = categories.value[0];
	}
	if (selectedCategory.value) {
		const fresh = categories.value.find((item) => item.id === selectedCategory.value.id);
		selectedCategory.value = fresh || categories.value[0];
		nextTick(() => categoryTreeRef.value?.setCurrentKey(selectedCategory.value?.id));
	}
	await loadUnits();
};

const loadUnits = async () => {
	if (!selectedCategory.value) {
		units.value = [];
		pagination.total = 0;
		return;
	}
	unitLoading.value = true;
	try {
		const res = await fetchUnitPage({
			current: pagination.current,
			size: pagination.size,
			categoryId: selectedCategory.value.id,
			...unitQuery,
		});
		units.value = res.data?.records || [];
		pagination.total = Number(res.data?.total || 0);
	} finally {
		unitLoading.value = false;
	}
};

const handleCategoryClick = async (data: any) => {
	selectedCategory.value = data;
	pagination.current = 1;
	await loadUnits();
};

const resetUnitQuery = () => {
	queryRef.value?.resetFields();
	pagination.current = 1;
	loadUnits();
};

const handleCurrentChange = (current: number) => {
	pagination.current = current;
	loadUnits();
};

const handleSizeChange = (size: number) => {
	pagination.size = size;
	pagination.current = 1;
	loadUnits();
};

const openCategoryDialog = (row?: any) => {
	resetCategoryForm(row);
	categoryDialog.title = row ? '编辑单位分类' : '新增单位分类';
	categoryDialog.visible = true;
};

const openUnitDialog = (row?: any) => {
	resetUnitForm(row);
	unitDialog.title = row ? '编辑单位' : '新增单位';
	unitDialog.visible = true;
};

const submitCategory = async () => {
	await categoryFormRef.value?.validate();
	categoryDialog.loading = true;
	try {
		if (categoryForm.id) {
			await putCategoryObj(categoryForm);
		} else {
			await addCategoryObj(categoryForm);
		}
		useMessage().success('保存成功');
		categoryDialog.visible = false;
		await loadCategories();
	} catch (err: any) {
		useMessage().error(err.msg || '保存失败');
	} finally {
		categoryDialog.loading = false;
	}
};

const submitUnit = async () => {
	await unitFormRef.value?.validate();
	unitDialog.loading = true;
	try {
		if (unitForm.id) {
			await putUnitObj(unitForm);
		} else {
			await addUnitObj(unitForm);
		}
		useMessage().success('保存成功');
		unitDialog.visible = false;
		await loadUnits();
	} catch (err: any) {
		useMessage().error(err.msg || '保存失败');
	} finally {
		unitDialog.loading = false;
	}
};

const handleDeleteCategory = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该单位分类吗？');
	} catch {
		return;
	}
	try {
		await delCategoryObj(row.id);
		useMessage().success('删除成功');
		selectedCategory.value = undefined;
		await loadCategories();
	} catch (err: any) {
		useMessage().error(err.msg || '删除失败');
	}
};

const handleDeleteUnit = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该单位吗？');
	} catch {
		return;
	}
	try {
		await delUnitObj(row.id);
		useMessage().success('删除成功');
		await loadUnits();
	} catch (err: any) {
		useMessage().error(err.msg || '删除失败');
	}
};

	onMounted(() => {
		loadNamespaces();
		loadCategories();
	if (pageRef.value) {
		recalcLeftPane(pageRef.value.clientWidth);
		resizeObserver = new ResizeObserver((entries) => {
			recalcLeftPane(entries[0].contentRect.width);
		});
		resizeObserver.observe(pageRef.value);
	}
});

onUnmounted(() => {
	resizeObserver?.disconnect();
	resizeObserver = null;
});
</script>

<style scoped>
.ontology-unit-page {
	height: 100%;
}
.category-panel,
.unit-panel {
	height: 100%;
}
.category-panel :deep(.el-tree-node__content) {
	height: 42px;
}
.custom-tree-node {
	display: flex;
	flex: 1;
	align-items: center;
	justify-content: space-between;
	font-size: 14px;
	padding-right: 16px;
	min-width: 0;
}
.custom-tree-node .label {
	font-weight: 500;
	white-space: nowrap;
}
.custom-tree-node-info {
	display: flex;
	align-items: center;
	flex-wrap: nowrap;
	gap: 6px;
	min-width: 0;
	overflow: hidden;
}
.custom-tree-node .code {
	font-size: 12px;
	color: #999;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}
.custom-tree-node .do {
	display: none;
	margin-left: 8px;
}
.custom-tree-node:hover .do {
	display: inline-block;
}
.selected-title {
	font-size: 16px;
	font-weight: 600;
	line-height: 32px;
}
.selected-code {
	font-size: 12px;
	font-weight: 400;
	color: #999;
	margin-left: 8px;
}
.ml6 {
	margin-left: 6px;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
