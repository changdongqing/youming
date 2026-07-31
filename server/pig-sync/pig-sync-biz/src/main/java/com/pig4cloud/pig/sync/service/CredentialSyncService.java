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

package com.pig4cloud.pig.sync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.sync.api.entity.EnterpriseCredential;
import com.pig4cloud.pig.sync.api.vo.CredentialListVO;

/**
 * 企业凭据同步服务接口
 *
 * @author youming
 * @date 2026-07-31
 */
public interface CredentialSyncService extends IService<EnterpriseCredential> {

	/**
	 * 查询该用户可访问的企业凭据（含全员可用 user_id IS NULL 的）。
	 * secrets 在库中加密存储，返回时解密。
	 */
	CredentialListVO listCredentials(String userId);

}
