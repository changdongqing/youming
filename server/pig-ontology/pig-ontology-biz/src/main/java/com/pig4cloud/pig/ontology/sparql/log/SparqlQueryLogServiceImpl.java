/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.sparql.log;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.ontology.sparql.log.entity.OntSparqlQueryLog;
import com.pig4cloud.pig.ontology.sparql.vo.SparqlQueryLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * SPARQL 查询日志服务实现。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SparqlQueryLogServiceImpl implements SparqlQueryLogService {

	/** 字符串字面量正则（用于生成 query_preview） */
	private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("\"[^\"]*\"");

	/** 查询预览最大长度 */
	private static final int PREVIEW_MAX_LENGTH = 2000;

	private final OntSparqlQueryLogMapper queryLogMapper;

	@Override
	public void log(Long ontologyId, String queryType, String queryText, String resultFormat,
			Integer rowCount, Long durationMs, String truncated, String status,
			String errorCode, String currentUser) {
		try {
			OntSparqlQueryLog entity = new OntSparqlQueryLog();
			entity.setOntologyId(ontologyId);
			entity.setQueryType(queryType);
			// query_text 默认不落库（设计文档 §5.1）
			entity.setQueryText(null);
			entity.setQueryPreview(generatePreview(queryText));
			entity.setQueryHash(computeHash(queryText));
			entity.setResultFormat(resultFormat != null ? resultFormat : "JSON");
			entity.setRowCount(rowCount != null ? rowCount : 0);
			entity.setDurationMs(durationMs != null ? durationMs : 0L);
			entity.setTruncated(truncated != null ? truncated : "0");
			entity.setStatus(status);
			entity.setErrorCode(errorCode);
			entity.setCreateBy(currentUser);
			queryLogMapper.insert(entity);
		}
		catch (Exception e) {
			// 日志记录失败不应影响查询结果
			log.error("SPARQL查询日志记录失败: ontologyId={}, status={}", ontologyId, status, e);
		}
	}

	@Override
	public IPage<SparqlQueryLogVO> queryHistory(Long ontologyId, String createBy, int page, int size) {
		Page<OntSparqlQueryLog> pageParam = new Page<>(page, size);
		IPage<OntSparqlQueryLog> result = queryLogMapper.selectPage(pageParam,
				Wrappers.<OntSparqlQueryLog>lambdaQuery()
						.eq(ontologyId != null, OntSparqlQueryLog::getOntologyId, ontologyId)
						.eq(StrUtil.isNotBlank(createBy), OntSparqlQueryLog::getCreateBy, createBy)
						.orderByDesc(OntSparqlQueryLog::getCreateTime));

		return result.convert(this::convertToVO);
	}

	@Override
	public SparqlQueryLogVO queryById(Long id, String currentUser) {
		OntSparqlQueryLog entity = queryLogMapper.selectById(id);
		if (entity == null) {
			return null;
		}
		// 所有权校验：非本人不能查看（管理员可扩展）
		if (!entity.getCreateBy().equals(currentUser)) {
			log.warn("非本人查询历史访问: id={}, owner={}, requester={}", id, entity.getCreateBy(), currentUser);
			return null;
		}
		return convertToVO(entity);
	}

	@Override
	public boolean deleteById(Long id, String currentUser) {
		OntSparqlQueryLog entity = queryLogMapper.selectById(id);
		if (entity == null) {
			return false;
		}
		// 所有权校验
		if (!entity.getCreateBy().equals(currentUser)) {
			log.warn("非本人查询历史删除: id={}, owner={}, requester={}", id, entity.getCreateBy(), currentUser);
			return false;
		}
		return queryLogMapper.deleteById(id) > 0;
	}

	/**
	 * 生成查询预览：去除字符串字面量后截断。
	 */
	private String generatePreview(String queryText) {
		if (queryText == null || queryText.isBlank()) {
			return "";
		}
		// 去除/替换字符串字面量为占位符
		String preview = STRING_LITERAL_PATTERN.matcher(queryText).replaceAll("\"...\"");
		if (preview.length() > PREVIEW_MAX_LENGTH) {
			preview = preview.substring(0, PREVIEW_MAX_LENGTH);
		}
		return preview;
	}

	/**
	 * 计算规范化查询文本的 SHA-256。
	 */
	private String computeHash(String queryText) {
		if (queryText == null || queryText.isBlank()) {
			return "";
		}
		try {
			// 规范化：去除前后空白、统一换行
			String normalized = queryText.trim().replaceAll("\\s+", " ");
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (Exception e) {
			log.warn("查询hash计算失败", e);
			return "";
		}
	}

	private SparqlQueryLogVO convertToVO(OntSparqlQueryLog entity) {
		SparqlQueryLogVO vo = new SparqlQueryLogVO();
		BeanUtils.copyProperties(entity, vo);
		return vo;
	}

}
