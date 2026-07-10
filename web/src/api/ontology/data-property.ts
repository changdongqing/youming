import request from '/@/utils/request';
import type {
	ApplicableDataProperty,
	DataPropertyCreateRequest,
	DataPropertyDetail,
	DataPropertyQuery,
	DataPropertySummary,
	DataPropertyUpdateRequest,
	OntologyId
} from '/@/types/ontology/data-property';

export function fetchDataPropertyPage(query?: DataPropertyQuery) {
	return request({
		url: '/admin/ontology/data-properties',
		method: 'get',
		params: query,
	});
}

export function fetchDataPropertyList(query?: DataPropertyQuery) {
	return request({
		url: '/admin/ontology/data-properties/list',
		method: 'get',
		params: query,
	});
}

export function fetchDataPropertyById(id: OntologyId) {
	return request({
		url: `/admin/ontology/data-properties/${id}`,
		method: 'get',
	});
}

export function fetchDataPropertiesByDomain(entityTypeId: OntologyId) {
	return request({
		url: `/admin/ontology/data-properties/by-domain/${entityTypeId}`,
		method: 'get',
	});
}

export function addDataPropertyObj(obj: DataPropertyCreateRequest) {
	return request({
		url: '/admin/ontology/data-properties',
		method: 'post',
		data: obj,
	});
}

export function putDataPropertyObj(obj: DataPropertyUpdateRequest) {
	return request({
		url: '/admin/ontology/data-properties',
		method: 'put',
		data: obj,
	});
}

export function delDataPropertyObj(id: OntologyId) {
	return request({
		url: `/admin/ontology/data-properties/${id}`,
		method: 'delete',
	});
}
