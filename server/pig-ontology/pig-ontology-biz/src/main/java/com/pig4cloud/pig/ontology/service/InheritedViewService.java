package com.pig4cloud.pig.ontology.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplateRef;
import com.pig4cloud.pig.ontology.api.vo.InheritedPropertyVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateMapper;
import com.pig4cloud.pig.ontology.mapper.ClassTemplateRefMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类模板继承视图计算 + Caffeine 缓存（FR-2，AC-2.2/2.3/2.5，NFR-12/14）
 * <p>
 * 给定分类模板 id，沿 parent_id 自底向上收集父链，合并全部 propertyRefs：
 * <ul>
 * <li>去重键 = propertyTemplateCode；</li>
 * <li>子模板同名覆盖父（AC-2.5）；</li>
 * <li>每条属性带 source 三态：node(本节点新增) / inherited(继承自父) / overridden(覆盖父同名)
 * （NFR-14）；</li>
 * <li>外观按 inherit_appearance 合并父链最近非空 icon/color（AC-2.2）。</li>
 * </ul>
 * 结果按 Caffeine 缓存（key=classTemplateId，TTL 5min，模板变更时失效，NFR-12）。
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class InheritedViewService {

	/**
	 * 继承视图缓存上限。
	 */
	private static final int CACHE_MAX_SIZE = 1000;

	/**
	 * 继承视图缓存 TTL（分钟）。
	 */
	private static final long CACHE_TTL_MINUTES = 5;

	private final ClassTemplateMapper classTemplateMapper;

	private final ClassTemplateRefMapper refMapper;

	/**
	 * Caffeine 本地缓存：key=classTemplateId，TTL 5min（NFR-12 P95&lt;300ms）。
	 */
	private final Cache<Long, InheritedViewVO> cache = Caffeine.newBuilder()
		.expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
		.maximumSize(CACHE_MAX_SIZE)
		.build();

	/**
	 * 计算继承视图（命中缓存直接返回）。
	 * @param id 分类模板 id
	 * @return 继承视图
	 */
	public InheritedViewVO compute(Long id) {
		return cache.get(id, this::doCompute);
	}

	/**
	 * 失效自身及所有后代的继承视图缓存（模板变更后调用，保证一致性）。
	 * <p>
	 * 后代的继承视图依赖本节点，故失效时需级联清除后代（AC-2.3 一致性）。
	 * @param id 发生变更的分类模板 id
	 */
	public void invalidate(Long id) {
		if (id == null) {
			return;
		}
		cache.invalidate(id);
		invalidateDescendants(id);
	}

	/**
	 * 自底向上收集父链 + 合并 refs + 合并外观，组装继承视图（实际计算）。
	 */
	private InheritedViewVO doCompute(Long id) {
		// 1. 自底向上收集父链（根在前，本节点在末尾）
		LinkedList<ClassTemplate> chain = collectParentChain(id);
		if (chain.isEmpty()) {
			return null;
		}
		ClassTemplate self = chain.getLast();

		// 2. 按父链顺序收集各节点 refs（根在前，子覆盖父）
		List<InheritedPropertyVO> properties = mergeRefs(chain, id);

		// 3. 外观合并：本节点 inherit_appearance='1' 且自身 icon/color 空 -> 取父链最近非空
		mergeAppearanceFromParent(self, chain);

		// 4. 组装 VO（含父链 template_code 列表，供溯源）
		InheritedViewVO vo = new InheritedViewVO();
		vo.setId(self.getId());
		vo.setTemplateCode(self.getTemplateCode());
		vo.setClassificationCode(self.getClassificationCode());
		vo.setLabel(self.getLabel());
		vo.setIcon(self.getIcon());
		vo.setColor(self.getColor());
		vo.setInheritAppearance(self.getInheritAppearance());
		vo.setProperties(properties);
		vo.setParentChain(chain.stream().map(ClassTemplate::getTemplateCode).collect(Collectors.toList()));
		return vo;
	}

	/**
	 * 自底向上查父链（本节点 -> parent -> ... -> 根），结果按根在前、本节点在末尾排序。
	 * 带 visited 集合防御性去环（数据异常时兜底，防止栈溢出）。
	 */
	private LinkedList<ClassTemplate> collectParentChain(Long id) {
		LinkedList<ClassTemplate> chain = new LinkedList<>();
		Set<Long> visited = new HashSet<>();
		ClassTemplate cur = classTemplateMapper.selectById(id);
		while (cur != null && visited.add(cur.getId())) {
			chain.addFirst(cur);
			cur = cur.getParentId() == null ? null : classTemplateMapper.selectById(cur.getParentId());
		}
		return chain;
	}

	/**
	 * 合并父链各节点 refs：根在前、本节点在末尾，LinkedHashMap.put 同名覆盖自然实现
	 * 「子覆盖父」（AC-2.5）。source 三态：merged 已含该 code 即 overridden；否则非本节点即
	 * inherited；本节点即 node（NFR-14）。
	 */
	private List<InheritedPropertyVO> mergeRefs(List<ClassTemplate> chain, Long selfId) {
		Map<String, InheritedPropertyVO> merged = new LinkedHashMap<>();
		for (ClassTemplate node : chain) {
			List<ClassTemplateRef> refs = refMapper.selectList(Wrappers.<ClassTemplateRef>lambdaQuery()
				.eq(ClassTemplateRef::getClassTemplateId, node.getId())
				.orderByAsc(ClassTemplateRef::getSortOrder));
			for (ClassTemplateRef ref : refs) {
				String code = ref.getPropertyTemplateCode();
				InheritedPropertyVO vo = new InheritedPropertyVO();
				vo.setPropertyTemplateCode(code);
				vo.setRefType(ref.getRefType());
				vo.setSortOrder(ref.getSortOrder());
				vo.setSourceClassTemplateId(node.getId());
				vo.setSourceClassTemplateCode(node.getTemplateCode());
				if (merged.containsKey(code)) {
					vo.setSource("overridden");
				}
				else if (!node.getId().equals(selfId)) {
					vo.setSource("inherited");
				}
				else {
					vo.setSource("node");
				}
				merged.put(code, vo);
			}
		}
		// 合并后重排序号（保持稳定顺序）
		List<InheritedPropertyVO> list = new ArrayList<>(merged.values());
		for (int i = 0; i < list.size(); i++) {
			list.get(i).setSortOrder(i + 1);
		}
		return list;
	}

	/**
	 * 外观合并：本节点 inherit_appearance='1' 且自身 icon/color 为空时，沿父链（从父向上）
	 * 取最近非空值（AC-2.2）。
	 */
	private void mergeAppearanceFromParent(ClassTemplate self, LinkedList<ClassTemplate> chain) {
		if (!"1".equals(self.getInheritAppearance())) {
			return;
		}
		if (StrUtil.isBlank(self.getIcon()) || StrUtil.isBlank(self.getColor())) {
			// chain 末尾是 self，从倒数第二个（父）向上找最近非空外观
			for (int i = chain.size() - 2; i >= 0; i--) {
				ClassTemplate ancestor = chain.get(i);
				if (StrUtil.isBlank(self.getIcon()) && StrUtil.isNotBlank(ancestor.getIcon())) {
					self.setIcon(ancestor.getIcon());
				}
				if (StrUtil.isBlank(self.getColor()) && StrUtil.isNotBlank(ancestor.getColor())) {
					self.setColor(ancestor.getColor());
				}
				if (StrUtil.isNotBlank(self.getIcon()) && StrUtil.isNotBlank(self.getColor())) {
					break;
				}
			}
		}
	}

	/**
	 * 失效所有后代节点的缓存：BFS 查子树，逐个 invalidate。
	 */
	private void invalidateDescendants(Long id) {
		Set<Long> visited = new HashSet<>();
		List<Long> frontier = new ArrayList<>();
		frontier.add(id);
		while (!frontier.isEmpty()) {
			List<Long> next = new ArrayList<>();
			for (Long parentId : frontier) {
				List<ClassTemplate> children = classTemplateMapper.selectList(
						Wrappers.<ClassTemplate>lambdaQuery().eq(ClassTemplate::getParentId, parentId));
				for (ClassTemplate child : children) {
					if (visited.add(child.getId())) {
						cache.invalidate(child.getId());
						next.add(child.getId());
					}
				}
			}
			frontier = next;
		}
	}

}
