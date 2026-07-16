/**
 * 数据源映射模块（18）类型定义
 * 对应后端 com.pig4cloud.pig.ontology.mapping 包下所有 VO/DTO
 */

// ==================== 枚举联合类型（与后端 @Pattern 完全一致） ====================

export type DataSourceStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export type ConnectionMode = 'HOST' | 'JDBC_URL';

export type MappingProjectStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export type MappingVersionStatus = 'DRAFT' | 'VALIDATING' | 'VALIDATED' | 'PUBLISHED' | 'RETIRED';

export type DeleteStrategy = 'IGNORE' | 'MARK_INACTIVE' | 'SOFT_DELETE' | 'BLOCK_AND_REVIEW' | 'REMOVE_ASSERTION' | 'KEEP_ASSERTION';

export type EntityDeleteStrategy = 'IGNORE' | 'MARK_INACTIVE' | 'SOFT_DELETE' | 'BLOCK_AND_REVIEW';

export type RelationDeleteStrategy = 'REMOVE_ASSERTION' | 'KEEP_ASSERTION' | 'BLOCK_AND_REVIEW';

export type ConflictPolicy = 'SOURCE_WINS' | 'MANUAL_WINS' | 'REJECT_CONFLICT';

export type RelationMode = 'FOREIGN_KEY' | 'SELF_REFERENCE' | 'JOIN_TABLE';

export type MissingTargetPolicy = 'PENDING' | 'SKIP' | 'FAIL_RECORD';

export type JobRunType = 'FULL' | 'INCREMENTAL' | 'RETRY' | 'RELATION_RETRY';

export type JobStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'PARTIALLY_COMPLETED' | 'FAILED' | 'CANCELLED';

export type JobTriggerType = 'MANUAL' | 'SCHEDULED' | 'RETRY';

export type JobPhase = 'ENTITY' | 'RELATION' | 'FINALIZE' | 'NONE';

export type ValidationReportStatus = 'RUNNING' | 'PASSED' | 'FAILED' | 'CANCELLED';

export type ValidationTriggerType = 'MANUAL' | 'PUBLISH_RECHECK' | 'SYSTEM';

export type ValidationSeverity = 'VIOLATION' | 'WARNING' | 'INFO';

export type ValidationScopeType = 'PROJECT' | 'VERSION' | 'SOURCE' | 'ENTITY' | 'FIELD' | 'RELATION' | 'RECORD';

export type SourceKind = 'COLUMN' | 'CONSTANT';

export type LiteralType = 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'DATE' | 'URI';

export type NullHandling = 'SKIP_NULL' | 'USE_DEFAULT' | 'REJECT_NULL';

export type MultiValueStrategy = 'SINGLE' | 'FIRST' | 'LAST' | 'ALL';

export type OwnershipPolicy = 'SOURCE_WINS' | 'MANUAL_WINS' | 'REJECT_CONFLICT';

export type ChangeClassification = 'PATCH' | 'MINOR' | 'MAJOR' | 'MAJOR_HIGH_RISK';

export type PendingStatus = 'PENDING' | 'RESOLVED' | 'IGNORED' | 'EXPIRED';

export type PreviewAction = 'WOULD_CREATE' | 'WOULD_UPDATE' | 'UNCHANGED' | 'FAIL';

export type RelationPreviewAction = 'WOULD_CREATE' | 'UNCHANGED' | 'PENDING';

export type SampleMode = 'FIRST_N' | 'KEYS';

export type ConnectionTestStatus = 'SUCCESS' | 'FAILURE';

export type RiskType = 'IRI_CHANGE' | 'PRIMARY_KEY_CHANGE' | 'DELETE_STRATEGY_CHANGE' | 'SCHEMA_DRIFT';

// ==================== 数据源 VO ====================

