export type OntologyId = string;

export interface EntityTypeQuery {
	name?: string;
	ontologyId?: OntologyId;
	namespaceId?: OntologyId;
	isBuiltin?: '0' | '1';
	current?: number;
	size?: number;
}

export interface EntityTypeLabel {
	entityTypeId: OntologyId;
	locale: string;
	label: string;
}

export interface NamespaceOption {
	id: OntologyId;
	prefix: string;
	uri: string;
	isDefault: '0' | '1';
	isBuiltin: '0' | '1';
}

export interface EntityType {
	id: OntologyId;
	iri: string;
	name: string;
	definition?: string;
	isAbstract: '0' | '1';
	isBuiltin: '0' | '1';
	ontologyId: OntologyId;
	namespaceId: OntologyId;
	sortOrder: number;
	remarks?: string;
}

export interface EntityTypeTreeNode {
	key: string;
	id: OntologyId;
	label: string;
	name: string;
	iri: string;
	definition?: string;
	isAbstract: '0' | '1';
	isBuiltin: '0' | '1';
	children: EntityTypeTreeNode[];
}

export interface EntityTypeDetail {
	entityType: EntityType;
	labels: EntityTypeLabel[];
	parentIds: OntologyId[];
	parents: EntityType[];
	childIds: OntologyId[];
	children: EntityType[];
	equivalents: EntityType[];
	disjoints: EntityType[];
	namespace?: NamespaceOption;
}

export interface EntityTypeForm {
	id?: OntologyId;
	namespaceId?: OntologyId;
	name: string;
	iri: string;
	label: string;
	definition: string;
	isAbstract: '0' | '1';
	parentIds: OntologyId[];
	sortOrder: number;
	remarks: string;
	isBuiltin?: '0' | '1';
}

export interface EntityTypeCreateRequest {
	namespaceId: OntologyId;
	name: string;
	iri?: string;
	label: string;
	definition?: string;
	isAbstract: '0' | '1';
	parentIds: OntologyId[];
	sortOrder: number;
	remarks?: string;
}

export interface EntityTypeUpdateRequest extends EntityTypeCreateRequest {
	id: OntologyId;
}
