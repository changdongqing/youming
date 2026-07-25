import request from '/@/utils/request';

export function pageList(query: any) {
	return request({
		url: '/admin/ont/property-template/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/property-template/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/property-template',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/property-template/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/property-template/' + id,
		method: 'delete',
	});
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/property-template/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

export function promoteObj(obj: any) {
	return request({
		url: '/admin/ont/property-template/promote',
		method: 'post',
		data: obj,
	});
}
