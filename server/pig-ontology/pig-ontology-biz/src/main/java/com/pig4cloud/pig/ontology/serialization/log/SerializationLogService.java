/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.ontology.serialization.log.entity.OntSerializationLog;
import com.pig4cloud.pig.ontology.serialization.log.mapper.OntSerializationLogMapper;
import com.pig4cloud.pig.ontology.serialization.vo.SerializationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 序列化审计日志服务。
 *
 * @author youming
 */
@Service
@RequiredArgsConstructor
public class SerializationLogService {

	private final OntSerializationLogMapper logMapper;

	/**
	 * 保存日志并返回ID。
	 */
	public Long saveLog(OntSerializationLog logEntry) {
		logMapper.insert(logEntry);
		return logEntry.getId();
	}

	/**
	 * 分页查询日志。
	 */
	public IPage<SerializationLogVO> queryLogs(Long ontologyId, String operationType, int page, int size) {
		Page<OntSerializationLog> pageParam = new Page<>(page, size);
		IPage<OntSerializationLog> result = logMapper.selectPage(pageParam,
			Wrappers.<OntSerializationLog>lambdaQuery()
				.eq(ontologyId != null, OntSerializationLog::getOntologyId, ontologyId)
				.eq(operationType != null, OntSerializationLog::getOperationType, operationType)
				.orderByDesc(OntSerializationLog::getCreateTime));

		return result.convert(this::toVO);
	}

	private SerializationLogVO toVO(OntSerializationLog log) {
		SerializationLogVO vo = new SerializationLogVO();
		vo.setId(log.getId());
		vo.setOntologyId(log.getOntologyId());
		vo.setOperationType(log.getOperationType());
		vo.setRdfFormat(log.getRdfFormat());
		vo.setExportScope(log.getExportScope());
		vo.setPredicateStrategy(log.getPredicateStrategy());
		vo.setTripleCount(log.getTripleCount());
		vo.setContentSize(log.getContentSize());
		vo.setInstanceCount(log.getInstanceCount());
		vo.setDataValueCount(log.getDataValueCount());
		vo.setObjectRelationCount(log.getObjectRelationCount());
		vo.setSkippedCount(log.getSkippedCount());
		vo.setFailedCount(log.getFailedCount());
		vo.setPrecheckPassed(log.getPrecheckPassed());
		vo.setValidationReportId(log.getValidationReportId());
		vo.setForceFlag(log.getForceFlag());
		vo.setImportIriMergeMode(log.getImportIriMergeMode());
		vo.setDurationMs(log.getDurationMs());
		vo.setCreateBy(log.getCreateBy());
		vo.setCreateTime(log.getCreateTime());
		return vo;
	}

}
