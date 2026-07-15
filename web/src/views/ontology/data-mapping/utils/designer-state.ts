/**
 * 设计器状态机辅助（18-10 §5）
 * 版本状态→可执行操作映射。
 */

import type { MappingVersionStatus } from '/@/types/ontology/data-mapping';

/**
 * 判断版本状态是否只读。
 * PUBLISHED / VALIDATED / RETIRED / VALIDATING 为只读。
 */
export function isReadonlyStatus(status: MappingVersionStatus): boolean {
	return ['PUBLISHED', 'VALIDATED', 'RETIRED', 'VALIDATING'].includes(status);
}

/**
 * 判断版本状态是否可编辑（DRAFT）。
 */
export function isEditableStatus(status: MappingVersionStatus): boolean {
	return status === 'DRAFT';
}

/**
 * 判断版本状态是否可发布（VALIDATED 或 DRAFT 可尝试发布）。
 */
export function isPublishableStatus(status: MappingVersionStatus): boolean {
	return status === 'DRAFT' || status === 'VALIDATED';
}

/**
 * 判断版本状态是否可校验。
 */
export function isValidatableStatus(status: MappingVersionStatus): boolean {
	return status === 'DRAFT';
}

/**
 * 判断版本状态是否可停用（PUBLISHED 可停用）。
 */
export function isRetirableStatus(status: MappingVersionStatus): boolean {
	return status === 'PUBLISHED';
}

/** 保存按钮可用性 */
export function canSave(status: MappingVersionStatus, isDirty: boolean): boolean {
	return isEditableStatus(status) && isDirty;
}

/** 校验按钮可用性 */
export function canValidate(status: MappingVersionStatus): boolean {
	return isValidatableStatus(status);
}

/** 发布按钮可用性 */
export function canPublish(status: MappingVersionStatus, isDirty: boolean): boolean {
	return isPublishableStatus(status) && !isDirty;
}

/** 新增实体映射可用性 */
export function canAddEntityMapping(status: MappingVersionStatus): boolean {
	return isEditableStatus(status);
}

/** 新增字段映射可用性 */
export function canAddFieldMapping(status: MappingVersionStatus): boolean {
	return isEditableStatus(status);
}

/** 新增关系映射可用性 */
export function canAddRelationMapping(status: MappingVersionStatus): boolean {
	return isEditableStatus(status);
}

/** 删除映射可用性 */
export function canDeleteMapping(status: MappingVersionStatus): boolean {
	return isEditableStatus(status);
}

/**
 * 根据 409 冲突判断是否需要重新加载。
 */
export function isRevisionConflict(httpStatus: number): boolean {
	return httpStatus === 409;
}

/**
 * 获取冲突提示消息。
 */
export function getConflictMessage(): string {
	return '配置已被其他用户修改（版本冲突），请重新加载后再编辑。当前修改未保存。';
}
