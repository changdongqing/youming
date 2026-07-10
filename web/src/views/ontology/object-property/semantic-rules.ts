/**
 * 对象属性语义特性联动规则。
 *
 * 根据设计文档 §5.2 语义特性组合校验实现。
 */

export interface SemanticFlags {
	isFunctional: boolean;
	isInverseFunctional: boolean;
	isTransitive: boolean;
	isSymmetric: boolean;
}

/**
 * 校验语义特性组合是否合法。
 * 返回错误消息列表，空数组表示合法。
 */
export function validateSemanticCombination(flags: SemanticFlags): string[] {
	const errors: string[] = [];
	// OWL 2 DL: 传递属性不能同时为功能性或反功能性
	if (flags.isTransitive && (flags.isFunctional || flags.isInverseFunctional)) {
		errors.push('传递性对象属性不能同时声明功能性或反功能性（违反OWL 2 DL简单对象属性限制）');
	}
	return errors;
}

/**
 * 计算对称属性的语义警告（不阻断提交）。
 */
export function computeSymmetricWarning(
	isSymmetric: boolean,
	domainTypeIds: string[],
	rangeTypeIds: string[]
): string | null {
	if (!isSymmetric) return null;
	const domainSet = new Set(domainTypeIds);
	const rangeSet = new Set(rangeTypeIds);
	if (domainSet.size === 0 || rangeSet.size === 0) return null;
	// 检查两个集合是否相同
	if (domainSet.size !== rangeSet.size) {
		return '对称属性的定义域和值域集合不同，可能通过对称推理使关系两端获得额外类型';
	}
	for (const id of domainSet) {
		if (!rangeSet.has(id)) {
			return '对称属性的定义域和值域集合不同，可能通过对称推理使关系两端获得额外类型';
		}
	}
	return null;
}

/**
 * 传递性变化时，自动禁用并清空功能性和反功能性。
 */
export function onTransitiveChange(
	isTransitive: boolean,
	current: SemanticFlags
): SemanticFlags {
	if (isTransitive) {
		return { ...current, isTransitive: true, isFunctional: false, isInverseFunctional: false };
	}
	return { ...current, isTransitive: false };
}

/**
 * 功能性或反功能性变化时，自动禁用传递性。
 */
export function onFunctionalChange(
	isFunctional: boolean,
	isInverseFunctional: boolean,
	current: SemanticFlags
): SemanticFlags {
	if (isFunctional || isInverseFunctional) {
		return { ...current, isFunctional, isInverseFunctional, isTransitive: false };
	}
	return { ...current, isFunctional, isInverseFunctional };
}

/**
 * 判断逆属性候选列表是否有效：不能选自身、内置属性或已配对属性。
 */
export function filterInverseCandidates<T extends { id: string; isBuiltin: string; inverseOfId?: string }>(
	all: T[],
	currentId?: string
): T[] {
	return all.filter((item) => {
		if (currentId && item.id === currentId) return false;
		if (item.isBuiltin === '1') return false;
		if (item.inverseOfId) return false;
		return true;
	});
}
