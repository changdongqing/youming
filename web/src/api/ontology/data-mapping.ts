import request from '/@/utils/request';
import type {
	DataSourceVO,
	DataSourceQuery,
	DataSourceCreateRequest,
	DataSourceUpdateRequest,
	SourceObjectSummary,
	SourceObjectMetadataVO,
	MetadataObjectQuery,
	ConnectionTestOutcome,
	MappingProjectVO,
	MappingProjectQuery,
	MappingProjectCreateRequest,
	MappingProjectUpdateRequest,
	MappingVersionVO,
	MappingVersionDiffVO,
	PublishPrepareResultVO,
	EntityMappingVO,
	EntityMappingQuery,
	EntityMappingCreateRequest,
	EntityMappingUpdateRequest,
	FieldMappingVO,
	FieldMappingCreateRequest,
	FieldMappingUpdateRequest,
	RelationMappingVO,
	RelationMappingQuery,
	RelationMappingCreateRequest,
	RelationMappingUpdateRequest,
	IriPreviewResultVO,
	RelationKeyPreviewResultVO,
	PendingRelationVO,
	PendingRelationQuery,
	TransformerInfo,
	MappingPreviewResult,
	MappingPreviewRequest,
	ValidationReportVO,
	ValidationIssueVO,
	ValidationIssueQueryParams,
	MappingJobVO,
	MappingJobRecordVO,
	JobQueryParams,
	JobCreateRequest,
	JobRetryRequest,
	MappingTemplateSummary,
	TemplateImportRequest,
	SchemaPreviewRequest,
} from '/@/types/ontology/data-mapping';

const BASE = '/admin/ontology/data-mapping';

// ==================== 数据源 API ====================

export const dataSourceApi = {
	/** 数据源分页 */
	page: (query?: DataSourceQuery) => {
		return request({ url: `${BASE}/sources/page`, method: 'get', params: query });
	},
	/** 数据源详情 */
	getById: (id: number | string) => {
		return request<DataSourceVO>({ url: `${BASE}/sources/${id}`, method: 'get' });
	},
	/** 新增数据源 */
	create: (data: DataSourceCreateRequest) => {
		return request<DataSourceVO>({ url: `${BASE}/sources`, method: 'post', data });
	},
	/** 修改数据源 */
	update: (id: number | string, data: DataSourceUpdateRequest) => {
		return request<DataSourceVO>({ url: `${BASE}/sources/${id}`, method: 'put', data });
	},
	/** 删除数据源 */
	remove: (id: number | string) => {
		return request({ url: `${BASE}/sources/${id}`, method: 'delete' });
	},
	/** 测试连接 */
	testConnection: (id: number | string) => {
		return request<ConnectionTestOutcome>({ url: `${BASE}/sources/${id}/test`, method: 'post' });
	},
	/** 启用/停用数据源 */
	updateStatus: (id: number | string, status: string) => {
		return request<DataSourceVO>({ url: `${BASE}/sources/${id}/status`, method: 'put', params: { status } });
	},
	/** 刷新元数据 */
	refreshMetadata: (id: number | string) => {
		return request<number>({ url: `${BASE}/sources/${id}/metadata/refresh`, method: 'post' });
	},
	/** 获取Schema列表 */
	listSchemas: (id: number | string) => {
		return request<string[]>({ url: `${BASE}/sources/${id}/schemas`, method: 'get' });
	},
	/** 预览Schema（新建向导不落库直连发现） */
	previewSchemas: (data: SchemaPreviewRequest) => {
		return request<string[]>({ url: `${BASE}/sources/schemas/preview`, method: 'post', data });
	},
	/** 获取对象摘要列表 */
	listObjects: (id: number | string, params?: MetadataObjectQuery) => {
		return request<SourceObjectSummary[]>({ url: `${BASE}/sources/${id}/objects`, method: 'get', params });
	},
	/** 获取对象元数据详情 */
	getObjectMetadata: (id: number | string, schema: string, objectName: string) => {
		return request<SourceObjectMetadataVO>({ url: `${BASE}/sources/${id}/objects/${schema}/${objectName}`, method: 'get' });
	},
};