export interface DataSourceVO {
	id: number;
	sourceCode: string;
	sourceName: string;
	sourceType?: string;
	databaseType?: string;
	connectionMode: ConnectionMode;
	/** 连接配置JSON，后端 @JsonRawValue 返回原始JSON对象 */
	connectionConfig: any;
	allowedSchemas?: string[];
	allowedObjects?: string[];
	status: DataSourceStatus;
	revision: number;
	lastTestStatus?: ConnectionTestStatus;
	lastTestAt?: string;
	lastTestLatencyMs?: number;
	lastTestErrorCode?: string;
	metadataRefreshedAt?: string;
	securityLevelCode?: string;
	remarks?: string;
	createTime: string;
	updateTime: string;
	// === 凭证脱敏信息 ===
	credentialConfigured?: boolean;
	credentialKeyId?: string;
	usernameMasked?: string;
}

// ==================== 数据源元数据 VO ====================

export interface SourceObjectSummary {
	schemaName: string;
	objectName: string;
	objectType: string;
}

export interface ColumnMetadata {
	name: string;
	jdbcType: string;
	nullable: boolean;
	ordinal: number;
	size?: number;
}

export interface UniqueKeyMetadata {
	name: string;
	columns: string[];
}

export interface ForeignKeyMetadata {
	name: string;
	columns: string[];
	targetSchema: string;
	targetObject: string;
	targetColumns: string[];
}

export interface SourceObjectMetadataVO {
	schemaName: string;
	objectName: string;
	objectType: string;
	columns: ColumnMetadata[];
	primaryKey: string[];
	uniqueKeys: UniqueKeyMetadata[];
	foreignKeys: ForeignKeyMetadata[];
	metadataHash: string;
}

// ==================== 映射工程 VO ====================

export interface VersionSummary {
	id: number;
	versionNumber: string;
	versionStatus: MappingVersionStatus;
	configHash?: string;
	publishedAt?: string;
	createTime: string;
}

export interface MappingProjectVO {
	id: number;
	mappingCode: string;
	mappingName: string;
	ontologyId: number;
	defaultNamespaceId: number;
	activeVersionId?: number;
	projectStatus: MappingProjectStatus;
	description?: string;
	scheduleEnabled: '0' | '1';
	scheduleCron?: string;
	scheduleRunType?: JobRunType;
	executionSubjectType?: 'USER' | 'ROLE';
	executionSubjectId?: number;
	securityLevelCode?: string;
	revision: number;
	lastJobId?: number;
	remarks?: string;
	createTime: string;
	updateTime: string;
	activeVersion?: VersionSummary;
	draftVersion?: VersionSummary;
}

// ==================== 映射版本 VO ====================

export interface MappingVersionVO {
	id: number;
	mappingProjectId: number;
	versionNumber: string;
	versionStatus: MappingVersionStatus;
	priorVersionId?: number;
	ontologyVersionConstraint: string;
	validatedOntologyVersionId?: number;
	validatedWorkspaceRevision?: number;
	configHash?: string;
	validationReportId?: number;
	/** 校验摘要JSON，后端 @JsonRawValue */
	validationSummary?: any;
	releaseNotes?: string;
	publishedBy?: string;
	publishedAt?: string;
	retiredAt?: string;
	revision: number;
	createTime: string;
	updateTime: string;
	/** 配置快照JSON，后端 @JsonRawValue */
	configSnapshot?: any;
}

// ==================== 版本差异 VO ====================

export interface VersionDiffChangeItem {
	field: string;
	oldValue?: string;
	newValue?: string;
	classification: ChangeClassification;
}

export interface MappingVersionDiffVO {
	baseVersionId: number;
	baseVersionNumber: string;
	compareVersionId: number;
	compareVersionNumber: string;
	changeClassification: ChangeClassification;
	changes: VersionDiffChangeItem[];
}

// ==================== 实体映射 VO ====================

export interface EntityMappingVO {
	id: number;
	mappingVersionId: number;
	mappingCode: string;
	mappingName: string;
	sourceId: number;
	sourceSchema: string;
	sourceObject: string;
	sourceObjectType: 'TABLE' | 'VIEW';
	targetEntityTypeId: number;
	targetNamespaceId: number;
	/** 键列配置JSONB */
	keyColumns: string;
	iriTemplate: string;
	labelTemplate?: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	incrementalColumn?: string;
	incrementalType?: 'TIMESTAMP' | 'NUMERIC';
	sourceDeleteFlagColumn?: string;
	/** 源删除值JSONB */
	sourceDeleteValues?: string;
	deleteStrategy: EntityDeleteStrategy;
	inactivePropertyId?: number;
	inactiveLiteralValue?: string;
	conflictPolicy: ConflictPolicy;
	syncOrder: number;
	enabled: '0' | '1';
	description?: string;
	revision: number;
	createTime: string;
	updateTime: string;
	fieldMappings?: FieldMappingVO[];
}

