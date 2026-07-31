/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary, with or without
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
import com.pig4cloud.pig.sync.api.entity.EnterpriseSkill;
import com.pig4cloud.pig.sync.api.vo.SkillManifestVO;
import com.pig4cloud.pig.sync.api.vo.SkillUploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 企业技能同步服务接口
 *
 * @author youming
 * @date 2026-07-31
 */
public interface SkillSyncService extends IService<EnterpriseSkill> {

	/**
	 * 技能 manifest（含 version + skills 列表，供客户端增量比对）。
	 */
	SkillManifestVO manifest();

	/**
	 * 流式下载技能 zip 到 HttpServletResponse。
	 */
	void download(String name, HttpServletResponse response);

	/**
	 * 个人技能上传（云备份），存对象存储并记录到 qwenpaw_personal_skills。
	 */
	SkillUploadResultVO uploadPersonal(String userId, MultipartFile file);

}
