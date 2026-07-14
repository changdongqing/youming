<template>
	<div>
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input v-model="query.ontologyId" placeholder="工程ID" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-button type="primary" @click="loadData">查询</el-button>
			<el-button type="success" @click="dialog.visible = true">新增 ACL</el-button>
		</el-form>

		<el-table v-loading="loading" :data="tableData" border>
			<el-table-column prop="ontologyId" label="工程ID" width="120" />
			<el-table-column prop="subjectType" label="主体类型" width="100" />
			<el-table-column prop="subjectId" label="主体ID" width="120" />
			<el-table-column prop="accessLevel" label="访问级别" width="100" />
			<el-table-column prop="createTime" label="创建时间" />
			<el-table-column label="操作" width="150">
				<template #default="{ row }">
					<el-button size="small" @click="handleEdit(row)">编辑</el-button>
					<el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
						<template #reference>
							<el-button size="small" type="danger">删除</el-button>
						</template>
					</el-popconfirm>
				</template>
			</el-table-column>
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next"
			@current-change="loadData"
		/>

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="500px">
			<el-form :model="form" label-width="100px">
				<el-form-item label="工程ID">
					<el-input v-model="form.ontologyId" :disabled="!!form.id" />
				</el-form-item>
				<el-form-item label="主体类型">
					<el-select v-model="form.subjectType">
						<el-option label="用户" value="USER" />
						<el-option label="角色" value="ROLE" />
						<el-option label="部门" value="DEPT" />
					</el-select>
				</el-form-item>
				<el-form-item label="主体ID">
					<el-input v-model="form.subjectId" />
				</el-form-item>
				<el-form-item label="访问级别">
					<el-select v-model="form.accessLevel">
						<el-option label="查看" value="VIEW" />
						<el-option label="编辑" value="EDIT" />
						<el-option label="发布" value="PUBLISH" />
						<el-option label="管理" value="ADMIN" />
					</el-select>
				</el-form-item>
			</el-form>
			<template #footer>
				<el-button @click="dialog.visible = false">取消</el-button>
				<el-button type="primary" @click="handleSubmit">保存</el-button>
			</template>
		</el-dialog>
	</div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchProjectAclPage, addProjectAcl, updateProjectAcl, deleteProjectAcl } from '/@/api/ontology/security';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const query = reactive({ ontologyId: '', current: 1, size: 10 });

const dialog = reactive({ visible: false, title: '新增 ACL' });
const form = reactive({ id: '', ontologyId: '', subjectType: 'USER', subjectId: '', accessLevel: 'VIEW' });

const loadData = async () => {
	loading.value = true;
	try {
		const res = await fetchProjectAclPage(query);
		tableData.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const handleEdit = (row: any) => {
	Object.assign(form, row);
	dialog.title = '编辑 ACL';
	dialog.visible = true;
};

const handleSubmit = async () => {
	if (form.id) {
		await updateProjectAcl(form);
	} else {
		await addProjectAcl(form);
	}
	ElMessage.success('保存成功');
	dialog.visible = false;
	Object.assign(form, { id: '', ontologyId: '', subjectType: 'USER', subjectId: '', accessLevel: 'VIEW' });
	loadData();
};

const handleDelete = async (id: string) => {
	await deleteProjectAcl(id);
	ElMessage.success('删除成功');
	loadData();
};

onMounted(loadData);
</script>
