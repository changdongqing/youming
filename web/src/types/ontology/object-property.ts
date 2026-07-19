import type { EntityType, NamespaceOption } from '/@/types/ontology/entity-type';

export type OntologyId = string;

export type SourceType = 'GB_TABLE1' | 'GB_TABLE1_DERIVED' | 'EXTENSION';

/** 本期引擎已实现的推理能力 */
export type InferenceCapability = 'DISJOINT_CHECK' | 'FUNCTIONAL_CHECK' | 'SUBCLASS_INFERENCE';

export interface ObjectPropertyQuery {
	name?: string;
	ontologyId?: OntologyId;
	namespaceId?: OntologyId;
	domainEntityTypeId?: OntologyId;
	rangeEntityTypeId?: OntologyId;
	sourceType?: string;
	isBuiltin?: '0' | '1';
	isFunctional?: '0' | '1';
	isInverseFunctional?: '0' | '1';
	isTransitive?: '0' | '1';
	isSymmetric?: '0' | '1';
	inferenceSupport?: string;
	current?: number;
	size?: number;
}

export interface ObjectPropertyLabel {
	objectPropertyId: OntologyId;
	locale: string;
	label: string;
}

export interface EntityTypeRef {
	id: OntologyId;
	name: string;
	label?: string;
	iri: string;
}

export interface ObjectProperty {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	name: string;
	definition?: string;
	inverseOfId?: OntologyId;
	isFunctional: '0' | '1';
	isInverseFunctional: '0' | '1';
	isTransitive: '0' | '1';
	isSymmetric: '0' | '1';
	sourceType: SourceType;
	sourceReference?: string;
	isBuiltin: '0' | '1';
	ontologyId: OntologyId;
	namespaceId: OntologyId;
	sortOrder: number;
	remarks?: string;
	inferenceSupport?: string[];
}

export interface ObjectPropertySummary {
	objectProperty: ObjectProperty;
	label?: string;
	domains: EntityTypeRef[];
	ranges: EntityTypeRef[];
	inversePropertyName?: string;
	inversePropertyLabel?: string;
	inferenceSupport?: string[];
}

export interface ObjectPropertyDetail {
	objectProperty: ObjectProperty;
	labels: ObjectPropertyLabel[];
	domains: EntityTypeRef[];
	ranges: EntityTypeRef[];
	namespace?: NamespaceOption;
	inverseProperty?: ObjectPropertySummary;
	semanticWarnings?: string[];
	inferenceSupport?: string[];
}

export interface ApplicableObjectProperty {
	objectProperty: ObjectProperty;
	label?: string;
	domains: EntityTypeRef[];
	ranges: EntityTypeRef[];
	inherited: boolean;
	inheritanceDistance: number;
	matchedDomainEntityTypeId: OntologyId;
	matchedDomainName?: string;
	matchedDomainLabel?: string;
}

export interface ApplicableObjectPropertyByRange {
	objectProperty: ObjectProperty;
	label?: string;
	domains: EntityTypeRef[];
	ranges: EntityTypeRef[];
	inherited: boolean;
	inheritanceDistance: number;
	matchedRangeEntityTypeId: OntologyId;
	matchedRangeName?: string;
	matchedRangeLabel?: string;
}

export interface ObjectPropertyForm {
	id?: OntologyId;
	namespaceId?: OntologyId;
	name: string;
	iriLocalName: string;
	iri: string;
	label: string;
	definition: string;
	domainEntityTypeIds: OntologyId[];
	rangeEntityTypeIds: OntologyId[];
	isFunctional: '0' | '1';
	isInverseFunctional: '0' | '1';
	isTransitive: '0' | '1';
	isSymmetric: '0' | '1';
	inverseOfId?: OntologyId;
	sortOrder: number;
	remarks: string;
	isBuiltin?: '0' | '1';
	inferenceSupport: string[];
}

export interface ObjectPropertyCreateRequest {
	namespaceId: OntologyId;
	name: string;
	iriLocalName?: string;
	iri?: string;
	label: string;
	definition?: string;
	domainEntityTypeIds: OntologyId[];
	rangeEntityTypeIds: OntologyId[];
	isFunctional: '0' | '1';
	isInverseFunctional: '0' | '1';
	isTransitive: '0' | '1';
	isSymmetric: '0' | '1';
	inverseOfId?: OntologyId;
	sortOrder: number;
	remarks?: string;
	inferenceSupport?: string[];
}

export interface ObjectPropertyUpdateRequest {
	id: OntologyId;
	namespaceId?: OntologyId;
	name?: string;
	iriLocalName?: string;
	iri?: string;
	label: string;
	definition?: string;
	domainEntityTypeIds?: OntologyId[];
	rangeEntityTypeIds?: OntologyId[];
	isFunctional?: '0' | '1';
	isInverseFunctional?: '0' | '1';
	isTransitive?: '0' | '1';
	isSymmetric?: '0' | '1';
	inverseOfId?: OntologyId;
	sortOrder: number;
	remarks?: string;
	inferenceSupport?: string[];
}
