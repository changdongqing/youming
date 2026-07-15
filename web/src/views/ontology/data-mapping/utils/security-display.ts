/**
 * 安全显示辅助工具（18-10 §3.3 / §7 / §13）
 * 凭证和敏感源值不在 DOM、缓存、日志中出现。
 */

/** 需要禁止选择的列名模式（不区分大小写匹配） */
const FORBIDDEN_COLUMN_PATTERNS = [
	/^password$/i,
	/^passwd$/i,
	/^pwd$/i,
	/^salt$/i,
	/^token$/i,
	/^secret$/i,
	/^credential$/i,
	/^api_key$/i,
	/^apikey$/i,
	/^private_key$/i,
	/^access_token$/i,
	/^refresh_token$/i,
	/^ssn$/i, // 社会保险号
	/^id_card$/i,
	/^idcard$/i,
];

/** 敏感但可选择的列名模式（显示提示但不禁止选择） */
const SENSITIVE_COLUMN_PATTERNS = [/^email$/i, /^phone$/i, /^mobile$/i, /^tel$/i];

/**
 * 判断列名是否禁止选择（password/salt/token 等）。
 */
export function isForbiddenColumn(columnName: string): boolean {
	if (!columnName) return false;
	return FORBIDDEN_COLUMN_PATTERNS.some((p) => p.test(columnName));
}

/**
 * 判断列名是否敏感（email/phone 等，可选择但显示提示）。
 */
export function isSensitiveColumn(columnName: string): boolean {
	if (!columnName) return false;
	return SENSITIVE_COLUMN_PATTERNS.some((p) => p.test(columnName));
}

/**
 * 获取禁止列的原因说明。
 */
export function getForbiddenReason(columnName: string): string {
	if (isForbiddenColumn(columnName)) {
		return `列「${columnName}」包含敏感凭证信息，禁止映射到图谱属性`;
	}
	return '';
}

/**
 * 获取敏感列的提示说明。
 */
export function getSensitiveHint(columnName: string): string {
	if (isSensitiveColumn(columnName)) {
		return `列「${columnName}」包含个人信息，建议使用脱敏转换器`;
	}
	return '';
}

/** 安全级别枚举 */
export type SecurityLevel = 'PUBLIC' | 'INTERNAL' | 'RESTRICTED' | 'CONFIDENTIAL';

/**
 * 判断安全级别是否需要锁图标。
 * CONFIDENTIAL / RESTRICTED 需要锁图标。
 */
export function needsLockIcon(securityLevel?: string): boolean {
	return securityLevel === 'CONFIDENTIAL' || securityLevel === 'RESTRICTED';
}

/**
 * 获取安全级别的 el-tag 类型。
 */
export function securityLevelTagType(securityLevel?: string): 'success' | 'info' | 'warning' | 'danger' {
	switch (securityLevel) {
		case 'PUBLIC':
			return 'success';
		case 'INTERNAL':
			return 'info';
		case 'RESTRICTED':
			return 'warning';
		case 'CONFIDENTIAL':
			return 'danger';
		default:
			return 'info';
	}
}

/**
 * 获取安全级别的中文标签。
 */
export function securityLevelLabel(securityLevel?: string): string {
	switch (securityLevel) {
		case 'PUBLIC':
			return '公开';
		case 'INTERNAL':
			return '内部';
		case 'RESTRICTED':
			return '受限';
		case 'CONFIDENTIAL':
			return '机密';
		default:
			return securityLevel || '内部';
	}
}

/**
 * 脱敏显示值：保留首尾各1个字符，中间用 * 替换。
 * 用于预览脱敏值。
 */
export function maskValue(value: string, visibleChars = 2): string {
	if (!value) return '';
	if (value.length <= visibleChars * 2) {
		return '*'.repeat(value.length);
	}
	const head = value.slice(0, visibleChars);
	const tail = value.slice(-visibleChars);
	const masked = '*'.repeat(Math.max(3, value.length - visibleChars * 2));
	return head + masked + tail;
}

/**
 * 键掩码摘要格式化：只显示 hash 前缀和后缀。
 * 如 "a1b2c3...f7e8"
 */
export function formatDigest(digest: string, prefixLen = 8, suffixLen = 4): string {
	if (!digest) return '-';
	if (digest.length <= prefixLen + suffixLen) return digest;
	return `${digest.slice(0, prefixLen)}...${digest.slice(-suffixLen)}`;
}

/**
 * 构建错误诊断信息的安全字段（不包含后端堆栈、SQL、JDBC URL 或原始异常）。
 */
export function buildSafeDiagnostic(info: { errorCode?: string; message?: string; scope?: string; mappingCode?: string; traceId?: string }): string {
	const parts: string[] = [];
	if (info.errorCode) parts.push(`错误码: ${info.errorCode}`);
	if (info.message) parts.push(`消息: ${info.message}`);
	if (info.scope) parts.push(`范围: ${info.scope}`);
	if (info.mappingCode) parts.push(`映射编码: ${info.mappingCode}`);
	if (info.traceId) parts.push(`traceId: ${info.traceId}`);
	return parts.join('\n');
}
