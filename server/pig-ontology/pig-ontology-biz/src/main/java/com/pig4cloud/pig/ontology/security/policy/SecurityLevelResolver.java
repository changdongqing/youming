/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.security.policy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.security.entity.OntSecurityLevel;
import com.pig4cloud.pig.ontology.security.mapper.OntSecurityLevelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 四层数据分级继承：
 * <pre>
 * entity type level → instance override → property level → instance value override
 * </pre>
 * 有效等级取各层 rank 最大值。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityLevelResolver {

	private final OntSecurityLevelMapper securityLevelMapper;

	/**
	 * 计算有效安全级别编码。
	 * <p>
	 * 四层继承取最大 rank：
	 * <ol>
	 *   <li>实体类型 security_level_code</li>
	 *   <li>实例 override security_level_code（可为空，继承上级）</li>
	 *   <li>数据/对象属性 security_level_code</li>
	 *   <li>实例值 override security_level_code（可为空，继承上级）</li>
	 * </ol>
	 *
	 * @param entityTypeLevel    实体类型安全级别编码
	 * @param instanceLevel      实例覆盖安全级别编码（可为 null）
	 * @param propertyLevel      数据/对象属性安全级别编码
	 * @param valueLevel         实例值覆盖安全级别编码（可为 null）
	 * @return 有效安全级别编码
	 */
	public String resolveEffectiveLevel(String entityTypeLevel, String instanceLevel,
			String propertyLevel, String valueLevel) {
		// 每层可为空时继承上级，非空时取实际值
		String level1 = entityTypeLevel != null ? entityTypeLevel : SecurityConstants.LEVEL_INTERNAL;
		String level2 = instanceLevel != null ? instanceLevel : level1;
		String level3 = propertyLevel != null ? propertyLevel : level2;
		String level4 = valueLevel != null ? valueLevel : level3;

		// 取最大 rank
		Map<String, Integer> rankMap = getLevelRankMap();
		return maxByRank(List.of(level1, level2, level3, level4), rankMap);
	}

	/**
	 * 计算对象关系的有效安全级别。
	 * <p>
	 * 对象关系的敏感级别至少取"主语实例、对象属性、宾语实例"三者最大值。
	 *
	 * @param subjectInstanceLevel 主语实例安全级别编码
	 * @param objectPropertyLevel  对象属性安全级别编码
	 * @param objectInstanceLevel  宾语实例安全级别编码
	 * @return 有效安全级别编码
	 */
	public String resolveRelationLevel(String subjectInstanceLevel, String objectPropertyLevel,
			String objectInstanceLevel) {
		Map<String, Integer> rankMap = getLevelRankMap();
		String l1 = subjectInstanceLevel != null ? subjectInstanceLevel : SecurityConstants.LEVEL_INTERNAL;
		String l2 = objectPropertyLevel != null ? objectPropertyLevel : SecurityConstants.LEVEL_INTERNAL;
		String l3 = objectInstanceLevel != null ? objectInstanceLevel : SecurityConstants.LEVEL_INTERNAL;
		return maxByRank(List.of(l1, l2, l3), rankMap);
	}

	/**
	 * 获取安全级别编码对应的 rank 值。
	 */
	public int getRank(String levelCode) {
		if (levelCode == null) {
			levelCode = SecurityConstants.LEVEL_INTERNAL;
		}
		Map<String, Integer> rankMap = getLevelRankMap();
		return rankMap.getOrDefault(levelCode, rankMap.getOrDefault(SecurityConstants.LEVEL_INTERNAL, 20));
	}

	/**
	 * 获取安全级别的默认查看效果。
	 */
	public String getDefaultViewEffect(String levelCode) {
		final String effectiveCode = levelCode != null ? levelCode : SecurityConstants.LEVEL_INTERNAL;
		List<OntSecurityLevel> levels = getAllLevels();
		return levels.stream()
			.filter(l -> effectiveCode.equals(l.getLevelCode()))
			.map(OntSecurityLevel::getDefaultViewEffect)
			.findFirst()
			.orElse("ALLOW");
	}

	/**
	 * 获取所有安全级别编码到 rank 的映射。
	 * <p>
	 * 使用 Spring Cache 缓存，安全级别变更后需清除缓存。
	 */
	@Cacheable(value = "ont_security_level_ranks", key = "'all'")
	public Map<String, Integer> getLevelRankMap() {
		List<OntSecurityLevel> levels = getAllLevels();
		Map<String, Integer> map = new HashMap<>(levels.size());
		for (OntSecurityLevel level : levels) {
			map.put(level.getLevelCode(), level.getLevelRank());
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * 获取所有安全级别记录。
	 */
	@Cacheable(value = "ont_security_levels", key = "'all'")
	public List<OntSecurityLevel> getAllLevels() {
		return securityLevelMapper.selectList(
			Wrappers.<OntSecurityLevel>lambdaQuery().eq(OntSecurityLevel::getDelFlag, "0"));
	}

	/**
	 * 从多个级别编码中取 rank 最大的。
	 */
	private String maxByRank(List<String> codes, Map<String, Integer> rankMap) {
		String maxCode = codes.get(0);
		int maxRank = rankMap.getOrDefault(maxCode, 0);
		for (int i = 1; i < codes.size(); i++) {
			int rank = rankMap.getOrDefault(codes.get(i), 0);
			if (rank > maxRank) {
				maxRank = rank;
				maxCode = codes.get(i);
			}
		}
		return maxCode;
	}

}
