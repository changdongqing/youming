package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.ClassificationRule;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import com.pig4cloud.pig.ontology.mapper.ClassificationRuleMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 分类编码生成/校验（FR-8，AC-8.1/8.2/8.5）
 * <p>
 * 据编码规则（{@link ClassificationRule}，按 tree_root 维度）生成/校验 classification_code：
 * <ul>
 * <li>自动生成：根节点用 base_number（如 30）；子节点用「父编码 + separator + 同级序号」，
 * 序号按 zero_pad 零填充到 level_digits 位（如 30-01-01，AC-8.2）。</li>
 * <li>手填校验：必须以父编码为前缀 + 末段符合规则（位数/数字），否则拒绝（AC-8.5）。</li>
 * </ul>
 * 规则变更只影响新节点（AC-8.1/R-14）：生成器只在 save 时调用，旧编码冻结。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ClassificationCodeGenerator {

	private final ClassificationRuleMapper ruleMapper;

	private final ClassTemplateMapper classTemplateMapper;

	/**
	 * 自动生成下一编码（AC-8.2）。
	 * <p>
	 * parentId=null 时用 base_number（根级）；否则「父编码 + sep + 同级已有数+1」。
	 * @param parentId 父分类模板 id，null 表示根节点
	 * @return 生成的编码
	 */
	public String nextCode(Long parentId) {
		ClassTemplate parent = parentId == null ? null : classTemplateMapper.selectById(parentId);
		String treeRoot = parent == null ? null : parent.getTreeRoot();
		ClassificationRule rule = getRule(treeRoot);
		String sep = separatorOf(rule);
		int digits = levelDigitsOf(rule);
		boolean zeroPad = zeroPadOf(rule);

		if (parent == null) {
			// 根节点：base_number（零填充到 digits 位）
			int base = baseNumberOf(rule);
			return pad(base, digits, zeroPad);
		}
		// 子节点：父编码 + sep + 同级已有数 + 1
		long siblingCnt = classTemplateMapper.selectCount(Wrappers.<ClassTemplate>lambdaQuery()
			.eq(ClassTemplate::getParentId, parentId));
		return parent.getClassificationCode() + sep + pad((int) (siblingCnt + 1), digits, zeroPad);
	}

	/**
	 * 手填编码校验（AC-8.5）：以父编码为前缀 + 末段符合规则。
	 * @param code 手填编码
	 * @param parentId 父分类模板 id，null 表示根节点
	 * @param treeRoot 所属分类树
	 * @return 校验通过返回 R.ok(true)，失败返回 R.failed(msg)
	 */
	public R validateManual(String code, Long parentId, String treeRoot) {
		if (StrUtil.isBlank(code)) {
			return R.failed("分类编码不能为空");
		}
		ClassificationRule rule = getRule(treeRoot);
		String sep = separatorOf(rule);
		int digits = levelDigitsOf(rule);

		if (parentId == null) {
			// 根节点手填：须为 digits 位数字
			if (code.length() != digits || !StrUtil.isNumeric(code)) {
				return R.failed("根级编码须为 " + digits + " 位数字");
			}
			return R.ok(true);
		}
		ClassTemplate parent = classTemplateMapper.selectById(parentId);
		if (parent == null) {
			return R.failed("父节点不存在");
		}
		String prefix = parent.getClassificationCode() + sep;
		if (!code.startsWith(prefix)) {
			return R.failed("分类编码须以父编码 '" + parent.getClassificationCode() + "' 为前缀");
		}
		String lastSeg = code.substring(prefix.length());
		if (lastSeg.length() != digits || !StrUtil.isNumeric(lastSeg)) {
			return R.failed("编码末段须为 " + digits + " 位数字");
		}
		return R.ok(true);
	}

	/**
	 * 取编码规则（按 tree_root）。无规则返回 null，调用方用默认值兜底。
	 */
	private ClassificationRule getRule(String treeRoot) {
		if (StrUtil.isBlank(treeRoot)) {
			return null;
		}
		return ruleMapper.selectOne(Wrappers.<ClassificationRule>lambdaQuery()
			.eq(ClassificationRule::getTreeRoot, treeRoot));
	}

	private String separatorOf(ClassificationRule rule) {
		return (rule == null || StrUtil.isBlank(rule.getSeparator())) ? "-" : rule.getSeparator();
	}

	private int levelDigitsOf(ClassificationRule rule) {
		return (rule == null || rule.getLevelDigits() == null) ? 2 : rule.getLevelDigits();
	}

	private int baseNumberOf(ClassificationRule rule) {
		return (rule == null || rule.getBaseNumber() == null) ? 0 : rule.getBaseNumber();
	}

	private boolean zeroPadOf(ClassificationRule rule) {
		// zeroPad 默认 '1'（零填充），仅当显式置 '0' 时关闭
		return rule == null || !"0".equals(rule.getZeroPad());
	}

	private String pad(int n, int digits, boolean zeroPad) {
		String s = String.valueOf(n);
		if (zeroPad && s.length() < digits) {
			// 零填充到 digits 位
			StringBuilder sb = new StringBuilder(digits);
			for (int i = s.length(); i < digits; i++) {
				sb.append('0');
			}
			return sb.append(s).toString();
		}
		return s;
	}

}
