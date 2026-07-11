import type { AxiomRuleTemplate, BindingRoleVO, TargetType } from '/@/types/ontology/axiom-rule';

/**
 * 根据模板代码获取绑定角色定义。
 */
export function getBindingRoles(templates: AxiomRuleTemplate[], templateCode: string): BindingRoleVO[] {
	const template = templates.find((t) => t.templateCode === templateCode);
	return template ? template.bindingRoles : [];
}

/**
 * 根据目标类型返回中文标签。
 */
export function getTargetTypeLabel(targetType: TargetType): string {
	switch (targetType) {
		case 'ENTITY_TYPE': return '实体类型';
		case 'DATA_PROPERTY': return '数据属性';
		case 'OBJECT_PROPERTY': return '对象属性';
		case 'UNIT_CATEGORY': return '单位分类';
		default: return targetType;
	}
}

/**
 * 根据状态返回标签类型。
 */
export function getStatusTagType(status: string): '' | 'success' | 'info' | 'warning' | 'danger' {
	switch (status) {
		case 'ACTIVE': return 'success';
		case 'DRAFT': return 'info';
		case 'BLOCKED': return 'danger';
		default: return '';
	}
}

/**
 * 根据严重级别返回标签类型。
 */
export function getSeverityTagType(severity: string): '' | 'success' | 'info' | 'warning' | 'danger' {
	switch (severity) {
		case 'VIOLATION': return 'danger';
		case 'WARNING': return 'warning';
		case 'INFO': return 'info';
		default: return '';
	}
}

/**
 * 判断规则是否可启用（仅ACTIVE扩展模板规则可启停）。
 */
export function canToggleEnabled(rule: { status: string; isBuiltin: string }): boolean {
	return rule.status === 'ACTIVE' && rule.isBuiltin === '0';
}

/**
 * 判断规则是否可删除（仅扩展规则可删除）。
 */
export function canDelete(rule: { isBuiltin: string }): boolean {
	return rule.isBuiltin === '0';
}

/**
 * 判断规则是否可编辑（内置仅排序和备注可编辑，扩展可完整编辑）。
 */
export function isBuiltinRule(rule: { isBuiltin: string }): boolean {
	return rule.isBuiltin === '1';
}

/**
 * 判断模板是否为CUSTOM（自定义草稿）。
 */
export function isCustomTemplate(templateCode: string): boolean {
	return templateCode === 'CUSTOM';
}

/**
 * 根据角色定义过滤可选目标列表。
 */
export function filterTargetsByRole<T extends { id: string }>(
	all: T[],
	_role: string,
	_targetType: TargetType
): T[] {
	return all;
}
