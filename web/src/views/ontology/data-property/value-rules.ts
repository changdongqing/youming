import type { BaseType, ValueMode } from '/@/types/ontology/data-property';

/**
 * 值域类型与值模式的联动规则。
 *
 * 根据设计文档 §5.2 值域一致性校验矩阵实现。
 */

export interface FieldVisibility {
	regexPattern: boolean;
	formatHint: boolean;
	isUnique: boolean;
	unitCategoryId: boolean;
	unitRefMode: boolean;
	preferredAlias: boolean;
	enumValues: boolean;
	valueSourceRef: boolean;
}

/**
 * 根据值域类型和值模式，返回各表单字段的可用状态。
 * true = 可编辑，false = 禁用。
 */
export function getFieldVisibility(baseType: BaseType | '', valueMode: ValueMode | ''): FieldVisibility {
	const visibility: FieldVisibility = {
		regexPattern: false,
		formatHint: true,
		isUnique: false,
		unitCategoryId: false,
		unitRefMode: false,
		preferredAlias: false,
		enumValues: false,
		valueSourceRef: false,
	};

	if (!baseType) {
		return visibility;
	}

	switch (baseType) {
		case 'BOOLEAN':
		case 'DATE':
			// 正则、唯一、枚举、单位、来源全部禁用
			break;
		case 'NUMERIC':
			visibility.isUnique = true;
			visibility.unitCategoryId = true;
			break;
		case 'TEXT':
			visibility.regexPattern = true;
			visibility.isUnique = true;
			break;
		case 'URI':
			visibility.regexPattern = true;
			visibility.isUnique = true;
			break;
		case 'TEXT_OR_NUMERIC':
			visibility.regexPattern = true;
			visibility.isUnique = true;
			break;
		case 'UNIT_REF':
			visibility.isUnique = true;
			visibility.unitCategoryId = true;
			visibility.unitRefMode = true;
			visibility.preferredAlias = true;
			break;
	}

	// 值模式联动
	if (valueMode === 'CLOSED_ENUM' || valueMode === 'OPEN_ENUM') {
		// 枚举编辑器仅在 TEXT / TEXT_OR_NUMERIC 下可用
		if (baseType === 'TEXT' || baseType === 'TEXT_OR_NUMERIC') {
			visibility.enumValues = true;
		}
	} else if (valueMode === 'EXTERNAL_DICTIONARY') {
		visibility.valueSourceRef = true;
	} else if (valueMode === 'UNIT_DICTIONARY') {
		// UNIT_REF + UNIT_DICTIONARY 时单位分类可选
		if (baseType === 'UNIT_REF') {
			visibility.unitCategoryId = true;
		}
	}

	return visibility;
}

/**
 * 判断当前值域类型是否要求单位分类为必填。
 * 仅 UNIT_REF + UNIT_DICTIONARY 时单位分类为可选（NULL表示不限分类）。
 * NUMERIC 时单位分类也为可选。
 */
export function isUnitCategoryRequired(baseType: BaseType | '', valueMode: ValueMode | ''): boolean {
	// 当前设计：单位分类始终可选，NULL表示不限分类
	return false;
}

/**
 * 判断值域类型为 UNIT_REF 时是否自动锁定值模式为 UNIT_DICTIONARY。
 */
export function autoResolveValueMode(baseType: BaseType | '', currentValueMode: ValueMode | ''): ValueMode | '' {
	if (baseType === 'UNIT_REF') {
		return 'UNIT_DICTIONARY';
	}
	// 非 UNIT_REF 类型不自动修改值模式
	return currentValueMode;
}

/**
 * 判断值域类型为 UNIT_REF 时是否自动锁定单位引用模式。
 */
export function autoResolveUnitRefMode(baseType: BaseType | ''): string {
	if (baseType === 'UNIT_REF') {
		return 'DICTIONARY_SYMBOL';
	}
	return '';
}
