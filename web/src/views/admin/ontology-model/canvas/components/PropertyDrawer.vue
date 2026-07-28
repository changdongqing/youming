<template>
	<el-drawer v-model="visible" :title="t('modelCanvas.propertyDrawer')" size="400px" @open="loadDetail">
		<el-descriptions :column="1" border size="small" v-loading="loading">
			<el-descriptions-item label="IRI">{{ detail.classIri }}</el-descriptions-item>
			<el-descriptions-item :label="t('modelCanvas.classInfo')">{{ detail.label || detail.localName }}</el-descriptions-item>
			<el-descriptions-item label="templateCode">{{ detail.templateCode || '-' }}</el-descriptions-item>
		</el-descriptions>
		<el-tabs class="mt12">
			<el-tab-pane :label="t('modelCanvas.datatypeProperties') + ' (' + (detail.datatypeProperties?.length || 0) + ')'">
				<el-table :data="detail.datatypeProperties || []" size="small" max-height="300">
					<el-table-column prop="localName" label="Name" show-overflow-tooltip />
					<el-table-column prop="xsdType" label="XSD" width="110" />
					<el-table-column prop="unitRef" label="Unit" show-overflow-tooltip />
				</el-table>
			</el-tab-pane>
			<el-tab-pane :label="t('modelCanvas.objectProperties') + ' (' + (detail.objectProperties?.length || 0) + ')'">
				<el-table :data="detail.objectProperties || []" size="small" max-height="300">
					<el-table-column prop="localName" label="Name" show-overflow-tooltip />
					<el-table-column prop="label" label="Label" show-overflow-tooltip />
					<el-table-column prop="rangeClassId" label="Range" width="100">
						<template #default="scope">{{ scope.row.rangeClassId || t('modelCanvas.empty') }}</template>
					</el-table-column>
				</el-table>
			</el-tab-pane>
		</el-tabs>
	</el-drawer>
</template>

<script lang="ts" setup>
import { getObj as getClass } from '/@/api/ontology-model/class';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const visible = ref(false);
const loading = ref(false);
const detail = ref<any>({});

const open = async (classId: string) => {
	visible.value = true;
	detail.value = { classId };
};

const loadDetail = async () => {
	if (!detail.value.classId) return;
	loading.value = true;
	try {
		const { data } = await getClass(detail.value.classId);
		detail.value = data;
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

defineExpose({ open });
</script>

<style scoped>
.mt12 {
	margin-top: 12px;
}
</style>
