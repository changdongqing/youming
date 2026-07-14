import request from '/@/utils/request';
import type {
	SecurityLevelVO,
	ProjectAclQuery,
	ProjectAclVO,
	ClearanceQuery,
	ClearanceVO,
	PolicyRuleQuery,
	PolicyRuleVO,
	AuditLogQuery,
	AuditLogVO,
	DeviceCredentialQuery,
	DeviceCredentialVO,
	DeviceTokenResult,
	ApprovalRequest,
	ApprovalActionRequest,
	ApprovalVO,
} from '/@/types/ontology/security';

const BASE = '/admin/ontology/security';

// ==================== 安全级别 ====================

/** 安全级别列表 */
export const fetchSecurityLevels = () => {
	return request<SecurityLevelVO[]>({ url: `${BASE}/levels`, method: 'get' });
};

// ==================== 工程 ACL ====================

/** 工程 ACL 分页 */
export const fetchProjectAclPage = (query?: ProjectAclQuery) => {
	return request({ url: `${BASE}/project-acl/page`, method: 'get', params: query });
};

/** 新增工程 ACL */
export const addProjectAcl = (data: Partial<ProjectAclVO>) => {
	return request({ url: `${BASE}/project-acl`, method: 'post', data });
};

/** 修改工程 ACL */
export const updateProjectAcl = (data: Partial<ProjectAclVO>) => {
	return request({ url: `${BASE}/project-acl`, method: 'put', data });
};

/** 删除工程 ACL */
export const deleteProjectAcl = (id: string) => {
	return request({ url: `${BASE}/project-acl/${id}`, method: 'delete' });
};

// ==================== 主体安全许可 ====================

/** 主体许可分页 */
export const fetchClearancesPage = (query?: ClearanceQuery) => {
	return request({ url: `${BASE}/clearances/page`, method: 'get', params: query });
};

/** 配置主体许可 */
export const addClearance = (data: Partial<ClearanceVO>) => {
	return request({ url: `${BASE}/clearances`, method: 'post', data });
};

// ==================== 数据策略规则 ====================

/** 数据策略规则分页 */
export const fetchPoliciesPage = (query?: PolicyRuleQuery) => {
	return request({ url: `${BASE}/policies/page`, method: 'get', params: query });
};

/** 新增数据策略规则 */
export const addPolicy = (data: Partial<PolicyRuleVO>) => {
	return request({ url: `${BASE}/policies`, method: 'post', data });
};

/** 修改数据策略规则 */
export const updatePolicy = (data: Partial<PolicyRuleVO>) => {
	return request({ url: `${BASE}/policies`, method: 'put', data });
};

// ==================== 审计日志 ====================

/** 审计日志分页 */
export const fetchAuditPage = (query?: AuditLogQuery) => {
	return request({ url: `${BASE}/audit/page`, method: 'get', params: query });
};

/** 校验哈希链 */
export const verifyAuditChain = (chainScope: string) => {
	return request<string>({ url: `${BASE}/audit/verify`, method: 'post', params: { chainScope } });
};

// ==================== 设备凭证 ====================

/** 设备凭证分页 */
export const fetchDeviceCredentialsPage = (query?: DeviceCredentialQuery) => {
	return request({ url: `${BASE}/device-credentials/page`, method: 'get', params: query });
};

/** 创建设备凭证（Token 只返回一次） */
export const createDeviceCredential = (deviceCode: string, expiresAt?: string) => {
	return request<DeviceTokenResult>({
		url: `${BASE}/device-credentials`,
		method: 'post',
		params: { deviceCode, expiresAt },
	});
};

/** 轮换设备 Token */
export const rotateDeviceToken = (id: string) => {
	return request<DeviceTokenResult>({ url: `${BASE}/device-credentials/${id}/rotate`, method: 'post' });
};

/** 吊销设备凭证 */
export const revokeDeviceCredential = (id: string) => {
	return request({ url: `${BASE}/device-credentials/${id}/revoke`, method: 'post' });
};

// ==================== 高风险审批 ====================

/** 发起审批 */
export const createApproval = (data: ApprovalRequest) => {
	return request<ApprovalVO>({ url: `${BASE}/approvals`, method: 'post', params: data });
};

/** 审批/拒绝 */
export const approveAction = (requestNo: string, data: ApprovalActionRequest) => {
	return request<ApprovalVO>({
		url: `${BASE}/approvals/${requestNo}/actions`,
		method: 'post',
		params: data,
	});
};

/** 查看审批状态 */
export const getApproval = (requestNo: string) => {
	return request<ApprovalVO>({ url: `${BASE}/approvals/${requestNo}`, method: 'get' });
};
