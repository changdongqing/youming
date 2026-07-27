package com.pig4cloud.pig.ontology.service;

import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 分类模板父子继承环路检测（FR-2，AC-2.6，NFR-13）
 * <p>
 * 设置某节点 parent 时校验是否成环：从候选父节点自底向上查父链，若途中遇到该节点自身，
 * 则会形成环。时间复杂度 O(树深)，建议树深 ≤5（R-13）。
 * <p>
 * 边界：类层级（subClassOf）的环路校验在建模侧执行（FR-9.5），本类仅用于分类模板树。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class CycleDetectorService {

	private final ClassTemplateMapper classTemplateMapper;

	/**
	 * 判断把 node 的 parent 设为 candidateParentId 是否会成环。
	 * <p>
	 * 算法：从 candidateParentId 向上查父链，若遇到 node 则成环。
	 * @param nodeId 待设置父节点的分类模板 id
	 * @param candidateParentId 候选父节点 id
	 * @return true 表示会成环（应拒绝），false 表示安全
	 */
	public boolean wouldCreateCycle(Long nodeId, Long candidateParentId) {
		if (nodeId == null || candidateParentId == null) {
			return false;
		}
		if (nodeId.equals(candidateParentId)) {
			// 自引用即成环
			return true;
		}
		Set<Long> visited = new HashSet<>();
		Long cur = candidateParentId;
		while (cur != null && visited.add(cur)) {
			if (cur.equals(nodeId)) {
				// 父链回到 node -> 成环
				return true;
			}
			ClassTemplate parent = classTemplateMapper.selectById(cur);
			cur = (parent == null) ? null : parent.getParentId();
		}
		return false;
	}

}
