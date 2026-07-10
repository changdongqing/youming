import type { EntityType, NamespaceOption } from '/@/types/ontology/entity-type';
import type { UnitCategory } from '/@/types/ontology/unit';

export type OntologyId = string;

export interface DataPropertyQuery {
	name?: string;
	ontologyId?: OntologyId;
	namespaceId?: OntologyId;
	domainEntityTypeId?: OntologyId;
	baseType?: string;
	valueMode?: string;
	sourceType?: string;
	isBuiltin?: '0' | '1';
	current?: number;
	size?: number;
}

export interface DataPropertyLabel {
	dataPropertyId: OntologyId;
	locale: string;
	label: string;
}

export interface DataPropertyEnum {
	dataPropertyId: OntologyId;
	enumValue: string;
	canonicalValue?: string;
	isStandard: '0' | '1';
	sourceReference?: string;
	sortOrder: number;
}

export type BaseType = 'BOOLEAN' | 'DATE' | 'NUMERIC' | 'TEXT' | 'URI' | 'UNIT_REF' | 'TEXT_OR_NUMERIC';

export type ValueMode = 'FREE' | 'CLOSED_ENUM' | 'OPEN_ENUM' | 'EXTERNAL_DICTIONARY' | 'UNIT_DICTIONARY';

export type SourceType = 'APPENDIX_C' | 'CLAUSE_REQUIRED' | 'APPENDIX_D_COMPAT' | 'EXTENSION';

export interface DataProperty {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	standardIri?: string;
	name: string;
	preferredAlias?: string;
	definition?: string;
	domainEntityTypeId: OntologyId;
	baseType: BaseType;
	valueMode: ValueMode;
	valueSourceRef?: string;
	regexPattern?: string;
	formatHint?: string;
	isUnique: '0' | '1';
	unitCategoryId?: OntologyId;
	unitRefMode?: string;
	sourceType: SourceType;
	sourceReference?: string;
	isBuiltin: '0' | '1';
	ontologyId: OntologyId;
	namespaceId: OntologyId;
	sortOrder: number;
	remarks?: string;
}

export interface DataPropertySummary {
	dataProperty: DataProperty;
	label?: string;
	displayName: string;
	domainEntityTypeName?: string;
	domainEntityTypeLabel?: string;
	enumCount: number;
	unitCategoryName?: string;
}

export interface DataPropertyDetail {
	dataProperty: DataProperty;
	labels: DataPropertyLabel[];
	enums: DataPropertyEnum[];
	domainEntityType?: EntityType;
	namespace?: NamespaceOption;
	unitCategory?: UnitCategory;
	displayName: string;
}

export interface ApplicableDataProperty {
	dataProperty: DataProperty;
	label?: string;
	displayName: string;
	inherited: boolean;
	inheritanceDistance: number;
	declaredDomainEntityTypeId: OntologyId;
	declaredDomainName?: string;
	declaredDomainLabel?: string;
}

export interface DataPropertyForm {
	id?: OntologyId;
	namespaceId?: OntologyId;
	name: string;
	iriLocalName: string;
	iri: string;
	label: string;
	definition: string;
	domainEntityTypeId?: OntologyId;
	baseType: BaseType;
	valueMode: ValueMode;
	valueSourceRef: string;
	regexPattern: string;
	formatHint: string;
	isUnique: '0' | '1';
	unitCategoryId?: OntologyId;
	unitRefMode: string;
	preferredAlias: string;
	enumValues: string[];
	sortOrder: number;
	remarks: string;
	isBuiltin?: '0' | '1';
}

export interface DataPropertyCreateRequest {
	namespaceId: OntologyId;
	name: string;
	iriLocalName?: string;
	iri?: string;
	label: string;
	definition?: string;
	domainEntityTypeId: OntologyId;
	baseType: BaseType;
	valueMode: ValueMode;
	valueSourceRef?: string;
	regexPattern?: string;
	formatHint?: string;
	isUnique: '0' | '1';
	unitCategoryId?: OntologyId;
	unitRefMode?: string;
	preferredAlias?: string;
	enumValues: string[];
	sortOrder: number;
	remarks?: string;
}

export interface DataPropertyUpdateRequest {
	id: OntologyId;
	namespaceId?: OntologyId;
	name?: string;
	iriLocalName?: string;
	iri?: string;
	label: string;
	definition?: string;
	domainEntityTypeId?: OntologyId;
	baseType?: BaseType;
	valueMode?: ValueMode;
	valueSourceRef?: string;
	regexPattern?: string;
	formatHint?: string;
	isUnique?: '0' | '1';
	unitCategoryId?: OntologyId;
	unitRefMode?: string;
	preferredAlias?: string;
	enumValues?: string[];
	sortOrder: number;
	remarks?: string;
}
