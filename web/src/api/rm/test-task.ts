import request from '/@/utils/request';

export const pageList = (params?: Object) => {
	return request({ url: '/admin/rm/test-task/page', method: 'get', params });
};

export const getObj = (id: string) => {
	return request({ url: '/admin/rm/test-task/' + id, method: 'get' });
};

export const execute = (data: object) => {
	return request({ url: '/admin/rm/test-task/execute', method: 'post', data });
};

export const toBug = (executionId: string, data: object) => {
	return request({ url: '/admin/rm/test-task/to-bug', method: 'post', params: { executionId }, data });
};

export const passTest = (id: string) => {
	return request({ url: '/admin/rm/test-task/pass/' + id, method: 'post' });
};

export const rejectTest = (id: string, remark?: string) => {
	return request({ url: '/admin/rm/test-task/reject/' + id, method: 'post', params: { remark } });
};

export const saveWorkload = (data: object) => {
	return request({ url: '/admin/rm/test-task/workload', method: 'post', data });
};
