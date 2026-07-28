import request from '/@/utils/request';

// ---------- 注释属性注册表 ----------

export function listObj(query?: any) {
	return request({
		url: '/admin/ont/annotation-property/list',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/annotation-property/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/annotation-property',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/annotation-property/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/annotation-property/' + id,
		method: 'delete',
	});
}

// ---------- 导出（Markdown） ----------

export function exportObj(query?: any) {
	return request({
		url: '/admin/ont/annotation-property/export',
		method: 'get',
		params: query,
	});
}
