<template>
	<el-dialog v-model="visible" title="枚举映射配置" width="600px" :close-on-click-modal="false" draggable destroy-on-close>
		<el-alert type="info" :closable="false" show-icon class="mb12">
			<template #title>配置源值到目标值的映射。仅支持枚举值域的数据属性（CLOSED_ENUM）。</template>
		</el-alert>
		<el-table :data="mappings" border size="small" style="width: 100%">
			<el-table-column label="源值" min-width="200">
				<template #default="{ row }">
					<el-input v-model="row.sourceValue" placeholder="源枚举值" size="small" />
				</template>
			</el-table-column>
			<el-table-column label="" width="40" align="center">→</el-table-column>
			<el-table-column label="目标值" min-width="200">
				<template #default="{ row }">
					<el-input v-model="row.targetValue" placeholder="目标属性值" size="small" />
				</template>
			</el-table-column>
			<el-table-column label="操作" width="60">
				<template #default="{ $index }">
					<el-button size="small" type="danger" link @click="removeMapping($index)">
						<el-icon><Delete /></el-icon>
					</el-button>
				</template>
			</el-table-column>
		</el-table>
		<el-button size="small" @click="addMapping" class="mt8">
			<el-icon><Plus /></el-icon> 添加映射
		</el-button>

		<template #footer>
			<el-button @click="visible = false">取消</el-button>
			<el-button type="primary" @click="handleConfirm">确认</el-button>
		</template>
	</el-dialog>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { Plus, Delete } from '@element-plus/icons-vue';

const emit = defineEmits<{
	(e: 'confirm', params: string): void;
}>();

const visible = ref(false);

interface EnumMapping {
	sourceValue: string;
	targetValue: string;
}

const mappings = ref<EnumMapping[]>([]);

const open = (existingParams?: string) => {
	visible.value = true;
	if (existingParams) {
		try {
			const parsed = JSON.parse(existingParams);
			if (Array.isArray(parsed)) {
				mappings.value = parsed.map((m: any) => ({
					sourceValue: m.sourceValue ?? m.source ?? '',
					targetValue: m.targetValue ?? m.target ?? '',
				}));
				return;
			}
			// 兼容对象格式 { "source1": "target1", ... }
			if (typeof parsed === 'object') {
				mappings.value = Object.entries(parsed).map(([k, v]) => ({ sourceValue: k, targetValue: String(v) }));
				return;
			}
		} catch {
			// 解析失败，使用空列表
		}
	}
	mappings.value = [];
};

const addMapping = () => {
	mappings.value.push({ sourceValue: '', targetValue: '' });
};

const removeMapping = (idx: number) => {
	mappings.value.splice(idx, 1);
};

const handleConfirm = () => {
	const params = JSON.stringify(
		mappings.value.filter((m) => m.sourceValue.trim()).map((m) => ({ sourceValue: m.sourceValue.trim(), targetValue: m.targetValue.trim() }))
	);
	emit('confirm', params);
	visible.value = false;
};

defineExpose({ open });
</script>

<style scoped>
.mb12 {
	margin-bottom: 12px;
}
.mt8 {
	margin-top: 8px;
}
</style>
