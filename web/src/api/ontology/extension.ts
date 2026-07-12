import request from '/@/utils/request';
import type {
	ExtensionModuleQuery,
	ExtensionModuleCreateRequest,
	ExtensionModuleUpdateRequest,
	ExtensionResourceAssociateRequest,
} from '/@/types/ontology/extension';

const BASE = '/admin/ontology/extension';

export function fetchExtensionModulePage(query?: ExtensionModuleQuery) {
	return request({
		url: `${BASE}/modules`,
		method: 'get',
		params: query,
	});
}

export function fetchExtensionModuleList(query?: ExtensionModuleQuery) {
	return request({
		url: `${BASE}/modules/list`,
		method: 'get',
		params: query,
	});
}

export function fetchExtensionModuleById(id: string | number) {
	return request({
		url: `${BASE}/modules/${id}`,
		method: 'get',
	});
}

export function addExtensionModuleObj(obj: ExtensionModuleCreateRequest) {
	return request({
		url: `${BASE}/modules`,
		method: 'post',
		data: obj,
	});
}

export function putExtensionModuleObj(obj: ExtensionModuleUpdateRequest) {
	return request({
		url: `${BASE}/modules`,
		method: 'put',
		data: obj,
	});
}

export function delExtensionModuleObj(id: string | number) {
	return request({
		url: `${BASE}/modules/${id}`,
		method: 'delete',
	});
}

export function fetchExtensionResources(moduleId: string | number, resourceType?: string) {
	return request({
		url: `${BASE}/modules/${moduleId}/resources`,
		method: 'get',
		params: { resourceType },
	});
}

export function associateExtensionResources(moduleId: string | number, obj: ExtensionResourceAssociateRequest) {
	return request({
		url: `${BASE}/modules/${moduleId}/resources`,
		method: 'post',
		data: obj,
	});
}

export function removeExtensionResource(moduleId: string | number, resourceId: string | number, resourceType: string) {
	return request({
		url: `${BASE}/modules/${moduleId}/resources/${resourceId}`,
		method: 'delete',
		params: { resourceType },
	});
}

export function validateExtensionModule(moduleId: string | number) {
	return request({
		url: `${BASE}/modules/${moduleId}/validate`,
		method: 'post',
	});
}

export function fetchExtensionImpact(moduleId: string | number) {
	return request({
		url: `${BASE}/modules/${moduleId}/impact`,
		method: 'get',
	});
}

export function exportExtensionModule(moduleId: string | number, format: string) {
	return request({
		url: `${BASE}/modules/${moduleId}/export`,
		method: 'get',
		params: { format },
		responseType: 'blob',
	});
}

export function fetchRegisteredComponents() {
	return request({
		url: `${BASE}/components/registered`,
		method: 'get',
	});
}
