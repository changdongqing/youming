import request from '/@/utils/request';
import type {
	ValidationRunRequest,
	ValidationStatusVO,
	ValidationSummary,
	ValidationReportDetail,
	ValidationInstanceResult,
	ExecutorInfo,
	ValidationReportQuery
} from '/@/types/ontology/validation';

const BASE = '/admin/ontology/validation';

export function fetchValidationReportPage(query?: ValidationReportQuery) {
	return request({ url: BASE + '/reports', method: 'get', params: query });
}

export function runFullValidation(data: ValidationRunRequest) {
	return request({ url: BASE + '/run', method: 'post', data });
}

export function validateInstance(instanceId: number | string) {
	return request({ url: `${BASE}/instance/${instanceId}`, method: 'post' });
}

export function fetchValidationReport(
	reportId: number | string,
	params?: { severity?: string; page?: number; size?: number }
) {
	return request({ url: `${BASE}/report/${reportId}`, method: 'get', params });
}

export function fetchValidationStatus(reportId: number | string) {
	return request({ url: `${BASE}/status/${reportId}`, method: 'get' });
}

export function fetchLatestReport(ontologyId: number | string) {
	return request({ url: `${BASE}/latest/${ontologyId}`, method: 'get' });
}

export function fetchExecutors() {
	return request({ url: BASE + '/executors', method: 'get' });
}
