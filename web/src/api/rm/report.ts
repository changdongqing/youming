import request from '/@/utils/request';

export const requirementDetail = (params?: Object) => {
	return request({ url: '/admin/rm/report/requirement-detail', method: 'get', params });
};

export const requirementStat = (params?: Object) => {
	return request({ url: '/admin/rm/report/requirement-stat', method: 'get', params });
};

export const devTaskStat = (params?: Object) => {
	return request({ url: '/admin/rm/report/dev-task-stat', method: 'get', params });
};
