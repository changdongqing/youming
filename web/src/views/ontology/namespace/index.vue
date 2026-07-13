<template>
	<div class="layout-padding ontology-namespace-page">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="mb8" justify="space-between">
				<el-form ref="queryRef" :model="query" inline @keyup.enter="loadList">
					<el-form-item label="前缀" prop="prefix">
						<el-input v-model="query.prefix" clearable placeholder="如 std" style="width: 140px" />
					</el-form-item>
					<el-form-item label="URI" prop="uri">
						<el-input v-model="query.uri" clearable placeholder="如 standard-ontology" style="width: 200px" />
					</el-form-item>
					<el-form-item label="类型" prop="isBuiltin">
						<el-select v-model="query.isBuiltin" clearable placeholder="全部" style="width: 120px">
							<el-option label="核心" value="1" />
							<el-option label="扩展" value="0" />
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button icon="search" type="primary" @click="loadList">查询</el-button>
						<el-button icon="refresh" @click="resetQuery">重置</el-button>
					</el-form-item>
				</el-form>
				<el-button-group>
					<el-button icon="folder-add" type="primary" v-auth="'ontology_namespace_add'" @click="openDialog()">新增</el-button>
					<el-button icon="refresh" @click="loadList">刷新</el-button>
					<el-button icon="copy-document" @click="openIriDialog()">IRI工具</el-button>
				</el-button-group>
			</el-row>

			<el-table v-loading="loading" :data="tableData" border style="width: 100%">
				<el-table-column label="序号" type="index" width="60" />
				<el-table-column label="前缀" prop="prefix" width="120" show-overflow-tooltip />
				<el-table-column label="URI" prop="uri" min-width="280" show-overflow-tooltip />
				<el-table-column label="默认" prop="isDefault" width="80">
					<template #default="scope">
						<el-tag v-if="scope.row.isDefault === '1'" type="success">默认</el-tag>
						<el-tag v-else type="info">否</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="类型" prop="isBuiltin" width="80">
					<template #default="scope">
						<el-tag v-if="scope.row.isBuiltin === '1'">核心</el-tag>
						<el-tag v-else type="warning">扩展</el-tag>
					</template>
				</el-table-column>
				<el-table-column label="描述" prop="description" min-width="160" show-overflow-tooltip />
				<el-table-column label="排序" prop="sortOrder" width="80" />
				<el-table-column label="操作" fixed="right" width="160">
					<template #default="scope">
						<el-button icon="edit-pen" text type="primary" v-auth="'ontology_namespace_edit'" @click="openDialog(scope.row)">编辑</el-button>
						<el-tooltip :content="scope.row.isBuiltin === '1' ? '核心命名空间不可删除' : '删除命名空间'" placement="top">
							<span>
								<el-button
									:disabled="scope.row.isBuiltin === '1'"
									icon="delete"
									text
									type="primary"
									v-auth="'ontology_namespace_del'"
									@click="handleDelete(scope.row)"
								>
									删除
								</el-button>
							</span>
						</el-tooltip>
					</template>
				</el-table-column>
			</el-table>
			<pagination :total="pagination.total" v-model:current="pagination.current" v-model:size="pagination.size" @current-change="handleCurrentChange" @size-change="handleSizeChange" />
		</div>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="560px" destroy-on-close>
			<el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
				<el-form-item label="前缀" prop="prefix">
					<el-input v-model="form.prefix" :disabled="isBuiltinEdit" placeholder="如 ext_medical" />
				</el-form-item>
				<el-form-item label="URI" prop="uri">
					<el-input v-model="form.uri" :disabled="isBuiltinEdit" placeholder="如 http://example.org/ontology/medical#" />
				</el-form-item>
				<el-form-item label="描述" prop="description">
					<el-input v-model="form.description" type="textarea" maxlength="255" show-word-limit />
				</el-form-item>
				<el-form-item label="排序" prop="sortOrder">
					<el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" :loading="dialog.loading" @click="submit">确定</el-button>
			</template>
		</el-dialog>

		<el-dialog v-model="iriDialog.visible" title="IRI工具" width="640px" destroy-on-close>
			<el-tabs v-model="iriDialog.activeTab">
				<el-tab-pane label="IRI生成" name="generate">
					<el-form :model="iriGenerateForm" label-width="100px">
						<el-form-item label="命名空间">
							<el-select v-model="iriGenerateForm.namespaceId" placeholder="请选择命名空间" style="width: 100%">
								<el-option v-for="item in allNamespaces" :key="item.id" :label="item.prefix + '（' + item.uri + '）'" :value="item.id" />
							</el-select>
						</el-form-item>
						<el-form-item label="本地标识符">
							<el-input v-model="iriGenerateForm.localName" placeholder="如 Standard 或 hasValue" />
						</el-form-item>
						<el-form-item label="元素类型">
							<el-select v-model="iriGenerateForm.elementType" style="width: 100%">
								<el-option label="实体类型（首字母大写）" value="ENTITY_TYPE" />
								<el-option label="属性（首字母小写）" value="PROPERTY" />
							</el-select>
						</el-form-item>
						<el-form-item>
							<el-button type="primary" @click="handleGenerateIri">生成</el-button>
						</el-form-item>
						<el-form-item v-if="iriGenerateResult" label="生成结果">
							<el-input v-model="iriGenerateResult" readonly>
								<template #append>
									<el-button icon="copy-document" @click="copyText(iriGenerateResult)" />
								</template>
							</el-input>
						</el-form-item>
					</el-form>
				</el-tab-pane>
				<el-tab-pane label="IRI校验" name="validate">
					<el-form :model="iriValidateForm" label-width="100px">
						<el-form-item label="IRI">
							<el-input v-model="iriValidateForm.iri" placeholder="如 http://example.org/standard-ontology#Standard" />
						</el-form-item>
						<el-form-item>
							<el-button type="primary" @click="handleValidateIri">校验</el-button>
						</el-form-item>
						<el-form-item v-if="iriValidateResult !== null" label="校验结果">
							<el-tag :type="iriValidateResult ? 'success' : 'danger'">
								{{ iriValidateResult ? '可用' : '已被占用' }}
							</el-tag>
						</el-form-item>
					</el-form>
				</el-tab-pane>
			</el-tabs>
		</el-dialog>
	</div>
