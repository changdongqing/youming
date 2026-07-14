/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.guard;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 跨模块工作区状态校验守卫。
 * <p>
 * 模块 04~11 的 Service 写方法调用 {@link #assertEditable(Long)} 在事务内校验工作区状态。
 * MIGRATING 状态下所有 Schema 元素和实例写入被拒绝。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceStatusGuard {

	private final OntOntologyProjectMapper projectMapper;

	/**
	 * 校验工作区是否可编辑。
	 * @param ontologyId 本体工程ID
	 * @return null 可编辑，否则返回拒绝原因
	 */
	public String checkEditable(Long ontologyId) {
		if (ontologyId == null) {
			return null;
		}
		OntOntologyProject project = projectMapper.selectOne(
			Wrappers.<OntOntologyProject>lambdaQuery().eq(OntOntologyProject::getId, ontologyId));
		if (project == null) {
			return null;
		}
		String status = project.getWorkspaceStatus();
		if (status == null || "EDITABLE".equals(status)) {
			return null;
		}
		return "工作区状态为 " + status + "，写入已锁定";
	}

	/**
	 * 断言工作区可编辑，不可编辑时抛出异常。
	 * @param ontologyId 本体工程ID
	 * @throws IllegalStateException 如果工作区不可编辑
	 */
	public void assertEditable(Long ontologyId) {
		String reason = checkEditable(ontologyId);
		if (reason != null) {
			throw new IllegalStateException(reason);
		}
	}

	/**
	 * 校验工作区可编辑，返回 R 包装结果。
	 * @param ontologyId 本体工程ID
	 * @return null 可编辑，否则返回 R.failed
	 */
	public R<Void> validateEditable(Long ontologyId) {
		String reason = checkEditable(ontologyId);
		if (reason != null) {
			return R.failed(reason);
		}
		return null;
	}

}
