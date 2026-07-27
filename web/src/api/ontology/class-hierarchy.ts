import request from '/@/utils/request';

export function tree(treeRoot?: string) {
	return request({
		url: '/admin/ont/class-hierarchy/tree',
		method: 'get',
		params: treeRoot ? { treeRoot } : {},
	});
}

export function pageList(query: any) {
	return request({
		url: '/admin/ont/class-hierarchy/page',
		method: 'get',
		params: query,
	});
}
