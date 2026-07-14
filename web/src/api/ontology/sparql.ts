import request from '/@/utils/request';
import type {
	SparqlQueryRequest,
	SparqlQueryResultVO,
	SparqlQueryLogVO,
	SparqlTemplateVO,
	SparqlHistoryQuery,
	PageResult
} from '/@/types/ontology/sparql';

const BASE = '/admin/ontology/sparql';

/** 执行 SPARQL 查询（SELECT/ASK） */
export function executeSparqlQuery(data: SparqlQueryRequest) {
	return request<SparqlQueryResultVO>({ url: BASE + '/query', method: 'post', data });
}

/** 导出 SPARQL 查询结果为 CSV（流式下载） */
export function exportSparqlCsv(data: SparqlQueryRequest) {
	return request({
		url: BASE + '/query/export',
		method: 'post',
		data,
		responseType: 'blob'
	});
}

/** 获取预置查询模板 */
export function fetchSparqlTemplates() {
	return request<SparqlTemplateVO[]>({ url: BASE + '/templates', method: 'get' });
}

/** 分页查询历史 */
export function fetchSparqlHistory(params: SparqlHistoryQuery) {
	return request<PageResult<SparqlQueryLogVO>>({ url: BASE + '/history/page', method: 'get', params });
}

/** 查询历史详情 */
export function getSparqlHistoryDetail(id: number) {
	return request<SparqlQueryLogVO>({ url: BASE + `/history/${id}`, method: 'get' });
}

/** 删除本人查询历史 */
export function deleteSparqlHistory(id: number) {
	return request<boolean>({ url: BASE + `/history/${id}`, method: 'delete' });
}
