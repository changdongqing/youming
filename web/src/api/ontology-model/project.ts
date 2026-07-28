import request from '/@/utils/request';

// ---------- 本体项目 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/project/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/project/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/project',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/project/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/project/' + id,
		method: 'delete',
	});
}

// ---------- IRI 前缀 ----------

export function listPrefix(projectId: string) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefixes',
		method: 'get',
	});
}

export function addPrefix(projectId: string, obj: any) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix',
		method: 'post',
		data: obj,
	});
}

export function putPrefix(projectId: string, obj: any) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delPrefix(projectId: string, id: string) {
	return request({
		url: '/admin/ont/model/project/' + projectId + '/prefix/' + id,
		method: 'delete',
	});
}
