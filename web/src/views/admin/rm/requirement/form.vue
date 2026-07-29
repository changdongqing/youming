<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? '编辑需求' : '新增需求'" draggable v-model="visible" width="800px">
		<el-form :model="form" :rules="dataRules" label-width="120px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="24" class="mb20">
					<el-form-item label="需求标题" prop="title">
						<el-input v-model="form.title" placeholder="请输入需求标题" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item label="需求来源" prop="source">
						<el-select v-model="form.source" placeholder="请选择" style="width: 100%">
							<el-option v-for="item in rm_req_source" :key="item.value" :label="item.label" :value="item.value" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item label="优先级" prop="priority">
						<el-select v-model="form.priority" placeholder="请选择" style="width: 100%">
							<el-option v-for="item in rm_priority" :key="item.value" :label="item.label" :value="item.value" />
						</el-select>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item label="关联客户/项目" prop="customerProject">
						<el-input v-model="form.customerProject" placeholder="选填" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item label="期望完成时间" prop="expectCompleteDate">
						<el-date-picker v-model="form.expectCompleteDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item label="需讨论会评审" prop="needReview">
						<el-radio-group v-model="form.needReview">
							<el-radio label="0">否</el-radio>
							<el-radio label="1">是</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item label="需求描述" prop="description">
						<el-input v-model="form.description" type="textarea" :rows="5" placeholder="请详细描述需求内容" />
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">取消</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="loading">确定</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="rmRequirementForm" setup>
import { useMessage } from '/@/hooks/message';
import { useDict } from '/@/hooks/dict';
import { addObj, getObj, putObj } from '/@/api/rm/requirement';

const emit = defineEmits(['refresh']);
const { rm_req_source, rm_priority } = useDict('rm_req_source', 'rm_priority');

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

const form = reactive({
	id: '' as string,
	title: '',
	source: '',
	customerProject: '',
	description: '',
	expectCompleteDate: '',
	needReview: '0',
	priority: 'MEDIUM',
});

const dataRules = computed(() => ({
	title: [{ required: true, message: '需求标题不能为空', trigger: 'blur' }],
	source: [{ required: true, message: '需求来源不能为空', trigger: 'change' }],
	description: [{ required: true, message: '需求描述不能为空', trigger: 'blur' }],
	expectCompleteDate: [{ required: true, message: '期望完成时间不能为空', trigger: 'change' }],
}));

const openDialog = async (id?: string) => {
	visible.value = true;
	form.id = '';
	nextTick(() => {
		dataFormRef.value?.resetFields();
	});
	if (id) {
		form.id = id;
		loading.value = true;
		try {
			const { data } = await getObj(id);
			Object.assign(form, data);
		} catch (err: any) {
			useMessage().error(err.msg);
		} finally {
			loading.value = false;
		}
	}
};

const onSubmit = async () => {
	if (loading.value) return;
	loading.value = true;
	try {
		const valid = await dataFormRef.value.validate().catch(() => {});
		if (!valid) {
			loading.value = false;
			return false;
		}
		form.id ? await putObj(form) : await addObj(form);
		useMessage().success(form.id ? '编辑成功' : '新增成功');
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({ openDialog });
</script>
