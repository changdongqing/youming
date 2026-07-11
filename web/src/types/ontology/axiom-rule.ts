export type OntologyId = string;

export type RuleCategory = 'ENTITY_TYPE' | 'PROPERTY' | 'RELATION';

export type RuleStatus = 'ACTIVE' | 'DRAFT' | 'BLOCKED';

export type RuleSeverity = 'VIOLATION' | 'WARNING' | 'INFO';

export type SourceType = 'GB_CLAUSE_8' | 'PRD_DERIVED' | 'EXTENSION';

export type TargetType = 'ENTITY_TYPE' | 'DATA_PROPERTY' | 'OBJECT_PROPERTY' | 'UNIT_CATEGORY';

export type ValidationMode = 'NONE' | 'OWL_CONSISTENCY' | 'SHACL_CORE' | 'SHACL_SPARQL' | 'APPLICATION' | 'COMPOSITE';

export interface AxiomRuleQuery {
	name?: string;
	ruleCode?: string;
	ontologyId?: OntologyId;
	category?: string;
	subType?: string;
	status?: string;
	severity?: string;
	isBuiltin?: '0' | '1';
	isEnabled?: '0' | '1';
	current?: number;
	size?: number;
}

export interface AxiomRule {
	id: OntologyId;
	ruleCode: string;
	name: string;
	category: RuleCategory;
	subType: string;
	description?: string;
	templateCode: string;
	templateVersion: number;
	formalizationMode: string;
	validationMode: ValidationMode;
	executorCode?: string;
	configJson?: string;
	owlAxiom?: string;
	shaclShape?: string;
	status: RuleStatus;
	isEnabled: '0' | '1';
	severity: RuleSeverity;
	sourceType: SourceType;
	sourceReference?: string;
	blockedReason?: string;
	isBuiltin: '0' | '1';
	ontologyId: OntologyId;
	sortOrder: number;
	remarks?: string;
}

export interface AxiomRuleTarget {
	id?: OntologyId;
	bindingRole: string;
	bindingOrder: number;
	targetType: TargetType;
	targetId: OntologyId;
	name?: string;
	label?: string;
	iri?: string;
}

export interface AxiomRuleSummary {
	axiomRule: AxiomRule;
	targetCount: number;
	validationModeLabel: string;
	statusLabel: string;
}

export interface AxiomRuleDetail {
	axiomRule: AxiomRule;
	targets: AxiomRuleTarget[];
	generatedOwlPreview?: string;
	generatedShaclPreview?: string;
	consistencyWarnings: string[];
}

export interface BindingRoleVO {
	role: string;
	targetType: TargetType;
	minCount: number;
	maxCount?: number;
	description: string;
}

export interface AxiomRuleTemplate {
	templateCode: string;
	templateVersion: number;
	category: RuleCategory;
	subType: string;
	formalizationMode: string;
	validationMode: ValidationMode;
	executorCode?: string;
	bindingRoles: BindingRoleVO[];
}

export interface EntityTypeRelation {
	typeAId: OntologyId;
	typeAName: string;
	typeALabel?: string;
	typeBId: OntologyId;
	typeBName: string;
	typeBLabel?: string;
}

export interface AxiomRuleTargetDTO {
	bindingRole: string;
	bindingOrder?: number;
	targetType: TargetType;
	targetId: OntologyId;
}

export interface AxiomRuleForm {
	id?: OntologyId;
	ruleCode: string;
	name: string;
	category: RuleCategory;
	subType: string;
	description: string;
	severity: RuleSeverity;
	templateCode: string;
	config: string;
	targets: AxiomRuleTargetDTO[];
	customOwlAxiom: string;
	customShaclShape: string;
	sortOrder: number;
	remarks: string;
	isBuiltin?: '0' | '1';
}

export interface AxiomRuleCreateRequest {
	ruleCode: string;
	name: string;
	category: RuleCategory;
	subType: string;
	description?: string;
	severity: RuleSeverity;
	templateCode: string;
	config?: string;
	targets: AxiomRuleTargetDTO[];
	customOwlAxiom?: string;
	customShaclShape?: string;
	sortOrder: number;
	remarks?: string;
}

export interface AxiomRuleUpdateRequest {
	id: OntologyId;
	name?: string;
	description?: string;
	severity?: RuleSeverity;
	config?: string;
	targets?: AxiomRuleTargetDTO[];
	customOwlAxiom?: string;
	customShaclShape?: string;
	sortOrder: number;
	remarks?: string;
}

export interface EntityTypeRelationCreateRequest {
	entityTypeAId: OntologyId;
	entityTypeBId: OntologyId;
}
