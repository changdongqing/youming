export interface SecurityLevelVO {
	id: string;
	levelCode: string;
	levelName: string;
	levelRank: number;
	defaultViewEffect: string;
	defaultExportEffect: string;
	description?: string;
	isBuiltin: string;
	sortOrder: number;
}

export interface ProjectAclQuery {
	ontologyId?: string;
	current?: number;
	size?: number;
}

export interface ProjectAclVO {
	id: string;
	ontologyId: string;
	subjectType: string;
	subjectId: string;
	accessLevel: string;
	createTime: string;
}

export interface ClearanceQuery {
	ontologyId?: string;
	current?: number;
	size?: number;
}

export interface ClearanceVO {
	id: string;
	ontologyId: string;
	subjectType: string;
	subjectId: string;
	maxLevelCode: string;
	createTime: string;
}

export interface PolicyRuleQuery {
	ontologyId?: string;
	current?: number;
	size?: number;
}

export interface PolicyRuleVO {
	id: string;
	ontologyId: string;
	subjectType: string;
	subjectId: string;
	resourceType: string;
	resourceId: string;
	action: string;
	effect: string;
	maskType?: string;
	maskParameter?: string;
	description?: string;
	createTime: string;
}

export interface AuditLogQuery {
	ontologyId?: string;
	userId?: string;
	current?: number;
	size?: number;
}

export interface AuditLogVO {
	id: string;
	auditEventId: string;
	chainScope: string;
	chainSeq: number;
	ontologyId?: string;
	userId?: string;
	username: string;
	accessType: string;
	resourceType: string;
	resourceRef?: string;
	actionSummary?: string;
	resultCount: number;
	maxSecurityLevel?: string;
	decision: string;
	outcome: string;
	errorCode?: string;
	traceId?: string;
	remoteAddr?: string;
	occurredAt: string;
}

export interface DeviceCredentialQuery {
	deviceCode?: string;
	status?: string;
	current?: number;
	size?: number;
}

export interface DeviceCredentialVO {
	id: string;
	deviceCode: string;
	authType: string;
	status: string;
	expiresAt?: string;
	lastAuthenticatedAt?: string;
	lastAuthenticatedIp?: string;
	failedCount: number;
	lockedUntil?: string;
	createTime: string;
}

export interface DeviceTokenResult {
	id: string;
	deviceCode: string;
	token: string;
	expiresAt?: string;
}

export interface ApprovalRequest {
	operationType: string;
	targetRef: string;
	payload: string;
	requiredApprovals?: number;
}

export interface ApprovalActionRequest {
	decision: string;
	comment?: string;
}

export interface ApprovalVO {
	id: string;
	requestNo: string;
	operationType: string;
	targetRef: string;
	requestedBy: string;
	requiredApprovals: number;
	status: string;
	expiresAt: string;
	executedAt?: string;
	createTime: string;
}
