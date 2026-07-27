<template>
	<el-dialog :close-on-click-modal="false" :title="form.id ? t('common.editBtn') : t('common.addBtn')" draggable v-model="visible" width="780px">
		<el-form :model="form" :rules="dataRules" label-width="110px" ref="dataFormRef" v-loading="loading">
			<el-row :gutter="20">
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.parentId')" prop="parentId">
						<el-tree-select
							v-model="form.parentId"
							:data="parentTreeData"
							:placeholder="t('classTemplate.selectParentTip')"
							:props="{ label: 'label', children: 'children', value: 'id' }"
							check-strictly
							clearable
							node-key="id"
							style="width: 100%"
							@change="handleParentChange"
						/>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.treeRoot')" prop="treeRoot">
						<el-input v-model="form.treeRoot" :disabled="!!form.parentId" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.templateCode')" prop="templateCode">
						<el-input :disabled="form.id !== ''" :placeholder="t('classTemplate.inputTemplateCodeTip')" v-model="form.templateCode" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.classificationCode')" prop="classificationCode">
						<el-input :placeholder="t('classTemplate.inputClassificationCodeTip')" v-model="form.classificationCode">
							<template #append>
								<el-button :loading="codeLoading" @click="handlePreviewCode">{{ t('classTemplate.previewCodeBtn') }}</el-button>
							</template>
						</el-input>
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.label')" prop="label">
						<el-input :placeholder="t('classTemplate.inputLabelTip')" v-model="form.label" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.labelCn')" prop="labelCn">
						<el-input :placeholder="t('classTemplate.inputLabelCnTip')" v-model="form.labelCn" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('classTemplate.description')" prop="description">
						<el-input :placeholder="t('classTemplate.inputDescriptionTip')" type="textarea" v-model="form.description" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.icon')" prop="icon">
						<el-input :placeholder="t('classTemplate.inputIconTip')" v-model="form.icon" />
					</el-form-item>
				</el-col>
				<el-col :span="12" class="mb20">
					<el-form-item :label="t('classTemplate.color')" prop="color">
						<el-color-picker v-model="form.color" />
					</el-form-item>
				</el-col>
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('classTemplate.inheritAppearance')" prop="inheritAppearance">
						<el-radio-group v-model="form.inheritAppearance">
							<el-radio value="1" border>{{ t('common.yes') }}</el-radio>
							<el-radio value="0" border>{{ t('common.no') }}</el-radio>
						</el-radio-group>
					</el-form-item>
				</el-col>
				<!-- 外观预设 -->
				<el-col :span="24" class="mb20">
					<el-form-item label=" ">
						<div class="preset-row">
							<el-tag
								v-for="(p, i) in appearancePresets"
								:key="i"
								@click="applyPreset(p)"
								class="preset-tag"
								effect="plain"
							>
								{{ p.icon }} {{ p.label }}
							</el-tag>
						</div>
					</el-form-item>
				</el-col>
				<!-- 结构骨架 refs -->
				<el-col :span="24" class="mb20">
					<el-form-item :label="t('classTemplate.propertyRefs')" prop="propertyRefs">
						<div style="width: 100%">
							<el-table :data="form.propertyRefs" border size="small">
								<el-table-column :label="t('classTemplate.propertyRefs')" min-width="180">
									<template #default="scope">
										<el-select
											v-model="scope.row.propertyTemplateCode"
											:placeholder="t('classTemplate.selectPropertyTemplateTip')"
											filterable
											style="width: 100%"
										>
											<el-option
												v-for="item in propertyTemplateOptions"
												:key="item.templateCode"
												:label="`${item.templateCode} (${item.label})`"
												:value="item.templateCode"
											/>
										</el-select>
									</template>
								</el-table-column>
								<el-table-column :label="t('classTemplate.refType')" width="130">
									<template #default="scope">
										<el-select v-model="scope.row.refType" :placeholder="t('classTemplate.selectRefTypeTip')">
											<el-option
												v-for="o in refTypeOptions"
												:key="o.value"
												:label="o.label"
												:value="o.value"
											/>
										</el-select>
									</template>
								</el-table-column>
								<el-table-column :label="t('classTemplate.sortOrder')" width="100">
									<template #default="scope">
										<el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" size="small" />
									</template>
								</el-table-column>
								<el-table-column :label="t('common.action')" width="80">
									<template #default="scope">
										<el-button :icon="'delete'" @click="removeRef(scope.$index)" size="small" text type="primary" />
									</template>
								</el-table-column>
							</el-table>
							<el-button @click="addRef" class="mt8" icon="plus" plain size="small" type="primary">
								{{ t('classTemplate.addRefTip') }}
							</el-button>
						</div>
					</el-form-item>
				</el-col>
			</el-row>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
				<el-button @click="onSubmit" type="primary" :disabled="loading">{{ t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="ClassTemplateDialog" setup>
import { addObj, getObj, putObj, previewCode, tree as fetchTree } from '/@/api/ontology/class-template';
import { fetchPropertyTemplateOptions, useClassTemplateOptions } from './composables';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const emit = defineEmits(['refresh']);
const { t } = useI18n();

const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);
const codeLoading = ref(false);
const parentTreeData = ref<any[]>([]);
const propertyTemplateOptions = ref<any[]>([]);

const { refTypeOptions, appearancePresets } = useClassTemplateOptions();

const form = reactive({
	id: '',
	templateCode: '',
	classificationCode: '',
	label: '',
	labelCn: '',
	description: '',
	parentId: '' as string | number,
	treeRoot: 'equipment',
	icon: '',
	color: '',
	inheritAppearance: '1',
	sortOrder: 0,
	propertyRefs: [] as any[],
});

const dataRules = computed(() => ({
	templateCode: [{ required: true, message: t('classTemplate.inputTemplateCodeTip'), trigger: 'blur' }],
	treeRoot: [{ required: true, message: t('classTemplate.treeRoot'), trigger: 'blur' }],
	label: [{ required: true, message: t('classTemplate.inputLabelTip'), trigger: 'blur' }],
}));

/**
 * 打开对话框。
	 * @param id 编辑时传入；新增时为 null
	 * @param treeRoot 当前分类树
	 * @param parentId 新增子节点时传入父 id
	 */
const openDialog = async (id: string | null, treeRoot = 'equipment', parentId?: number) => {
	visible.value = true;
	form.id = '';
	form.propertyRefs = [];
	form.parentId = parentId || '';

	nextTick(() => {
		dataFormRef.value?.resetFields();
	});

	form.treeRoot = treeRoot;
	await loadParentTree(treeRoot);
	await loadPropertyTemplates();

	if (id) {
		form.id = id;
		await getClassTemplateData(id);
	} else if (parentId) {
		// 新增子节点：自动预览编码
		await handlePreviewCode();
	} else {
		// 新增根节点：预览根编码
		await handlePreviewCode();
	}
};

/**
 * 拉取父节点候选树（el-tree-select 数据，展示 编码+label）。
 */
const loadParentTree = async (treeRoot: string) => {
	try {
		const { data } = await fetchTree(treeRoot);
		parentTreeData.value = decorateTreeLabel(data || []);
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const decorateTreeLabel = (nodes: any[]): any[] => {
	return nodes.map((n) => ({
		...n,
		label: `${n.classificationCode} ${n.label}`,
		children: n.children ? decorateTreeLabel(n.children) : [],
	}));
};

const loadPropertyTemplates = async () => {
	try {
		const { data } = await fetchPropertyTemplateOptions();
		propertyTemplateOptions.value = data || [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

/**
 * 编码预览（AC-8.2）：据 parentId + 规则生成下一编码。
 */
const handlePreviewCode = async () => {
	codeLoading.value = true;
	try {
		const parentId = form.parentId || undefined;
		const { data } = await previewCode(parentId as any);
		form.classificationCode = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		codeLoading.value = false;
	}
};

const handleParentChange = async () => {
	// 选了父节点后 treeRoot 跟随父节点（父子须同 tree_root）
	if (form.parentId) {
		const parent = findNode(parentTreeData.value, form.parentId);
		if (parent) {
			form.treeRoot = parent.treeRoot;
		}
	}
	await handlePreviewCode();
};

const findNode = (nodes: any[], id: any): any => {
	for (const n of nodes) {
		if (n.id === id) return n;
		if (n.children) {
			const found = findNode(n.children, id);
			if (found) return found;
		}
	}
	return null;
};

const applyPreset = (p: any) => {
	form.icon = p.icon;
	form.color = p.color;
};

const addRef = () => {
	form.propertyRefs.push({ propertyTemplateCode: '', refType: 'property', sortOrder: form.propertyRefs.length + 1 });
};

const removeRef = (index: number) => {
	form.propertyRefs.splice(index, 1);
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
		useMessage().success(t(form.id ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

const getClassTemplateData = async (id: string) => {
	try {
		const { data } = await getObj(id);
		Object.assign(form, data);
		// refs 取本节点的（详情接口已返回 refs）
		form.propertyRefs = (data.refs || []).map((r: any) => ({
			propertyTemplateCode: r.propertyTemplateCode,
			refType: r.refType,
			sortOrder: r.sortOrder,
			inheritFlag: r.inheritFlag,
		}));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

defineExpose({
	openDialog,
});
</script>

<style scoped>
.preset-row {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
}
.preset-tag {
	cursor: pointer;
}
.mb20 {
	margin-bottom: 20px;
}
.mt8 {
	margin-top: 8px;
}
</style>
