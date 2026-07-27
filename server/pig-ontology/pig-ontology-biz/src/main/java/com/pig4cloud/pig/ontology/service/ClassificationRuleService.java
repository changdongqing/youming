package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassificationRule;

/**
 * 分类编码规则 Service（FR-8）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ClassificationRuleService extends IService<ClassificationRule> {

	/**
	 * 按 tree_root 查询编码规则。
	 * @param treeRoot 分类树标识
	 * @return 编码规则，不存在返回 null
	 */
	ClassificationRule getByTreeRoot(String treeRoot);

	/**
	 * 新增或更新编码规则（按 tree_root 维度，不存在则新建）。
	 * <p>
	 * 仅影响新节点（AC-8.1），旧编码冻结。
	 * @param rule 编码规则
	 * @return 操作结果
	 */
	R saveOrUpdateRule(ClassificationRule rule);

}
