/**
 * SPARQL 查询请求
 */
export interface SparqlQueryRequest {
	ontologyId: number;
	query: string;
	format?: string;
}

/**
 * SPARQL 结果绑定值对象
 */
export interface SparqlBindingVO {
	value: string;
	nodeType: 'IRI' | 'BNODE' | 'LITERAL';
	datatypeIri?: string;
	language?: string;
}

/**
 * SPARQL 查询结果
 */
export interface SparqlQueryResultVO {
	queryType: 'SELECT' | 'ASK' | 'ERROR';
	variables?: string[];
	rows?: Record<string, SparqlBindingVO>[];
	booleanResult?: boolean;
	rowCount?: number;
	durationMs?: number;
	truncated?: boolean;
}

/**
 * SPARQL 查询历史记录
 */
export interface SparqlQueryLogVO {
	id: number;
	ontologyId: number;
	queryType: string;
	queryPreview: string;
	resultFormat: string;
	rowCount: number;
	durationMs: number;
	truncated: string;
	status: 'SUCCESS' | 'REJECTED' | 'TIMEOUT' | 'FAILED';
	errorCode?: string;
	createBy: string;
	createTime: string;
}

/**
 * SPARQL 预置查询模板
 */
export interface SparqlTemplateVO {
	id: string;
	name: string;
	queryType: string;
	description: string;
	query: string;
	placeholders: string[];
}

/**
 * 查询历史分页查询参数
 */
export interface SparqlHistoryQuery {
	ontologyId?: number;
	createBy?: string;
	page?: number;
	size?: number;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
	records: T[];
	total: number;
	current: number;
	size: number;
}
