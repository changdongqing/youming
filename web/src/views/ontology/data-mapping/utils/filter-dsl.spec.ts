import { describe, expect, it } from 'vitest';
import {
	createEmptyDsl,
	createEmptyGroup,
	serializeFilterDsl,
	deserializeFilterDsl,
	getDepth,
	canAddGroup,
	isNullOperator,
	isListOperator,
	validateInValues,
	listValueToString,
	stringToListValue,
	MAX_IN_VALUES,
	MAX_NESTING_DEPTH,
} from './filter-dsl';
import type { FilterCondition, FilterGroup } from './filter-dsl';

describe('Filter DSL utilities', () => {
	describe('createEmptyDsl / createEmptyGroup', () => {
		it('creates empty DSL with null root', () => {
			const dsl = createEmptyDsl();
			expect(dsl.root).toBeNull();
		});

		it('creates empty group with default AND logic', () => {
			const group = createEmptyGroup();
			expect(group.logic).toBe('AND');
			expect(group.conditions).toEqual([]);
			expect(group.groups).toEqual([]);
		});

		it('creates empty group with specified logic', () => {
			const group = createEmptyGroup('OR');
			expect(group.logic).toBe('OR');
		});
	});

	describe('isNullOperator', () => {
		it('returns true for IS_NULL', () => {
			expect(isNullOperator('IS_NULL')).toBe(true);
		});
		it('returns true for IS_NOT_NULL', () => {
			expect(isNullOperator('IS_NOT_NULL')).toBe(true);
		});
		it('returns false for EQ', () => {
			expect(isNullOperator('EQ')).toBe(false);
		});
		it('returns false for IN', () => {
			expect(isNullOperator('IN')).toBe(false);
		});
	});

	describe('isListOperator', () => {
		it('returns true for IN', () => {
			expect(isListOperator('IN')).toBe(true);
		});
		it('returns true for NOT_IN', () => {
			expect(isListOperator('NOT_IN')).toBe(true);
		});
		it('returns false for EQ', () => {
			expect(isListOperator('EQ')).toBe(false);
		});
	});

	describe('serializeFilterDsl / deserializeFilterDsl', () => {
		it('serializes null root as empty string', () => {
			expect(serializeFilterDsl(createEmptyDsl())).toBe('');
		});

		it('serializes and deserializes a simple group', () => {
			const condition: FilterCondition = { column: 'status', operator: 'EQ', value: 'ACTIVE' };
			const group = createEmptyGroup('AND');
			group.conditions.push(condition);
			const dsl = { root: group };

			const json = serializeFilterDsl(dsl);
			const restored = deserializeFilterDsl(json);

			expect(restored).not.toBeNull();
			expect(restored!.root).not.toBeNull();
			expect(restored!.root!.logic).toBe('AND');
			expect(restored!.root!.conditions).toHaveLength(1);
			expect(restored!.root!.conditions[0].column).toBe('status');
		});

		it('deserializes empty string as null root', () => {
			const result = deserializeFilterDsl('');
			expect(result?.root).toBeNull();
		});

		it('deserializes null/undefined as null root', () => {
			expect(deserializeFilterDsl(null)?.root).toBeNull();
			expect(deserializeFilterDsl(undefined)?.root).toBeNull();
		});

		it('deserializes malformed JSON as null', () => {
			expect(deserializeFilterDsl('{invalid json')).toBeNull();
		});

		it('handles nested groups serialization round-trip', () => {
			const innerGroup = createEmptyGroup('OR');
			innerGroup.conditions.push({ column: 'age', operator: 'GT', value: '18' });
			const outerGroup = createEmptyGroup('AND');
			outerGroup.conditions.push({ column: 'status', operator: 'EQ', value: 'ACTIVE' });
			outerGroup.groups.push(innerGroup);
			const dsl = { root: outerGroup };

			const json = serializeFilterDsl(dsl);
			const restored = deserializeFilterDsl(json);

			expect(restored!.root!.groups).toHaveLength(1);
			expect(restored!.root!.groups[0].logic).toBe('OR');
			expect(restored!.root!.groups[0].conditions[0].column).toBe('age');
		});
	});

	describe('getDepth / canAddGroup', () => {
		it('returns 0 for null group', () => {
			expect(getDepth(null)).toBe(0);
		});

		it('returns 1 for flat group without children', () => {
			const group = createEmptyGroup();
			expect(getDepth(group)).toBe(1);
		});

		it('returns 2 for group with one child group', () => {
			const group = createEmptyGroup();
			group.groups.push(createEmptyGroup());
			expect(getDepth(group)).toBe(2);
		});

		it('returns 3 for doubly-nested group', () => {
			const group = createEmptyGroup();
			const child = createEmptyGroup();
			child.groups.push(createEmptyGroup());
			group.groups.push(child);
			expect(getDepth(group)).toBe(3);
		});

		it('canAddGroup returns true when depth < MAX_NESTING_DEPTH', () => {
			const group = createEmptyGroup(); // depth 1
			expect(canAddGroup(group)).toBe(true);
		});

		it('canAddGroup returns false when depth reaches MAX_NESTING_DEPTH', () => {
			const group = createEmptyGroup();
			const child = createEmptyGroup();
			child.groups.push(createEmptyGroup());
			group.groups.push(child); // depth 3 = MAX_NESTING_DEPTH
			expect(canAddGroup(group)).toBe(false);
		});

		it('canAddGroup returns true for null group', () => {
			expect(canAddGroup(null)).toBe(true);
		});
	});

	describe('validateInValues', () => {
		it('returns true for values within limit', () => {
			expect(validateInValues(['a', 'b', 'c'])).toBe(true);
		});

		it('returns true for exactly MAX_IN_VALUES values', () => {
			const values = Array(MAX_IN_VALUES).fill('x');
			expect(validateInValues(values)).toBe(true);
		});

		it('returns false when exceeding MAX_IN_VALUES', () => {
			const values = Array(MAX_IN_VALUES + 1).fill('x');
			expect(validateInValues(values)).toBe(false);
		});

		it('returns true for empty array', () => {
			expect(validateInValues([])).toBe(true);
		});
	});

	describe('listValueToString / stringToListValue', () => {
		it('joins values with comma', () => {
			expect(listValueToString(['a', 'b', 'c'])).toBe('a, b, c');
		});

		it('returns empty string for empty array', () => {
			expect(listValueToString([])).toBe('');
		});

		it('splits comma-separated string', () => {
			expect(stringToListValue('a, b, c')).toEqual(['a', 'b', 'c']);
		});

		it('trims whitespace', () => {
			expect(stringToListValue('  a  ,  b  ')).toEqual(['a', 'b']);
		});

		it('filters empty entries', () => {
			expect(stringToListValue('a, , b,')).toEqual(['a', 'b']);
		});

		it('returns empty array for empty string', () => {
			expect(stringToListValue('')).toEqual([]);
		});
	});

	describe('constants', () => {
		it('MAX_IN_VALUES is 100', () => {
			expect(MAX_IN_VALUES).toBe(100);
		});

		it('MAX_NESTING_DEPTH is 3', () => {
			expect(MAX_NESTING_DEPTH).toBe(3);
		});
	});
});
