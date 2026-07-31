/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.sync.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.sync.api.entity.EnterpriseCredential;
import com.pig4cloud.pig.sync.api.vo.CredentialListVO;
import com.pig4cloud.pig.sync.api.vo.CredentialVO;
import com.pig4cloud.pig.sync.mapper.EnterpriseCredentialMapper;
import com.pig4cloud.pig.sync.service.CredentialSyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 企业凭据同步服务实现
 * <p>
 * secret_data 在库中由 Jasypt StringEncryptor 加密，返回时解密（传输走 TLS）。
 *
 * @author youming
 * @date 2026-07-31
 */
@Slf4j
@Service
@AllArgsConstructor
public class CredentialSyncServiceImpl extends ServiceImpl<EnterpriseCredentialMapper, EnterpriseCredential>
		implements CredentialSyncService {

	private final StringEncryptor stringEncryptor;

	private final ObjectMapper objectMapper;

	@Override
	public CredentialListVO listCredentials(String userId) {
		// 查询该用户专属 + 全员可用（user_id IS NULL）的凭据
		List<EnterpriseCredential> records = list(Wrappers.<EnterpriseCredential>lambdaQuery()
			.and(w -> w.eq(EnterpriseCredential::getUserId, userId).or().isNull(EnterpriseCredential::getUserId)));

		List<CredentialVO> credentials = new ArrayList<>();
		for (EnterpriseCredential record : records) {
			credentials.add(toVO(record));
		}

		CredentialListVO vo = new CredentialListVO();
		vo.setCredentials(credentials);
		return vo;
	}

	// ================================================================
	// 转换辅助
	// ================================================================

	private CredentialVO toVO(EnterpriseCredential record) {
		CredentialVO vo = new CredentialVO();
		vo.setRef(record.getRef());
		vo.setKind(record.getKind());
		vo.setPublicInfo(parseJson(record.getPublicData()));
		vo.setMeta(parseJson(record.getMetaData()));
		// secrets 解密后解析
		vo.setSecrets(parseJson(decryptSafe(record.getSecretData(), record.getRef())));
		return vo;
	}

	/**
	 * 解密 secret_data，失败时返回原始值（降级，不阻断查询）。
	 */
	private String decryptSafe(String encrypted, String ref) {
		if (StrUtil.isBlank(encrypted)) {
			return null;
		}
		try {
			return stringEncryptor.decrypt(encrypted);
		}
		catch (Exception ex) {
			log.warn("secret_data decrypt failed for ref={}, returning raw", ref, ex);
			return encrypted;
		}
	}

	/**
	 * 安全解析 JSON 文本为 Map，失败返回空 Map。
	 */
	private Map<String, Object> parseJson(String json) {
		if (StrUtil.isBlank(json)) {
			return Collections.emptyMap();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (Exception ex) {
			log.warn("json parse failed: {}", json, ex);
			return Collections.emptyMap();
		}
	}

}