</template>

<script lang="ts" name="ontologyNamespace" setup>
import { addNamespaceObj, delNamespaceObj, fetchNamespaceList, fetchNamespacePage, generateIri, putNamespaceObj, validateIri } from '/@/api/ontology/namespace';
import { useMessage, useMessageBox } from '/@/hooks/message';

const formRef = ref();
const queryRef = ref();

const loading = ref(false);
const tableData = ref<any[]>([]);
const allNamespaces = ref<any[]>([]);

const query = reactive({
	prefix: '',
	uri: '',
	isBuiltin: '',
});

const pagination = reactive({
	current: 1,
	size: 10,
	total: 0,
});

const dialog = reactive({
	visible: false,
	title: '新增命名空间',
	loading: false,
});

const form = reactive<any>({});

const isBuiltinEdit = computed(() => form.id && form.isBuiltin === '1');

const rules = {
	prefix: [{ required: true, message: '请输入前缀', trigger: 'blur' }],
	uri: [{ required: true, message: '请输入命名空间URI', trigger: 'blur' }],
};

const resetForm = (row?: any) => {
	Object.keys(form).forEach((key) => delete form[key]);
	Object.assign(form, row ? { ...row } : { prefix: '', uri: '', description: '', sortOrder: 0 });
};

const loadList = async () => {
	loading.value = true;
	try {
		const res = await fetchNamespacePage({
			current: pagination.current,
			size: pagination.size,
			...query,
		});
		tableData.value = res.data?.records || [];
		pagination.total = Number(res.data?.total || 0);
	} finally {
		loading.value = false;
	}
};

const loadAllNamespaces = async () => {
	const res = await fetchNamespaceList();
	allNamespaces.value = res.data || [];
};

const resetQuery = () => {
	queryRef.value?.resetFields();
	pagination.current = 1;
	loadList();
};

const handleCurrentChange = (current: number) => {
	pagination.current = current;
	loadList();
};

const handleSizeChange = (size: number) => {
	pagination.size = size;
	pagination.current = 1;
	loadList();
};

const openDialog = (row?: any) => {
	resetForm(row);
	dialog.title = row ? '编辑命名空间' : '新增命名空间';
	dialog.visible = true;
};

const submit = async () => {
	await formRef.value?.validate();
	dialog.loading = true;
	try {
		if (form.id) {
			await putNamespaceObj(form);
		} else {
			await addNamespaceObj(form);
		}
		useMessage().success('保存成功');
		dialog.visible = false;
		await loadList();
	} catch (err: any) {
		useMessage().error(err.msg || '保存失败');
	} finally {
		dialog.loading = false;
	}
};

const handleDelete = async (row: any) => {
	try {
		await useMessageBox().confirm('确认删除该命名空间吗？');
	} catch {
		return;
	}
	try {
		await delNamespaceObj(row.id);
		useMessage().success('删除成功');
		await loadList();
	} catch (err: any) {
		useMessage().error(err.msg || '删除失败');
	}
};

// IRI 工具
const iriDialog = reactive({
	visible: false,
	activeTab: 'generate',
});

const iriGenerateForm = reactive({
	namespaceId: '' as string | number,
	localName: '',
	elementType: 'ENTITY_TYPE',
});

const iriGenerateResult = ref('');

const iriValidateForm = reactive({
	iri: '',
});

const iriValidateResult = ref<boolean | null>(null);

const openIriDialog = () => {
	iriDialog.visible = true;
};

const handleGenerateIri = async () => {
	if (!iriGenerateForm.namespaceId) {
		useMessage().warning('请选择命名空间');
		return;
	}
	if (!iriGenerateForm.localName) {
		useMessage().warning('请输入本地标识符');
		return;
	}
	try {
		const res = await generateIri({
			namespaceId: iriGenerateForm.namespaceId,
			localName: iriGenerateForm.localName,
			elementType: iriGenerateForm.elementType,
		});
		iriGenerateResult.value = res.data || '';
	} catch (err: any) {
		useMessage().error(err.msg || '生成失败');
	}
};

const handleValidateIri = async () => {
	if (!iriValidateForm.iri) {
		useMessage().warning('请输入IRI');
		return;
	}
	try {
		const res = await validateIri(iriValidateForm.iri);
		iriValidateResult.value = res.data;
	} catch (err: any) {
		useMessage().error(err.msg || '校验失败');
	}
};

const copyText = (text: string) => {
	navigator.clipboard.writeText(text);
	useMessage().success('已复制');
};

onMounted(() => {
	loadList();
	loadAllNamespaces();
});
</script>

<style scoped>
.ontology-namespace-page {
	height: 100%;
}
.mb8 {
	margin-bottom: 8px;
}
</style>
