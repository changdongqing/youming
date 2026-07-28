import request from '/@/utils/request';

// ---------- 浏览 ----------

export function listOntologies() {
	return request({
		url: '/admin/ont/reference/ontologies',
		method: 'get',
	});
}

export function pageUnits(query: any) {
	return request({
		url: '/admin/ont/reference/qudt/units',
		method: 'get',
		params: query,
	});
}

export function pageClasses(query: any) {
	return request({
		url: '/admin/ont/reference/brick/classes',
		method: 'get',
		params: query,
	});
}

export function pageAnnotationProperties(query: any) {
	return request({
		url: '/admin/ont/reference/cco/annotation-properties',
		method: 'get',
		params: query,
	});
}

// ---------- 导入 ----------

export function importQudtUnit(iri: string) {
	return request({
		url: '/admin/ont/reference/qudt/unit/import',
		method: 'post',
		params: { iri },
	});
}

export function importBrickClass(iri: string) {
	return request({
		url: '/admin/ont/reference/brick/class/import',
		method: 'post',
		params: { iri },
	});
}
