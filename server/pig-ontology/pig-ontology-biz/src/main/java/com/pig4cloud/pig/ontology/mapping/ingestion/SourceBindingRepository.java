/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.mapping.ingestion.entity.OntSourceInstanceBinding;
import com.pig4cloud.pig.ontology.mapping.ingestion.mapper.OntSourceInstanceBindingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 来源绑定仓储。
 * <p>
 * 封装来源绑定的查询和并发安全创建。
 * <p>
 * 并发通过唯一索引 {@code uk_ont_binding_identity} 兜底；冲突事务捕获唯一键异常后重新查询绑定，
 * 不通过"先查再插"假设实现幂等。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SourceBindingRepository {

	private final OntSourceInstanceBindingMapper bindingMapper;

	/**
	 * 按来源身份键哈希悲观锁查询绑定。
	 * @param sourceIdentity 来源身份
	 * @return 绑定记录，不存在返回 null
	 */
	public OntSourceInstanceBinding findByIdentityForUpdate(SourceIdentity sourceIdentity) {
		return bindingMapper.selectByIdentityForUpdate(
			sourceIdentity.mappingProjectId(),
			sourceIdentity.entityMappingCode(),
			sourceIdentity.keyHash()
		);
	}

	/**
	 * 并发安全地创建来源绑定。
	 * <p>
	 * 如果并发插入触发唯一键冲突，重新查询并返回已有绑定。
	 * 哈希命中后必须由调用方再次比较完整 sourceRecordKey。
	 *
	 * @param binding 待插入的绑定记录
	 * @return 插入成功返回传入的 binding，冲突返回已有绑定
	 */
	public OntSourceInstanceBinding insertOrFind(OntSourceInstanceBinding binding) {
		try {
			bindingMapper.insert(binding);
			return binding;
		}
		catch (DuplicateKeyException e) {
			log.debug("来源绑定并发冲突，重新查询: project={}, code={}, keyHash={}",
				binding.getMappingProjectId(),
				binding.getEntityMappingCode(),
				binding.getSourceRecordKeyHash());
			return bindingMapper.selectOne(Wrappers.<OntSourceInstanceBinding>lambdaQuery()
				.eq(OntSourceInstanceBinding::getMappingProjectId, binding.getMappingProjectId())
				.eq(OntSourceInstanceBinding::getEntityMappingCode, binding.getEntityMappingCode())
				.eq(OntSourceInstanceBinding::getSourceRecordKeyHash, binding.getSourceRecordKeyHash())
				.last("FOR UPDATE"));
		}
	}

	/**
	 * 更新绑定。
	 * @param binding 绑定记录
	 */
	public void update(OntSourceInstanceBinding binding) {
		bindingMapper.updateById(binding);
	}

}
