import request from '/@/utils/request';

// ---------- 画布状态 ----------

export function getState(projectId: string) {
	return request({
		url: '/admin/ont/model/canvas/' + projectId + '/state',
		method: 'get',
	});
}

export function saveState(projectId: string, graphData: string) {
	return request({
		url: '/admin/ont/model/canvas/' + projectId + '/state',
		method: 'put',
		data: { graphData },
	});
}

// ---------- 画布图数据 ----------

export function getGraph(projectId: string) {
	return request({
		url: '/admin/ont/model/canvas/' + projectId + '/graph',
		method: 'get',
	});
}
