import request from '/@/utils/request';
import type {
	AxiomRuleCreateRequest,
	AxiomRuleDetail,
	AxiomRuleQuery,
	AxiomRuleSummary,
	AxiomRuleTemplate,
	AxiomRuleUpdateRequest,
	EntityTypeRelation,
	EntityTypeRelationCreateRequest,
	OntologyId
} from '/@/types/ontology/axiom-rule';

export function fetchAxiomRulePage(query?: AxiomRuleQuery) {
	return request({ url: '/admin/ontology/axiom-rules', method: 'get', params: query });
}

export function fetchAxiomRuleList(query?: AxiomRuleQuery) {
	return request({ url: '/admin/ontology/axiom-rules/list', method: 'get', params: query });
}

export function fetchAxiomRuleTemplates() {
	return request({ url: '/admin/ontology/axiom-rules/templates', method: 'get' });
}

export function fetchAxiomRuleById(id: OntologyId) {
	return request({ url: `/admin/ontology/axiom-rules/${id}`, method: 'get' });
}

export function addAxiomRuleObj(obj: AxiomRuleCreateRequest) {
	return request({ url: '/admin/ontology/axiom-rules', method: 'post', data: obj });
}

export function putAxiomRuleObj(obj: AxiomRuleUpdateRequest) {
	return request({ url: '/admin/ontology/axiom-rules', method: 'put', data: obj });
}

export function setAxiomRuleEnabled(id: OntologyId, enabled: string) {
	return request({ url: `/admin/ontology/axiom-rules/${id}/enabled`, method: 'put', data: { enabled } });
}

export function delAxiomRuleObj(id: OntologyId) {
	return request({ url: `/admin/ontology/axiom-rules/${id}`, method: 'delete' });
}

export function fetchDisjointList() {
	return request({ url: '/admin/ontology/axiom-rules/disjoint', method: 'get' });
}

export function addDisjointObj(obj: EntityTypeRelationCreateRequest) {
	return request({ url: '/admin/ontology/axiom-rules/disjoint', method: 'post', data: obj });
}

export function delDisjointObj(typeAId: OntologyId, typeBId: OntologyId) {
	return request({ url: `/admin/ontology/axiom-rules/disjoint/${typeAId}/${typeBId}`, method: 'delete' });
}

export function fetchEquivalentList() {
	return request({ url: '/admin/ontology/axiom-rules/equivalent', method: 'get' });
}

export function addEquivalentObj(obj: EntityTypeRelationCreateRequest) {
	return request({ url: '/admin/ontology/axiom-rules/equivalent', method: 'post', data: obj });
}

export function delEquivalentObj(typeAId: OntologyId, typeBId: OntologyId) {
	return request({ url: `/admin/ontology/axiom-rules/equivalent/${typeAId}/${typeBId}`, method: 'delete' });
}
