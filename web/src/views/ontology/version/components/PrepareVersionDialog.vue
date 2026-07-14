<template>
	<el-dialog v-model="show" title="发布版本" width="640px" :close-on-click-modal="false">
		<el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
			<el-form-item label="版本号" prop="versionNumber">
				<el-input v-model="form.versionNumber" placeholder="如 1.0.0" style="width: 200px" />
				<span class="form-hint">格式：MAJOR.MINOR.PATCH</span>
			</el-form-item>
			<el-form-item label="声明兼容性">
				<el-select v-model="form.declaredCompatibility" placeholder="不声明（由系统判定）" clearable style="width: 240px">
					<el-option label="补丁（PATCH_ONLY）" value="PATCH_ONLY" />
					<el-option label="向后兼容（BACKWARD_COMPATIBLE）" value="BACKWARD_COMPATIBLE" />
					<el-option label="破坏性（BREAKING）" value="BREAKING" />
				</el-select>
				<span class="form-hint">不能将系统判定的 BREAKING 降级</span>
			</el-form-item>
			<el-form-item label="发布说明">
				<el-input v-model="form.releaseNotes" type="textarea" :rows="3" placeholder="描述本次版本变更内容" />
			</el-form-item>
			<el-form-item label="迁移计划" v-if="form.declaredCompatibility === 'BREAKING'">
				<el-input v-model="form.migrationPlan" type="textarea" :rows="4" placeholder='JSON格式，如 {"rules":[{"ruleType":"DROP_ASSERTION",...}]}' />
			</el-form-item>
		</el-form>

		<!-- prepare 结果展示 -->
		<div v-if="prepareResult" class="prepare-result">
			<el-divider content-position="left">准备结果</el-divider>
			<el-descriptions :column="1" border size="small">
				<el-descriptions-item label="系统判定兼容性">
					<el-tag :type="compatibilityTagType(prepareResult.compatibility)" size="small">
						{{ compatibilityText(prepareResult.compatibility) }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="快照哈希">
					<span class="hash-text">{{ prepareResult.snapshotHash }}</span>
				</el-descriptions-item>
				<el-descriptions-item label="可直接激活">
					<el-tag :type="prepareResult.canActivate ? 'success' : 'danger'" size="small">
						{{ prepareResult.canActivate ? '是' : '否（需迁移）' }}
					</el-tag>
				</el-descriptions-item>
				<el-descriptions-item label="BREAKING原因" v-if="prepareResult.breakingReasons.length > 0">
					<ul class="breaking-list">
						<li v-for="(reason, i) in prepareResult.breakingReasons" :key="i">{{ reason }}</li>
					</ul>
				</el-descriptions-item>
			</el-descriptions>
		</div>

		<template #footer>
			<el-button @click="show = false">关闭</el-button>
			<el-button type="primary" @click="handlePrepare" :loading="loading">准备版本</el-button>
			<el-button type="success" @click="handleActivate" :loading="loading"
				v-if="prepareResult && prepareResult.canActivate">立即激活</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { prepareVersion, activateVersion } from '/@/api/ontology/version';
import type { VersionPrepareRequest, VersionPrepareResultVO } from '/@/types/ontology/version';

const props = defineProps<{ visible: boolean; ontologyId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void; (e: 'success'): void }>();

const { success: msgSuccess, error: msgError } = useMessage();
const formRef = ref<FormInstance>();
const loading = ref(false);
const prepareResult = ref<VersionPrepareResultVO | null>(null);

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const form = ref<VersionPrepareRequest>({
	ontologyId: 935001,
	versionNumber: '',
	releaseNotes: '',
	declaredCompatibility: undefined,
	migrationPlan: '',
});

const rules: FormRules = {
	versionNumber: [
		{ required: true, message: '请输入版本号', trigger: 'blur' },
		{ pattern: /^\d+\.\d+\.\d+$/, message: '格式必须为 MAJOR.MINOR.PATCH', trigger: 'blur' },
	],
};

const compatibilityTagType = (compat: string) => {
	switch (compat) {
		case 'PATCH_ONLY': return 'info';
		case 'BACKWARD_COMPATIBLE': return 'success';
		case 'BREAKING': return 'danger';
		default: return 'info';
	}
};

const compatibilityText = (compat: string) => {
	switch (compat) {
		case 'PATCH_ONLY': return '补丁';
		case 'BACKWARD_COMPATIBLE': return '向后兼容';
		case 'BREAKING': return '破坏性';
		default: return compat;
	}
};

const handlePrepare = async () => {
	if (!formRef.value) return;
	await formRef.value.validate(async (valid) => {
		if (!valid) return;
		loading.value = true;
		try {
			form.value.ontologyId = props.ontologyId;
			const { data } = await prepareVersion(form.value);
			prepareResult.value = data;
			msgSuccess('版本准备成功');
		} catch (e: any) {
			msgError(e.message || '准备失败');
		} finally {
			loading.value = false;
		}
	});
};

const handleActivate = async () => {
	if (!prepareResult.value) return;
	loading.value = true;
	try {
		await activateVersion(prepareResult.value.versionId);
		msgSuccess('版本激活成功');
		show.value = false;
		emit('success');
		prepareResult.value = null;
	} catch (e: any) {
		msgError(e.message || '激活失败');
	} finally {
		loading.value = false;
	}
};

watch(show, (v) => {
	if (v) {
		prepareResult.value = null;
		form.value.versionNumber = '';
		form.value.releaseNotes = '';
		form.value.declaredCompatibility = undefined;
		form.value.migrationPlan = '';
	}
});
</script>

<style scoped>
.form-hint { color: var(--el-text-color-secondary); font-size: 12px; margin-left: 8px; }
.prepare-result { margin-top: 16px; }
.hash-text { font-family: monospace; font-size: 12px; word-break: break-all; }
.breaking-list { margin: 0; padding-left: 16px; color: var(--el-color-danger); }
</style>
