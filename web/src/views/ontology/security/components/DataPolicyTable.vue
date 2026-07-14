<template>
	<div>
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input v-model="query.ontologyId" placeholder="工程ID" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-button type="primary" @click="loadData">查询</el-button>
			<el-button type="success" @click="openAdd">新增策略</el-button>
		</el-form>

		<el-table v-loading="loading" :data="tableData" border>
			<el-table-column prop="ontologyId" label="工程ID" width="100" />
			<el-table-column prop="subjectType" label="主体类型" width="90" />
			<el-table-column prop="subjectId" label="主体ID" width="100" />
			<el-table-column prop="resourceType" label="资源类型" width="130" />
			<el-table-column prop="resourceId" label="资源ID" width="100" />
			<el-table-column prop="action" label="动作" width="80" />
			<el-table-column prop="effect" label="效果" width="80">
				<template #default="{ row }">
					<el-tag :type="effectTag(row.effect)">{{ row.effect }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="maskType" label="脱敏类型" width="100" />
			<el-table-column prop="description" label="描述" />
			<el-table-column label="操作" width="100">
				<template #default="{ row }">
					<el-button size="small" @click="handleEdit(row)">编辑</el-button>
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

		<el-dialog v-model="dialog.visible" :title="dialog.title" width="600px">
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
				<el-form-item label="资源类型">
					<el-select v-model="form.resourceType">
						<el-option label="实体类型" value="ENTITY_TYPE" />
						<el-option label="数据属性" value="DATA_PROPERTY" />
						<el-option label="对象属性" value="OBJECT_PROPERTY" />
					</el-select>
				</el-form-item>
				<el-form-item label="资源ID">
					<el-input v-model="form.resourceId" />
				</el-form-item>
				<el-form-item label="动作">
					<el-select v-model="form.action">
						<el-option label="查看" value="VIEW" />
						<el-option label="编辑" value="EDIT" />
						<el-option label="导出" value="EXPORT" />
						<el-option label="SPARQL" value="SPARQL" />
					</el-select>
				</el-form-item>
				<el-form-item label="效果">
					<el-select v-model="form.effect" @change="onEffectChange">
						<el-option label="允许" value="ALLOW" />
						<el-option label="脱敏" value="MASK" />
						<el-option label="拒绝" value="DENY" />
					</el-select>
				</el-form-item>
				<el-form-item v-if="form.effect === 'MASK'" label="脱敏类型">
					<el-select v-model="form.maskType">
						<el-option label="手机号" value="PHONE" />
						<el-option label="证件号" value="ID_CARD" />
						<el-option label="邮箱" value="EMAIL" />
						<el-option label="IP" value="IP" />
						<el-option label="姓名" value="NAME" />
						<el-option label="固定***" value="FIXED" />
						<el-option label="部分" value="PARTIAL" />
					</el-select>
				</el-form-item>
				<el-form-item label="描述">
					<el-input v-model="form.description" type="textarea" />
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
import { fetchPoliciesPage, addPolicy, updatePolicy } from '/@/api/ontology/security';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const query = reactive({ ontologyId: '', current: 1, size: 10 });

const dialog = reactive({ visible: false, title: '新增策略' });
const defaultForm = () => ({
	id: '', ontologyId: '', subjectType: 'USER', subjectId: '',
	resourceType: 'ENTITY_TYPE', resourceId: '', action: 'VIEW',
	effect: 'ALLOW', maskType: '', description: '',
});
const form = reactive(defaultForm());

const effectTag = (effect: string) => {
	if (effect === 'DENY') return 'danger';
	if (effect === 'MASK') return 'warning';
	return 'success';
};

const onEffectChange = () => {
	if (form.effect !== 'MASK') form.maskType = '';
};

const openAdd = () => {
	Object.assign(form, defaultForm());
	dialog.title = '新增策略';
	dialog.visible = true;
};

const handleEdit = (row: any) => {
	Object.assign(form, row);
	dialog.title = '编辑策略';
	dialog.visible = true;
};

const handleSubmit = async () => {
	if (form.id) {
		await updatePolicy(form);
	} else {
		await addPolicy(form);
	}
	ElMessage.success('保存成功');
	dialog.visible = false;
	loadData();
};

const loadData = async () => {
	loading.value = true;
	try {
		const res = await fetchPoliciesPage(query);
		tableData.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

onMounted(loadData);
</script>