// ==================== 映射工程与版本 API ====================

export const mappingProjectApi = {
	/** 映射工程分页 */
	projectPage: (query?: MappingProjectQuery) => {
		return request({ url: `${BASE}/projects/page`, method: 'get', params: query });
	},
	/** 映射工程详情 */
	getProjectDetail: (id: number | string) => {
		return request<MappingProjectVO>({ url: `${BASE}/projects/${id}`, method: 'get' });
	},
	/** 新建映射工程 */
	createProject: (data: MappingProjectCreateRequest) => {
		return request<MappingProjectVO>({ url: `${BASE}/projects`, method: 'post', data });
	},
	/** 列出可用映射模板 */
	listTemplates: () => {
		return request<MappingTemplateSummary[]>({ url: `${BASE}/templates`, method: 'get' });
	},
	/** 从模板创建映射工程（含全部映射配置） */
	createProjectFromTemplate: (data: TemplateImportRequest) => {
		return request<MappingProjectVO>({ url: `${BASE}/projects/from-template`, method: 'post', data });
	},
	/** 修改映射工程 */
	updateProject: (id: number | string, data: MappingProjectUpdateRequest) => {
		return request<MappingProjectVO>({ url: `${BASE}/projects/${id}`, method: 'put', data });
	},
	/** 删除映射工程 */
	deleteProject: (id: number | string) => {
		return request({ url: `${BASE}/projects/${id}`, method: 'delete' });
	},
	/** 启用/停用/归档映射工程 */
	updateProjectStatus: (id: number | string, status: string) => {
		return request<MappingProjectVO>({ url: `${BASE}/projects/${id}/status`, method: 'put', params: { status } });
	},
	/** 版本列表 */
	listVersions: (projectId: number | string, params?: { current?: number; size?: number }) => {
		return request({ url: `${BASE}/projects/${projectId}/versions`, method: 'get', params });
	},
	/** 创建下一版本 */
	createNextVersion: (projectId: number | string, releaseNotes?: string) => {
		return request<MappingVersionVO>({ url: `${BASE}/projects/${projectId}/versions`, method: 'post', params: { releaseNotes } });
	},
	/** 版本详情 */
	getVersionDetail: (versionId: number | string) => {
		return request<MappingVersionVO>({ url: `${BASE}/versions/${versionId}`, method: 'get' });
	},
	/** 重新打开已停用版本 */
	reopenVersion: (versionId: number | string) => {
		return request<MappingVersionVO>({ url: `${BASE}/versions/${versionId}/reopen`, method: 'post' });
	},
	/** 发布版本 */
	publishVersion: (versionId: number | string, data?: { releaseNotes?: string; confirmCode?: string }) => {
		return request<MappingVersionVO>({ url: `${BASE}/versions/${versionId}/publish`, method: 'post', data });
	},
	/** 停用版本 */
	retireVersion: (versionId: number | string) => {
		return request<MappingVersionVO>({ url: `${BASE}/versions/${versionId}/retire`, method: 'post' });
	},
	/** 版本配置快照 */
	getSnapshot: (versionId: number | string) => {
		return request<string>({ url: `${BASE}/versions/${versionId}/snapshot`, method: 'get' });
	},
	/** 版本差异对比 */
	diffVersions: (baseId: number | string, compareId: number | string) => {
		return request<MappingVersionDiffVO>({ url: `${BASE}/versions/${baseId}/diff/${compareId}`, method: 'get' });
	},
	/** 发布风险摘要 */
	preparePublish: (versionId: number | string) => {
		return request<PublishPrepareResultVO>({ url: `${BASE}/versions/${versionId}/publish/prepare`, method: 'post' });
	},
};

