import { Graph } from '@antv/x6';
import { registerVueNode } from '@antv/x6-vue-shape';
import { DagreLayout } from '@antv/layout';
import ClassNode from '../components/ClassNode.vue';
import emitter from '/@/utils/mitt';

/**
 * 画布核心逻辑（初始化/拖拽/布局/事件，FR-17.1/17.4/17.5/17.7）
 */
export function useCanvas() {
	let graph: Graph | null = null;

	const initGraph = (container: HTMLElement) => {
		// 注册自定义节点
		registerVueNode('class-node', ClassNode);

		graph = new Graph({
			container,
			grid: true,
			panning: true,
			zooming: true,
			interacting: {
				nodeMovable: true, // 节点可拖拽（AC-17.7 手动拖拽）
			},
			connecting: {
				allowBlank: false,
				allowLoop: false, // 不允许自环
				allowMulti: true, // 允许多条边（多继承）
				router: 'manhattan',
				connector: 'rounded',
			},
		});

		// 节点双击 -> 打开属性抽屉（AC-17.4）
		graph.on('node:dblclick', ({ node }) => {
			emitter.emit('openPropertyDrawer', node.getData());
		});

		// 边连接完成 -> 弹关系选择（AC-17.5）
		graph.on('edge:connected', ({ edge }) => {
			emitter.emit('edgeConnected', {
				source: edge.getSourceNode()?.getData(),
				target: edge.getTargetNode()?.getData(),
				edge,
			});
		});
	};

	const renderGraph = (nodes: any[], edges: any[]) => {
		if (!graph) return;
		graph.clearCells();
		nodes.forEach((n) => graph!.addNode(n));
		edges.forEach((e) => graph!.addEdge(e));
		autoLayout();
	};

	const autoLayout = () => {
		if (!graph) return;
		const dagre = new DagreLayout({
			type: 'dagre',
			rankdir: 'TB', // 自上而下
			nodesep: 40,
			ranksep: 80,
		});
		const data = graph.toJSON();
		const result = dagre.layout(data);
		graph.fromJSON(result);
	};

	const destroyGraph = () => {
		if (graph) {
			graph.dispose();
			graph = null;
		}
	};

	const getGraph = () => graph;

	return { initGraph, renderGraph, destroyGraph, getGraph, autoLayout };
}
