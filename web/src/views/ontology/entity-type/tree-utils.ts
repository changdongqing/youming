import type { EntityTypeTreeNode, OntologyId } from '/@/types/ontology/entity-type';

export function filterEntityTypeTree(nodes: EntityTypeTreeNode[], rawKeyword: string): EntityTypeTreeNode[] {
	const keyword = rawKeyword.trim().toLowerCase();
	if (!keyword) return nodes;
	return nodes.flatMap((node) => {
		const children = filterEntityTypeTree(node.children || [], keyword);
		const matched = node.label.toLowerCase().includes(keyword) || node.name.toLowerCase().includes(keyword);
		return matched || children.length > 0 ? [{ ...node, children }] : [];
	});
}

export function collectInvalidParentIds(nodes: EntityTypeTreeNode[], currentId?: OntologyId): Set<OntologyId> {
	const invalid = new Set<OntologyId>();
	if (!currentId) return invalid;
	invalid.add(currentId);

	const collectDescendants = (node: EntityTypeTreeNode) => {
		for (const child of node.children || []) {
			invalid.add(child.id);
			collectDescendants(child);
		}
	};
	const visit = (treeNodes: EntityTypeTreeNode[]) => {
		for (const node of treeNodes) {
			if (node.id === currentId) collectDescendants(node);
			visit(node.children || []);
		}
	};
	visit(nodes);
	return invalid;
}
