import request from '/@/utils/request';

export function tree(treeRoot: string, includeDeprecated = false) {
	return request({
		url: '/admin/ont/class-template/tree',
		method: 'get',
		params: { treeRoot, includeDeprecated },
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/class-template/' + id,
		method: 'get',
	});
}

export function inherited(id: string) {
	return request({
		url: `/admin/ont/class-template/${id}/inherited`,
		method: 'get',
	});
}

export function pageList(query: any) {
	return request({
		url: '/admin/ont/class-template/page',
		method: 'get',
		params: query,
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/class-template',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/class-template/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/class-template/' + id,
		method: 'delete',
	});
}

export function deprecateObj(id: string, deprecated: string) {
	return request({
		url: '/admin/ont/class-template/' + id + '/deprecate',
		method: 'put',
		params: { deprecated },
	});
}

export function previewCode(parentId?: string) {
	return request({
		url: '/admin/ont/class-template/code/preview',
		method: 'get',
		params: parentId ? { parentId } : {},
	});
}

export function getRule(treeRoot: string) {
	return request({
		url: '/admin/ont/classification-rule',
		method: 'get',
		params: { treeRoot },
	});
}

export function saveRule(rule: any) {
	return request({
		url: '/admin/ont/classification-rule',
		method: 'put',
		data: rule,
	});
}
