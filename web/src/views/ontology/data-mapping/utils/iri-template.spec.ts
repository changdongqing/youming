import { describe, expect, it } from 'vitest';
import { extractIriVariables, validateIriVariables, hasInvalidVariables, appendVariable, renderLocalName, renderFullIri } from './iri-template';
import type { KeyColumnConfig } from './iri-template';

describe('IRI template utilities', () => {
	describe('extractIriVariables', () => {
		it('extracts simple variable without normalizer', () => {
			const vars = extractIriVariables('user-account/{user_id}');
			expect(vars).toHaveLength(1);
			expect(vars[0].column).toBe('user_id');
			expect(vars[0].normalizer).toBeUndefined();
		});

		it('extracts variable with normalizer', () => {
			const vars = extractIriVariables('user-account/{user_id|LONG}');
			expect(vars).toHaveLength(1);
			expect(vars[0].column).toBe('user_id');
			expect(vars[0].normalizer).toBe('LONG');
		});

		it('extracts multiple variables', () => {
			const vars = extractIriVariables('dept/{dept_id|LONG}/user/{user_id|URL}');
			expect(vars).toHaveLength(2);
			expect(vars[0].column).toBe('dept_id');
			expect(vars[1].column).toBe('user_id');
		});

		it('returns empty for empty template', () => {
			expect(extractIriVariables('')).toEqual([]);
			expect(extractIriVariables('static-prefix')).toEqual([]);
		});

		it('handles whitespace in variable content', () => {
			const vars = extractIriVariables('{ user_id | LONG }');
			expect(vars).toHaveLength(1);
			expect(vars[0].column).toBe('user_id');
			expect(vars[0].normalizer).toBe('LONG');
		});
	});

	describe('validateIriVariables', () => {
		const keyColumns: KeyColumnConfig[] = [
			{ column: 'user_id', order: 1, normalizer: 'LONG' },
			{ column: 'dept_id', order: 2 },
		];

		it('returns empty array when all variables are from keyColumns', () => {
			const invalid = validateIriVariables('user/{user_id}', keyColumns);
			expect(invalid).toEqual([]);
		});

		it('returns invalid variables not in keyColumns', () => {
			const invalid = validateIriVariables('user/{username}', keyColumns);
			expect(invalid).toHaveLength(1);
			expect(invalid[0].column).toBe('username');
		});

		it('returns multiple invalid variables', () => {
			const invalid = validateIriVariables('{username}/{email}', keyColumns);
			expect(invalid).toHaveLength(2);
		});

		it('passes when mixing valid and invalid variables', () => {
			const invalid = validateIriVariables('user/{user_id}/{username}', keyColumns);
			expect(invalid).toHaveLength(1);
			expect(invalid[0].column).toBe('username');
		});
	});

	describe('hasInvalidVariables', () => {
		const keyColumns: KeyColumnConfig[] = [{ column: 'id', order: 1 }];

		it('returns false when all variables valid', () => {
			expect(hasInvalidVariables('prefix/{id}', keyColumns)).toBe(false);
		});

		it('returns true when variable not in keyColumns', () => {
			expect(hasInvalidVariables('prefix/{name}', keyColumns)).toBe(true);
		});

		it('returns false for empty template', () => {
			expect(hasInvalidVariables('', keyColumns)).toBe(false);
		});
	});

	describe('appendVariable', () => {
		it('appends variable without normalizer', () => {
			expect(appendVariable('prefix/', 'user_id')).toBe('prefix/{user_id}');
		});

		it('appends variable with normalizer', () => {
			expect(appendVariable('prefix/', 'user_id', 'LONG')).toBe('prefix/{user_id|LONG}');
		});

		it('appends to empty template', () => {
			expect(appendVariable('', 'user_id')).toBe('{user_id}');
		});
	});

	describe('renderLocalName', () => {
		it('replaces variables with sample values', () => {
			const result = renderLocalName('user/{user_id}/dept/{dept_id}', {
				user_id: '123',
				dept_id: '456',
			});
			expect(result).toBe('user/123/dept/456');
		});

		it('leaves variable as-is when sample value missing', () => {
			const result = renderLocalName('user/{user_id}', {});
			expect(result).toBe('user/{user_id}');
		});

		it('handles empty template', () => {
			expect(renderLocalName('', { user_id: '123' })).toBe('');
		});
	});

	describe('renderFullIri', () => {
		it('concatenates namespace and local name with separator', () => {
			const result = renderFullIri('http://example.org', 'user/{user_id}', { user_id: '123' });
			expect(result).toBe('http://example.org/user/123');
		});

		it('does not add separator when namespace ends with /', () => {
			const result = renderFullIri('http://example.org/', 'user/{user_id}', { user_id: '123' });
			expect(result).toBe('http://example.org/user/123');
		});

		it('does not add separator when namespace ends with #', () => {
			const result = renderFullIri('http://example.org#', 'user/{user_id}', { user_id: '123' });
			expect(result).toBe('http://example.org#user/123');
		});

		it('returns local name when namespace is empty', () => {
			const result = renderFullIri('', 'user/{user_id}', { user_id: '123' });
			expect(result).toBe('user/123');
		});
	});
});
