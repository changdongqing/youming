/**
 * 映射模块状态枚举映射工具（18-10 §13）
 * 将后端字符串枚举映射为中文标签和 el-tag 类型，用于表格列展示。
 */

import type {
	DataSourceStatus,
	MappingProjectStatus,
	MappingVersionStatus,
	JobStatus,
	JobRunType,
	JobTriggerType,
	ConnectionTestStatus,
	PendingStatus,
	ValidationReportStatus,
	ValidationSeverity,
	ChangeClassification,
	RelationMode,
	EntityDeleteStrategy,
	RelationDeleteStrategy,
	ConflictPolicy,
	MissingTargetPolicy,
	SourceKind,
	NullHandling,
	MultiValueStrategy,
	OwnershipPolicy,
	JobPhase,
} from '/@/types/ontology/data-mapping';

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary';

// ==================== 数据源状态 ====================

export function dataSourceStatusLabel(status: DataSourceStatus): string {
	const map: Record<DataSourceStatus, string> = {
		ACTIVE: '启用',
		INACTIVE: '停用',
		ARCHIVED: '已归档',
	};
	return map[status] || status;
}

export function dataSourceStatusTagType(status: DataSourceStatus): TagType {
	const map: Record<DataSourceStatus, TagType> = {
		ACTIVE: 'success',
		INACTIVE: 'warning',
		ARCHIVED: 'info',
	};
	return map[status] || 'info';
}

// ==================== 连接测试状态 ====================

export function connectionTestStatusLabel(status: ConnectionTestStatus): string {
	const map: Record<ConnectionTestStatus, string> = {
		SUCCESS: '成功',
		FAILURE: '失败',
	};
	return map[status] || status;
}

export function connectionTestTagType(status: ConnectionTestStatus): TagType {
	const map: Record<ConnectionTestStatus, TagType> = {
		SUCCESS: 'success',
		FAILURE: 'danger',
	};
	return map[status] || 'info';
}

// ==================== 映射工程状态 ====================

export function projectStatusLabel(status: MappingProjectStatus): string {
	const map: Record<MappingProjectStatus, string> = {
		ACTIVE: '活跃',
		INACTIVE: '停用',
		ARCHIVED: '已归档',
	};
	return map[status] || status;
}

export function projectStatusTagType(status: MappingProjectStatus): TagType {
	const map: Record<MappingProjectStatus, TagType> = {
		ACTIVE: 'success',
		INACTIVE: 'warning',
		ARCHIVED: 'info',
	};
	return map[status] || 'info';
}

// ==================== 映射版本状态 ====================

export function versionStatusLabel(status: MappingVersionStatus): string {
	const map: Record<MappingVersionStatus, string> = {
		DRAFT: '草稿',
		VALIDATING: '校验中',
		VALIDATED: '已校验',
		PUBLISHED: '已发布',
		RETIRED: '已停用',
	};
	return map[status] || status;
}

export function versionStatusTagType(status: MappingVersionStatus): TagType {
	const map: Record<MappingVersionStatus, TagType> = {
		DRAFT: 'info',
		VALIDATING: 'warning',
		VALIDATED: 'primary',
		PUBLISHED: 'success',
		RETIRED: 'info',
	};
	return map[status] || 'info';
}

// ==================== 作业状态 ====================

export function jobStatusLabel(status: JobStatus): string {
	const map: Record<JobStatus, string> = {
		PENDING: '等待中',
		RUNNING: '运行中',
		COMPLETED: '已完成',
		PARTIALLY_COMPLETED: '部分完成',
		FAILED: '失败',
		CANCELLED: '已取消',
	};
	return map[status] || status;
}

export function jobStatusTagType(status: JobStatus): TagType {
	const map: Record<JobStatus, TagType> = {
		PENDING: 'info',
		RUNNING: 'warning',
		COMPLETED: 'success',
		PARTIALLY_COMPLETED: 'warning',
		FAILED: 'danger',
		CANCELLED: 'info',
	};
	return map[status] || 'info';
}

/** 作业是否为终态（停止轮询的依据） */
export function isJobTerminal(status: JobStatus): boolean {
	return ['COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED', 'CANCELLED'].includes(status);
}

// ==================== 作业运行类型 ====================

export function runTypeLabel(runType: JobRunType): string {
	const map: Record<JobRunType, string> = {
		FULL: '全量',
		INCREMENTAL: '增量',
		RETRY: '失败重试',
		RELATION_RETRY: '关系重试',
	};
	return map[runType] || runType;
}

// ==================== 作业触发类型 ====================

export function triggerTypeLabel(triggerType: JobTriggerType): string {
	const map: Record<JobTriggerType, string> = {
		MANUAL: '手动',
		SCHEDULED: '调度',
		RETRY: '重试',
	};
	return map[triggerType] || triggerType;
}

// ==================== 作业阶段 ====================

export function phaseLabel(phase: JobPhase): string {
	const map: Record<JobPhase, string> = {
		ENTITY: '实体同步',
		RELATION: '关系同步',
		FINALIZE: '收尾',
		NONE: '-',
	};
	return map[phase] || phase;
}

// ==================== 待解析关系状态 ====================

