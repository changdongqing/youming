/**
 * 序列化与交换类型定义
 */

export type RdfFormat = 'TURTLE' | 'JSON_LD' | 'RDF_XML' | 'N_TRIPLES';

export type ExportScope = 'FULL' | 'SCHEMA_ONLY' | 'INSTANCE_ONLY' | 'INSTANCE_SUBTREE';

export type PredicateStrategy = 'PREFERRED_ALIAS' | 'STANDARD_IRI' | 'INTERNAL_IRI';

export type OperationType = 'EXPORT' | 'IMPORT';

export type IriMergeMode = 'SKIP' | 'MERGE' | 'OVERWRITE';

export interface FormatOption {
	value: string;
	label: string;
	extension: string;
}

export interface ScopeOption {
	value: string;
	label: string;
}

export interface ExportRequest {
	ontologyId?: number;
	format?: string;
	scope?: string;
	predicateStrategy?: string;
	targetTypeId?: number;
	force?: boolean;
	download?: boolean;
}

export interface PrecheckVO {
	passed: boolean;
	blockedReason?: string;
	validationReportId?: number;
}

export interface ExportResultVO {
	format: string;
	content: string;
	tripleCount: number;
	scope: string;
	predicateStrategy: string;
	precheck: PrecheckVO;
	logId?: number;
}

export interface ExportPreviewVO {
	format: string;
	content: string;
	tripleCount: number;
	scope: string;
	predicateStrategy: string;
	contentSize: number;
	precheck: PrecheckVO;
}

export interface ImportPreviewSummary {
	totalInstances: number;
	totalDataValues: number;
	totalObjectRelations: number;
	schemaSkipped: number;
	conflictCount: number;
	errorCount: number;
	warningCount: number;
}

export interface ImportInstanceInfo {
	iri: string;
	label?: string;
	rdfTypeIri?: string;
	rdfTypeId?: number;
	conflictType?: string;
	dataValueCount: number;
	objectRelationCount: number;
}

export interface ImportConflictInfo {
	iri: string;
	conflictType: string;
}

export interface ImportIssueInfo {
	instanceIri: string;
	message: string;
}

export interface ImportPreviewVO {
	previewId: string;
	ontologyId: number;
	summary: ImportPreviewSummary;
	instances: ImportInstanceInfo[];
	conflicts: ImportConflictInfo[];
	errors: ImportIssueInfo[];
	warnings: ImportIssueInfo[];
}

export interface ImportConfirmRequest {
	previewId: string;
	iriMergeMode?: string;
	ontologyId?: number;
}

export interface ImportResultVO {
	totalInstances: number;
	importedCount: number;
	skippedCount: number;
	failedCount: number;
	dataValueCount: number;
	objectRelationCount: number;
	durationMs: number;
	logId?: number;
}

export interface SerializationLogVO {
	id: number;
	ontologyId: number;
	operationType: string;
	rdfFormat: string;
	exportScope?: string;
	predicateStrategy?: string;
	tripleCount: number;
	contentSize: number;
	instanceCount: number;
	dataValueCount: number;
	objectRelationCount: number;
	skippedCount: number;
	failedCount: number;
	precheckPassed?: string;
	validationReportId?: number;
	forceFlag: string;
	importIriMergeMode?: string;
	durationMs: number;
	createBy: string;
	createTime: string;
}

export interface SerializationLogQuery {
	ontologyId?: number;
	operationType?: string;
	page?: number;
	size?: number;
}
