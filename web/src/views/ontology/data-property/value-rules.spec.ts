import { describe, expect, it } from 'vitest';
import {
	autoResolveUnitRefMode,
	autoResolveValueMode,
	getFieldVisibility
} from './value-rules';

describe('getFieldVisibility', () => {
	it('BOOLEAN 类型下正则/唯一/枚举/单位/来源均禁用', () => {
		const v = getFieldVisibility('BOOLEAN', 'FREE');
		expect(v.regexPattern).toBe(false);
		expect(v.isUnique).toBe(false);
		expect(v.enumValues).toBe(false);
		expect(v.unitCategoryId).toBe(false);
		expect(v.valueSourceRef).toBe(false);
		expect(v.formatHint).toBe(true);
	});

	it('DATE 类型下正则/唯一/枚举/单位均禁用', () => {
		const v = getFieldVisibility('DATE', 'FREE');
		expect(v.regexPattern).toBe(false);
		expect(v.isUnique).toBe(false);
		expect(v.enumValues).toBe(false);
		expect(v.unitCategoryId).toBe(false);
	});

	it('NUMERIC 类型下唯一和单位分类可用，正则禁用', () => {
		const v = getFieldVisibility('NUMERIC', 'FREE');
		expect(v.isUnique).toBe(true);
		expect(v.unitCategoryId).toBe(true);
		expect(v.regexPattern).toBe(false);
		expect(v.enumValues).toBe(false);
	});

	it('TEXT + CLOSED_ENUM 时枚举编辑器可用', () => {
		const v = getFieldVisibility('TEXT', 'CLOSED_ENUM');
		expect(v.enumValues).toBe(true);
		expect(v.regexPattern).toBe(true);
	});

	it('TEXT + FREE 时枚举编辑器不可用', () => {
		const v = getFieldVisibility('TEXT', 'FREE');
		expect(v.enumValues).toBe(false);
	});

	it('TEXT + OPEN_ENUM 时枚举编辑器可用', () => {
		const v = getFieldVisibility('TEXT', 'OPEN_ENUM');
		expect(v.enumValues).toBe(true);
	});

	it('TEXT_OR_NUMERIC + OPEN_ENUM 时枚举编辑器可用', () => {
		const v = getFieldVisibility('TEXT_OR_NUMERIC', 'OPEN_ENUM');
		expect(v.enumValues).toBe(true);
		expect(v.regexPattern).toBe(true);
	});

	it('URI 类型下正则和唯一可用，枚举不可用', () => {
		const v = getFieldVisibility('URI', 'FREE');
		expect(v.regexPattern).toBe(true);
		expect(v.isUnique).toBe(true);
		expect(v.enumValues).toBe(false);
	});

	it('UNIT_REF + UNIT_DICTIONARY 时单位分类、引用模式、别名均可用', () => {
		const v = getFieldVisibility('UNIT_REF', 'UNIT_DICTIONARY');
		expect(v.unitCategoryId).toBe(true);
		expect(v.unitRefMode).toBe(true);
		expect(v.preferredAlias).toBe(true);
		expect(v.isUnique).toBe(true);
	});

	it('NUMERIC + EXTERNAL_DICTIONARY 时值源可用', () => {
		// 理论上 NUMERIC 不应配 EXTERNAL_DICTIONARY，但规则函数仍应正确返回
		const v = getFieldVisibility('NUMERIC', 'EXTERNAL_DICTIONARY');
		expect(v.valueSourceRef).toBe(true);
	});

	it('空 baseType 时所有字段禁用', () => {
		const v = getFieldVisibility('', '');
		expect(v.regexPattern).toBe(false);
		expect(v.isUnique).toBe(false);
		expect(v.enumValues).toBe(false);
		expect(v.unitCategoryId).toBe(false);
		expect(v.formatHint).toBe(true);
	});
});

describe('autoResolveValueMode', () => {
	it('UNIT_REF 自动锁定为 UNIT_DICTIONARY', () => {
		expect(autoResolveValueMode('UNIT_REF', 'FREE')).toBe('UNIT_DICTIONARY');
	});

	it('TEXT 不自动修改值模式', () => {
		expect(autoResolveValueMode('TEXT', 'FREE')).toBe('FREE');
	});

	it('空类型不自动修改值模式', () => {
		expect(autoResolveValueMode('', '')).toBe('');
	});
});

describe('autoResolveUnitRefMode', () => {
	it('UNIT_REF 返回 DICTIONARY_SYMBOL', () => {
		expect(autoResolveUnitRefMode('UNIT_REF')).toBe('DICTIONARY_SYMBOL');
	});

	it('TEXT 返回空字符串', () => {
		expect(autoResolveUnitRefMode('TEXT')).toBe('');
	});
});