export function pendingStatusLabel(status: PendingStatus): string {
	const map: Record<PendingStatus, string> = {
		PENDING: '待解析',
		RESOLVED: '已解析',
		IGNORED: '已忽略',
		EXPIRED: '已过期',
	};
	return map[status] || status;
}

export function pendingStatusTagType(status: PendingStatus): TagType {
	const map: Record<PendingStatus, TagType> = {
		PENDING: 'warning',
		RESOLVED: 'success',
		IGNORED: 'info',
		EXPIRED: 'info',
	};
	return map[status] || 'info';
}

// ==================== 校验报告状态 ====================

export function reportStatusLabel(status: ValidationReportStatus): string {
	const map: Record<ValidationReportStatus, string> = {
		RUNNING: '校验中',
		PASSED: '通过',
		FAILED: '未通过',
		CANCELLED: '已取消',
	};
	return map[status] || status;
}

export function reportStatusTagType(status: ValidationReportStatus): TagType {
	const map: Record<ValidationReportStatus, TagType> = {
		RUNNING: 'warning',
		PASSED: 'success',
		FAILED: 'danger',
		CANCELLED: 'info',
	};
	return map[status] || 'info';
}

// ==================== 校验严重级别 ====================

export function severityLabel(severity: ValidationSeverity): string {
	const map: Record<ValidationSeverity, string> = {
		VIOLATION: '违规',
		WARNING: '警告',
		INFO: '信息',
	};
	return map[severity] || severity;
}

export function severityTagType(severity: ValidationSeverity): TagType {
	const map: Record<ValidationSeverity, TagType> = {
		VIOLATION: 'danger',
		WARNING: 'warning',
		INFO: 'info',
	};
	return map[severity] || 'info';
}

// ==================== 变更分类 ====================

export function changeClassificationLabel(cls: ChangeClassification): string {
	const map: Record<ChangeClassification, string> = {
		PATCH: '补丁',
		MINOR: '次要',
		MAJOR: '主要',
		MAJOR_HIGH_RISK: '高风险',
	};
	return map[cls] || cls;
}

export function changeClassificationTagType(cls: ChangeClassification): TagType {
	const map: Record<ChangeClassification, TagType> = {
		PATCH: 'info',
		MINOR: 'primary',
		MAJOR: 'warning',
		MAJOR_HIGH_RISK: 'danger',
	};
	return map[cls] || 'info';
}

// ==================== 关系模式 ====================

export function relationModeLabel(mode: RelationMode): string {
	const map: Record<RelationMode, string> = {
		FOREIGN_KEY: '外键关联',
		SELF_REFERENCE: '自引用',
		JOIN_TABLE: '关联表',
	};
	return map[mode] || mode;
}

// ==================== 删除策略 ====================

export function entityDeleteStrategyLabel(strategy: EntityDeleteStrategy): string {
	const map: Record<EntityDeleteStrategy, string> = {
		IGNORE: '忽略',
		MARK_INACTIVE: '标记失活',
		SOFT_DELETE: '软删除',
		BLOCK_AND_REVIEW: '阻断审查',
	};
	return map[strategy] || strategy;
}

export function relationDeleteStrategyLabel(strategy: RelationDeleteStrategy): string {
	const map: Record<RelationDeleteStrategy, string> = {
		REMOVE_ASSERTION: '移除断言',
		KEEP_ASSERTION: '保留断言',
		BLOCK_AND_REVIEW: '阻断审查',
	};
	return map[strategy] || strategy;
}

// ==================== 冲突/所有权策略 ====================

export function conflictPolicyLabel(policy: ConflictPolicy): string {
	const map: Record<ConflictPolicy, string> = {
		SOURCE_WINS: '源优先',
		MANUAL_WINS: '人工优先',
		REJECT_CONFLICT: '拒绝冲突',
	};
	return map[policy] || policy;
}

export function ownershipPolicyLabel(policy: OwnershipPolicy): string {
	return conflictPolicyLabel(policy);
}

// ==================== 缺失目标策略 ====================

export function missingTargetPolicyLabel(policy: MissingTargetPolicy): string {
	const map: Record<MissingTargetPolicy, string> = {
		PENDING: '挂起待解析',
		SKIP: '跳过',
		FAIL_RECORD: '记录失败',
	};
	return map[policy] || policy;
}

// ==================== 来源类型 ====================

export function sourceKindLabel(kind: SourceKind): string {
	const map: Record<SourceKind, string> = {
		COLUMN: '源列',
		CONSTANT: '常量',
	};
	return map[kind] || kind;
}

// ==================== 空值处理 ====================

export function nullHandlingLabel(handling: NullHandling): string {
	const map: Record<NullHandling, string> = {
		SKIP_NULL: '跳过空值',
		USE_DEFAULT: '使用默认值',
		REJECT_NULL: '拒绝空值',
	};
	return map[handling] || handling;
}

// ==================== 多值策略 ====================

export function multiValueStrategyLabel(strategy: MultiValueStrategy): string {
	const map: Record<MultiValueStrategy, string> = {
		SINGLE: '单值',
		FIRST: '取首个',
		LAST: '取末个',
		ALL: '全部',
	};
	return map[strategy] || strategy;
}
