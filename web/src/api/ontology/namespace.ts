import request from '/@/utils/request';

export function fetchNamespacePage(query?: any) {
	return request({
		url: '/admin/ontology/namespaces',
		method: 'get',
		params: query,
	});
}

export function fetchNamespaceList(query?: any) {
	return request({
		url: '/admin/ontology/namespaces/list',
		method: 'get',
		params: query,
	});
}

export function addNamespaceObj(obj: any) {
	return request({
		url: '/admin/ontology/namespaces',
		method: 'post',
		data: obj,
	});
}

export function putNamespaceObj(obj: any) {
	return request({
		url: '/admin/ontology/namespaces',
		method: 'put',
		data: obj,
	});
}

export function delNamespaceObj(id: string | number) {
	return request({
		url: `/admin/ontology/namespaces/${id}`,
		method: 'delete',
	});
}

export function validateIri(iri: string) {
	return request({
		url: '/admin/ontology/iris/validate',
		method: 'get',
		params: { iri },
	});
}

export function generateIri(obj: { namespaceId: string | number; localName: string; elementType?: string }) {
	return request({
		url: '/admin/ontology/iris/generate',
		method: 'post',
		data: obj,
	});
}
