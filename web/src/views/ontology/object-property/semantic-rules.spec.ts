import { describe, expect, it } from 'vitest';
import {
	computeSymmetricWarning,
	filterInverseCandidates,
	onFunctionalChange,
	onTransitiveChange,
	validateSemanticCombination,
} from './semantic-rules';

describe('validateSemanticCombination', () => {
	it('传递+功能拒绝', () => {
		const errors = validateSemanticCombination({
			isFunctional: true, isInverseFunctional: false, isTransitive: true, isSymmetric: false,
		});
		expect(errors.length).toBeGreaterThan(0);
	});

	it('传递+反功能拒绝', () => {
		const errors = validateSemanticCombination({
			isFunctional: false, isInverseFunctional: true, isTransitive: true, isSymmetric: false,
		});
		expect(errors.length).toBeGreaterThan(0);
	});

	it('传递+功能+反功能拒绝', () => {
		const errors = validateSemanticCombination({
			isFunctional: true, isInverseFunctional: true, isTransitive: true, isSymmetric: false,
		});
		expect(errors.length).toBeGreaterThan(0);
	});

	it('功能+反功能（一一对应）允许', () => {
		const errors = validateSemanticCombination({
			isFunctional: true, isInverseFunctional: true, isTransitive: false, isSymmetric: false,
		});
		expect(errors.length).toBe(0);
	});

	it('全部关闭允许', () => {
		const errors = validateSemanticCombination({
			isFunctional: false, isInverseFunctional: false, isTransitive: false, isSymmetric: false,
		});
		expect(errors.length).toBe(0);
	});

	it('仅对称允许', () => {
		const errors = validateSemanticCombination({
			isFunctional: false, isInverseFunctional: false, isTransitive: false, isSymmetric: true,
		});
		expect(errors.length).toBe(0);
	});
});

describe('computeSymmetricWarning', () => {
	it('对称且定义域值域不同时产生警告', () => {
		const warning = computeSymmetricWarning(true, ['1', '2'], ['3']);
		expect(warning).not.toBeNull();
	});

	it('对称且定义域值域相同时无警告', () => {
		const warning = computeSymmetricWarning(true, ['1', '2'], ['2', '1']);
		expect(warning).toBeNull();
	});

	it('非对称时无警告', () => {
		const warning = computeSymmetricWarning(false, ['1'], ['2']);
		expect(warning).toBeNull();
	});

	it('对称但定义域值域均为空时无警告', () => {
		const warning = computeSymmetricWarning(true, [], []);
		expect(warning).toBeNull();
	});
});

describe('onTransitiveChange', () => {
	it('勾选传递时自动清空功能和反功能', () => {
		const result = onTransitiveChange(true, {
			isFunctional: true, isInverseFunctional: true, isTransitive: false, isSymmetric: false,
		});
		expect(result.isTransitive).toBe(true);
		expect(result.isFunctional).toBe(false);
		expect(result.isInverseFunctional).toBe(false);
	});

	it('取消传递时保持其他特性', () => {
		const result = onTransitiveChange(false, {
			isFunctional: true, isInverseFunctional: false, isTransitive: true, isSymmetric: false,
		});
		expect(result.isTransitive).toBe(false);
		expect(result.isFunctional).toBe(true);
	});
});

describe('onFunctionalChange', () => {
	it('勾选功能时自动取消传递', () => {
		const result = onFunctionalChange(true, false, {
			isFunctional: false, isInverseFunctional: false, isTransitive: true, isSymmetric: false,
		});
		expect(result.isFunctional).toBe(true);
		expect(result.isTransitive).toBe(false);
	});

	it('勾选反功能时自动取消传递', () => {
		const result = onFunctionalChange(false, true, {
			isFunctional: false, isInverseFunctional: false, isTransitive: true, isSymmetric: false,
		});
		expect(result.isInverseFunctional).toBe(true);
		expect(result.isTransitive).toBe(false);
	});
});

describe('filterInverseCandidates', () => {
	const mockItems = [
		{ id: '1', name: 'A', isBuiltin: '0', inverseOfId: undefined },
		{ id: '2', name: 'B', isBuiltin: '1', inverseOfId: undefined },
		{ id: '3', name: 'C', isBuiltin: '0', inverseOfId: '9' },
		{ id: '4', name: 'D', isBuiltin: '0', inverseOfId: undefined },
	];

	it('排除自身、内置和已配对属性', () => {
		const result = filterInverseCandidates(mockItems, '1');
		expect(result.length).toBe(1);
		expect(result[0].id).toBe('4');
	});

	it('currentId为空时排除内置和已配对', () => {
		const result = filterInverseCandidates(mockItems, undefined);
		expect(result.length).toBe(2);
		expect(result.map((i) => i.id)).toEqual(['1', '4']);
	});
});
