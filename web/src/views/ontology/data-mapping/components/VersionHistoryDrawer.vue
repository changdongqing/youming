<template>
	<el-drawer v-model="visible" title="版本历史" size="50%" destroy-on-close>
		<el-table v-loading="loading" :data="versionList" border size="small">
			<el-table-column prop="versionNumber" label="版本号" width="100" />
			<el-table-column label="状态" width="90">
				<template #default="{ row }">
					<el-tag :type="versionStatusTagType(row.versionStatus)" size="small">
						{{ versionStatusLabel(row.versionStatus) }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column prop="publishedAt" label="发布时间" width="170" />
			<el-table-column prop="createTime" label="创建时间" width="170" />
			<el-table-column label="操作" width="200">
				<template #default="{ row }">
					<el-button link type="primary" size="small" @click="handleSnapshot(row)">快照</el-button>
					<el-button link type="primary" size="small" @click="$emit('editDraft', row)" v-if="row.versionStatus === 'DRAFT'">编辑</el-button>
					<el-button
						link
						type="success"
						size="small"
						@click="$emit('publish', row)"
						v-if="row.versionStatus === 'VALIDATED' || row.versionStatus === 'DRAFT'"
					>
						发布
					</el-button>
					<el-button link type="warning" size="small" @click="$emit('retire', row)" v-if="row.versionStatus === 'PUBLISHED'">停用</el-button>
				</template>
			</el-table-column>
		</el-table>
	</el-drawer>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useMessage } from '/@/hooks/message';
import { mappingProjectApi } from '/@/api/ontology/data-mapping';
import type { MappingVersionVO } from '/@/types/ontology/data-mapping';
import { versionStatusLabel, versionStatusTagType } from '../utils/mapping-status';

const props = defineProps<{ projectId: number }>();
defineEmits<{
	(e: 'editDraft', version: MappingVersionVO): void;
	(e: 'publish', version: MappingVersionVO): void;
	(e: 'retire', version: MappingVersionVO): void;
}>();

const { success: msgSuccess, error: msgError } = useMessage();

const visible = ref(false);
const loading = ref(false);
const versionList = ref<MappingVersionVO[]>([]);

const open = async () => {
	visible.value = true;
	await loadVersions();
};

const loadVersions = async () => {
	loading.value = true;
	try {
		const { data } = await mappingProjectApi.listVersions(props.projectId, { current: 1, size: 50 });
		versionList.value = data?.records || [];
	} catch (e: any) {
		msgError(e.message || '获取版本列表失败');
	} finally {
		loading.value = false;
	}
};

const handleSnapshot = async (row: MappingVersionVO) => {
	try {
		const { data } = await mappingProjectApi.getSnapshot(row.id);
		ElMessageBox.alert(
			`<pre style="max-height:500px;overflow:auto;font-size:12px;">${JSON.stringify(
				typeof data === 'string' ? JSON.parse(data) : data,
				null,
				2
			)}</pre>`,
			'版本快照',
			{
				dangerouslyUseHTMLString: true,
			}
		);
	} catch (e: any) {
		msgError(e.message || '获取快照失败');
	}
};

defineExpose({ open });
</script>
