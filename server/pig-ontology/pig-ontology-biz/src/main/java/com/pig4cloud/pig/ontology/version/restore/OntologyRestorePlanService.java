/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.restore;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.version.diff.OntologyCompatibilityClassifier;
import com.pig4cloud.pig.ontology.version.diff.OntologyVersionDiffService;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotBuilder;
import com.pig4cloud.pig.ontology.version.snapshot.OntologySnapshotCanonicalizer;
import com.pig4cloud.pig.ontology.version.vo.RestorePlanVO;
import com.pig4cloud.pig.ontology.version.vo.VersionDiffVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 从历史快照生成正向恢复计划。
 * <p>
 * 按设计文档 §5.6：选择历史版本 → 当前工作区快照 vs 历史快照 diff → 生成恢复计划与 breaking 风险。
 * 管理员确认后进入维护窗口 → 将恢复计划应用到工作区 → 执行必要的实例迁移 → 全量校验 →
 * 发布新的更高 SemVer 版本 → 新版本 restore_source_version_id 指向历史版本。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyRestorePlanService {

	private final OntOntologyVersionMapper versionMapper;

	private final OntologySnapshotBuilder snapshotBuilder;

	private final OntologySnapshotCanonicalizer canonicalizer;

	private final OntologyVersionDiffService diffService;

	private final OntologyCompatibilityClassifier classifier;

	/**
	 * 生成恢复计划（不直接改数据）。
	 * @param ontologyId 本体工程ID
	 * @param targetVersionId 要恢复到的历史版本ID
	 * @return 恢复计划
	 */
	public R<RestorePlanVO> generateRestorePlan(Long ontologyId, Long targetVersionId) {
		OntOntologyVersion targetVersion = versionMapper.selectById(targetVersionId);
		if (targetVersion == null) {
			return R.failed("目标历史版本不存在");
		}
		if (!"PUBLISHED".equals(targetVersion.getReleaseStatus())) {
			return R.failed("只能恢复到已发布的历史版本");
		}

		// 构建当前工作区快照
		String currentSnapshotJson = snapshotBuilder.buildSnapshot(ontologyId,
			targetVersion.getVersionIri() != null ? targetVersion.getVersionIri().replace(targetVersion.getVersionNumber(), "") : "",
			"");
		ObjectNode currentSnap = canonicalizer.canonicalize(currentSnapshotJson);

		// 构建历史版本快照
		ObjectNode targetSnap = canonicalizer.canonicalize(targetVersion.getSchemaSnapshot());

		// diff：current vs target（注意方向：恢复=把当前变回历史）
		VersionDiffVO diff = diffService.diffCanonicalized(currentSnap, targetSnap);

		// 兼容性判定
		OntologyCompatibilityClassifier.CompatibilityResult compatResult = classifier.classify(currentSnap, targetSnap);

		// 构建恢复计划
		RestorePlanVO plan = new RestorePlanVO();
		plan.setTargetVersionId(targetVersionId);
		plan.setTargetVersionNumber(targetVersion.getVersionNumber());
		plan.setCompatibility(compatResult.compatibility());
		plan.setBreakingReasons(compatResult.breakingReasons());

		// 需要恢复的 Schema = diff 中 removed（当前没有但历史有的）
		plan.setSchemaToRestore(diff.getRemoved());

		// 需要移除的 Schema = diff 中 added（当前有但历史没有的）
		plan.setSchemaToRemove(diff.getAdded());

		// 推断需要的迁移规则
		plan.setMigrationRules(inferMigrationRules(diff));

		// 风险提示
		plan.setRiskWarnings(generateRiskWarnings(compatResult, diff));

		return R.ok(plan);
	}

	private List<String> inferMigrationRules(VersionDiffVO diff) {
		List<String> rules = new ArrayList<>();

		if (diff.getRemoved() != null) {
			for (VersionDiffVO.DiffResource res : diff.getRemoved()) {
				if ("DATA_PROPERTY".equals(res.getResourceType())) {
					rules.add("SET_DEFAULT_LITERAL: 恢复数据属性 " + res.getIri());
				}
				if ("ENTITY_TYPE".equals(res.getResourceType())) {
					rules.add("RETYPE_INSTANCE: 恢复实体类型 " + res.getIri());
				}
			}
		}

		if (diff.getAdded() != null) {
			for (VersionDiffVO.DiffResource res : diff.getAdded()) {
				if ("DATA_PROPERTY".equals(res.getResourceType())) {
					rules.add("DROP_ASSERTION: 移除数据属性 " + res.getIri() + " 的实例断言");
				}
				if ("ENTITY_TYPE".equals(res.getResourceType())) {
					rules.add("RETYPE_INSTANCE: 将实体类型 " + res.getIri() + " 的实例迁移到父类");
				}
			}
		}

		return rules;
	}

	private List<String> generateRiskWarnings(OntologyCompatibilityClassifier.CompatibilityResult compat,
			VersionDiffVO diff) {
		List<String> warnings = new ArrayList<>();

		if ("BREAKING".equals(compat.compatibility())) {
			warnings.add("恢复操作包含 BREAKING 变更，必须执行实例迁移和全量校验后才能发布新版本");
		}

		int removedCount = diff.getRemoved() != null ? diff.getRemoved().size() : 0;
		int addedCount = diff.getAdded() != null ? diff.getAdded().size() : 0;
		if (removedCount > 0) {
			warnings.add("恢复将移除 " + removedCount + " 项当前 Schema 元素，相关实例需要迁移");
		}
		if (addedCount > 0) {
			warnings.add("恢复将恢复 " + addedCount + " 项历史 Schema 元素");
		}

		warnings.add("恢复不会修改或覆盖历史 PUBLISHED 快照，恢复后发布的是新的更高版本");
		warnings.add("禁止直接将 current_version_id 指回旧版本");

		return warnings;
	}

}
