import { describe, expect, it } from 'vitest';
import type { EntityTypeTreeNode } from '/@/types/ontology/entity-type';
import { collectInvalidParentIds, filterEntityTypeTree } from './tree-utils';

const node = (key: string, id: string, name: string, label: string, children: EntityTypeTreeNode[] = []): EntityTypeTreeNode => ({
	key,
	id,
	name,
	label,
	iri: `http://example.org#${name}`,
	isAbstract: '0',
	isBuiltin: '1',
	children,
});

describe('entity type tree utilities', () => {
	it('keeps the complete parent chain when filtering a nested match', () => {
		const tree = [node('1', '1', 'StructuralElement', '层次', [node('1/2', '2', 'Clause', '条款')])];

		const result = filterEntityTypeTree(tree, '条款');

		expect(result).toHaveLength(1);
		expect(result[0].children).toHaveLength(1);
		expect(result[0].children[0].id).toBe('2');
	});

	it('collects current node and descendants from every multiple-inheritance occurrence', () => {
		const clauseUnderStructure = node('1/3', '3', 'Clause', '条款', [node('1/3/4', '4', 'TitledClause', '有标题条')]);
		const clauseUnderInformation = node('2/3', '3', 'Clause', '条款', [node('2/3/5', '5', 'UntitledClause', '无标题条')]);
		const tree = [node('1', '1', 'StructuralElement', '层次', [clauseUnderStructure]), node('2', '2', 'InformationUnit', '信息单元', [clauseUnderInformation])];

		const invalid = collectInvalidParentIds(tree, '3');

		expect([...invalid].sort()).toEqual(['3', '4', '5']);
	});
});
