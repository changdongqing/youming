import { describe, expect, it } from 'vitest';
import {
	canDelete,
	canToggleEnabled,
	getBindingRoles,
	getSeverityTagType,
	getStatusTagType,
	getTargetTypeLabel,
	isCustomTemplate,
	isBuiltinRule,
} from './rule-form';

const mockTemplates = [
	{
		templateCode: 'GLOBAL_UNIQUE_VALUE',
		templateVersion: 1,
		category: 'PROPERTY',
		subType: 'UNIQUENESS',
		formalizationMode: 'GENERATED',
		validationMode: 'SHACL_SPARQL',
		executorCode: 'GLOBAL_UNIQUE_VALUE',
		bindingRoles: [
			{ role: 'TARGET_CLASS', targetType: 'ENTITY_TYPE', minCount: 1, maxCount: 1, description: '目标类' },
			{ role: 'KEY_PROPERTY', targetType: 'DATA_PROPERTY', minCount: 1, maxCount: 1, description: '唯一属性' },
		],
	},
	{
		templateCode: 'CUSTOM',
		templateVersion: 1,
		category: 'PROPERTY',
		subType: 'CUSTOM',
		formalizationMode: 'CUSTOM_DRAFT',
		validationMode: 'NONE',
		executorCode: undefined,
		bindingRoles: [{ role: 'CUSTOM_TARGET', targetType: 'ENTITY_TYPE', minCount: 0, maxCount: 5, description: '自定义目标' }],
	},
];

describe('getBindingRoles', () => {
	it('找到模板时返回绑定角色', () => {
		const roles = getBindingRoles(mockTemplates, 'GLOBAL_UNIQUE_VALUE');
		expect(roles.length).toBe(2);
		expect(roles[0].role).toBe('TARGET_CLASS');
	});

	it('未找到模板时返回空数组', () => {
		const roles = getBindingRoles(mockTemplates, 'NOT_EXIST');
		expect(roles.length).toBe(0);
	});
});

describe('getTargetTypeLabel', () => {
	it('返回中文标签', () => {
		expect(getTargetTypeLabel('ENTITY_TYPE')).toBe('实体类型');
		expect(getTargetTypeLabel('DATA_PROPERTY')).toBe('数据属性');
		expect(getTargetTypeLabel('OBJECT_PROPERTY')).toBe('对象属性');
		expect(getTargetTypeLabel('UNIT_CATEGORY')).toBe('单位分类');
	});
});

describe('getStatusTagType', () => {
	it('ACTIVE返回success', () => {
		expect(getStatusTagType('ACTIVE')).toBe('success');
	});
	it('DRAFT返回info', () => {
		expect(getStatusTagType('DRAFT')).toBe('info');
	});
	it('BLOCKED返回danger', () => {
		expect(getStatusTagType('BLOCKED')).toBe('danger');
	});
});

describe('getSeverityTagType', () => {
	it('VIOLATION返回danger', () => {
		expect(getSeverityTagType('VIOLATION')).toBe('danger');
	});
	it('WARNING返回warning', () => {
		expect(getSeverityTagType('WARNING')).toBe('warning');
	});
	it('INFO返回info', () => {
		expect(getSeverityTagType('INFO')).toBe('info');
	});
});

describe('canToggleEnabled', () => {
	it('ACTIVE扩展规则可启停', () => {
		expect(canToggleEnabled({ status: 'ACTIVE', isBuiltin: '0' })).toBe(true);
	});
	it('内置规则不可启停', () => {
		expect(canToggleEnabled({ status: 'ACTIVE', isBuiltin: '1' })).toBe(false);
	});
	it('BLOCKED规则不可启停', () => {
		expect(canToggleEnabled({ status: 'BLOCKED', isBuiltin: '0' })).toBe(false);
	});
	it('DRAFT规则不可启停', () => {
		expect(canToggleEnabled({ status: 'DRAFT', isBuiltin: '0' })).toBe(false);
	});
});

describe('canDelete', () => {
	it('扩展规则可删除', () => {
		expect(canDelete({ isBuiltin: '0' })).toBe(true);
	});
	it('内置规则不可删除', () => {
		expect(canDelete({ isBuiltin: '1' })).toBe(false);
	});
});

describe('isBuiltinRule', () => {
	it('内置返回true', () => {
		expect(isBuiltinRule({ isBuiltin: '1' })).toBe(true);
	});
	it('扩展返回false', () => {
		expect(isBuiltinRule({ isBuiltin: '0' })).toBe(false);
	});
});

describe('isCustomTemplate', () => {
	it('CUSTOM返回true', () => {
		expect(isCustomTemplate('CUSTOM')).toBe(true);
	});
	it('其他模板返回false', () => {
		expect(isCustomTemplate('GLOBAL_UNIQUE_VALUE')).toBe(false);
	});
});