// ==================== 字段映射 VO ====================

export interface FieldMappingVO {
	id: number;
	entityMappingId: number;
	fieldMappingCode: string;
	fieldMappingName?: string;
	targetDataPropertyId: number;
	sourceColumn?: string;
	sourceKind: SourceKind;
	constantValue?: string;
	constantLiteralType?: LiteralType;
	constantUnitId?: number;
	transformer?: string;
	/** 转换器参数JSONB */
	transformerParams?: string;
	nullHandling: NullHandling;
	defaultValue?: string;
	defaultLiteralType?: LiteralType;
	multiValueStrategy: MultiValueStrategy;
	unitId?: number;
	ownershipPolicy: OwnershipPolicy;
	sortOrder: number;
	enabled: '0' | '1';
	description?: string;
	createTime: string;
	updateTime: string;
}

// ==================== 关系映射 VO ====================

export interface RelationMappingVO {
	id: number;
	mappingVersionId: number;
	mappingCode: string;
	mappingName: string;
	relationMode: RelationMode;
	objectPropertyId: number;
	subjectEntityMappingId: number;
	objectEntityMappingId: number;
	sourceId: number;
	sourceSchema: string;
	sourceObject: string;
	/** 主体键映射JSONB */
	subjectKeyMapping: string;
	/** 客体键映射JSONB */
	objectKeyMapping: string;
	/** 关系键列JSONB */
	relationKeyColumns: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	missingTargetPolicy: MissingTargetPolicy;
	deleteStrategy: RelationDeleteStrategy;
	ownershipPolicy: OwnershipPolicy;
	syncOrder: number;
	enabled: '0' | '1';
	description?: string;
	revision: number;
	createTime: string;
	updateTime: string;
}

// ==================== 待解析关系 VO ====================

export interface PendingRelationVO {
	id: number;
	mappingProjectId: number;
	mappingVersionId: number;
	relationMappingCode: string;
	sourceId: number;
	sourceRelationKeyDigest: string;
	subjectEntityMappingCode: string;
	subjectRecordKeyDigest: string;
	objectEntityMappingCode: string;
	objectRecordKeyDigest: string;
	pendingReason?: string;
	pendingStatus: PendingStatus;
	retryCount: number;
	nextRetryAt?: string;
	lastErrorCode?: string;
	lastErrorMessage?: string;
	firstJobId?: number;
	lastJobId?: number;
	resolvedRelationId?: number;
	createTime: string;
	updateTime: string;
}

// ==================== IRI预览 VO ====================

export interface IriPreviewResultVO {
	namespaceUri: string;
	localName: string;
	fullIri: string;
	label?: string;
	success: boolean;
	errorMessage?: string;
}

// ==================== 关系键预览 VO ====================

export interface RelationKeyPreviewResultVO {
	subjectRecordKey: string;
	subjectRecordKeyHash: string;
	objectRecordKey: string;
	objectRecordKeyHash: string;
	relationKey: string;
	relationKeyHash: string;
	success: boolean;
	errorMessage?: string;
}

// ==================== 发布风险摘要 VO ====================

export interface PublishRiskItem {
	riskType: RiskType;
	mappingCode: string;
	description: string;
}

export interface PublishGateCheckItem {
	checkName: string;
	passed: boolean;
	message?: string;
}

export interface PublishPrepareResultVO {
	versionId: number;
	versionNumber: string;
	versionStatus: MappingVersionStatus;
	publishable: boolean;
	validationReportId?: number;
	reportStatus?: ValidationReportStatus;
	violationCount: number;
	warningCount: number;
	unacknowledgedWarningCount: number;
	configHash?: string;
	ontologyVersionId?: number;
	workspaceRevision?: number;
	highRiskChanges: PublishRiskItem[];
	gateChecks: PublishGateCheckItem[];
}

