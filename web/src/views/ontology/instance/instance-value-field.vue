<!--
	实例数据属性值动态字段。
	根据数据属性的 baseType + valueMode 渲染对应控件（设计 §9.2 Schema 驱动表单）。
	支持多值（新增值/删除值）。
-->
<template>
	<div class="instance-value-field">
		<div v-for="(item, idx) in modelValue" :key="idx" class="value-row">
			<!-- TEXT_OR_NUMERIC：先选字面量类型，再显示对应输入 -->
			<el-select
				v-if="visibility.textOrNumericTypeSelect && !isMultiValueTypeFixed"
				v-model="item.literalType"
				placeholder="值类型"
				class="type-select"
				@change="onLiteralTypeChange(item)"
			>
				<el-option v-for="t in literalTypeOptions" :key="t" :label="t" :value="t" />
			</el-select>

			<!-- CLOSED_ENUM / OPEN_ENUM 选择器 -->
			<el-select
				v-if="visibility.enumSelect"
				v-model="item.literalValue"
				:filterable="true"
				:allow-create="visibility.enumAllowCreate"
				default-first-option
				placeholder="选择枚举值"
				class="value-input"
			>
				<el-option v-for="opt in enumOptions" :key="opt.value" :label="opt.value" :value="opt.canonical || opt.value" />
			</el-select>

			<!-- 日期控件 -->
			<el-date-picker
				v-else-if="visibility.datePicker"
				v-model="item.literalValue"
				type="date"
				value-format="YYYY-MM-DD"
				placeholder="选择日期"
				class="value-input"
			/>

			<!-- 数值输入（保留字符串提交，避免精度损失） -->
			<el-input
				v-else-if="visibility.numericInput"
				v-model="item.literalValue"
				placeholder="数值（整数或小数）"
				class="value-input"
			/>

			<!-- 布尔选择 -->
			<el-select
				v-else-if="visibility.booleanSelect"
				v-model="item.literalValue"
				placeholder="布尔值"
				class="value-input"
			>
				<el-option label="true" value="true" />
				<el-option label="false" value="false" />
			</el-select>

			<!-- URI 文本输入 -->
			<el-input
				v-else-if="visibility.uriInput"
				v-model="item.literalValue"
				placeholder="绝对IRI，如 http://example.org/..."
				class="value-input"
			/>

			<!-- UNIT_REF 单位级联选择 -->
			<template v-else-if="visibility.unitRefSelect">
				<el-select
					v-model="item.unitId"
					placeholder="选择单位"
					filterable
					class="unit-select"
					@change="onUnitChange(item, $event)"
				>
					<el-option
						v-for="u in unitOptions"
						:key="u.id"
						:label="u.symbol + (u.name ? '（' + u.name + '）' : '')"
						:value="u.id"
					/>
				</el-select>
			</template>

			<!-- 普通文本输入 -->
			<el-input
				v-else-if="visibility.textInput || textOrNumericIsString"
				v-model="item.literalValue"
				:type="textInputType"
				placeholder="文本值"
				class="value-input"
			/>

			<el-button link type="danger" icon="delete" @click="removeValue(idx)" />
		</div>
		<el-button link type="primary" icon="plus" @click="addValue">新增值</el-button>
	</div>
</template>

<script lang="ts" setup>
import { getValueFieldVisibility, getLiteralTypeOptions, normalizeLiteralValue } from './instance-form';
import type { DataPropertyMeta, InstanceDataValueDTO, LiteralType, OntologyId } from '/@/types/ontology/instance';

const props = defineProps<{
	propMeta: DataPropertyMeta;
	enumOptions: { value: string; canonical?: string }[];
	unitOptions: { id: OntologyId; symbol: string; name?: string }[];
	values: InstanceDataValueDTO[];
}>();

const emit = defineEmits<{
	(e: 'update:values', values: InstanceDataValueDTO[]): void;
}>();

// 使用 values 作为受控数据（父组件持有），本地 modelValue 仅为 emit 的桥接
const modelValue = computed({
	get: () => props.values,
	set: (val: InstanceDataValueDTO[]) => emit('update:values', val),
});

const visibility = computed(() => getValueFieldVisibility(props.propMeta.baseType, props.propMeta.valueMode));

const literalTypeOptions = computed<LiteralType[]>(() => getLiteralTypeOptions(props.propMeta.baseType));

/** TEXT_OR_NUMERIC 选了 STRING 时显示文本输入 */
const textOrNumericIsString = computed(() => {
	if (!visibility.value.textOrNumericTypeSelect) return false;
	return modelValue.value.some((v: InstanceDataValueDTO) => v.literalType === 'STRING');
});

/** TEXT_OR_NUMERIC 不展示独立类型选择器的情况（非 TEXT_OR_NUMERIC 时类型固定） */
const isMultiValueTypeFixed = computed(() => props.propMeta.baseType !== 'TEXT_OR_NUMERIC');

/** TEXT 基础类型且 formatHint 含长文本提示时用 textarea */
const textInputType = computed<'text' | 'textarea'>(() => {
	if (props.propMeta.formatHint && props.propMeta.formatHint.length > 20) return 'textarea';
	return 'text';
});

/** 推断默认 literalType（非 TEXT_OR_NUMERIC 时由 baseType 决定） */
const defaultLiteralType = (): LiteralType => {
	switch (props.propMeta.baseType) {
		case 'TEXT':
			return 'STRING';
		case 'URI':
			return 'URI';
		case 'DATE':
			return 'DATE';
		case 'NUMERIC':
			return 'INTEGER';
		case 'BOOLEAN':
			return 'BOOLEAN';
		case 'UNIT_REF':
			return 'STRING';
		case 'TEXT_OR_NUMERIC':
			return 'STRING';
		default:
			return 'STRING';
	}
};

const addValue = () => {
	const newVal: InstanceDataValueDTO = {
		dataPropertyId: props.propMeta.dataPropertyId,
		literalValue: '',
		literalType: defaultLiteralType(),
		sortOrder: modelValue.value.length + 1,
	};
	emit('update:values', [...modelValue.value, newVal]);
};

const removeValue = (idx: number) => {
	const next = modelValue.value.filter((_: InstanceDataValueDTO, i: number) => i !== idx);
	emit('update:values', next);
};

const onLiteralTypeChange = (item: InstanceDataValueDTO) => {
	// TEXT_OR_NUMERIC 切换类型时，如果是 INTEGER/DECIMAL，尝试规范化当前值
	if (item.literalType === 'INTEGER' || item.literalType === 'DECIMAL') {
		const normalized = normalizeLiteralValue(item.literalValue, item.literalType);
		if (normalized) item.literalValue = normalized;
	}
};

const onUnitChange = (item: InstanceDataValueDTO, unitId: OntologyId) => {
	const unit = props.unitOptions.find((u: { id: OntologyId; symbol: string; name?: string }) => u.id === unitId);
	if (unit) {
		item.literalSymbol = unit.symbol;
		item.literalValue = unit.symbol;
	}
};
</script>

<style scoped>
.instance-value-field {
	width: 100%;
}
.value-row {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 8px;
}
.type-select {
	width: 140px;
	flex-shrink: 0;
}
.value-input {
	flex: 1;
}
.unit-select {
	width: 200px;
	flex-shrink: 0;
}
</style>
