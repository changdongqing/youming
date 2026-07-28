import request from '/@/utils/request';

// ---------- 量纲 ----------

export function listQuantityKind() {
	return request({
		url: '/admin/ont/quantity-kind/list',
		method: 'get',
	});
}

// ---------- 单位 ----------

export function pageList(query: any) {
	return request({
		url: '/admin/ont/unit/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/unit/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/unit',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/unit/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/unit/' + id,
		method: 'delete',
	});
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/unit/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

// ---------- 换算 ----------

export function convert(value: number | string, fromIri: string, toIri: string) {
	return request({
		url: '/admin/ont/unit/convert',
		method: 'get',
		params: { value, fromIri, toIri },
	});
}
