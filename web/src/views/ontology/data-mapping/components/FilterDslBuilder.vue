<template>
	<div class="filter-dsl-builder">
		<template v-if="!dsl?.root">
			<el-button size="small" type="primary" plain @click="initRoot" :disabled="readonly">
				<el-icon><Plus /></el-icon> 添加过滤条件
			</el-button>
		</template>

		<template v-else>
			<!-- 逻辑根 -->
			<div class="dsl-group" v-for="(cond, idx) in dsl.root.conditions" :key="'cond-' + idx">
				<el-tag v-if="idx > 0" size="small" class="logic-tag">{{ dsl.root.logic }}</el-tag>
				<el-row :gutter="4" align="middle" class="cond-row">
					<el-col :span="6">
						<el-input v-model="cond.column" placeholder="列名" size="small" :disabled="readonly" />
					</el-col>
					<el-col :span="5">
						<el-select v-model="cond.operator" placeholder="操作符" size="small" :disabled="readonly" @change="handleOperatorChange(cond)">
							<el-option v-for="op in OPERATOR_OPTIONS" :key="op.value" :label="op.label" :value="op.value" />
						</el-select>
					</el-col>
					<el-col :span="10">
						<!-- 空操作符不显示值输入 -->
						<el-input v-if="isNullOperator(cond.operator)" value="" disabled placeholder="无需参数" size="small" />
						<!-- 列表操作符 -->
						<el-input
							v-else-if="isListOperator(cond.operator)"
							v-model="listValueTemp[idx]"
							placeholder="逗号分隔，最多100个值"
							size="small"
							:disabled="readonly"
							@blur="handleListBlur(cond, idx)"
						/>
						<!-- 普通值 -->
						<el-input v-else v-model="cond.value as string" placeholder="参数值" size="small" :disabled="readonly" />
					</el-col>
					<el-col :span="3">
						<el-button size="small" type="danger" link @click="removeCondition(idx)" :disabled="readonly">
							<el-icon><Delete /></el-icon>
						</el-button>
					</el-col>
				</el-row>
			</div>

			<!-- 添加条件按钮 -->
			<el-button size="small" @click="addCondition" :disabled="readonly" class="mt4">
				<el-icon><Plus /></el-icon> 条件
			</el-button>

			<!-- 逻辑切换 -->
			<el-radio-group v-model="dsl.root.logic" size="small" class="ml8" :disabled="readonly">
				<el-radio-button value="AND">AND</el-radio-button>
				<el-radio-button value="OR">OR</el-radio-button>
			</el-radio-group>

			<!-- 清空 -->
			<el-button size="small" type="danger" link @click="clearAll" :disabled="readonly" class="ml8">清空</el-button>
		</template>
	</div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import { Plus, Delete } from '@element-plus/icons-vue';
import {
	type FilterDsl,
	type FilterCondition,
	type FilterOperator,
	createEmptyDsl,
	createEmptyGroup,
	OPERATOR_OPTIONS,
	isNullOperator,
	isListOperator,
	stringToListValue,
	listValueToString,
	serializeFilterDsl,
	deserializeFilterDsl,
	validateInValues,
} from '../utils/filter-dsl';

const props = defineProps<{
	modelValue?: string;
	readonly?: boolean;
}>();
const emit = defineEmits<{
	(e: 'update:modelValue', val: string): void;
}>();

const dsl = ref<FilterDsl>(deserializeFilterDsl(props.modelValue) || createEmptyDsl());
const listValueTemp = ref<Record<number, string>>({});

// 初始化列表值的临时显示
watch(
	() => props.modelValue,
	(val) => {
		dsl.value = deserializeFilterDsl(val) || createEmptyDsl();
		// 初始化列表值临时显示
		if (dsl.value.root) {
			dsl.value.root.conditions.forEach((cond, idx) => {
				if (isListOperator(cond.operator) && Array.isArray(cond.value)) {
					listValueTemp.value[idx] = listValueToString(cond.value);
				}
			});
		}
	},
	{ immediate: true }
);

const emitChange = () => {
	emit('update:modelValue', serializeFilterDsl(dsl.value));
};

const initRoot = () => {
	dsl.value.root = createEmptyGroup('AND');
	emitChange();
};

const addCondition = () => {
	if (!dsl.value.root) dsl.value.root = createEmptyGroup('AND');
	dsl.value.root.conditions.push({ column: '', operator: 'EQ' as FilterOperator, value: '' });
	emitChange();
};

const removeCondition = (idx: number) => {
	dsl.value.root?.conditions.splice(idx, 1);
	emitChange();
};

const handleOperatorChange = (cond: FilterCondition) => {
	// 切换操作符时重置值
	if (isNullOperator(cond.operator)) {
		cond.value = undefined;
	} else if (isListOperator(cond.operator)) {
		cond.value = [];
	} else {
		cond.value = '';
	}
	emitChange();
};

const handleListBlur = (cond: FilterCondition, idx: number) => {
	const values = stringToListValue(listValueTemp.value[idx] || '');
	if (!validateInValues(values)) {
		// 超过100值，截断
		cond.value = values.slice(0, 100);
	} else {
		cond.value = values;
	}
	emitChange();
};

const clearAll = () => {
	dsl.value = createEmptyDsl();
	listValueTemp.value = {};
	emitChange();
};
</script>

<style scoped>
.filter-dsl-builder {
	padding: 4px 0;
}
.dsl-group {
	margin-bottom: 4px;
}
.logic-tag {
	margin: 0 4px;
}
.cond-row {
	margin-bottom: 4px;
}
.mt4 {
	margin-top: 4px;
}
.ml8 {
	margin-left: 8px;
}
</style>
