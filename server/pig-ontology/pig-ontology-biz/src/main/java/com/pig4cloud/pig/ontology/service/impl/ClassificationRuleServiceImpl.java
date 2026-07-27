package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassificationRule;
import com.pig4cloud.pig.ontology.mapper.ClassificationRuleMapper;
import com.pig4cloud.pig.ontology.service.ClassificationRuleService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分类编码规则 Service 实现（FR-8）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ClassificationRuleServiceImpl extends ServiceImpl<ClassificationRuleMapper, ClassificationRule>
		implements ClassificationRuleService {

	@Override
	public ClassificationRule getByTreeRoot(String treeRoot) {
		if (StrUtil.isBlank(treeRoot)) {
			return null;
		}
		return getOne(Wrappers.<ClassificationRule>lambdaQuery().eq(ClassificationRule::getTreeRoot, treeRoot));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveOrUpdateRule(ClassificationRule rule) {
		if (StrUtil.isBlank(rule.getTreeRoot())) {
			return R.failed("分类树标识不能为空");
		}
		ClassificationRule existing = getByTreeRoot(rule.getTreeRoot());
		if (existing == null) {
			// 新建：tree_root 唯一约束 uk_ont_cls_rule_tree_root 保护
			save(rule);
		}
		else {
			rule.setId(existing.getId());
			// tree_root 不可改（保持与 ont_class_template.tree_root 的对应关系）
			rule.setTreeRoot(existing.getTreeRoot());
			updateById(rule);
		}
		return R.ok(rule);
	}

}
