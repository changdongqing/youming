/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.diff;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.mapping.project.MappingErrorCode;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingProject;
import com.pig4cloud.pig.ontology.mapping.project.entity.OntMappingVersion;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingProjectMapper;
import com.pig4cloud.pig.ontology.mapping.project.mapper.OntMappingVersionMapper;
import com.pig4cloud.pig.ontology.mapping.project.vo.MappingVersionDiffVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 映射版本配置差异对比服务（18-03 §12）。
 * <p>
 * 比较两个版本的配置（工程信息+版本信息），返回变更分类和变更项列表。
 * 不包含凭证或源样例值。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MappingVersionDiffService {

	private final OntMappingVersionMapper versionMapper;

	private final OntMappingProjectMapper projectMapper;

	private final MappingChangeClassifier changeClassifier;

	/**
	 * 对比两个映射版本的配置差异。
	 * @param baseVersionId 基准版本ID
	 * @param compareVersionId 对比版本ID
	 * @return 差异结果
	 */
	public MappingVersionDiffVO diff(Long baseVersionId, Long compareVersionId) {
		OntMappingVersion baseVersion = findVersionOrThrow(baseVersionId);
		OntMappingVersion compareVersion = findVersionOrThrow(compareVersionId);

		OntMappingProject baseProject = projectMapper.selectById(baseVersion.getMappingProjectId());
		OntMappingProject compareProject = projectMapper.selectById(compareVersion.getMappingProjectId());

		List<MappingChangeClassifier.ChangeRecord> changes = changeClassifier.classify(
				baseProject, baseVersion, compareProject, compareVersion);

		MappingVersionDiffVO vo = new MappingVersionDiffVO();
		vo.setBaseVersionId(baseVersionId);
		vo.setBaseVersionNumber(baseVersion.getVersionNumber());
		vo.setCompareVersionId(compareVersionId);
		vo.setCompareVersionNumber(compareVersion.getVersionNumber());
		vo.setChangeClassification(changeClassifier.getHighestClassification(changes));

		List<MappingVersionDiffVO.ChangeItem> items = changes.stream().map(c -> {
			MappingVersionDiffVO.ChangeItem item = new MappingVersionDiffVO.ChangeItem();
			item.setField(c.field());
			item.setOldValue(c.oldValue());
			item.setNewValue(c.newValue());
			item.setClassification(c.classification());
			return item;
		}).toList();
		vo.setChanges(items);

		return vo;
	}

	/**
	 * 查找版本或抛出异常。
	 */
	private OntMappingVersion findVersionOrThrow(Long versionId) {
		OntMappingVersion version = versionMapper.selectOne(
				Wrappers.<OntMappingVersion>lambdaQuery()
						.eq(OntMappingVersion::getId, versionId)
						.eq(OntMappingVersion::getDelFlag, "0"));
		if (version == null) {
			throw new IllegalArgumentException(MappingErrorCode.ONT_MAP_002.getMessage());
		}
		return version;
	}

}
