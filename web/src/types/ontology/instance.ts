import type { EntityType, NamespaceOption } from '/@/types/ontology/entity-type';

export type OntologyId = string;

export type LiteralType = 'STRING' | 'URI' | 'DATE' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN';

export type ObjectKind = 'INSTANCE' | 'ENTITY_TYPE';

export type InstanceSourceType = 'APPENDIX_D' | 'EXTENSION';

export type DeclarationMode = 'EXPLICIT' | 'REFERENCE_ONLY';

export interface InstanceQuery {
	keyword?: string;
	rdfTypeId?: OntologyId;
	includeSubtypes?: boolean;
	ontologyId?: OntologyId;
	namespaceId?: OntologyId;
	sourceType?: string;
	isBuiltin?: '0' | '1';
	current?: number;
	size?: number;
}

export interface InstanceOptionQuery {
	objectPropertyId?: OntologyId;
	keyword?: string;
	ontologyId?: OntologyId;
	current?: number;
	size?: number;
}

export interface Instance {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	rdfTypeId: OntologyId;
	label?: string;
	namespaceId: OntologyId;
	ontologyId: OntologyId;
	sourceType: InstanceSourceType;
	sourceReference?: string;
	declarationMode: DeclarationMode;
	isBuiltin: '0' | '1';
	sortOrder: number;
	remarks?: string;
}

export interface InstanceSummary {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	label?: string;
	rdfTypeId: OntologyId;
	rdfTypeName?: string;
	rdfTypeLabel?: string;
	namespaceId: OntologyId;
	namespacePrefix?: string;
	sourceType: InstanceSourceType;
	isBuiltin: '0' | '1';
	sortOrder: number;
	dataValueCount: number;
	outgoingRelationCount: number;
	incomingRelationCount: number;
}

export interface InstanceDataValue {
	id: OntologyId;
	instanceId: OntologyId;
	dataPropertyId: OntologyId;
	literalValue: string;
	literalType: LiteralType;
	unitId?: OntologyId;
	literalSymbol?: string;
	sortOrder: number;
}

export interface InstanceDataValueVO {
	id: OntologyId;
	dataPropertyId: OntologyId;
	dataPropertyName?: string;
	dataPropertyLabel?: string;
	literalValue: string;
	literalType: LiteralType;
	unitId?: OntologyId;
	unitSymbol?: string;
	sortOrder: number;
}

export interface InstanceObjectRelation {
	id: OntologyId;
	subjectInstanceId: OntologyId;
	objectPropertyId: OntologyId;
	objectKind: ObjectKind;
	objectInstanceId?: OntologyId;
	objectEntityTypeId?: OntologyId;
	sortOrder: number;
}

export interface InstanceObjectRelationVO {
	id: OntologyId;
	objectPropertyId: OntologyId;
	objectPropertyName?: string;
	objectPropertyLabel?: string;
	objectKind: ObjectKind;
	objectResourceId: OntologyId;
	objectIri?: string;
	objectLabel?: string;
	objectRdfTypeId?: OntologyId;
	asserted: boolean;
	sortOrder: number;
}

export interface InstanceInboundRelationVO {
	id: OntologyId;
	subjectInstanceId: OntologyId;
	subjectIri?: string;
	subjectLabel?: string;
	objectPropertyId: OntologyId;
	objectPropertyName?: string;
	objectPropertyLabel?: string;
}

export interface InstanceDetail {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	label?: string;
	rdfTypeId: OntologyId;
	rdfTypeName?: string;
	rdfTypeLabel?: string;
	namespaceId: OntologyId;
	namespacePrefix?: string;
	namespaceUri?: string;
	ontologyId: OntologyId;
	sourceType: InstanceSourceType;
	sourceReference?: string;
	declarationMode: DeclarationMode;
	isBuiltin: '0' | '1';
	sortOrder: number;
	remarks?: string;
	dataValues: InstanceDataValueVO[];
	outgoingRelations: InstanceObjectRelationVO[];
	inboundRelations: InstanceInboundRelationVO[];
}

export interface InstanceOption {
	id: OntologyId;
	iri: string;
	iriLocalName: string;
	label?: string;
	rdfTypeId: OntologyId;
	rdfTypeName?: string;
}

export interface DataPropertyMeta {
	dataPropertyId: OntologyId;
	dataPropertyName: string;
	dataPropertyLabel?: string;
	baseType: string;
	valueMode: string;
	isUnique: '0' | '1';
	unitCategoryId?: OntologyId;
	regexPattern?: string;
	formatHint?: string;
	inherited: boolean;
	inheritanceDistance: number;
}

export interface ObjectPropertyMeta {
	objectPropertyId: OntologyId;
	objectPropertyName: string;
	objectPropertyLabel?: string;
	isFunctional: '0' | '1';
	rangeEntityTypes?: EntityType[];
	inherited: boolean;
	inheritanceDistance: number;
}

export interface InstanceFormMeta {
	rdfTypeId: OntologyId;
	rdfTypeName?: string;
	rdfTypeLabel?: string;
	isAbstract: '0' | '1';
	applicableDataProperties: DataPropertyMeta[];
	applicableObjectProperties: ObjectPropertyMeta[];
}

export interface InstanceDataValueDTO {
	dataPropertyId: OntologyId;
	literalValue: string;
	literalType: LiteralType;
	unitId?: OntologyId;
	literalSymbol?: string;
	sortOrder?: number;
}

export interface InstanceObjectRelationDTO {
	objectPropertyId: OntologyId;
	objectInstanceId: OntologyId;
	sortOrder?: number;
}

export interface InstanceCreateRequest {
	ontologyId?: OntologyId;
	namespaceId: OntologyId;
	iriLocalName?: string;
	rdfTypeId: OntologyId;
	label?: string;
	sortOrder?: number;
	remarks?: string;
	dataValues?: InstanceDataValueDTO[];
	objectRelations?: InstanceObjectRelationDTO[];
}

export interface InstanceUpdateRequest {
	id: OntologyId;
	namespaceId?: OntologyId;
	iriLocalName?: string;
	rdfTypeId?: OntologyId;
	label?: string;
	sortOrder?: number;
	remarks?: string;
}

export interface InstanceForm {
	id?: OntologyId;
	ontologyId?: OntologyId;
	namespaceId?: OntologyId;
	iriLocalName: string;
	rdfTypeId?: OntologyId;
	label: string;
	sortOrder: number;
	remarks: string;
	isBuiltin?: '0' | '1';
	dataValues: InstanceDataValueDTO[];
	objectRelations: InstanceObjectRelationDTO[];
}
