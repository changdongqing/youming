/**
 * 本体版本演化模块类型定义
 */

/** 兼容性等级 */
export type Compatibility = 'PATCH_ONLY' | 'BACKWARD_COMPATIBLE' | 'BREAKING';

/** 版本发布状态 */
export type ReleaseStatus = 'PREPARED' | 'MIGRATING' | 'PUBLISHED' | 'FAILED' | 'CANCELLED';

/** 工作区状态 */
export type WorkspaceStatus = 'EDITABLE' | 'PREPARED' | 'MIGRATING';

/** 迁移作业状态 */
export type MigrationJobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'PARTIAL_FAILED' | 'FAILED' | 'CANCELLED';

/** 版本详情 */
export interface VersionDetailVO {
	id: number;
	ontologyId: number;
	versionNumber: string;
	versionIri: string;
	priorVersionId?: number;
	priorVersionNumber?: string;
	restoreSourceVersionId?: number;
	restoreSourceVersionNumber?: string;
	compatibility: Compatibility;
	releaseStatus: ReleaseStatus;
	releaseNotes?: string;
	snapshotFormatVersion: number;
	snapshotHash: string;
	diffSummary?: string;
	migrationPlan?: string;
	validationReportId?: number;
	workspaceRevision: number;
	publishedBy?: string;
	publishedAt?: string;
	createTime: string;
	isCurrent: boolean;
}

/** 版本分页查询参数 */
export interface VersionQuery {
	ontologyId?: number;
	versionNumber?: string;
	compatibility?: Compatibility;
	releaseStatus?: ReleaseStatus;
	pageNum?: number;
	pageSize?: number;
}

/** 版本发布请求 */
export interface VersionPrepareRequest {
	ontologyId: number;
	versionNumber: string;
	releaseNotes?: string;
	declaredCompatibility?: Compatibility;
	migrationPlan?: string;
}

/** 版本发布结果 */
export interface VersionPrepareResultVO {
	versionId: number;
	versionNumber: string;
	versionIri: string;
	compatibility: Compatibility;
	declaredCompatibility?: Compatibility;
	breakingReasons: string[];
	diffSummary?: string;
	snapshotHash: string;
	canActivate: boolean;
}

/** 版本配置请求 */
export interface VersionConfigRequest {
	ontologyIri: string;
	versionIriBase: string;
}

/** 差异资源项 */
export interface DiffResource {
	resourceType: string;
	iri: string;
}

/** 字段变更 */
export interface FieldChange {
	field: string;
	oldValue?: string;
	newValue?: string;
}

/** 修改资源项 */
export interface DiffModified {
	resourceType: string;
	iri: string;
	changes: FieldChange[];
}

/** 版本差异结果 */
export interface VersionDiffVO {
	added: DiffResource[];
	removed: DiffResource[];
	modified: DiffModified[];
	compatibility: Compatibility;
	breakingReasons: string[];
}

/** 迁移规则预览 */
export interface MigrationRulePreview {
	ruleType: string;
	description: string;
	affectedCount: number;
}

/** 迁移影响预览 */
export interface MigrationPreviewVO {
	candidateVersionId: number;
	affectedInstanceCount: number;
	rules: MigrationRulePreview[];
}

/** 迁移作业状态 */
export interface MigrationJobStatusVO {
	id: number;
	ontologyId: number;
	candidateVersionId: number;
	status: MigrationJobStatus;
	totalCount: number;
	processedCount: number;
	successCount: number;
	failedCount: number;
	errorSummary?: string;
	startedAt?: string;
	completedAt?: string;
	progressPercent: number;
}

/** 迁移作业查询参数 */
export interface MigrationJobQuery {
	ontologyId?: number;
	candidateVersionId?: number;
	status?: MigrationJobStatus;
	pageNum?: number;
	pageSize?: number;
}

/** 迁移作业 */
export interface MigrationJob {
	id: number;
	ontologyId: number;
	candidateVersionId: number;
	status: MigrationJobStatus;
	totalCount: number;
	processedCount: number;
	successCount: number;
	failedCount: number;
	errorSummary?: string;
	startedAt?: string;
	completedAt?: string;
	createTime: string;
}

/** 恢复计划 */
export interface RestorePlanVO {
	targetVersionId: number;
	targetVersionNumber: string;
	compatibility: Compatibility;
	breakingReasons: string[];
	schemaToRestore: DiffResource[];
	schemaToRemove: DiffResource[];
	migrationRules: string[];
	riskWarnings: string[];
}

/** 恢复计划请求 */
export interface RestorePlanRequest {
	ontologyId: number;
	targetVersionId: number;
}