// ==================== 校验报告 VO ====================

export interface ValidationReportVO {
	id: number;
	mappingVersionId: number;
	reportStatus: ValidationReportStatus;
	triggerType: ValidationTriggerType;
	configRevision: number;
	candidateConfigHash?: string;
	ontologyVersionId?: number;
	workspaceRevision?: number;
	metadataHashSummary?: string;
	sampleSize?: number;
	violationCount: number;
	warningCount: number;
	infoCount: number;
	summaryJson?: string;
	startedAt?: string;
	completedAt?: string;
	requestedBy?: string;
	traceId?: string;
	createTime: string;
}

// ==================== 校验问题 VO ====================

export interface ValidationIssueVO {
	id: number;
	reportId: number;
	severity: ValidationSeverity;
	issueCode: string;
	scopeType: ValidationScopeType;
	scopeRef?: string;
	message: string;
	suggestion?: string;
	sourceRecordKeyHash?: string;
	acknowledged: '0' | '1';
	acknowledgedBy?: string;
	acknowledgedAt?: string;
	sortOrder: number;
}

// ==================== 映射作业 VO ====================

export interface MappingJobVO {
	id: number;
	mappingProjectId: number;
	mappingVersionId: number;
	runType: JobRunType;
	triggerType: JobTriggerType;
	jobStatus: JobStatus;
	requestedBy?: string;
	configHash?: string;
	ontologyVersionId?: number;
	workspaceRevision?: number;
	currentPhase?: JobPhase;
	currentMappingCode?: string;
	currentPageNo?: number;
	pageSize?: number;
	totalRead: number;
	totalCreated: number;
	totalUpdated: number;
	totalUnchanged: number;
	totalSkipped: number;
	totalFailed: number;
	totalRelations: number;
	cancelRequested: '0' | '1';
	startedAt?: string;
	finishedAt?: string;
	errorCode?: string;
	errorMessage?: string;
	traceId?: string;
	createTime: string;
	updateTime: string;
}

// ==================== 映射作业记录 VO ====================

export interface MappingJobRecordVO {
	id: number;
	jobId: number;
	retryOfRecordId?: number;
	phase: JobPhase;
	mappingCode: string;
	sourceObject?: string;
	sourceRecordKeyMasked: string;
	recordAction: string;
	recordStatus: string;
	instanceId?: number;
	errorCode?: string;
	errorMessage?: string;
	retryCount: number;
	durationMs?: number;
	sourceUpdatedAt?: string;
	createTime: string;
}

// ==================== 映射预览 VO ====================

export interface PreviewValueItem {
	propertyId: number;
	resultType: string;
	maskedPreview: string;
}

export interface EntityPreviewResult {
	mappingCode: string;
	sourceRecordKeyHash: string;
	generatedIri: string;
	label?: string;
	action: PreviewAction;
	values: PreviewValueItem[];
	issues: string[];
}

export interface RelationPreviewResult {
	mappingCode: string;
	subjectRecordKeyHash: string;
	objectRecordKeyHash: string;
	action: RelationPreviewAction;
	issues: string[];
}

export interface MappingPreviewResult {
	reportId: number;
	entityResults: EntityPreviewResult[];
	relationResults: RelationPreviewResult[];
	truncated: boolean;
}

// ==================== 转换器信息 ====================

export interface TransformerInfo {
	code: string;
	description: string;
	supportedSourceTypes: string[];
	supportedTargetTypes: string[];
	configSchema?: string;
}

// ==================== 连接测试结果 ====================

export interface ConnectionTestOutcome {
	success: boolean;
	latencyMs?: number;
	errorCode?: string;
	errorMessage?: string;
}

// ==================== 查询接口 ====================

export interface DataSourceQuery {
	sourceCode?: string;
	sourceName?: string;
	sourceType?: string;
	databaseType?: string;
	status?: DataSourceStatus;
	current?: number;
	size?: number;
}

export interface MappingProjectQuery {
	mappingCode?: string;
	mappingName?: string;
	ontologyId?: number;
	projectStatus?: MappingProjectStatus;
	current?: number;
	size?: number;
}

export interface EntityMappingQuery {
	mappingCode?: string;
	mappingName?: string;
	current?: number;
	size?: number;
}

