import request from '/@/utils/request';

// ---------- 本体类实体 ----------

export function pageList(query?: any) {
	return request({
		url: '/admin/ont/model/class/page',
		method: 'get',
		params: query,
	});
}

export function getObj(id: string) {
	return request({
		url: '/admin/ont/model/class/' + id,
		method: 'get',
	});
}

export function addObj(obj: any) {
	return request({
		url: '/admin/ont/model/class',
		method: 'post',
		data: obj,
	});
}

export function putObj(obj: any) {
	return request({
		url: '/admin/ont/model/class/' + obj.id,
		method: 'put',
		data: obj,
	});
}

export function delObj(id: string) {
	return request({
		url: '/admin/ont/model/class/' + id,
		method: 'delete',
	});
}

// ---------- 模板实例化（对已有类追加属性，场景三） ----------

export function instantiateObj(id: string, templateCode: string) {
	return request({
		url: '/admin/ont/model/class/' + id + '/instantiate',
		method: 'post',
		data: { templateCode },
	});
}

// ---------- 供给接口（消费治理域，选模板用，前端预览继承属性） ----------

export function supplyClassTemplateTree(treeRoot: string) {
	return request({
		url: '/admin/ont/supply/v1/class-template/tree',
		method: 'get',
		params: { treeRoot, includeDeprecated: false },
	});
}

export function supplyInherited(templateCode: string) {
	return request({
		url: '/admin/ont/supply/v1/class-template/' + templateCode + '/inherited',
		method: 'get',
	});
}
