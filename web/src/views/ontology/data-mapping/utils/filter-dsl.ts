/**
 * 过滤DSL构建器工具（18-10 §9）
 * UI仅提供受控条件：列 + 操作符 + 参数值 + AND/OR分组，最大嵌套3层。
 * 不提供SQL文本框。序列化为18-02 DSL JSON。
 */

// ==================== DSL 类型定义 ====================

export type FilterOperator = 'EQ' | 'NE' | 'GT' | 'GE' | 'LT' | 'LE' | 'LIKE' | 'NOT_LIKE' | 'IN' | 'NOT_IN' | 'IS_NULL' | 'IS_NOT_NULL';

export type LogicalOperator = 'AND' | 'OR';

/** 叶子条件 */
export interface FilterCondition {
	column: string;
	operator: FilterOperator;
	/** 参数值；IS_NULL/IS_NOT_NULL 时为 undefined；IN/NOT_IN 时为数组 */
	value?: string | string[];
}

/** 逻辑分组（最大嵌套3层） */
export interface FilterGroup {
	logic: LogicalOperator;
	conditions: FilterCondition[];
	groups: FilterGroup[];
}

/** DSL 根节点 */
export interface FilterDsl {
	root: FilterGroup | null;
}

// ==================== 操作符选项 ====================

export const OPERATOR_OPTIONS: { label: string; value: FilterOperator }[] = [
	{ label: '等于', value: 'EQ' },
	{ label: '不等于', value: 'NE' },
	{ label: '大于', value: 'GT' },
	{ label: '大于等于', value: 'GE' },
	{ label: '小于', value: 'LT' },
	{ label: '小于等于', value: 'LE' },
	{ label: '相似', value: 'LIKE' },
	{ label: '不相似', value: 'NOT_LIKE' },
	{ label: '在列表中(IN)', value: 'IN' },
	{ label: '不在列表中(NOT_IN)', value: 'NOT_IN' },
	{ label: '为空(IS NULL)', value: 'IS_NULL' },
	{ label: '不为空(IS NOT NULL)', value: 'IS_NOT_NULL' },
];

export const LOGIC_OPTIONS: { label: string; value: LogicalOperator }[] = [
	{ label: 'AND（且）', value: 'AND' },
	{ label: 'OR（或）', value: 'OR' },
];

/** IN/NOT_IN 最大值数量 */
export const MAX_IN_VALUES = 100;
/** 最大嵌套层数 */
export const MAX_NESTING_DEPTH = 3;

// ==================== 工具函数 ====================

/** 是否为不需要参数值的操作符 */
export function isNullOperator(op: FilterOperator): boolean {
	return op === 'IS_NULL' || op === 'IS_NOT_NULL';
}

/** 是否为列表型操作符 */
export function isListOperator(op: FilterOperator): boolean {
	return op === 'IN' || op === 'NOT_IN';
}

/** 创建空DSL */
export function createEmptyDsl(): FilterDsl {
	return { root: null };
}

/** 创建空分组 */
export function createEmptyGroup(logic: LogicalOperator = 'AND'): FilterGroup {
	return { logic, conditions: [], groups: [] };
}

/** 获取分组嵌套深度（根分组为0） */
export function getDepth(group: FilterGroup | null): number {
	if (!group) return 0;
	let maxChildDepth = 0;
	for (const child of group.groups) {
		const childDepth = getDepth(child);
		if (childDepth > maxChildDepth) maxChildDepth = childDepth;
	}
	return 1 + maxChildDepth;
}

/** 是否还可以添加子分组 */
export function canAddGroup(group: FilterGroup | null): boolean {
	return getDepth(group) < MAX_NESTING_DEPTH;
}

/** 序列化为JSON字符串 */
export function serializeFilterDsl(dsl: FilterDsl): string {
	if (!dsl.root) return '';
	return JSON.stringify(dsl);
}

/** 从JSON字符串反序列化 */
export function deserializeFilterDsl(json: string | undefined | null): FilterDsl | null {
	if (!json) return { root: null };
	try {
		const parsed = JSON.parse(json);
		if (parsed && typeof parsed === 'object' && 'root' in parsed) {
			return parsed as FilterDsl;
		}
		// 兼容直接是 root 分组的情况
		if (parsed && typeof parsed === 'object' && 'logic' in parsed) {
			return { root: parsed as FilterGroup };
		}
		return null;
	} catch {
		return null;
	}
}

/** 校验IN值数量不超过上限 */
export function validateInValues(values: string[]): boolean {
	return values.length <= MAX_IN_VALUES;
}

/** 将条件值数组转为逗号分隔字符串（用于UI展示） */
export function listValueToString(values: string[]): string {
	return values.join(', ');
}

/** 将逗号分隔字符串转为数组（用于UI输入） */
export function stringToListValue(str: string): string[] {
	return str
		.split(',')
		.map((s) => s.trim())
		.filter((s) => s.length > 0);
}
