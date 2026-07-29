import request from '/@/utils/request';

export const pageList = (params?: Object) => {
	return request({ url: '/admin/rm/requirement/page', method: 'get', params });
};

export const getObj = (id: string) => {
	return request({ url: '/admin/rm/requirement/' + id, method: 'get' });
};

export const addObj = (obj: Object) => {
	return request({ url: '/admin/rm/requirement', method: 'post', data: obj });
};

export const putObj = (obj: Object) => {
	return request({ url: '/admin/rm/requirement', method: 'put', data: obj });
};

export const delObj = (id: string) => {
	return request({ url: '/admin/rm/requirement/' + id, method: 'delete' });
};

export const submitReq = (id: string) => {
	return request({ url: '/admin/rm/requirement/submit/' + id, method: 'post' });
};

export const approveReq = (data: object) => {
	return request({ url: '/admin/rm/requirement/approve', method: 'post', data });
};

export const reviewReq = (data: object) => {
	return request({ url: '/admin/rm/requirement/review', method: 'post', data });
};

export const saveDesign = (data: object) => {
	return request({ url: '/admin/rm/requirement/design', method: 'post', data });
};

export const submitDesignReview = (id: string) => {
	return request({ url: '/admin/rm/requirement/design-review/submit/' + id, method: 'post' });
};

export const designReview = (data: object) => {
	return request({ url: '/admin/rm/requirement/design-review', method: 'post', data });
};

export const scheduleReq = (data: object) => {
	return request({ url: '/admin/rm/requirement/schedule', method: 'post', data });
};

export const qualityConfirm = (id: string) => {
	return request({ url: '/admin/rm/requirement/quality-confirm/' + id, method: 'post' });
};

export const acceptReq = (id: string, conclusion: string, remark?: string) => {
	return request({ url: '/admin/rm/requirement/accept/' + id, method: 'post', params: { conclusion, remark } });
};

export const getStatistics = () => {
	return request({ url: '/admin/rm/requirement/statistics', method: 'get' });
};
