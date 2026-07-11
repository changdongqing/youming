import { describe, expect, it } from 'vitest';
import {
	collectRangeTypeIds,
	filterOptionsByRangeTypes,
	formatRelationLabel,
	isFunctionalSelect,
	isSchemaResourceRelation,
} from './relation-editor';
import type {
	InstanceObjectRelationVO,
	ObjectPropertyMeta,
	OntologyId,
} from '/@/types/ontology/instance';

describe('isFunctionalSelect', () => {
	it('功能性属性返回true', () => {
		const prop: ObjectPropertyMeta = {
			objectPropertyId: '1',
			objectPropertyName: 'issuedBy',
			isFunctional: '1',
			inherited: false,
			inheritanceDistance: 0,
		};
		expect(isFunctionalSelect(prop)).toBe(true);
	});

	it('非功能性属性返回false', () => {
		const prop: ObjectPropertyMeta = {
			objectPropertyId: '1',
			objectPropertyName: 'hasClause',
			isFunctional: '0',
			inherited: false,
			inheritanceDistance: 0,
		};
		expect(isFunctionalSelect(prop)).toBe(false);
	});

	it('undefined返回false', () => {
		expect(isFunctionalSelect(undefined)).toBe(false);
		expect(isFunctionalSelect(null)).toBe(false);
	});
});

describe('formatRelationLabel', () => {
	it('有标签时返回"谓词标签 → 客体标签"', () => {
		const rel: InstanceObjectRelationVO = {
			id: '1',
			objectPropertyId: '10',
			objectPropertyName: 'hasClause',
			objectPropertyLabel: '包含条',
			objectKind: 'INSTANCE',
			objectResourceId: '20',
			objectIri: 'http://example.org/test/Clause_5_1',
			objectLabel: '5.1 外观',
			asserted: true,
			sortOrder: 1,
		};
		expect(formatRelationLabel(rel)).toBe('包含条 → 5.1 外观');
	});

	it('无标签时降级为名称和IRI', () => {
		const rel: InstanceObjectRelationVO = {
			id: '1',
			objectPropertyId: '10',
			objectPropertyName: 'hasClause',
			objectKind: 'INSTANCE',
			objectResourceId: '20',
			objectIri: 'http://example.org/test/Clause_5_1',
			asserted: true,
			sortOrder: 1,
		};
		expect(formatRelationLabel(rel)).toBe('hasClause → http://example.org/test/Clause_5_1');
	});

	it('只有谓词时返回谓词', () => {
		const rel: InstanceObjectRelationVO = {
			id: '1',
			objectPropertyId: '10',
			objectPropertyName: 'hasClause',
			objectKind: 'INSTANCE',
			objectResourceId: '20',
			asserted: true,
			sortOrder: 1,
		};
		expect(formatRelationLabel(rel)).toBe('hasClause');
	});
});

describe('isSchemaResourceRelation', () => {
	it('ENTITY_TYPE客体返回true', () => {
		const rel: InstanceObjectRelationVO = {
			id: '1',
			objectPropertyId: '10',
			objectKind: 'ENTITY_TYPE',
			objectResourceId: '940056',
			asserted: true,
			sortOrder: 1,
		};
		expect(isSchemaResourceRelation(rel)).toBe(true);
	});

	it('INSTANCE客体返回false', () => {
		const rel: InstanceObjectRelationVO = {
			id: '1',
			objectPropertyId: '10',
			objectKind: 'INSTANCE',
			objectResourceId: '980001',
			asserted: true,
			sortOrder: 1,
		};
		expect(isSchemaResourceRelation(rel)).toBe(false);
	});
});

describe('filterOptionsByRangeTypes', () => {
	interface TestOption {
		id: OntologyId;
		rdfTypeId?: OntologyId;
		label: string;
	}

	const options: TestOption[] = [
		{ id: '1', rdfTypeId: '940040', label: '条款1' },
		{ id: '2', rdfTypeId: '940041', label: '有标题条1' },
		{ id: '3', rdfTypeId: '940039', label: '章1' },
	];

	it('按值域类型过滤', () => {
		const rangeIds = new Set<OntologyId>(['940040', '940041']);
		const filtered = filterOptionsByRangeTypes(options, rangeIds);
		expect(filtered).toHaveLength(2);
		expect(filtered[0].label).toBe('条款1');
		expect(filtered[1].label).toBe('有标题条1');
	});

	it('空值域集合返回全部', () => {
		const filtered = filterOptionsByRangeTypes(options, new Set());
		expect(filtered).toHaveLength(3);
	});
});

describe('collectRangeTypeIds', () => {
	it('从对象属性元数据收集值域类型ID', () => {
		const prop: ObjectPropertyMeta = {
			objectPropertyId: '1',
			objectPropertyName: 'hasClause',
			isFunctional: '0',
			rangeEntityTypes: [
				{ id: '940040', name: 'Clause', iri: 'http://example.org/test#Clause' },
				{ id: '940041', name: 'TitledClause', iri: 'http://example.org/test#TitledClause' },
			],
			inherited: false,
			inheritanceDistance: 0,
		};
		const ids = collectRangeTypeIds(prop);
		expect(ids.size).toBe(2);
		expect(ids.has('940040')).toBe(true);
		expect(ids.has('940041')).toBe(true);
	});

	it('无值域时返回空集合', () => {
		const prop: ObjectPropertyMeta = {
			objectPropertyId: '1',
			objectPropertyName: 'hasClause',
			isFunctional: '0',
			inherited: false,
			inheritanceDistance: 0,
		};
		expect(collectRangeTypeIds(prop).size).toBe(0);
	});

	it('undefined返回空集合', () => {
		expect(collectRangeTypeIds(undefined).size).toBe(0);
		expect(collectRangeTypeIds(null).size).toBe(0);
	});
});