export interface RelationMappingQuery {
	mappingCode?: string;
	mappingName?: string;
	current?: number;
	size?: number;
}

export interface JobQueryParams {
	projectId?: number;
	runType?: JobRunType;
	jobStatus?: JobStatus;
	triggerType?: JobTriggerType;
	current?: number;
	size?: number;
}

export interface PendingRelationQuery {
	mappingProjectId?: number;
	relationMappingCode?: string;
	pendingStatus?: PendingStatus;
	current?: number;
	size?: number;
}

export interface ValidationIssueQueryParams {
	severity?: ValidationSeverity;
	scopeType?: ValidationScopeType;
	acknowledged?: '0' | '1';
	current?: number;
	size?: number;
}

export interface MetadataObjectQuery {
	schema?: string;
	objectName?: string;
}

// ==================== 请求 DTO 接口 ====================

export interface DataSourceCreateRequest {
	sourceCode: string;
	sourceName: string;
	connectionMode: ConnectionMode;
	/** 连接配置JSON（不含凭证） */
	connectionConfig: string;
	username: string;
	password: string;
	allowedSchemas?: string[];
	allowedObjects?: string[];
	securityLevelCode?: string;
	remarks?: string;
}

export interface DataSourceUpdateRequest {
	id: number;
	sourceName: string;
	connectionMode: ConnectionMode;
	/** 连接配置JSON（不含凭证） */
	connectionConfig: string;
	/** 用户名，为空时不变更凭证 */
	username?: string;
	/** 密码，为空时不变更凭证 */
	password?: string;
	allowedSchemas?: string[];
	allowedObjects?: string[];
	securityLevelCode?: string;
	remarks?: string;
}

export interface MappingProjectCreateRequest {
	mappingCode: string;
	mappingName: string;
	ontologyId: number;
	defaultNamespaceId: number;
	description?: string;
	ontologyVersionConstraint: string;
	securityLevelCode?: string;
	scheduleCron?: string;
	scheduleRunType?: JobRunType;
	executionSubjectType?: 'USER' | 'ROLE';
	executionSubjectId?: number;
	remarks?: string;
	releaseNotes?: string;
}

export interface MappingProjectUpdateRequest {
	id: number;
	mappingName?: string;
	description?: string;
	securityLevelCode?: string;
	scheduleEnabled?: '0' | '1';
	scheduleCron?: string;
	scheduleRunType?: JobRunType;
	executionSubjectType?: 'USER' | 'ROLE';
	executionSubjectId?: number;
	remarks?: string;
	revision: number;
}

export interface EntityMappingCreateRequest {
	mappingCode: string;
	mappingName: string;
	sourceId: number;
	sourceSchema: string;
	sourceObject: string;
	sourceObjectType?: 'TABLE' | 'VIEW';
	targetEntityTypeId: number;
	targetNamespaceId: number;
	/** 键列配置JSONB */
	keyColumns: string;
	iriTemplate: string;
	labelTemplate?: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	incrementalColumn?: string;
	incrementalType?: 'TIMESTAMP' | 'NUMERIC';
	sourceDeleteFlagColumn?: string;
	/** 源删除值JSONB */
	sourceDeleteValues?: string;
	deleteStrategy?: EntityDeleteStrategy;
	inactivePropertyId?: number;
	inactiveLiteralValue?: string;
	conflictPolicy?: ConflictPolicy;
	syncOrder?: number;
	enabled?: '0' | '1';
	description?: string;
	revision: number;
}

export interface EntityMappingUpdateRequest {
	id: number;
	mappingName?: string;
	sourceObjectType?: 'TABLE' | 'VIEW';
	targetEntityTypeId?: number;
	targetNamespaceId?: number;
	/** 键列配置JSONB */
	keyColumns?: string;
	iriTemplate?: string;
	labelTemplate?: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	incrementalColumn?: string;
	incrementalType?: 'TIMESTAMP' | 'NUMERIC';
	sourceDeleteFlagColumn?: string;
	/** 源删除值JSONB */
	sourceDeleteValues?: string;
	deleteStrategy?: EntityDeleteStrategy;
	inactivePropertyId?: number;
	inactiveLiteralValue?: string;
	conflictPolicy?: ConflictPolicy;
	syncOrder?: number;
	enabled?: '0' | '1';
	description?: string;
	revision: number;
}

