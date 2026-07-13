import type { GraphData, GraphLayout, GraphNode, GraphEdge } from '/@/types/ontology/visualization';

/** 边类型中文标签映射 */
const EDGE_TYPE_LABELS: Record<string, string> = {
	SUBCLASS_OF: '继承关系',
	OBJECT_PROPERTY: '对象属性',
	EQUIVALENT: '等价类',
	DISJOINT: '不相交',
	INSTANCE_OF: '实例类型',
	INSTANCE_RELATION: '实例关系',
};

/**
 * 将后端 GraphData 转换为 echarts graph series option。
 */
export function transformToEchartsOption(graphData: GraphData, layout: GraphLayout = 'force') {
	const { nodes, edges, categories } = graphData;

	const layoutConfig =
		layout === 'force'
			? {
					layout: 'force',
					force: {
						repulsion: 200,
						edgeLength: [80, 200],
						gravity: 0.1,
						layoutAnimation: true,
					},
				}
			: layout === 'circular'
				? { layout: 'circular' }
				: { layout: 'none' };

	return {
		tooltip: {
			formatter: (params: any) => {
				if (params.dataType === 'node') {
					const n: GraphNode = params.data;
					return [
						`<b>${n.label}</b>`,
						n.rdfTypeLabel ? `类型: ${n.rdfTypeLabel}` : '',
						n.definition ? `定义: ${n.definition}` : '',
						n.isBuiltin ? '🔒 核心内置' : '扩展',
						n.dataPropertyCount != null ? `数据属性: ${n.dataPropertyCount}` : '',
						n.instanceCount != null ? `实例数: ${n.instanceCount}` : '',
						n.axiomCount != null ? `公理约束: ${n.axiomCount}` : '',
					]
						.filter(Boolean)
						.join('<br/>');
				}
				if (params.dataType === 'edge') {
					const e: GraphEdge = params.data;
					return [
						`<b>${e.label}</b>`,
						`类型: ${EDGE_TYPE_LABELS[e.edgeType] || e.edgeType}`,
						e.isFunctional ? '⚙ 功能属性' : '',
					]
						.filter(Boolean)
						.join('<br/>');
				}
				return '';
			},
		},
		legend: {
			data: categories.map((c) => c.name),
			bottom: 10,
			textStyle: { fontSize: 12 },
		},
		toolbox: {
			right: 10,
			feature: {
				restore: { title: '重置' },
				saveAsImage: { title: '保存图片', name: 'ontology-graph' },
			},
		},
		series: [
			{
				type: 'graph',
				...layoutConfig,
				roam: true,
				draggable: true,
				focusNodeAdjacency: true,
				categories: categories.map((c) => ({ name: c.name })),
				data: nodes.map((n) => ({
					...n,
					name: n.id,
					value: n.label,
					symbolSize: n.symbolSize,
					category: n.category,
					itemStyle: {
						color: categories[n.category]?.color,
						borderColor: n.isBuiltin ? '#333' : 'transparent',
						borderWidth: n.isBuiltin ? 2 : 0,
					},
					label: {
						show: true,
						position: 'bottom',
						formatter: n.label,
						fontSize: 11,
					},
				})),
				links: edges.map((e) => ({
					...e,
					source: e.source,
					target: e.target,
					label: {
						show: true,
						formatter: e.label + (e.isFunctional ? ' [F]' : ''),
						fontSize: 10,
						color: '#666',
					},
					lineStyle: {
						type: e.lineStyle,
						color: e.color,
						width: e.width,
						curveness: 0.1,
					},
					symbol: e.directed ? ['none', 'arrow'] : 'none',
					symbolSize: e.directed ? [0, 8] : 0,
				})),
				emphasis: {
					focus: 'adjacency',
					lineStyle: { width: 4 },
					label: { fontSize: 14 },
				},
			},
		],
	};
}
