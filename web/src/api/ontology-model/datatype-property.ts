import request from '/@/utils/request';

// ---------- 数据属性 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/datatype-property/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/datatype-property/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/datatype-property',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/datatype-property/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/datatype-property/' + id,
		method: 'delete',
	});
}
