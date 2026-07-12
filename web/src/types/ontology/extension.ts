export type OntologyId = string;

export type ResourceType = 'ENTITY_TYPE' | 'DATA_PROPERTY' | 'OBJECT_PROPERTY' | 'AXIOM_RULE' | 'UNIT';

export type RdfFormat = 'TURTLE' | 'JSON-LD' | 'RDF-XML' | 'N-TRIPLES';

export type ValidationSeverity = 'VIOLATION' | 'WARNING' | 'INFO';

export interface ExtensionModule {
	id: OntologyId;
	moduleCode: string;
	moduleName: string;
	namespaceId: OntologyId;
	namespacePrefix?: string;
	namespaceUri?: string;
	ontologyId: number;
	description?: string;
	version?: string;
	isBuiltin: '0' | '1';
	sortOrder: number;
	remarks?: string;
	resourceCount?: number;
	createTime?: string;
}

export interface ExtensionModuleQuery {
	moduleCode?: string;
	moduleName?: string;
	namespaceId?: OntologyId;
	pageNum?: number;
	pageSize?: number;
}

export interface ExtensionModuleCreateRequest {
	moduleCode: string;
	moduleName: string;
	namespaceId: OntologyId;
	ontologyId?: number;
	description?: string;
	version?: string;
}

export interface ExtensionModuleUpdateRequest {
	id: OntologyId;
	moduleName?: string;
	namespaceId?: OntologyId;
	description?: string;
	version?: string;
	sortOrder?: number;
	remarks?: string;
}

export interface ExtensionResource {
	id: OntologyId;
	moduleId: OntologyId;
	resourceType: ResourceType;
	resourceId: OntologyId;
	resourceIri?: string;
	resourceName?: string;
	createTime?: string;
}

export interface ExtensionResourceAssociateRequest {
	resources: Array<{
		resourceType: ResourceType;
		resourceId: OntologyId;
	}>;
}

export interface ExtensionValidationResult {
	ruleCode: string;
	ruleName: string;
	severity: ValidationSeverity;
	passed: boolean;
	resourceIri?: string;
	message: string;
	suggestion?: string;
	gbClause?: string;
}

export interface ExtensionValidationReport {
	conforms: boolean;
	violationCount: number;
	warningCount: number;
	triggeredAt?: string;
	results: ExtensionValidationResult[];
}

export interface ExtensionImpactSummary {
	moduleId: OntologyId;
	moduleCode: string;
	resourceSummary: {
		entityType: number;
		dataProperty: number;
		objectProperty: number;
		axiomRule: number;
		unit: number;
	};
	instanceImpact: {
		totalInstances: number;
		instancesByType: Array<{
			entityTypeIri: string;
			count: number;
		}>;
	};
	exportImpact: {
		activeExportTasks: number;
		lastExportLogId: string | null;
	};
}

export interface ExtensionComponent {
	beanName: string;
	reasonerName: string;
	capabilities: string[];
	checkConsistencySupported: boolean;
	inferEntailmentsSupported: boolean;
}

export interface ExtensionAssociateResult {
	associatedCount: number;
	skippedCount: number;
	validationReport: ExtensionValidationReport;
}
