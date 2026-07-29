import request from '/@/utils/request';

export const pageList = (params?: Object) => {
	return request({ url: '/admin/rm/bug/page', method: 'get', params });
};

export const getObj = (id: string) => {
	return request({ url: '/admin/rm/bug/' + id, method: 'get' });
};

export const handle = (data: object) => {
	return request({ url: '/admin/rm/bug/handle', method: 'post', data });
};
