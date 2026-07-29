import request from '/@/utils/request';

export const pageList = (params?: Object) => {
	return request({ url: '/admin/rm/test-case/page', method: 'get', params });
};

export const addObj = (obj: Object) => {
	return request({ url: '/admin/rm/test-case', method: 'post', data: obj });
};

export const putObj = (obj: Object) => {
	return request({ url: '/admin/rm/test-case', method: 'put', data: obj });
};

export const delObj = (id: string) => {
	return request({ url: '/admin/rm/test-case/' + id, method: 'delete' });
};

export const coverage = () => {
	return request({ url: '/admin/rm/test-case/coverage', method: 'get' });
};
