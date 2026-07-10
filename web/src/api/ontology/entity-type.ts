import request from '/@/utils/request';
import type { EntityTypeCreateRequest, EntityTypeQuery, EntityTypeUpdateRequest, OntologyId } from '/@/types/ontology/entity-type';

export function fetchEntityTypePage(query?: EntityTypeQuery) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'get',
		params: query,
	});
}

export function fetchEntityTypeList(query?: EntityTypeQuery) {
	return request({
		url: '/admin/ontology/entity-types/list',
		method: 'get',
		params: query,
	});
}

export function fetchEntityTypeTree(ontologyId?: OntologyId) {
	return request({
		url: '/admin/ontology/entity-types/tree',
		method: 'get',
		params: ontologyId ? { ontologyId } : undefined,
	});
}

export function fetchEntityTypeById(id: OntologyId) {
	return request({
		url: `/admin/ontology/entity-types/${id}`,
		method: 'get',
	});
}

export function addEntityTypeObj(obj: EntityTypeCreateRequest) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'post',
		data: obj,
	});
}

export function putEntityTypeObj(obj: EntityTypeUpdateRequest) {
	return request({
		url: '/admin/ontology/entity-types',
		method: 'put',
		data: obj,
	});
}

export function delEntityTypeObj(id: OntologyId) {
	return request({
		url: `/admin/ontology/entity-types/${id}`,
		method: 'delete',
	});
}
