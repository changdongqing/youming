package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassTemplateSaveDTO;
import com.pig4cloud.pig.ontology.api.entity.ClassHierarchy;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplateRef;
import com.pig4cloud.pig.ontology.api.entity.PropertyTemplate;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateDetailVO;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateNodeVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.mapper.ClassHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateRefMapper;
import com.pig4cloud.pig.ontology.mapper.PropertyTemplateMapper;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import com.pig4cloud.pig.ontology.service.ClassificationCodeGenerator;
import com.pig4cloud.pig.ontology.service.CycleDetectorService;
import com.pig4cloud.pig.ontology.service.InheritedViewService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类模板 Service 实现（FR-2/FR-8）
 * <p>
 * 组合三个核心算法 Service：{@link InheritedViewService}（继承视图 + 缓存）、
 * {@link CycleDetectorService}（环路检测）、{@link ClassificationCodeGenerator}（编码生成/校验）。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ClassTemplateServiceImpl extends ServiceImpl<ClassTemplateMapper, ClassTemplate>
		implements ClassTemplateService {

	private final ClassTemplateRefMapper refMapper;

	private final PropertyTemplateMapper propertyTemplateMapper;

	private final ClassHierarchyMapper classHierarchyMapper;

	private final InheritedViewService inheritedViewService;

	private final CycleDetectorService cycleDetectorService;

	private final ClassificationCodeGenerator codeGenerator;

	@Override
	public List<ClassTemplateNodeVO> tree(String treeRoot, Boolean includeDeprecated) {
		List<ClassTemplate> list = list(Wrappers.<ClassTemplate>lambdaQuery()
			.eq(StrUtil.isNotBlank(treeRoot), ClassTemplate::getTreeRoot, treeRoot)
			.eq(!Boolean.TRUE.equals(includeDeprecated), ClassTemplate::getDeprecated, "0")
			.orderByAsc(ClassTemplate::getSortOrder)
			.orderByAsc(ClassTemplate::getClassificationCode));
		// 外观合并：inherit_appearance=1 且自身空 -> 取父链最近非空
		for (ClassTemplate tpl : list) {
			mergeAppearanceFromParent(tpl, list);
		}
		return buildTree(list);
	}

	@Override
	public ClassTemplateDetailVO getDetail(Long id) {
		ClassTemplate tpl = getById(id);
		if (tpl == null) {
			return null;
		}
		ClassTemplateDetailVO vo = ClassTemplateDetailVO.from(tpl);
		// 外观合并
		mergeAppearanceFromParent(tpl);
		vo.setIcon(tpl.getIcon());
		vo.setColor(tpl.getColor());
		// 父信息（溯源）
		if (tpl.getParentId() != null) {
			ClassTemplate parent = getById(tpl.getParentId());
			if (parent != null) {
				vo.setParentTemplateCode(parent.getTemplateCode());
				vo.setParentClassificationCode(parent.getClassificationCode());
			}
		}
		// 本节点 refs（不含继承的，继承视图用独立接口）
		List<ClassTemplateRef> refs = refMapper.selectList(Wrappers.<ClassTemplateRef>lambdaQuery()
			.eq(ClassTemplateRef::getClassTemplateId, id)
			.orderByAsc(ClassTemplateRef::getSortOrder));
		vo.setRefs(refs);
		return vo;
	}

	@Override
	public InheritedViewVO inheritedView(Long id) {
		return inheritedViewService.compute(id);
	}

	@Override
	public IPage<ClassTemplate> page(Page page, ClassTemplate template) {
		return baseMapper.selectPage(page,
				Wrappers.<ClassTemplate>lambdaQuery()
					.eq(StrUtil.isNotBlank(template.getTreeRoot()), ClassTemplate::getTreeRoot,
							template.getTreeRoot())
					// AC-8.4：classificationCode 前缀查子树（如 30-01 -> 30-01-01/02/03）
					.likeRight(StrUtil.isNotBlank(template.getClassificationCode()),
							ClassTemplate::getClassificationCode, template.getClassificationCode())
					.and(StrUtil.isNotBlank(template.getTemplateCode()),
						w -> w.like(ClassTemplate::getTemplateCode, template.getTemplateCode())
							.or()
							.like(ClassTemplate::getLabel, template.getTemplateCode()))
					.eq(StrUtil.isNotBlank(template.getDeprecated()), ClassTemplate::getDeprecated,
							template.getDeprecated())
					.orderByAsc(ClassTemplate::getClassificationCode));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveTemplate(ClassTemplateSaveDTO dto) {
		// 1. templateCode 查重（AC-2.8）
		long codeCnt = count(Wrappers.<ClassTemplate>lambdaQuery()
			.eq(ClassTemplate::getTemplateCode, dto.getTemplateCode()));
		if (codeCnt > 0) {
			return R.failed("模板标识 '" + dto.getTemplateCode() + "' 已存在");
		}

		// 2. 父节点校验 + treeRoot 一致性（父子须同 tree_root）
		ClassTemplate parent = null;
		if (dto.getParentId() != null) {
			parent = getById(dto.getParentId());
			if (parent == null) {
				return R.failed("父节点不存在");
			}
			if (!parent.getTreeRoot().equals(dto.getTreeRoot())) {
				return R.failed("子节点与父节点须属于同一分类树");
			}
		}

		// 3. classification_code 生成或手填校验（FR-8）
		String clsCode;
		if (StrUtil.isNotBlank(dto.getClassificationCode())) {
			// 手填：校验前缀一致 + 符合规则（AC-8.5）
			R codeValid = codeGenerator.validateManual(dto.getClassificationCode(), dto.getParentId(),
					dto.getTreeRoot());
			if (!codeValid.isOk()) {
				return codeValid;
			}
			clsCode = dto.getClassificationCode();
		}
		else {
			// 自动生成（AC-8.2）
			clsCode = codeGenerator.nextCode(dto.getParentId());
		}
		long clsCnt = count(Wrappers.<ClassTemplate>lambdaQuery()
			.eq(ClassTemplate::getClassificationCode, clsCode));
		if (clsCnt > 0) {
			return R.failed("分类编码 '" + clsCode + "' 已存在");
		}

		// 4. propertyRefs 校验：指向的 templateCode 必须存在（AC-2.4）
		R refCheck = validateRefs(dto.getPropertyRefs());
		if (!refCheck.isOk()) {
			return refCheck;
		}

		// 5. 落库（source=custom）
		ClassTemplate tpl = new ClassTemplate();
		BeanUtil.copyProperties(dto, tpl);
		tpl.setId(null);
		tpl.setClassificationCode(clsCode);
		tpl.setSource("custom");
		tpl.setDeprecated("0");
		if (StrUtil.isBlank(tpl.getInheritAppearance())) {
			tpl.setInheritAppearance("1");
		}
		save(tpl);
		saveRefs(tpl.getId(), dto.getPropertyRefs());
		inheritedViewService.invalidate(tpl.getId());
		return R.ok(tpl);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateTemplate(ClassTemplateSaveDTO dto) {
		ClassTemplate existing = getById(dto.getId());
		if (existing == null) {
			return R.failed("模板不存在");
		}
		// builtin 不可编辑（对齐 DD2 builtin 保护）
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可编辑");
		}
		// templateCode 不可改（引用稳定性）
		dto.setTemplateCode(existing.getTemplateCode());

		// 改 parent 须校验不成环（AC-2.6）+ treeRoot 一致
		if (dto.getParentId() != null && !dto.getParentId().equals(existing.getParentId())) {
			if (cycleDetectorService.wouldCreateCycle(dto.getId(), dto.getParentId())) {
				return R.failed("设置该父节点会导致分类树成环");
			}
			ClassTemplate newParent = getById(dto.getParentId());
			if (newParent == null) {
				return R.failed("父节点不存在");
			}
			if (!newParent.getTreeRoot().equals(existing.getTreeRoot())) {
				return R.failed("子节点与父节点须属于同一分类树");
			}
		}

		// classification_code 手填时校验前缀一致 + 符合规则（AC-8.5）
		if (StrUtil.isNotBlank(dto.getClassificationCode())
				&& !dto.getClassificationCode().equals(existing.getClassificationCode())) {
			R codeValid = codeGenerator.validateManual(dto.getClassificationCode(), dto.getParentId(),
					dto.getTreeRoot());
			if (!codeValid.isOk()) {
				return codeValid;
			}
			// 查重（排除自身）
			long clsCnt = count(Wrappers.<ClassTemplate>lambdaQuery()
				.eq(ClassTemplate::getClassificationCode, dto.getClassificationCode())
				.ne(ClassTemplate::getId, dto.getId()));
			if (clsCnt > 0) {
				return R.failed("分类编码 '" + dto.getClassificationCode() + "' 已存在");
			}
		}

		// refs 校验（AC-2.4）
		R refCheck = validateRefs(dto.getPropertyRefs());
		if (!refCheck.isOk()) {
			return refCheck;
		}

		// 落库
		ClassTemplate tpl = new ClassTemplate();
		BeanUtil.copyProperties(dto, tpl);
		tpl.setId(existing.getId());
		// treeRoot 不可改（与编码规则/父子关系绑定）
		tpl.setTreeRoot(existing.getTreeRoot());
		tpl.setSource(existing.getSource());
		updateById(tpl);
		// refs 全量替换（先删后插）
		refMapper.delete(Wrappers.<ClassTemplateRef>lambdaQuery()
			.eq(ClassTemplateRef::getClassTemplateId, dto.getId()));
		saveRefs(dto.getId(), dto.getPropertyRefs());
		inheritedViewService.invalidate(dto.getId());
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeTemplate(Long id) {
		ClassTemplate existing = getById(id);
		if (existing == null) {
			return R.failed("模板不存在");
		}
		// builtin 不可删除
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置模板不可删除");
		}
		// AC-2.7：有子节点拒绝
		long childCnt = count(Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getParentId, id));
		if (childCnt > 0) {
			return R.failed("存在子节点，不可直接删除（请先处理子节点）");
		}
		refMapper.delete(Wrappers.<ClassTemplateRef>lambdaQuery().eq(ClassTemplateRef::getClassTemplateId, id));
		removeById(id);
		inheritedViewService.invalidate(id);
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<ClassTemplate>lambdaUpdate()
			.eq(ClassTemplate::getId, id)
			.set(ClassTemplate::getDeprecated, deprecated)));
	}

	@Override
	public String previewClassificationCode(Long parentId) {
		return codeGenerator.nextCode(parentId);
	}

	@Override
	public ClassTemplate getByCode(String templateCode) {
		if (StrUtil.isBlank(templateCode)) {
			return null;
		}
		return getOne(Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getTemplateCode, templateCode));
	}

	@Override
	public List<String> suggestParentClassIris(String templateCode) {
		// FR-9 AC-9.1：据模板父链推荐 subClassOf 父类 IRI
		// 逻辑：找到本模板 -> 取其 parent 模板 template_code -> 在 ont_class_hierarchy 中查
		// source_template_ref = 父模板 template_code 的 parent_class_iri（即父模板派生的类）
		ClassTemplate tpl = getByCode(templateCode);
		if (tpl == null || tpl.getParentId() == null) {
			return Collections.emptyList();
		}
		ClassTemplate parentTpl = getById(tpl.getParentId());
		if (parentTpl == null) {
			return Collections.emptyList();
		}
		List<ClassHierarchy> mirrored = classHierarchyMapper.selectList(Wrappers.<ClassHierarchy>lambdaQuery()
			.eq(ClassHierarchy::getSourceTemplateRef, parentTpl.getTemplateCode())
			.eq(ClassHierarchy::getSyncStatus, "1"));
		// 去重：parent_class_iri 即建议的父类（从父模板派生的类）
		Set<String> iris = new HashSet<>();
		for (ClassHierarchy edge : mirrored) {
			iris.add(edge.getParentClassIri());
		}
		return new ArrayList<>(iris);
	}

	// —— 私有辅助 ——

	/**
	 * propertyRefs 存在性校验（AC-2.4）：指向的 templateCode 必须在 ont_property_template 中存在。
	 */
	private R validateRefs(List<ClassTemplateSaveDTO.RefItem> refs) {
		if (refs == null || refs.isEmpty()) {
			return R.ok(true);
		}
		Set<String> codes = new HashSet<>();
		for (ClassTemplateSaveDTO.RefItem r : refs) {
			if (StrUtil.isBlank(r.getPropertyTemplateCode())) {
				return R.failed("结构骨架引用的属性模板标识不能为空");
			}
			codes.add(r.getPropertyTemplateCode());
		}
		Long existCnt = propertyTemplateMapper.selectCount(Wrappers.<PropertyTemplate>lambdaQuery()
			.in(PropertyTemplate::getTemplateCode, codes));
		if (existCnt == null || existCnt < codes.size()) {
			return R.failed("结构骨架存在不存在的属性模板标识（property_template_code），请检查");
		}
		return R.ok(true);
	}

	/**
	 * 保存 refs（去重：同 classTemplateId + propertyTemplateCode + refType 保留首条）。
	 * V4 ont_class_template_ref 无唯一约束，Service 层兜底去重避免重复注入。
	 */
	private void saveRefs(Long classTemplateId, List<ClassTemplateSaveDTO.RefItem> refs) {
		if (refs == null || refs.isEmpty()) {
			return;
		}
		Set<String> dedupKeys = new HashSet<>();
		int order = 1;
		for (ClassTemplateSaveDTO.RefItem r : refs) {
			String key = r.getPropertyTemplateCode() + "|" + r.getRefType();
			if (!dedupKeys.add(key)) {
				continue;
			}
			ClassTemplateRef ref = new ClassTemplateRef();
			ref.setClassTemplateId(classTemplateId);
			ref.setPropertyTemplateCode(r.getPropertyTemplateCode());
			ref.setRefType(r.getRefType());
			ref.setSortOrder(r.getSortOrder() == null ? order : r.getSortOrder());
			ref.setInheritFlag(StrUtil.isBlank(r.getInheritFlag()) ? "0" : r.getInheritFlag());
			refMapper.insert(ref);
			order++;
		}
	}

	/**
	 * 外观合并（AC-2.2）：inherit_appearance='1' 且自身 icon/color 空 -> 沿父链取最近非空。
	 * 单节点版本（按需查父）。
	 */
	private void mergeAppearanceFromParent(ClassTemplate tpl) {
		if (!"1".equals(tpl.getInheritAppearance())) {
			return;
		}
		if (StrUtil.isNotBlank(tpl.getIcon()) && StrUtil.isNotBlank(tpl.getColor())) {
			return;
		}
		ClassTemplate cur = tpl.getParentId() == null ? null : getById(tpl.getParentId());
		while (cur != null) {
			if (StrUtil.isBlank(tpl.getIcon()) && StrUtil.isNotBlank(cur.getIcon())) {
				tpl.setIcon(cur.getIcon());
			}
			if (StrUtil.isBlank(tpl.getColor()) && StrUtil.isNotBlank(cur.getColor())) {
				tpl.setColor(cur.getColor());
			}
			if (StrUtil.isNotBlank(tpl.getIcon()) && StrUtil.isNotBlank(tpl.getColor())) {
				break;
			}
			cur = cur.getParentId() == null ? null : getById(cur.getParentId());
		}
	}

	/**
	 * 外观合并（AC-2.2）：批量版本，用 list 内的 id->entity 映射避免逐个查询。
	 */
	private void mergeAppearanceFromParent(ClassTemplate tpl, List<ClassTemplate> all) {
		if (!"1".equals(tpl.getInheritAppearance())) {
			return;
		}
		if (StrUtil.isNotBlank(tpl.getIcon()) && StrUtil.isNotBlank(tpl.getColor())) {
			return;
		}
		Map<Long, ClassTemplate> idMap = new HashMap<>();
		for (ClassTemplate t : all) {
			idMap.put(t.getId(), t);
		}
		ClassTemplate cur = tpl.getParentId() == null ? null : idMap.get(tpl.getParentId());
		while (cur != null) {
			if (StrUtil.isBlank(tpl.getIcon()) && StrUtil.isNotBlank(cur.getIcon())) {
				tpl.setIcon(cur.getIcon());
			}
			if (StrUtil.isBlank(tpl.getColor()) && StrUtil.isNotBlank(cur.getColor())) {
				tpl.setColor(cur.getColor());
			}
			if (StrUtil.isNotBlank(tpl.getIcon()) && StrUtil.isNotBlank(tpl.getColor())) {
				break;
			}
			cur = cur.getParentId() == null ? null : idMap.get(cur.getParentId());
		}
	}

	/**
	 * 组装分类树：parentId 自引用 -> 森林（rootId = null）。
	 */
	private List<ClassTemplateNodeVO> buildTree(List<ClassTemplate> list) {
		Map<Long, ClassTemplateNodeVO> nodeMap = new HashMap<>();
		for (ClassTemplate tpl : list) {
			nodeMap.put(tpl.getId(), ClassTemplateNodeVO.from(tpl));
		}
		List<ClassTemplateNodeVO> roots = new ArrayList<>();
		for (ClassTemplate tpl : list) {
			ClassTemplateNodeVO node = nodeMap.get(tpl.getId());
			if (tpl.getParentId() == null || !nodeMap.containsKey(tpl.getParentId())) {
				roots.add(node);
			}
			else {
				ClassTemplateNodeVO parent = nodeMap.get(tpl.getParentId());
				List<ClassTemplateNodeVO> children = parent.getChildren();
				if (children == null) {
					children = new ArrayList<>();
					parent.setChildren(children);
				}
				children.add(node);
			}
		}
		return roots;
	}

}
