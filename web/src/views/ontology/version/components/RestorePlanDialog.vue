<template>
	<el-dialog v-model="show" title="生成恢复计划" width="800px">
		<div v-loading="loading">
			<el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
				<template #title>恢复计划说明</template>
				恢复不是直接回滚，而是从历史版本生成正向变更计划。执行后发布新的更高版本，历史版本永不被覆盖。
			</el-alert>

			<div v-if="plan" class="restore-plan">
				<el-descriptions :column="2" border size="small" style="margin-bottom:16px">
					<el-descriptions-item label="目标版本">{{ plan.targetVersionNumber }}</el-descriptions-item>
					<el-descriptions-item label="兼容性">
						<el-tag :type="plan.compatibility === 'BREAKING' ? 'danger' : 'success'" size="small">
							{{ plan.compatibility }}
						</el-tag>
					</el-descriptions-item>
					<el-descriptions-item label="需恢复Schema">{{ plan.schemaToRestore.length }} 项</el-descriptions-item>
					<el-descriptions-item label="需移除Schema">{{ plan.schemaToRemove.length }} 项</el-descriptions-item>
				</el-descriptions>

				<div v-if="plan.breakingReasons.length > 0" style="margin-bottom:16px">
					<el-alert type="error" show-icon :closable="false">
						<template #title>BREAKING 原因</template>
						<ul style="margin:0;padding-left:16px">
							<li v-for="(r, i) in plan.breakingReasons" :key="i">{{ r }}</li>
						</ul>
					</el-alert>
				</div>

				<el-tabs>
					<el-tab-pane :label="`需恢复 (${plan.schemaToRestore.length})`">
						<el-table :data="plan.schemaToRestore" border size="small">
							<el-table-column prop="resourceType" label="类型" width="160" />
							<el-table-column prop="iri" label="IRI" />
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`需移除 (${plan.schemaToRemove.length})`">
						<el-table :data="plan.schemaToRemove" border size="small">
							<el-table-column prop="resourceType" label="类型" width="160" />
							<el-table-column prop="iri" label="IRI" />
						</el-table>
					</el-tab-pane>
					<el-tab-pane :label="`迁移规则 (${plan.migrationRules.length})`">
						<ul class="rule-list">
							<li v-for="(r, i) in plan.migrationRules" :key="i">{{ r }}</li>
						</ul>
					</el-tab-pane>
					<el-tab-pane :label="`风险提示 (${plan.riskWarnings.length})`">
						<ul class="warning-list">
							<li v-for="(w, i) in plan.riskWarnings" :key="i">{{ w }}</li>
						</ul>
					</el-tab-pane>
				</el-tabs>
			</div>
			<el-empty v-else description="点击下方按钮生成恢复计划" />
		</div>

		<template #footer>
			<el-button @click="show = false">关闭</el-button>
			<el-button type="primary" @click="loadPlan" :loading="loading">生成恢复计划</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue';
import { generateRestorePlan } from '/@/api/ontology/version';
import type { RestorePlanVO } from '/@/types/ontology/version';

const props = defineProps<{ visible: boolean; versionId: number; ontologyId: number }>();
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void }>();

const loading = ref(false);
const plan = ref<RestorePlanVO | null>(null);

const show = computed({
	get: () => props.visible,
	set: (v) => emit('update:visible', v),
});

const loadPlan = async () => {
	loading.value = true;
	try {
		const { data } = await generateRestorePlan(props.versionId, {
			ontologyId: props.ontologyId,
			targetVersionId: props.versionId,
		});
		plan.value = data;
	} catch (e: any) {
		// ignore
	} finally {
		loading.value = false;
	}
};

watch(() => props.visible, (v) => { if (v) plan.value = null; });
</script>

<style scoped>
.rule-list { margin: 0; padding-left: 16px; }
.warning-list { margin: 0; padding-left: 16px; color: var(--el-color-warning); }
</style>
