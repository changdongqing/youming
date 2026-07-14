/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntInstanceValueProvenance;
import com.pig4cloud.pig.ontology.mapping.ingestion.mapper.OntInstanceValueProvenanceMapper;
import com.pig4cloud.pig.ontology.security.policy.SecuritySubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 映射来源值写入守卫实现。
 * <p>
 * 检查数据值是否被映射来源拥有。如果被拥有且非受控覆盖（forceOverride），
 * 拒绝普通修改。MANUAL_WINS 策略允许受控覆盖；SOURCE_WINS 仅管理员可强制覆盖。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MappedValueWriteGuardImpl implements MappedValueWriteGuard {

	private final OntInstanceValueProvenanceMapper valueProvenanceMapper;

	@Override
	public void assertWritable(Long dataValueId, SecuritySubject subject, boolean forceOverride) {
		OntInstanceValueProvenance provenance = valueProvenanceMapper.selectOne(
			Wrappers.<OntInstanceValueProvenance>lambdaQuery()
				.eq(OntInstanceValueProvenance::getDataValueId, dataValueId)
				.eq(OntInstanceValueProvenance::getDelFlag, "0"));

		if (provenance == null) {
			// 无来源记录，允许手工修改
			return;
		}

		if (!"ACTIVE".equals(provenance.getProvenanceStatus())) {
			// 溯源状态非ACTIVE（已OVERRIDDEN或STALE），允许修改
			return;
		}

		// 值被映射来源拥有
		if (forceOverride) {
			// 受控覆盖：将溯源状态改为OVERRIDDEN
			provenance.setProvenanceStatus("OVERRIDDEN");
			valueProvenanceMapper.updateById(provenance);
			log.info("受控人工覆盖映射来源值: dataValueId={}, fieldMappingCode={}, subject={}",
				dataValueId, provenance.getFieldMappingCode(),
				subject != null ? subject.getUsername() : "system");
			return;
		}

		// 非受控覆盖，拒绝
		throw new IllegalStateException(
			"数据值被映射来源拥有（fieldMappingCode=" + provenance.getFieldMappingCode()
				+ "，ownershipPolicy=" + provenance.getOwnershipPolicy()
				+ "），需管理员受控覆盖方可修改");
	}

}
