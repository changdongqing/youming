import request from '/@/utils/request';
import type {
	ExportRequest,
	ExportResultVO,
	ExportPreviewVO,
	FormatOption,
	ScopeOption,
	ImportPreviewVO,
	ImportConfirmRequest,
	ImportResultVO,
	SerializationLogVO,
	SerializationLogQuery
} from '/@/types/ontology/serialization';

const BASE = '/admin/ontology/serialization';

/** 导出本体（返回文本内容） */
export function exportOntology(params: ExportRequest) {
	return request<ExportResultVO>({ url: BASE + '/export', method: 'get', params });
}

/** 导出本体（文件下载） */
export function downloadOntology(params: ExportRequest) {
	return request({
		url: BASE + '/export',
		method: 'get',
		params: { ...params, download: true },
		responseType: 'blob'
	});
}

/** 预览导出内容 */
export function previewExport(params: ExportRequest) {
	return request<ExportPreviewVO>({ url: BASE + '/preview', method: 'get', params });
}

/** 获取支持的格式 */
export function fetchFormats() {
	return request<FormatOption[]>({ url: BASE + '/formats', method: 'get' });
}

/** 获取支持的导出范围 */
export function fetchScopes() {
	return request<ScopeOption[]>({ url: BASE + '/scopes', method: 'get' });
}

/** 导入预检 */
export function importPreview(file: File, ontologyId?: number, format?: string) {
	const formData = new FormData();
	formData.append('file', file);
	return request<ImportPreviewVO>({
		url: BASE + '/import/preview',
		method: 'post',
		data: formData,
		params: { ontologyId, format },
		headers: { 'Content-Type': 'multipart/form-data' }
	});
}

/** 导入确认 */
export function importConfirm(data: ImportConfirmRequest) {
	return request<ImportResultVO>({ url: BASE + '/import/confirm', method: 'post', data });
}

/** 审计日志查询 */
export function fetchSerializationLogs(params: SerializationLogQuery) {
	return request<{ records: SerializationLogVO[]; total: number; current: number; size: number }>({
		url: BASE + '/logs',
		method: 'get',
		params
	});
}
