/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pig4cloud.pig.ontology.serialization.dto.ExportRequest;
import com.pig4cloud.pig.ontology.serialization.dto.ImportConfirmRequest;
import com.pig4cloud.pig.ontology.serialization.format.RdfFormat;
import com.pig4cloud.pig.ontology.serialization.vo.ExportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ExportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.ImportPreviewVO;
import com.pig4cloud.pig.ontology.serialization.vo.ImportResultVO;
import com.pig4cloud.pig.ontology.serialization.vo.SerializationLogVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 序列化与交换编排服务。
 *
 * @author youming
 */
public interface SerializationService {

	/**
	 * 导出本体为 RDF 字符串。
	 * @param request 导出请求
	 * @return 导出结果
	 */
	ExportResultVO exportOntology(ExportRequest request);

	/**
	 * 预览导出内容（限200行）。
	 * @param request 导出请求
	 * @return 预览结果
	 */
	ExportPreviewVO previewExport(ExportRequest request);

	/**
	 * 导出本体为文件流（直接写入 HttpServletResponse）。
	 * @param request 导出请求
	 * @param response HTTP 响应
	 */
	void exportToFile(ExportRequest request, HttpServletResponse response);

	/**
	 * 导入预检（解析RDF文件，生成映射预览，不写入数据库）。
	 * @param file 上传的 RDF 文件
	 * @param ontologyId 本体工程ID
	 * @param format 指定格式（null则默认Turtle）
	 * @return 预检结果
	 */
	ImportPreviewVO importPreview(MultipartFile file, Long ontologyId, RdfFormat format);

	/**
	 * 导入确认（按预检结果写入数据库）。
	 * @param request 确认请求
	 * @return 导入结果
	 */
	ImportResultVO importConfirm(ImportConfirmRequest request);

	/**
	 * 查询审计日志。
	 * @param ontologyId 本体工程ID
	 * @param operationType 操作类型
	 * @param page 页码
	 * @param size 每页条数
	 * @return 分页结果
	 */
	IPage<SerializationLogVO> queryLogs(Long ontologyId, String operationType, int page, int size);

}
