import { describe, expect, it } from 'vitest';
import {
	getLiteralTypeOptions,
	getValueFieldVisibility,
	normalizeLiteralValue,
	validateDataValue,
} from './instance-form';

describe('getValueFieldVisibility', () => {
	it('TEXT + FREE 应显示普通文本输入', () => {
		const v = getValueFieldVisibility('TEXT', 'FREE');
		expect(v.textInput).toBe(true);
		expect(v.enumSelect).toBe(false);
	});

	it('TEXT + CLOSED_ENUM 应显示不可新增枚举选择器', () => {
		const v = getValueFieldVisibility('TEXT', 'CLOSED_ENUM');
		expect(v.enumSelect).toBe(true);
		expect(v.enumAllowCreate).toBe(false);
	});

	it('TEXT + OPEN_ENUM 应显示可新增枚举选择器', () => {
		const v = getValueFieldVisibility('TEXT', 'OPEN_ENUM');
		expect(v.enumSelect).toBe(true);
		expect(v.enumAllowCreate).toBe(true);
	});

	it('DATE + FREE 应显示日期控件', () => {
		const v = getValueFieldVisibility('DATE', 'FREE');
		expect(v.datePicker).toBe(true);
	});

	it('NUMERIC + FREE 应显示数值输入', () => {
		const v = getValueFieldVisibility('NUMERIC', 'FREE');
		expect(v.numericInput).toBe(true);
	});

	it('BOOLEAN + FREE 应显示布尔选择', () => {
		const v = getValueFieldVisibility('BOOLEAN', 'FREE');
		expect(v.booleanSelect).toBe(true);
	});

	it('URI + FREE 应显示URI文本输入', () => {
		const v = getValueFieldVisibility('URI', 'FREE');
		expect(v.uriInput).toBe(true);
	});

	it('UNIT_REF + UNIT_DICTIONARY 应显示单位级联选择', () => {
		const v = getValueFieldVisibility('UNIT_REF', 'UNIT_DICTIONARY');
		expect(v.unitRefSelect).toBe(true);
	});

	it('TEXT_OR_NUMERIC + FREE 应显示类型选择', () => {
		const v = getValueFieldVisibility('TEXT_OR_NUMERIC', 'FREE');
		expect(v.textOrNumericTypeSelect).toBe(true);
	});
});

describe('getLiteralTypeOptions', () => {
	it('TEXT_OR_NUMERIC 返回STRING/INTEGER/DECIMAL', () => {
		const opts = getLiteralTypeOptions('TEXT_OR_NUMERIC');
		expect(opts).toEqual(['STRING', 'INTEGER', 'DECIMAL']);
	});

	it('非TEXT_OR_NUMERIC 返回空数组', () => {
		expect(getLiteralTypeOptions('TEXT')).toEqual([]);
		expect(getLiteralTypeOptions('DATE')).toEqual([]);
	});
});

describe('normalizeLiteralValue', () => {
	it('STRING 原样返回', () => {
		expect(normalizeLiteralValue('hello', 'STRING')).toBe('hello');
	});

	it('URI 去空白且必须为绝对IRI', () => {
		expect(normalizeLiteralValue('  http://example.org/test  ', 'URI')).toBe('http://example.org/test');
		expect(normalizeLiteralValue('not-a-uri', 'URI')).toBeNull();
	});

	it('DATE YYYYMMDD 转为 YYYY-MM-DD', () => {
		expect(normalizeLiteralValue('20240929', 'DATE')).toBe('2024-09-29');
	});

	it('DATE YYYY-MM-DD 原样返回', () => {
		expect(normalizeLiteralValue('2024-09-29', 'DATE')).toBe('2024-09-29');
	});

	it('DATE 非法格式返回null', () => {
		expect(normalizeLiteralValue('2024/09/29', 'DATE')).toBeNull();
		expect(normalizeLiteralValue('not-a-date', 'DATE')).toBeNull();
	});

	it('INTEGER 规范化为无前导加号整数', () => {
		expect(normalizeLiteralValue('100', 'INTEGER')).toBe('100');
		expect(normalizeLiteralValue('  42  ', 'INTEGER')).toBe('42');
	});

	it('INTEGER 非整数返回null', () => {
		expect(normalizeLiteralValue('3.14', 'INTEGER')).toBeNull();
		expect(normalizeLiteralValue('abc', 'INTEGER')).toBeNull();
	});

	it('DECIMAL 规范化为纯数字字符串', () => {
		expect(normalizeLiteralValue('3.14', 'DECIMAL')).toBe('3.14');
		expect(normalizeLiteralValue('  100  ', 'DECIMAL')).toBe('100');
	});

	it('BOOLEAN 规范化为true/false', () => {
		expect(normalizeLiteralValue('true', 'BOOLEAN')).toBe('true');
		expect(normalizeLiteralValue('1', 'BOOLEAN')).toBe('true');
		expect(normalizeLiteralValue('false', 'BOOLEAN')).toBe('false');
		expect(normalizeLiteralValue('0', 'BOOLEAN')).toBe('false');
	});

	it('BOOLEAN 非法值返回null', () => {
		expect(normalizeLiteralValue('yes', 'BOOLEAN')).toBeNull();
	});

	it('空值返回null', () => {
		expect(normalizeLiteralValue('', 'STRING')).toBeNull();
		expect(normalizeLiteralValue('  ', 'STRING')).toBeNull();
	});
});

describe('validateDataValue', () => {
	it('TEXT + STRING 通过', () => {
		expect(validateDataValue('TEXT', 'FREE', 'STRING', 'hello')).toBeNull();
	});

	it('TEXT + STRING 类型不匹配报错', () => {
		expect(validateDataValue('TEXT', 'FREE', 'INTEGER', '100')).toBe('TEXT基础类型只能使用STRING字面量');
	});

	it('NUMERIC + INTEGER 通过', () => {
		expect(validateDataValue('NUMERIC', 'FREE', 'INTEGER', '100')).toBeNull();
	});

	it('NUMERIC + DECIMAL 通过', () => {
		expect(validateDataValue('NUMERIC', 'FREE', 'DECIMAL', '3.14')).toBeNull();
	});

	it('DATE + DATE 通过', () => {
		expect(validateDataValue('DATE', 'FREE', 'DATE', '2024-09-29')).toBeNull();
	});

	it('DATE + DATE 紧凑格式通过', () => {
		expect(validateDataValue('DATE', 'FREE', 'DATE', '20240929')).toBeNull();
	});

	it('BOOLEAN + BOOLEAN 通过', () => {
		expect(validateDataValue('BOOLEAN', 'FREE', 'BOOLEAN', 'true')).toBeNull();
	});

	it('URI + URI 绝对IRI通过', () => {
		expect(validateDataValue('URI', 'FREE', 'URI', 'http://example.org/test')).toBeNull();
	});

	it('URI + URI 非绝对IRI报错', () => {
		expect(validateDataValue('URI', 'FREE', 'URI', 'not-a-uri')).not.toBeNull();
	});

	it('TEXT_OR_NUMERIC + STRING 通过', () => {
		expect(validateDataValue('TEXT_OR_NUMERIC', 'FREE', 'STRING', '符合条件')).toBeNull();
	});

	it('TEXT_OR_NUMERIC + INTEGER 通过', () => {
		expect(validateDataValue('TEXT_OR_NUMERIC', 'FREE', 'INTEGER', '100')).toBeNull();
	});

	it('TEXT_OR_NUMERIC + BOOLEAN 不通过', () => {
		expect(validateDataValue('TEXT_OR_NUMERIC', 'FREE', 'BOOLEAN', 'true')).not.toBeNull();
	});
});
