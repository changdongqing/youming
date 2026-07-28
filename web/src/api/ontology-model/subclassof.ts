import request from '/@/utils/request';

// ---------- 类层级（subClassOf） ----------

export function tree(projectId: string) {
	return request({
		url: '/admin/ont/model/subclassof/tree',
		method: 'get',
		params: { projectId },
	});
}

export function suggestParent(classId: string) {
	return request({
		url: '/admin/ont/model/subclassof/suggest-parent',
		method: 'get',
		params: { classId },
	});
}

export function addEdge(obj: any) {
	return request({
		url: '/admin/ont/model/subclassof',
		method: 'post',
		data: obj,
	});
}

export function delEdge(childClassId: string, parentClassId: string) {
	return request({
		url: '/admin/ont/model/subclassof',
		method: 'delete',
		params: { childClassId, parentClassId },
	});
}
