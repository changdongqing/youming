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

package com.pig4cloud.pig.sync.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.file.core.FileObject;
import com.pig4cloud.pig.common.file.core.FileProperties;
import com.pig4cloud.pig.common.file.core.FileTemplate;
import com.pig4cloud.pig.sync.api.entity.EnterpriseSkill;
import com.pig4cloud.pig.sync.api.entity.PersonalSkill;
import com.pig4cloud.pig.sync.api.vo.SkillManifestItemVO;
import com.pig4cloud.pig.sync.api.vo.SkillManifestVO;
import com.pig4cloud.pig.sync.api.vo.SkillUploadResultVO;
import com.pig4cloud.pig.sync.mapper.EnterpriseSkillMapper;
import com.pig4cloud.pig.sync.mapper.PersonalSkillMapper;
import com.pig4cloud.pig.sync.service.SkillSyncService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 企业技能同步服务实现
 * <p>
 * 技能包文件存对象存储（FileTemplate），数据库只存元数据+路径。
 *
 * @author youming
 * @date 2026-07-31
 */
@Slf4j
@Service
@AllArgsConstructor
public class SkillSyncServiceImpl extends ServiceImpl<EnterpriseSkillMapper, EnterpriseSkill>
		implements SkillSyncService {

	/**
	 * 技能包在对象存储中的目录前缀。
	 */
	private static final String SKILL_DIR = "qwenpaw-skills";

	private final FileTemplate fileTemplate;

	private final FileProperties fileProperties;

	private final PersonalSkillMapper personalSkillMapper;

	@Override
	public SkillManifestVO manifest() {
		List<EnterpriseSkill> records = list();
		List<SkillManifestItemVO> items = records.stream().map(this::toManifestItem).toList();

		// version = 当前最大 enterprise_version（无记录时为 0）
		int version = items.stream()
			.mapToInt(SkillManifestItemVO::getEnterpriseVersion)
			.max()
			.orElse(0);

		SkillManifestVO vo = new SkillManifestVO();
		vo.setVersion(version);
		vo.setSkills(items);
		return vo;
	}

	@Override
	public void download(String name, HttpServletResponse response) {
		EnterpriseSkill skill = getOne(Wrappers.<EnterpriseSkill>lambdaQuery()
			.eq(EnterpriseSkill::getName, name));
		if (skill == null || StrUtil.isBlank(skill.getStoragePath())) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String[] parts = skill.getStoragePath().split("/", 2);
		String dir = parts.length > 0 ? parts[0] : SKILL_DIR;
		String objectName = parts.length > 1 ? parts[1] : name + ".zip";

		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".zip\"");
		try (FileObject fileObject = fileTemplate.getObject(fileProperties.getBucketName(), dir, objectName);
				OutputStream out = response.getOutputStream()) {
			if (fileObject == null || fileObject.getObjectContent() == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			IoUtil.copy(fileObject.getObjectContent(), out);
		}
		catch (Exception ex) {
			log.error("skill download failed: {}", name, ex);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SkillUploadResultVO uploadPersonal(String userId, MultipartFile file) {
		String originalName = file.getOriginalFilename();
		String skillName = StrUtil.isNotBlank(originalName) ? originalName.replaceFirst("\\.zip$", "") : "unnamed";
		String objectName = skillName + "-" + System.currentTimeMillis() + ".zip";

		try (InputStream in = file.getInputStream()) {
			fileTemplate.putObject(fileProperties.getBucketName(), SKILL_DIR, objectName, in, "application/zip");
		}
		catch (Exception ex) {
			log.error("skill upload failed: {}", skillName, ex);
			throw new RuntimeException("技能上传失败：" + ex.getMessage(), ex);
		}

		// 记录到个人技能表（同名覆盖）
		PersonalSkill exist = personalSkillMapper.selectOne(Wrappers.<PersonalSkill>lambdaQuery()
			.eq(PersonalSkill::getName, skillName)
			.eq(PersonalSkill::getUserId, userId));
		PersonalSkill record = exist != null ? exist : new PersonalSkill();
		record.setName(skillName);
		record.setStoragePath(SKILL_DIR + "/" + objectName);
		record.setUserId(userId);
		if (exist == null) {
			personalSkillMapper.insert(record);
		}
		else {
			personalSkillMapper.updateById(record);
		}

		SkillUploadResultVO vo = new SkillUploadResultVO();
		vo.setName(skillName);
		vo.setStored(true);
		vo.setVersion(record.getVersion());
		return vo;
	}

	// ================================================================
	// 转换辅助
	// ================================================================

	private SkillManifestItemVO toManifestItem(EnterpriseSkill skill) {
		SkillManifestItemVO item = new SkillManifestItemVO();
		item.setName(skill.getName());
		item.setVersion(skill.getVersion());
		item.setMdHash(skill.getMdHash());
		item.setEnterpriseVersion(skill.getEnterpriseVersion());
		item.setDescription(skill.getDescription());
		return item;
	}

}
