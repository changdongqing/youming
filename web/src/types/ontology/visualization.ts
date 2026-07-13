export type OntologyId = string;

/** 图谱节点 */
export interface GraphNode {
	id: string;
	label: string;
	name?: string;
	layer: 'SCHEMA' | 'INSTANCE';
	nodeType: 'ENTITY_TYPE' | 'INSTANCE';
	entityTypeId?: OntologyId;
	instanceId?: OntologyId;
	rdfTypeIri?: string;
	rdfTypeLabel?: string;
	category: number;
	symbolSize: number;
	isBuiltin: boolean;
	isAbstract?: boolean;
	definition?: string;
	namespacePrefix?: string;
	axiomCount?: number;
	dataPropertyCount?: number;
	instanceCount?: number;
}

/** 图谱边 */
export interface GraphEdge {
	id: string;
	source: string;
	target: string;
	label: string;
	edgeType: 'SUBCLASS_OF' | 'OBJECT_PROPERTY' | 'EQUIVALENT' | 'DISJOINT' | 'INSTANCE_OF' | 'INSTANCE_RELATION';
	objectPropertyId?: OntologyId;
	lineStyle: 'solid' | 'dashed' | 'dotted';
	color: string;
	directed: boolean;
	width: number;
	isFunctional?: boolean;
}

/** 节点分类 */
export interface GraphCategory {
	name: string;
	color: string;
}

/** 图谱统计 */
export interface GraphSummary {
	nodeCount: number;
	edgeCount: number;
	entityTypeCount: number;
	instanceCount: number;
	objectPropertyCount: number;
	subclassCount: number;
	disjointCount: number;
	equivalentCount: number;
	axiomRuleCount: number;
	dataPropertyCount: number;
}

/** 图谱数据（后端 GraphDataVO 对应） */
export interface GraphData {
	nodes: GraphNode[];
	edges: GraphEdge[];
	categories: GraphCategory[];
	summary: GraphSummary;
}

/** 名称值对 */
export interface NameValue {
	name: string;
	value: number;
}

/** 可视化统计 */
export interface VisualizationStats {
	ontologyId: OntologyId;
	ontologyName: string;
	entityTypeCount: number;
	dataPropertyCount: number;
	objectPropertyCount: number;
	instanceCount: number;
	axiomRuleCount: number;
	latestValidationStatus: 'PASS' | 'FAIL' | 'PENDING' | 'NONE';
	latestValidationTime?: string;
	latestViolationCount: number;
	exportCount: number;
	extensionModuleCount: number;
	entityTypeDistribution: NameValue[];
	instanceDistribution: NameValue[];
}

/** 子图展开查询参数 */
export interface InstanceSubgraphQuery {
	instanceId: OntologyId;
	depth?: number;
	direction?: 'OUTGOING' | 'INCOMING' | 'BOTH';
}

/** 布局类型 */
export type GraphLayout = 'force' | 'circular' | 'none';