// ==================== 映射配置 API ====================

export const mappingConfigApi = {
	// --- 实体映射 ---
	/** 实体映射分页 */
	listEntityMappings: (versionId: number | string, query?: EntityMappingQuery) => {
		return request({ url: `${BASE}/versions/${versionId}/entity-mappings`, method: 'get', params: query });
	},
	/** 实体映射详情 */
	getEntityMappingDetail: (id: number | string) => {
		return request<EntityMappingVO>({ url: `${BASE}/entity-mappings/${id}`, method: 'get' });
	},
	/** 新增实体映射 */
	createEntityMapping: (versionId: number | string, data: EntityMappingCreateRequest) => {
		return request<EntityMappingVO>({ url: `${BASE}/versions/${versionId}/entity-mappings`, method: 'post', data });
	},
	/** 修改实体映射 */
	updateEntityMapping: (id: number | string, data: EntityMappingUpdateRequest) => {
		return request<EntityMappingVO>({ url: `${BASE}/entity-mappings/${id}`, method: 'put', data });
	},
	/** 删除实体映射 */
	deleteEntityMapping: (id: number | string) => {
		return request({ url: `${BASE}/entity-mappings/${id}`, method: 'delete' });
	},
	/** IRI预览 */
	iriPreview: (id: number | string, data: Record<string, string>) => {
		return request<IriPreviewResultVO>({ url: `${BASE}/entity-mappings/${id}/iri-preview`, method: 'post', data });
	},
	// --- 字段映射 ---
	/** 字段映射列表 */
	listFieldMappings: (entityMappingId: number | string) => {
		return request<FieldMappingVO[]>({ url: `${BASE}/entity-mappings/${entityMappingId}/fields`, method: 'get' });
	},
	/** 新增字段映射 */
	createFieldMapping: (entityMappingId: number | string, data: FieldMappingCreateRequest) => {
		return request<FieldMappingVO>({ url: `${BASE}/entity-mappings/${entityMappingId}/fields`, method: 'post', data });
	},
	/** 修改字段映射 */
	updateFieldMapping: (id: number | string, data: FieldMappingUpdateRequest) => {
		return request<FieldMappingVO>({ url: `${BASE}/field-mappings/${id}`, method: 'put', data });
	},
	/** 删除字段映射 */
	deleteFieldMapping: (id: number | string) => {
		return request({ url: `${BASE}/field-mappings/${id}`, method: 'delete' });
	},
	// --- 关系映射 ---
	/** 关系映射分页 */
	listRelationMappings: (versionId: number | string, query?: RelationMappingQuery) => {
		return request({ url: `${BASE}/versions/${versionId}/relation-mappings`, method: 'get', params: query });
	},
	/** 关系映射详情 */
	getRelationMappingDetail: (id: number | string) => {
		return request<RelationMappingVO>({ url: `${BASE}/relation-mappings/${id}`, method: 'get' });
	},
	/** 新增关系映射 */
	createRelationMapping: (versionId: number | string, data: RelationMappingCreateRequest) => {
		return request<RelationMappingVO>({ url: `${BASE}/versions/${versionId}/relation-mappings`, method: 'post', data });
	},
	/** 修改关系映射 */
	updateRelationMapping: (id: number | string, data: RelationMappingUpdateRequest) => {
		return request<RelationMappingVO>({ url: `${BASE}/relation-mappings/${id}`, method: 'put', data });
	},
	/** 删除关系映射 */
	deleteRelationMapping: (id: number | string) => {
		return request({ url: `${BASE}/relation-mappings/${id}`, method: 'delete' });
	},
	/** 关系键预览 */
	keyPreview: (id: number | string, data: Record<string, string>) => {
		return request<RelationKeyPreviewResultVO>({ url: `${BASE}/relation-mappings/${id}/key-preview`, method: 'post', data });
	},
	// --- 转换器 ---
	/** 转换器列表 */
	listTransformers: () => {
		return request<TransformerInfo[]>({ url: `${BASE}/transformers`, method: 'get' });
	},
};

