import request from '/@/utils/request';

export const myTodo = (params?: Object) => {
	return request({ url: '/admin/rm/todo/my', method: 'get', params });
};

export const todoCount = () => {
	return request({ url: '/admin/rm/todo/count', method: 'get' });
};

export const markDone = (id: string) => {
	return request({ url: '/admin/rm/todo/done/' + id, method: 'put' });
};

export const myNotify = (params?: Object) => {
	return request({ url: '/admin/rm/notify/my', method: 'get', params });
};

export const markRead = (id: string) => {
	return request({ url: '/admin/rm/notify/read/' + id, method: 'put' });
};

export const markAllRead = () => {
	return request({ url: '/admin/rm/notify/read-all', method: 'put' });
};
