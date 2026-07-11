import type { InstanceObjectRelationVO, ObjectPropertyMeta } from '/@/types/ontology/instance';
import type { OntologyId } from '/@/types/ontology/instance';

/**
 * 关系编辑器辅助函数。
 *
 * 根据设计文档 §9.3 关系编辑器实现。
 */

/**
 * 判断对象属性是否为功能性属性，决定使用单选还是多选模式。
 */
export function isFunctionalSelect(prop: ObjectPropertyMeta | undefined | null): boolean {
	if (!prop) {
		return false;
	}
	return prop.isFunctional === '1';
}

/**
 * 格式化断言显示标签。
 * 返回 "谓词标签 → 客体标签" 格式。
 */
export function formatRelationLabel(rel: InstanceObjectRelationVO): string {
	const parts: string[] = [];
	if (rel.objectPropertyLabel) {
		parts.push(rel.objectPropertyLabel);
	} else if (rel.objectPropertyName) {
		parts.push(rel.objectPropertyName);
	}
	if (rel.objectLabel) {
		parts.push(rel.objectLabel);
	} else if (rel.objectIri) {
		parts.push(rel.objectIri);
	}
	return parts.join(' → ');
}

/**
 * 判断断言客体是否为Schema资源（ENTITY_TYPE）。
 */
export function isSchemaResourceRelation(rel: InstanceObjectRelationVO): boolean {
	return rel.objectKind === 'ENTITY_TYPE';
}

/**
 * 按值域类型ID集合过滤可选实例。
 */
export function filterOptionsByRangeTypes<T extends { rdfTypeId?: OntologyId }>(
	options: T[],
	rangeTypeIds: Set<OntologyId>
): T[] {
	if (rangeTypeIds.size === 0) {
		return options;
	}
	return options.filter((opt) => opt.rdfTypeId !== undefined && rangeTypeIds.has(opt.rdfTypeId));
}

/**
 * 从对象属性元数据收集值域类型ID集合。
 */
export function collectRangeTypeIds(prop: ObjectPropertyMeta | undefined | null): Set<OntologyId> {
	if (!prop || !prop.rangeEntityTypes) {
		return new Set();
	}
	return new Set(prop.rangeEntityTypes.map((et) => et.id));
}
