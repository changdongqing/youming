import request from '/@/utils/request';
import type {
	GraphData,
	VisualizationStats,
	InstanceSubgraphQuery,
	OntologyId,
} from '/@/types/ontology/visualization';

/** 获取 Schema 图谱 */
export function fetchSchemaGraph(ontologyId: OntologyId) {
	return request({
		url: '/admin/ontology/visualization/schema-graph',
		method: 'get',
		params: { ontologyId },
	});
}

/** 获取实例图谱 */
export function fetchInstanceGraph(ontologyId: OntologyId, entityTypeId?: OntologyId, maxNodes?: number) {
	return request({
		url: '/admin/ontology/visualization/instance-graph',
		method: 'get',
		params: { ontologyId, entityTypeId, maxNodes },
	});
}

/** 获取实例子图 */
export function fetchInstanceSubgraph(params: InstanceSubgraphQuery) {
	return request({
		url: '/admin/ontology/visualization/instance-subgraph',
		method: 'get',
		params: params,
	});
}

/** 获取实体类型子图 */
export function fetchEntityTypeSubgraph(entityTypeId: OntologyId, includeObjectProperties = true, includeAxioms = true) {
	return request({
		url: '/admin/ontology/visualization/entity-type-subgraph',
		method: 'get',
		params: { entityTypeId, includeObjectProperties, includeAxioms },
	});
}

/** 获取可视化统计 */
export function fetchVisualizationStats(ontologyId: OntologyId) {
	return request({
		url: '/admin/ontology/visualization/stats',
		method: 'get',
		params: { ontologyId },
	});
}
