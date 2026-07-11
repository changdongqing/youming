import request from '/@/utils/request';
import type {
	InstanceCreateRequest,
	InstanceDataValueDTO,
	InstanceDataValueVO,
	InstanceDetail,
	InstanceFormMeta,
	InstanceObjectRelationDTO,
	InstanceObjectRelationVO,
	InstanceOption,
	InstanceOptionQuery,
	InstanceQuery,
	InstanceSummary,
	InstanceUpdateRequest,
	OntologyId
} from '/@/types/ontology/instance';

export function fetchInstancePage(query?: InstanceQuery) {
	return request({ url: '/admin/ontology/instances', method: 'get', params: query });
}

export function fetchInstanceById(id: OntologyId) {
	return request({ url: `/admin/ontology/instances/${id}`, method: 'get' });
}

export function fetchInstanceFormMeta(entityTypeId: OntologyId) {
	return request({ url: `/admin/ontology/instances/form-meta/${entityTypeId}`, method: 'get' });
}

export function fetchInstanceOptions(query?: InstanceOptionQuery) {
	return request({ url: '/admin/ontology/instances/options', method: 'get', params: query });
}

export function addInstanceObj(obj: InstanceCreateRequest) {
	return request({ url: '/admin/ontology/instances', method: 'post', data: obj });
}

export function putInstanceObj(obj: InstanceUpdateRequest) {
	return request({ url: '/admin/ontology/instances', method: 'put', data: obj });
}

export function delInstanceObj(id: OntologyId) {
	return request({ url: `/admin/ontology/instances/${id}`, method: 'delete' });
}

export function fetchInstanceDataValues(id: OntologyId) {
	return request({ url: `/admin/ontology/instances/${id}/data-values`, method: 'get' });
}

export function putInstanceDataValues(id: OntologyId, dataValues: InstanceDataValueDTO[]) {
	return request({ url: `/admin/ontology/instances/${id}/data-values`, method: 'put', data: dataValues });
}

export function delInstanceDataValues(id: OntologyId, dataPropertyId: OntologyId) {
	return request({ url: `/admin/ontology/instances/${id}/data-values/${dataPropertyId}`, method: 'delete' });
}

export function fetchInstanceRelations(id: OntologyId, direction?: string) {
	return request({ url: `/admin/ontology/instances/${id}/relations`, method: 'get', params: { direction: direction || 'OUTGOING' } });
}

export function addInstanceRelation(id: OntologyId, obj: InstanceObjectRelationDTO) {
	return request({ url: `/admin/ontology/instances/${id}/relations`, method: 'post', data: obj });
}

export function delInstanceRelation(id: OntologyId, relationId: OntologyId) {
	return request({ url: `/admin/ontology/instances/${id}/relations/${relationId}`, method: 'delete' });
}
