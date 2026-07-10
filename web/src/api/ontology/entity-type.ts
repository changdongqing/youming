import request from '/@/utils/request';

export function fetchEntityTypePage(query?: any) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'get',
		params: query,
	});
}

export function fetchEntityTypeList(query?: any) {
	return request({
		url: '/admin/ontology/entity-types/list',
		method: 'get',
		params: query,
	});
}

export function fetchEntityTypeTree() {
	return request({
		url: '/admin/ontology/entity-types/tree',
		method: 'get',
	});
}

export function fetchEntityTypeById(id: string | number) {
	return request({
		url: `/admin/ontology/entity-types/${id}`,
		method: 'get',
	});
}

export function addEntityTypeObj(obj: any) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'post',
		data: obj,
	});
}

export function putEntityTypeObj(obj: any) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'put',
		data: obj,
	});
}

export function delEntityTypeObj(id: string | number) {
	return request({
		url: `/admin/ontology/entity-types/${id}`,
		method: 'delete',
	});
}
