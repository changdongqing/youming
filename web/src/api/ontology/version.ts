import request from '/@/utils/request';
import type {
	MigrationJob,
	MigrationJobQuery,
	MigrationJobStatusVO,
	MigrationPreviewVO,
	RestorePlanRequest,
	RestorePlanVO,
	VersionConfigRequest,
	VersionDetailVO,
	VersionDiffVO,
	VersionPrepareRequest,
	VersionPrepareResultVO,
	VersionQuery,
} from '/@/types/ontology/version';

const BASE = '/admin/ontology/versions';

/** 版本分页 */
export const fetchVersionPage = (query: VersionQuery) => {
	return request({
		url: `${BASE}/page`,
		method: 'get',
		params: query,
	});
};

/** 版本详情 */
export const fetchVersionById = (id: number) => {
	return request<VersionDetailVO>({
		url: `${BASE}/${id}`,
		method: 'get',
	});
};

/** 构建候选版本 */
export const prepareVersion = (data: VersionPrepareRequest) => {
	return request<VersionPrepareResultVO>({
		url: `${BASE}/prepare`,
		method: 'post',
		data,
	});
};

/** 激活候选版本 */
export const activateVersion = (id: number) => {
	return request({
		url: `${BASE}/${id}/activate`,
		method: 'post',
	});
};

/** 取消候选版本 */
export const cancelVersion = (id: number) => {
	return request({
		url: `${BASE}/${id}/cancel`,
		method: 'post',
	});
};

/** 版本差异 */
export const fetchVersionDiff = (id: number, targetId: number) => {
	return request<VersionDiffVO>({
		url: `${BASE}/${id}/diff/${targetId}`,
		method: 'get',
	});
};

/** 获取快照 */
export const fetchVersionSnapshot = (id: number) => {
	return request<string>({
		url: `${BASE}/${id}/snapshot`,
		method: 'get',
	});
};

/** 迁移影响预览 */
export const previewMigration = (id: number) => {
	return request<MigrationPreviewVO>({
		url: `${BASE}/${id}/migration/preview`,
		method: 'post',
	});
};

/** 创建迁移作业 */
export const createMigrationJob = (id: number, candidateVersionId?: number) => {
	return request<number>({
		url: `${BASE}/${id}/migration/jobs`,
		method: 'post',
		data: { candidateVersionId },
	});
};

/** 迁移作业分页 */
export const fetchMigrationJobPage = (query: MigrationJobQuery) => {
	return request({
		url: `${BASE}/migration/jobs/page`,
		method: 'get',
		params: query,
	});
};

/** 迁移作业状态 */
export const fetchMigrationJobStatus = (jobId: number) => {
	return request<MigrationJobStatusVO>({
		url: `${BASE}/migration/jobs/${jobId}`,
		method: 'get',
	});
};

/** 取消迁移作业 */
export const cancelMigrationJob = (jobId: number) => {
	return request({
		url: `${BASE}/migration/jobs/${jobId}/cancel`,
		method: 'post',
	});
};

/** 生成恢复计划 */
export const generateRestorePlan = (id: number, data: RestorePlanRequest) => {
	return request<RestorePlanVO>({
		url: `${BASE}/${id}/restore-plan`,
		method: 'post',
		data,
	});
};

/** 配置工程版本IRI */
export const updateVersionConfig = (projectId: number, data: VersionConfigRequest) => {
	return request({
		url: `${BASE}/projects/${projectId}/version-config`,
		method: 'put',
		data,
	});
};
