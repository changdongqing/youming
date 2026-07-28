<template>
	<div class="template-sidebar">
		<el-tabs v-model="activeTab">
			<el-tab-pane :label="t('modelCanvas.classTemplate')" name="class">
				<el-scrollbar height="500px">
					<el-tree
						:data="classTree"
						:props="{ label: 'label', children: 'children' }"
						node-key="templateCode"
						draggable
						@node-drag-start="onDragStart"
					>
						<template #default="{ data }">
							<span class="tree-node">
								<span class="tree-node-icon">{{ data.icon || '📁' }}</span>
								<span>{{ data.classificationCode }} {{ data.label }}</span>
							</span>
						</template>
					</el-tree>
				</el-scrollbar>
			</el-tab-pane>
			<el-tab-pane :label="t('modelCanvas.propertyTemplate')" name="property">
				<el-scrollbar height="500px">
					<el-table :data="propertyTemplates" size="small">
						<el-table-column prop="templateCode" label="Code" show-overflow-tooltip />
						<el-table-column prop="label" label="Label" show-overflow-tooltip />
						<el-table-column prop="kind" label="Kind" width="80" />
					</el-table>
				</el-scrollbar>
			</el-tab-pane>
			<el-tab-pane :label="t('modelCanvas.unit')" name="unit">
				<el-scrollbar height="500px">
					<el-table :data="units" size="small">
						<el-table-column prop="label" label="Label" show-overflow-tooltip />
						<el-table-column prop="qudtIri" label="QUDT IRI" show-overflow-tooltip />
					</el-table>
				</el-scrollbar>
			</el-tab-pane>
		</el-tabs>
	</div>
</template>

<script lang="ts" setup>
import { supplyClassTemplateTree } from '/@/api/ontology-model/class';
import request from '/@/utils/request';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
const activeTab = ref('class');
const classTree = ref<any[]>([]);
const propertyTemplates = ref<any[]>([]);
const units = ref<any[]>([]);

const onDragStart = (node: any, event: DragEvent) => {
	const data = node.data;
	if (data.templateCode) {
		event.dataTransfer?.setData('templateCode', data.templateCode);
		event.dataTransfer?.setData('type', 'class-template');
	}
};

const loadClassTree = async () => {
	try {
		const { data } = await supplyClassTemplateTree('equipment');
		classTree.value = data ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const loadPropertyTemplates = async () => {
	try {
		const { data } = await request({
			url: '/admin/ont/supply/v1/property-templates',
			method: 'get',
			params: { includeDeprecated: false },
		});
		propertyTemplates.value = data ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

const loadUnits = async () => {
	try {
		const { data } = await request({
			url: '/admin/ont/supply/v1/units',
			method: 'get',
			params: { includeDeprecated: false },
		});
		units.value = data ?? [];
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};

onMounted(() => {
	loadClassTree();
	loadPropertyTemplates();
	loadUnits();
});
</script>

<style scoped>
.template-sidebar {
	padding: 8px;
}
.tree-node {
	display: flex;
	align-items: center;
	gap: 4px;
}
.tree-node-icon {
	font-size: 14px;
}
</style>
