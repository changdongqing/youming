<template>
	<div>
		<el-form :inline="true" :model="query" class="mb8">
			<el-form-item label="工程ID">
				<el-input v-model="query.ontologyId" placeholder="工程ID" clearable @keyup.enter="loadData" />
			</el-form-item>
			<el-button type="primary" @click="loadData">查询</el-button>
			<el-button type="success" @click="dialog.visible = true">新增许可</el-button>
		</el-form>

		<el-table v-loading="loading" :data="tableData" border>
			<el-table-column prop="ontologyId" label="工程ID" width="120" />
			<el-table-column prop="subjectType" label="主体类型" width="100" />
			<el-table-column prop="subjectId" label="主体ID" width="120" />
			<el-table-column prop="maxLevelCode" label="最高级别" width="150" />
			<el-table-column prop="createTime" label="创建时间" />
		</el-table>

		<el-pagination
			v-model:current-page="query.current"
			v-model:page-size="query.size"
			:total="total"
			layout="total, prev, pager, next"
			@current-change="loadData"
		/>

		<el-dialog v-model="dialog.visible" title="新增主体许可" width="500px">
			<el-form :model="form" label-width="100px">
				<el-form-item label="工程ID">
					<el-input v-model="form.ontologyId" />
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
				<el-form-item label="最高级别">
					<el-select v-model="form.maxLevelCode">
						<el-option label="公开" value="PUBLIC" />
						<el-option label="内部" value="INTERNAL" />
						<el-option label="机密" value="CONFIDENTIAL" />
						<el-option label="受限" value="RESTRICTED" />
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
import { fetchClearancesPage, addClearance } from '/@/api/ontology/security';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const query = reactive({ ontologyId: '', current: 1, size: 10 });

const dialog = reactive({ visible: false });
const form = reactive({ ontologyId: '', subjectType: 'USER', subjectId: '', maxLevelCode: 'INTERNAL' });

const loadData = async () => {
	loading.value = true;
	try {
		const res = await fetchClearancesPage(query);
		tableData.value = res.data?.records || [];
		total.value = res.data?.total || 0;
	} finally {
		loading.value = false;
	}
};

const handleSubmit = async () => {
	await addClearance(form);
	ElMessage.success('保存成功');
	dialog.visible = false;
	Object.assign(form, { ontologyId: '', subjectType: 'USER', subjectId: '', maxLevelCode: 'INTERNAL' });
	loadData();
};

onMounted(loadData);
</script>
