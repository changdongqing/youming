import request from '/@/utils/request';

// ---------- 对象属性 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/object-property/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/object-property/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/object-property',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/object-property/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/object-property/' + id,
		method: 'delete',
	});
}

// ---------- 反向关系建议（AC-13.5） ----------

export function suggestInverse(obj: any) {
	return request({
		url: '/admin/ont/model/object-property/suggest-inverse',
		method: 'post',
		data: obj,
	});
}
