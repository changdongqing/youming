import request from '/@/utils/request';

// ---------- 序列化预览 ----------

export function preview(projectId: string, format: string = 'TTL') {
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/preview',
		method: 'get',
		params: { format },
	});
}

// ---------- 下载序列化文件（直接返回 URL，前端 window.open） ----------

export function downloadUrl(projectId: string, format: string = 'TTL') {
	return '/admin/ont/model/serialize/' + projectId + '/download?format=' + format;
}

// ---------- 导入 RDF 文件 ----------

export function importRdf(projectId: string, file: File, conflictStrategy: string = 'SKIP') {
	const formData = new FormData();
	formData.append('file', file);
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/import',
		method: 'post',
		params: { conflictStrategy },
		data: formData,
		headers: { 'Content-Type': 'multipart/form-data' },
	});
}

// ---------- 往返一致性校验 ----------

export function validate(projectId: string) {
	return request({
		url: '/admin/ont/model/serialize/' + projectId + '/validate',
		method: 'get',
	});
}
