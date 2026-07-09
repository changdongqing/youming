import request from '/@/utils/request';

export function fetchCategoryList(query?: any) {
	return request({
		url: '/admin/ontology/unit-categories',
		method: 'get',
		params: query,
	});
}

export function addCategoryObj(obj: any) {
	return request({
		url: '/admin/ontology/unit-categories',
		method: 'post',
		data: obj,
	});
}

export function putCategoryObj(obj: any) {
	return request({
		url: '/admin/ontology/unit-categories',
		method: 'put',
		data: obj,
	});
}

export function delCategoryObj(id: string | number) {
	return request({
		url: `/admin/ontology/unit-categories/${id}`,
		method: 'delete',
	});
}

export function fetchUnitPage(query?: any) {
	return request({
		url: '/admin/ontology/units',
		method: 'get',
		params: query,
	});
}

export function fetchUnitList(query?: any) {
	return request({
		url: '/admin/ontology/units/list',
		method: 'get',
		params: query,
	});
}

export function fetchUnitTree() {
	return request({
		url: '/admin/ontology/units/tree',
		method: 'get',
	});
}

export function addUnitObj(obj: any) {
	return request({
		url: '/admin/ontology/units',
		method: 'post',
		data: obj,
	});
}

export function putUnitObj(obj: any) {
	return request({
		url: '/admin/ontology/units',
		method: 'put',
		data: obj,
	});
}

export function delUnitObj(id: string | number) {
	return request({
		url: `/admin/ontology/units/${id}`,
		method: 'delete',
	});
}

export function convertUnit(query: { from: string | number; to: string | number; value: number | string }) {
	return request({
		url: '/admin/ontology/units/convert',
		method: 'get',
		params: query,
	});
}
