import request from '/@/utils/request';

export const pageList = (params?: Object) => {
	return request({ url: '/admin/rm/dev-task/page', method: 'get', params });
};

export const getObj = (id: string) => {
	return request({ url: '/admin/rm/dev-task/' + id, method: 'get' });
};

export const createTasks = (requirementId: string, tasks: any[]) => {
	return request({ url: '/admin/rm/dev-task', method: 'post', params: { requirementId }, data: tasks });
};

export const putObj = (obj: Object) => {
	return request({ url: '/admin/rm/dev-task', method: 'put', data: obj });
};

export const saveDetailDesign = (data: object) => {
	return request({ url: '/admin/rm/dev-task/detail-design', method: 'post', data });
};

export const submitDesignReview = (id: string) => {
	return request({ url: '/admin/rm/dev-task/design-review/submit/' + id, method: 'post' });
};

export const designReview = (data: object) => {
	return request({ url: '/admin/rm/dev-task/design-review', method: 'post', data });
};

export const startDev = (id: string) => {
	return request({ url: '/admin/rm/dev-task/start/' + id, method: 'post' });
};

export const submitTest = (id: string) => {
	return request({ url: '/admin/rm/dev-task/submit-test/' + id, method: 'post' });
};
