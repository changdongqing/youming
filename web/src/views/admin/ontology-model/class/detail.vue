<template>
	<el-dialog :close-on-click-modal="false" :title="t('modelClass.detail')" draggable v-model="visible" width="860px">
		<el-descriptions :column="2" border v-loading="loading">
			<el-descriptions-item :label="t('modelClass.localName')">{{ detail.localName }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.label')">{{ detail.label || '-' }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.labelCn')">{{ detail.labelCn || '-' }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.classificationCode')">{{ detail.classificationCode || '-' }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.templateCode')">{{ detail.templateCode || t('modelClass.noTemplate') }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.classIri')" :span="2">{{ detail.classIri }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.description')" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.parentClasses')">{{ detail.parentClassIris?.length ? detail.parentClassIris.join(', ') : t('modelClass.empty') }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelClass.childClasses')">{{ detail.childClassIris?.length ? detail.childClassIris.join(', ') : t('modelClass.empty') }}</el-descriptions-item>
		</el-descriptions>

		<el-tabs class="mt12">
			<el-tab-pane :label="t('modelClass.datatypeProperties') + ' (' + (detail.datatypeProperties?.length || 0) + ')'">
				<el-table :data="detail.datatypeProperties || []" border size="small" max-height="280">
					<el-table-column prop="localName" :label="t('modelClass.localName')" show-overflow-tooltip />
					<el-table-column prop="label" :label="t('modelClass.label')" show-overflow-tooltip />
					<el-table-column prop="xsdType" label="XSD" width="120" />
					<el-table-column prop="unitRef" label="Unit" show-overflow-tooltip />
					<el-table-column prop="templateCode" :label="t('modelClass.templateCode')" width="120">
						<template #default="scope">
							<el-tag size="small" type="warning" v-if="scope.row.templateCode">{{ t('modelClass.hasTemplate') }}</el-tag>
							<el-tag size="small" type="info" v-else>{{ t('modelClass.noTemplate') }}</el-tag>
						</template>
					</el-table-column>
				</el-table>
			</el-tab-pane>
			<el-tab-pane :label="t('modelClass.objectProperties') + ' (' + (detail.objectProperties?.length || 0) + ')'">
				<el-table :data="detail.objectProperties || []" border size="small" max-height="280">
					<el-table-column prop="localName" :label="t('modelClass.localName')" show-overflow-tooltip />
					<el-table-column prop="label" :label="t('modelClass.label')" show-overflow-tooltip />
					<el-table-column prop="rangeClassId" label="Range ID" width="120">
						<template #default="scope">{{ scope.row.rangeClassId || t('modelClass.empty') }}</template>
					</el-table-column>
					<el-table-column prop="templateCode" :label="t('modelClass.templateCode')" width="120">
						<template #default="scope">
							<el-tag size="small" type="warning" v-if="scope.row.templateCode">{{ t('modelClass.hasTemplate') }}</el-tag>
							<el-tag size="small" type="info" v-else>{{ t('modelClass.noTemplate') }}</el-tag>
						</template>
					</el-table-column>
				</el-table>
			</el-tab-pane>
		</el-tabs>

		<template #footer>
			<el-button @click="visible = false">{{ t('common.cancelButtonText') }}</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" name="ModelClassDetailDialog" setup>
import { useMessage } from '/@/hooks/message';
import { getObj } from '/@/api/ontology-model/class';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const visible = ref(false);
const loading = ref(false);
const detail = ref<any>({});

const openDialog = async (id: string) => {
	visible.value = true;
	detail.value = {};
	loading.value = true;
	try {
		const { data } = await getObj(id);
		detail.value = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({
	openDialog,
});
</script>

<style scoped>
.mt12 {
	margin-top: 12px;
}
</style>
