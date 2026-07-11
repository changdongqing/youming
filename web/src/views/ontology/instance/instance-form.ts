import type { LiteralType } from '/@/types/ontology/instance';

/**
 * 根据数据属性的baseType和valueMode返回表单字段可见性矩阵。
 *
 * 根据设计文档 §9.2 Schema驱动表单实现。
 */
export interface ValueFieldVisibility {
	/** 是否显示枚举选择器 */
	enumSelect: boolean;
	/** 是否允许枚举新增值（OPEN_ENUM） */
	enumAllowCreate: boolean;
	/** 是否显示日期控件 */
	datePicker: boolean;
	/** 是否显示数值输入 */
	numericInput: boolean;
	/** 是否显示布尔选择 */
	booleanSelect: boolean;
	/** 是否显示URI文本输入 */
	uriInput: boolean;
	/** 是否显示单位级联选择 */
	unitRefSelect: boolean;
	/** 是否显示文本/数值类型选择 */
	textOrNumericTypeSelect: boolean;
	/** 是否显示普通文本输入 */
	textInput: boolean;
}

/**
 * 根据基础类型和值模式返回字段可见性矩阵。
 */
export function getValueFieldVisibility(baseType: string, valueMode: string): ValueFieldVisibility {
	const visibility: ValueFieldVisibility = {
		enumSelect: false,
		enumAllowCreate: false,
		datePicker: false,
		numericInput: false,
		booleanSelect: false,
		uriInput: false,
		unitRefSelect: false,
		textOrNumericTypeSelect: false,
		textInput: false,
	};

	switch (baseType) {
		case 'TEXT':
			if (valueMode === 'CLOSED_ENUM') {
				visibility.enumSelect = true;
			} else if (valueMode === 'OPEN_ENUM') {
				visibility.enumSelect = true;
				visibility.enumAllowCreate = true;
			} else {
				visibility.textInput = true;
			}
			break;
		case 'URI':
			visibility.uriInput = true;
			break;
		case 'DATE':
			visibility.datePicker = true;
			break;
		case 'NUMERIC':
			visibility.numericInput = true;
			break;
		case 'BOOLEAN':
			visibility.booleanSelect = true;
			break;
		case 'UNIT_REF':
			visibility.unitRefSelect = true;
			break;
		case 'TEXT_OR_NUMERIC':
			visibility.textOrNumericTypeSelect = true;
			break;
	}

	return visibility;
}

/**
 * TEXT_OR_NUMERIC基础类型的可选字面量类型。
 */
export function getLiteralTypeOptions(baseType: string): LiteralType[] {
	if (baseType === 'TEXT_OR_NUMERIC') {
		return ['STRING', 'INTEGER', 'DECIMAL'];
	}
	return [];
}

/**
 * 前端预校验和规范化字面量值。
 *
 * 根据设计文档 §2.3 字面量类型规范化规则实现。
 * @returns 规范化后的值，校验失败返回null
 */
export function normalizeLiteralValue(value: string, literalType: LiteralType): string | null {
	if (!value || value.trim() === '') {
		return null;
	}
	try {
		switch (literalType) {
			case 'STRING':
				return value;
			case 'URI': {
				const trimmed = value.trim();
				if (!/^[A-Za-z][A-Za-z0-9+.-]*:./.test(trimmed)) {
					return null;
				}
				return trimmed;
			}
			case 'DATE': {
				const trimmed = value.trim();
				if (trimmed.includes('-')) {
					if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
						return null;
					}
					return trimmed;
				}
				if (!/^\d{8}$/.test(trimmed)) {
					return null;
				}
				return `${trimmed.slice(0, 4)}-${trimmed.slice(4, 6)}-${trimmed.slice(6, 8)}`;
			}
			case 'INTEGER': {
				const num = Number(value.trim());
				if (!Number.isFinite(num) || !Number.isInteger(num)) {
					return null;
				}
				return String(BigInt(value.trim()));
			}
			case 'DECIMAL': {
				const num = Number(value.trim());
				if (!Number.isFinite(num)) {
					return null;
				}
				return String(num);
			}
			case 'BOOLEAN': {
				const v = value.trim().toLowerCase();
				if (v === 'true' || v === '1') {
					return 'true';
				}
				if (v === 'false' || v === '0') {
					return 'false';
				}
				return null;
			}
			default:
				return null;
		}
	} catch {
		return null;
	}
}

/**
 * 前端预校验数据属性值。
 * @returns 错误消息，null表示通过
 */
export function validateDataValue(
	baseType: string,
	valueMode: string,
	literalType: LiteralType,
	value: string
): string | null {
	// 基础类型兼容性
	const compatError = checkBaseTypeCompatibility(baseType, literalType);
	if (compatError) {
		return compatError;
	}

	const normalized = normalizeLiteralValue(value, literalType);
	if (normalized === null) {
		return `值与类型${literalType}不匹配`;
	}

	// CLOSED_ENUM需命中枚举（前端仅做非空预校验，后端为权威）
	if (valueMode === 'CLOSED_ENUM' && !normalized) {
		return '闭枚举属性必须选择有效枚举值';
	}

	return null;
}

function checkBaseTypeCompatibility(baseType: string, literalType: LiteralType): string | null {
	switch (baseType) {
		case 'TEXT':
			return literalType === 'STRING' ? null : 'TEXT基础类型只能使用STRING字面量';
		case 'URI':
			return literalType === 'URI' ? null : 'URI基础类型只能使用URI字面量';
		case 'DATE':
			return literalType === 'DATE' ? null : 'DATE基础类型只能使用DATE字面量';
		case 'NUMERIC':
			return literalType === 'INTEGER' || literalType === 'DECIMAL'
				? null
				: 'NUMERIC基础类型只能使用INTEGER或DECIMAL字面量';
		case 'BOOLEAN':
			return literalType === 'BOOLEAN' ? null : 'BOOLEAN基础类型只能使用BOOLEAN字面量';
		case 'UNIT_REF':
			return literalType === 'STRING' ? null : 'UNIT_REF基础类型只能使用STRING字面量';
		case 'TEXT_OR_NUMERIC':
			return ['STRING', 'INTEGER', 'DECIMAL'].includes(literalType)
				? null
				: 'TEXT_OR_NUMERIC只能使用STRING/INTEGER/DECIMAL字面量';
		default:
			return `未知基础类型: ${baseType}`;
	}
}
