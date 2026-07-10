import request from '/@/utils/request';
import type {
	ApplicableObjectProperty,
	ApplicableObjectPropertyByRange,
	ObjectPropertyCreateRequest,
	ObjectPropertyDetail,
	ObjectPropertyQuery,
	ObjectPropertySummary,
	ObjectPropertyUpdateRequest,
	OntologyId
} from '/@/types/ontology/object-property';

export function fetchObjectPropertyPage(query?: ObjectPropertyQuery) {
	return request({
		url: '/admin/ontology/object-properties',
		method: 'get',
		params: query,
	});
}

export function fetchObjectPropertyList(query?: ObjectPropertyQuery) {
	return request({
		url: '/admin/ontology/object-properties/list',
		method: 'get',
		params: query,
	});
}

export function fetchObjectPropertyById(id: OntologyId) {
	return request({
		url: `/admin/ontology/object-properties/${id}`,
		method: 'get',
	});
}

export function fetchObjectPropertiesByDomain(entityTypeId: OntologyId) {
	return request({
		url: `/admin/ontology/object-properties/by-domain/${entityTypeId}`,
		method: 'get',
	});
}

export function fetchObjectPropertiesByRange(entityTypeId: OntologyId) {
	return request({
		url: `/admin/ontology/object-properties/by-range/${entityTypeId}`,
		method: 'get',
	});
}

export function addObjectPropertyObj(obj: ObjectPropertyCreateRequest) {
	return request({
		url: '/admin/ontology/object-properties',
		method: 'post',
		data: obj,
	});
}

export function putObjectPropertyObj(obj: ObjectPropertyUpdateRequest) {
	return request({
		url: '/admin/ontology/object-properties',
		method: 'put',
		data: obj,
	});
}

export function delObjectPropertyObj(id: OntologyId) {
	return request({
		url: `/admin/ontology/object-properties/${id}`,
		method: 'delete',
	});
}