export interface FieldMappingCreateRequest {
	fieldMappingCode: string;
	fieldMappingName?: string;
	targetDataPropertyId: number;
	sourceColumn?: string;
	sourceKind?: SourceKind;
	constantValue?: string;
	constantLiteralType?: LiteralType;
	constantUnitId?: number;
	transformer?: string;
	/** 转换器参数JSONB */
	transformerParams?: string;
	nullHandling?: NullHandling;
	defaultValue?: string;
	defaultLiteralType?: LiteralType;
	multiValueStrategy?: MultiValueStrategy;
	unitId?: number;
	ownershipPolicy?: OwnershipPolicy;
	sortOrder?: number;
	enabled?: '0' | '1';
	description?: string;
}

export interface FieldMappingUpdateRequest {
	id: number;
	fieldMappingName?: string;
	targetDataPropertyId?: number;
	sourceColumn?: string;
	sourceKind?: SourceKind;
	constantValue?: string;
	constantLiteralType?: LiteralType;
	constantUnitId?: number;
	transformer?: string;
	/** 转换器参数JSONB */
	transformerParams?: string;
	nullHandling?: NullHandling;
	defaultValue?: string;
	defaultLiteralType?: LiteralType;
	multiValueStrategy?: MultiValueStrategy;
	unitId?: number;
	ownershipPolicy?: OwnershipPolicy;
	sortOrder?: number;
	enabled?: '0' | '1';
	description?: string;
}

export interface RelationMappingCreateRequest {
	mappingCode: string;
	mappingName: string;
	relationMode: RelationMode;
	objectPropertyId: number;
	subjectEntityMappingId: number;
	objectEntityMappingId: number;
	sourceId: number;
	sourceSchema: string;
	sourceObject: string;
	/** 主体键映射JSONB */
	subjectKeyMapping: string;
	/** 客体键映射JSONB */
	objectKeyMapping: string;
	/** 关系键列JSONB */
	relationKeyColumns: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	missingTargetPolicy?: MissingTargetPolicy;
	deleteStrategy?: RelationDeleteStrategy;
	ownershipPolicy?: OwnershipPolicy;
	syncOrder?: number;
	enabled?: '0' | '1';
	description?: string;
	revision: number;
}

export interface RelationMappingUpdateRequest {
	id: number;
	mappingName?: string;
	relationMode?: RelationMode;
	objectPropertyId?: number;
	subjectEntityMappingId?: number;
	objectEntityMappingId?: number;
	/** 主体键映射JSONB */
	subjectKeyMapping?: string;
	/** 客体键映射JSONB */
	objectKeyMapping?: string;
	/** 关系键列JSONB */
	relationKeyColumns?: string;
	/** 过滤条件DSL JSONB */
	filterDsl?: string;
	missingTargetPolicy?: MissingTargetPolicy;
	deleteStrategy?: RelationDeleteStrategy;
	ownershipPolicy?: OwnershipPolicy;
	syncOrder?: number;
	enabled?: '0' | '1';
	description?: string;
	revision: number;
}

export interface MappingPreviewRequest {
	mappingVersionId: number;
	entityMappingCodes?: string[];
	sampleSize: number;
	sampleMode?: SampleMode;
	startKeys?: Record<string, string>;
}

export interface JobCreateRequest {
	runType: JobRunType;
	entityMappingCodes?: string[];
	dryRun?: boolean;
	maxErrorRate?: number;
}

export interface JobRetryRequest {
	recordIds?: number[];
	errorCodes?: string[];
	mappingCode?: string;
	maxErrorRate?: number;
}

// ==================== 模板导入 ====================

export interface MappingTemplateSummary {
	templateCode: string;
	templateName: string;
	description?: string;
	version?: string;
	namespacePrefix?: string;
	entityMappingCount?: number;
	relationMappingCount?: number;
}

export interface TemplateImportRequest {
	templateCode: string;
	dataSourceId: number;
	mappingCodeOverride?: string;
}
