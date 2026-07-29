<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? '编辑用例' : '新增用例'" draggable v-model="visible" width="700px">
		<el-form :model="form" :rules="dataRules" label-width="100px" ref="dataFormRef" v-loading="loading">
			<el-form-item label="用例标题" prop="title"><el-input v-model="form.title" placeholder="请输入" /></el-form-item>
			<el-form-item label="关联需求" prop="requirementId"><el-input v-model="form.requirementId" placeholder="需求ID（选填）" /></el-form-item>
			<el-form-item label="用例类型" prop="caseType"><el-select v-model="form.caseType"><el-option v-for="i in rm_case_type" :key="i.value" :label="i.label" :value="i.value" /></el-select></el-form-item>
			<el-form-item label="前置条件" prop="preCondition"><el-input v-model="form.preCondition" type="textarea" :rows="2" /></el-form-item>
			<el-form-item label="测试步骤" prop="steps"><el-input v-model="form.steps" type="textarea" :rows="4" /></el-form-item>
			<el-form-item label="预期结果" prop="expectResult"><el-input v-model="form.expectResult" type="textarea" :rows="3" /></el-form-item>
		</el-form>
		<template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="onSubmit" :disabled="loading">确定</el-button></template>
	</el-dialog>
</template>

<script lang="ts" name="rmTestCaseForm" setup>
import { useMessage } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';
import { addObj, putObj } from '/@/api/rm/test-case';
const emit = defineEmits(['refresh']);
const { rm_case_type } = useDict('rm_case_type');
const dataFormRef = ref(); const visible = ref(false); const loading = ref(false);
const form = reactive({ id:'', title:'', requirementId:'', caseType:'FUNCTIONAL', preCondition:'', steps:'', expectResult:'', status:'DRAFT' });
const dataRules = computed(() => ({ title: [{required:true, message:'标题不能为空', trigger:'blur'}] }));
const openDialog = async (id?: string) => { visible.value = true; form.id = ''; nextTick(() => dataFormRef.value?.resetFields()); };
const onSubmit = async () => { if(loading.value) return; loading.value = true; try { const v = await dataFormRef.value.validate().catch(()=>{}); if(!v){loading.value=false;return;} form.id ? await putObj(form) : await addObj(form); useMessage().success(form.id?'编辑成功':'新增成功'); visible.value=false; emit('refresh'); } catch(err:any){useMessage().error(err.msg);} finally { loading.value=false; } };
defineExpose({ openDialog });
</script>
