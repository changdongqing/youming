import { Graph, Node, Edge } from '@antv/x6';
import { register } from '@antv/x6-vue-shape';
import { DagreLayout } from '@antv/layout';
import ClassNode from '../components/ClassNode.vue';
import emitter from '/@/utils/mitt';

/**
 * 画布核心逻辑（初始化/拖拽/布局/事件，FR-17.1/17.4/17.5/17.7）
 * 适配 @antv/x6 3.x + @antv/x6-vue-shape 3.x + @antv/layout 2.x API
 */
export function useCanvas() {
	let graph: Graph | null = null;
	let dagreLayout: DagreLayout | null = null;

	const initGraph = (container: HTMLElement) => {
		// 注册自定义节点（X6 3.x 用 register({ shape, component })，非 2.x 的 registerVueNode）
		register({
			shape: 'class-node',
			component: ClassNode,
		});

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

		// 初始化 dagre 布局（@antv/layout 2.x：构造函数传入 options）
		dagreLayout = new DagreLayout({
			type: 'dagre',
			rankdir: 'TB', // 自上而下
			nodesep: 40,
			ranksep: 80,
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

	const autoLayout = async () => {
		if (!graph || !dagreLayout) return;
		// @antv/layout 2.x：dagre.layout(data) 返回布局后的数据
		// 3.x 改为异步 execute(graphData)，这里用兼容方式：取 X6 nodes/edges 转 GraphData 执行
		const nodes = graph.getNodes();
		const edges = graph.getEdges();
		const graphData = {
			nodes: nodes.map((n: Node) => ({
				id: n.id,
				data: { ...n.getData(), ...n.getPosition() },
			})),
			edges: edges.map((e: Edge) => ({
				id: e.id,
				source: e.getSourceNode()?.id || '',
				target: e.getTargetNode()?.id || '',
			})),
		};
		try {
			await dagreLayout.execute(graphData);
			// execute 后 graphData.nodes 的 data 会被填充 x/y
			graphData.nodes.forEach((layoutNode: any) => {
				const node = graph!.getCellById(layoutNode.id) as Node;
				if (node && layoutNode.data) {
					node.setPosition(layoutNode.data.x ?? 0, layoutNode.data.y ?? 0);
				}
			});
		} catch (err) {
			// 布局失败不阻塞画布使用，保持原位置
			console.warn('dagre 布局失败，保持原位置', err);
		}
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