// ==================== 校验与预览 API ====================

export const mappingValidationApi = {
	/** 映射预览 */
	preview: (versionId: number | string, data: MappingPreviewRequest) => {
		return request<MappingPreviewResult>({ url: `${BASE}/versions/${versionId}/preview`, method: 'post', data });
	},
	/** 执行校验 */
	validate: (versionId: number | string) => {
		return request<ValidationReportVO>({ url: `${BASE}/versions/${versionId}/validate`, method: 'post' });
	},
	/** 获取校验报告 */
	getReport: (reportId: number | string) => {
		return request<ValidationReportVO>({ url: `${BASE}/validation-reports/${reportId}`, method: 'get' });
	},
	/** 校验问题分页 */
	getIssues: (reportId: number | string, query?: ValidationIssueQueryParams) => {
		return request({ url: `${BASE}/validation-reports/${reportId}/issues`, method: 'get', params: query });
	},
	/** 确认WARNING */
	acknowledgeIssue: (issueId: number | string) => {
		return request<ValidationIssueVO>({ url: `${BASE}/validation-issues/${issueId}/acknowledge`, method: 'post' });
	},
};

// ==================== 作业 API ====================

export const mappingJobApi = {
	/** 创建作业 */
	createJob: (versionId: number | string, data: JobCreateRequest) => {
		return request<MappingJobVO>({ url: `${BASE}/versions/${versionId}/jobs`, method: 'post', data });
	},
	/** 作业分页 */
	jobPage: (query?: JobQueryParams) => {
		return request({ url: `${BASE}/jobs/page`, method: 'get', params: query });
	},
	/** 作业详情 */
	getJobDetail: (id: number | string) => {
		return request<MappingJobVO>({ url: `${BASE}/jobs/${id}`, method: 'get' });
	},
	/** 作业记录分页 */
	getJobRecords: (id: number | string, params?: { recordStatus?: string; current?: number; size?: number }) => {
		return request({ url: `${BASE}/jobs/${id}/records`, method: 'get', params });
	},
	/** 取消作业 */
	cancelJob: (id: number | string) => {
		return request<MappingJobVO>({ url: `${BASE}/jobs/${id}/cancel`, method: 'post' });
	},
	/** 重试作业 */
	retryJob: (id: number | string, data?: JobRetryRequest) => {
		return request<MappingJobVO>({ url: `${BASE}/jobs/${id}/retry`, method: 'post', data });
	},
	/** 获取工程增量游标 */
	getProjectCursor: (projectId: number | string) => {
		return request<string>({ url: `${BASE}/projects/${projectId}/cursor`, method: 'get' });
	},
	/** 更新工程调度 */
	updateSchedule: (projectId: number | string, scheduleEnabled: boolean, scheduleCron?: string, scheduleRunType?: string) => {
		return request({ url: `${BASE}/projects/${projectId}/schedule`, method: 'put', params: { scheduleEnabled, scheduleCron, scheduleRunType } });
	},
};

// ==================== 待解析关系 API ====================

export const pendingRelationApi = {
	/** 待解析关系分页 */
	listPendingRelations: (query?: PendingRelationQuery) => {
		return request({ url: `${BASE}/pending-relations/page`, method: 'get', params: query });
	},
	/** 重试待解析关系 */
	retryPendingRelation: (id: number | string) => {
		return request<PendingRelationVO>({ url: `${BASE}/pending-relations/${id}/retry`, method: 'post' });
	},
	/** 忽略待解析关系 */
	ignorePendingRelation: (id: number | string) => {
		return request<PendingRelationVO>({ url: `${BASE}/pending-relations/${id}/ignore`, method: 'post' });
	},
};
