/**
 * 校验引擎类型定义
 */

export type OntologyId = string | number;

export type ValidationScope = 'FULL' | 'INSTANCE';

export type ValidationStatus = 'RUNNING' | 'COMPLETED' | 'FAILED';

export type ValidationResultSeverity = 'VIOLATION' | 'WARNING' | 'INFO';

export interface ValidationRunRequest {
	ontologyId: number;
}

export interface ValidationStatusVO {
	reportId: number;
	status: ValidationStatus;
	conforms?: boolean;
	violationCount?: number;
	durationMs?: number;
	message?: string;
}

export interface ValidationSummary {
	id: number;
	ontologyId: number;
	conforms: boolean;
	totalCount: number;
	violationCount: number;
	warningCount: number;
	infoCount: number;
	ruleCount: number;
	instanceCount: number;
	durationMs: number;
	scope: ValidationScope;
	status: ValidationStatus;
	triggeredBy: string;
	triggeredAt: string;
	completedAt?: string;
	errorMessage?: string;
}

export interface ValidationResultItem {
	id: number;
	severity: ValidationResultSeverity;
	focusNode: string;
	resultPath?: string;
	ruleName: string;
	ruleCode: string;
	message: string;
	expectedValue?: string;
	actualValue?: string;
	suggestion?: string;
}

export interface ValidationReportDetail {
	report: ValidationSummary;
	results: {
		records: ValidationResultItem[];
		total: number;
		size: number;
		current: number;
	};
}

export interface ValidationInstanceResult {
	instanceId: number;
	instanceIri?: string;
	conforms: boolean;
	results: ValidationResultItem[];
	violationCount: number;
	warningCount: number;
	infoCount: number;
}

export interface ExecutorInfo {
	executorCode: string;
	validationMode: string;
	executorClass: string;
}

export interface ValidationReportQuery {
	ontologyId?: number;
	status?: ValidationStatus;
	conforms?: boolean;
	page?: number;
	size?: number;
}
