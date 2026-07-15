/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.mapping.RelationMappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.compiler.RelationKeyCompiler;
import com.pig4cloud.pig.ontology.mapping.entity.OntPendingRelation;
import com.pig4cloud.pig.ontology.mapping.mapper.OntPendingRelationMapper;
import com.pig4cloud.pig.ontology.mapping.service.PendingRelationService;
import com.pig4cloud.pig.ontology.mapping.vo.PendingRelationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 待解析关系服务实现（18-05 §13）。
 * <p>
 * 提供查询和手动管理（人工重试、管理员忽略），不实现后台自动调度。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingRelationServiceImpl extends ServiceImpl<OntPendingRelationMapper, OntPendingRelation>
		implements PendingRelationService {

	private final RelationKeyCompiler relationKeyCompiler;

	// ==================== 查询 ====================

	@Override
	public Page<PendingRelationVO> page(Page<OntPendingRelation> page, Long mappingProjectId,
			String pendingStatus, String relationMappingCode) {
		Page<OntPendingRelation> result = baseMapper.selectPage(page,
				Wrappers.<OntPendingRelation>lambdaQuery()
						.eq(mappingProjectId != null, OntPendingRelation::getMappingProjectId, mappingProjectId)
						.eq(StrUtil.isNotBlank(pendingStatus), OntPendingRelation::getPendingStatus, pendingStatus)
						.eq(StrUtil.isNotBlank(relationMappingCode),
								OntPendingRelation::getRelationMappingCode, relationMappingCode)
						.eq(OntPendingRelation::getDelFlag, "0")
						.orderByAsc(OntPendingRelation::getNextRetryAt)
						.orderByDesc(OntPendingRelation::getCreateTime));

		Page<PendingRelationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
		voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
		return voPage;
	}

	// ==================== 人工重试 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PendingRelationVO retry(Long id) {
		OntPendingRelation pending = findByIdOrThrow(id);

		// 只有 PENDING 和 FAILED 状态可重试
		if (!"PENDING".equals(pending.getPendingStatus())
				&& !"FAILED".equals(pending.getPendingStatus())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_016.getMessage()
					+ ": 仅 PENDING / FAILED 状态可重试");
		}

		// 重置为 PENDING，重试次数 +1，设置立即重试
		pending.setPendingStatus("PENDING");
		pending.setRetryCount(pending.getRetryCount() + 1);
		pending.setNextRetryAt(LocalDateTime.now());
		pending.setLastErrorCode(null);
		pending.setLastErrorMessage(null);

		baseMapper.updateById(pending);

		log.info("Manual retry pending relation: id={}, retryCount={}", id, pending.getRetryCount());
		return toVO(pending);
	}

	// ==================== 管理员忽略 ====================

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PendingRelationVO ignore(Long id) {
		OntPendingRelation pending = findByIdOrThrow(id);

		// 只有 PENDING 和 FAILED 状态可忽略
		if (!"PENDING".equals(pending.getPendingStatus())
				&& !"FAILED".equals(pending.getPendingStatus())) {
			throw new IllegalStateException(RelationMappingErrorCode.ONT_REL_016.getMessage()
					+ ": 仅 PENDING / FAILED 状态可忽略");
		}

		pending.setPendingStatus("IGNORED");

		baseMapper.updateById(pending);

		log.info("Ignored pending relation: id={}", id);
		return toVO(pending);
	}

	// ==================== 内部方法 ====================

	private OntPendingRelation findByIdOrThrow(Long id) {
		OntPendingRelation pending = baseMapper.selectById(id);
		if (pending == null || "1".equals(pending.getDelFlag())) {
			throw new IllegalArgumentException(RelationMappingErrorCode.ONT_REL_015.getMessage());
		}
		return pending;
	}

	/**
	 * 转换为脱敏 VO。
	 * <p>
	 * 不返回 sourceRelationKey / subjectRecordKey / objectRecordKey 明文，
	 * 只返回 hash 后缀摘要。
	 */
	private PendingRelationVO toVO(OntPendingRelation pending) {
		PendingRelationVO vo = new PendingRelationVO();
		vo.setId(pending.getId());
		vo.setMappingProjectId(pending.getMappingProjectId());
		vo.setMappingVersionId(pending.getMappingVersionId());
		vo.setRelationMappingCode(pending.getRelationMappingCode());
		vo.setSourceId(pending.getSourceId());
		// 脱敏：只返回 hash 后缀摘要
		vo.setSourceRelationKeyDigest(relationKeyCompiler.toDigest(pending.getSourceRelationKeyHash()));
		vo.setSubjectEntityMappingCode(pending.getSubjectEntityMappingCode());
		vo.setSubjectRecordKeyDigest(relationKeyCompiler.toDigest(pending.getSubjectRecordKeyHash()));
		vo.setObjectEntityMappingCode(pending.getObjectEntityMappingCode());
		vo.setObjectRecordKeyDigest(relationKeyCompiler.toDigest(pending.getObjectRecordKeyHash()));
		vo.setPendingReason(pending.getPendingReason());
		vo.setPendingStatus(pending.getPendingStatus());
		vo.setRetryCount(pending.getRetryCount());
		vo.setNextRetryAt(pending.getNextRetryAt());
		vo.setLastErrorCode(pending.getLastErrorCode());
		vo.setLastErrorMessage(pending.getLastErrorMessage());
		vo.setFirstJobId(pending.getFirstJobId());
		vo.setLastJobId(pending.getLastJobId());
		vo.setResolvedRelationId(pending.getResolvedRelationId());
		vo.setCreateTime(pending.getCreateTime());
		vo.setUpdateTime(pending.getUpdateTime());
		return vo;
	}

}
