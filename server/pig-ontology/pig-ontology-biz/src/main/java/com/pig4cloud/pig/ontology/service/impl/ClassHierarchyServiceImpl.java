package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassHierarchySyncDTO;
import com.pig4cloud.pig.ontology.api.entity.ClassHierarchy;
import com.pig4cloud.pig.ontology.api.vo.ClassHierarchyNodeVO;
import com.pig4cloud.pig.ontology.mapper.ClassHierarchyMapper;
import com.pig4cloud.pig.ontology.service.ClassHierarchyService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类分类树镜像 Service 实现（FR-9）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ClassHierarchyServiceImpl extends ServiceImpl<ClassHierarchyMapper, ClassHierarchy>
		implements ClassHierarchyService {

	@Override
	public List<ClassHierarchyNodeVO> tree(String treeRoot) {
		// 查全部已同步（sync_status='1'）的镜像边，按 treeRoot 可选过滤
		List<ClassHierarchy> edges = list(Wrappers.<ClassHierarchy>lambdaQuery()
			.eq(ClassHierarchy::getSyncStatus, "1")
			.eq(StrUtil.isNotBlank(treeRoot), ClassHierarchy::getTreeRoot, treeRoot));
		return buildTree(edges);
	}

	@Override
	public IPage<ClassHierarchy> page(Page page, ClassHierarchy filter) {
		return baseMapper.selectPage(page,
				Wrappers.<ClassHierarchy>lambdaQuery()
					.eq(StrUtil.isNotBlank(filter.getTreeRoot()), ClassHierarchy::getTreeRoot, filter.getTreeRoot())
					.eq(StrUtil.isNotBlank(filter.getSyncStatus()), ClassHierarchy::getSyncStatus,
							filter.getSyncStatus())
					.like(StrUtil.isNotBlank(filter.getChildClassIri()), ClassHierarchy::getChildClassIri,
							filter.getChildClassIri())
					.orderByDesc(ClassHierarchy::getSyncTime));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R upsertMirror(List<ClassHierarchySyncDTO> edges) {
		if (edges == null || edges.isEmpty()) {
			return R.ok(true);
		}
		LocalDateTime now = LocalDateTime.now();
		for (ClassHierarchySyncDTO edge : edges) {
			if (StrUtil.isBlank(edge.getChildClassIri()) || StrUtil.isBlank(edge.getParentClassIri())) {
				continue;
			}
			// 按 (child, parent) 去重 upsert
			ClassHierarchy existing = getOne(Wrappers.<ClassHierarchy>lambdaQuery()
				.eq(ClassHierarchy::getChildClassIri, edge.getChildClassIri())
				.eq(ClassHierarchy::getParentClassIri, edge.getParentClassIri()));
			if (existing == null) {
				ClassHierarchy mirror = new ClassHierarchy();
				BeanUtil.copyProperties(edge, mirror);
				mirror.setSyncStatus("1");
				mirror.setSyncTime(now);
				save(mirror);
			}
			else {
				existing.setSourceTemplateRef(edge.getSourceTemplateRef());
				existing.setTreeRoot(edge.getTreeRoot());
				existing.setSyncStatus("1");
				existing.setSyncTime(now);
				updateById(existing);
			}
		}
		return R.ok(true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R invalidateMirror(String childClassIri, String parentClassIri) {
		if (StrUtil.isBlank(childClassIri) || StrUtil.isBlank(parentClassIri)) {
			return R.failed("子类/父类 IRI 不能为空");
		}
		// 软失效：置 sync_status='2'，保留历史镜像便于统计
		update(Wrappers.<ClassHierarchy>lambdaUpdate()
			.eq(ClassHierarchy::getChildClassIri, childClassIri)
			.eq(ClassHierarchy::getParentClassIri, parentClassIri)
			.set(ClassHierarchy::getSyncStatus, "2")
			.set(ClassHierarchy::getSyncTime, LocalDateTime.now()));
		return R.ok(true);
	}

	/**
	 * 组装类分类树：以 childClassIri 为节点 key、parentClassIri 为父引用，构建森林。
	 * <p>
	 * 一个子类可能有多个父类（subClassOf 多继承，R-16），此处按「第一条边」组装成树用于展示；
	 * 完整多继承关系可通过分页接口查看。
	 */
	private List<ClassHierarchyNodeVO> buildTree(List<ClassHierarchy> edges) {
		// 收集所有节点（去重：同一 childIri 可能有多条边，保留首条作 parent）
		Map<String, ClassHierarchyNodeVO> nodeMap = new HashMap<>();
		Map<String, String> firstParent = new HashMap<>();
		for (ClassHierarchy edge : edges) {
			nodeMap.computeIfAbsent(edge.getChildClassIri(), k -> ClassHierarchyNodeVO.from(edge));
			firstParent.putIfAbsent(edge.getChildClassIri(), edge.getParentClassIri());
			// 父类也登记为节点（可能尚无作为 child 的边，即根）
			nodeMap.computeIfAbsent(edge.getParentClassIri(), k -> {
				ClassHierarchyNodeVO p = new ClassHierarchyNodeVO();
				p.setClassIri(edge.getParentClassIri());
				return p;
			});
		}
		// 组装父子关系
		List<ClassHierarchyNodeVO> roots = new ArrayList<>();
		for (ClassHierarchyNodeVO node : nodeMap.values()) {
			String parentIri = firstParent.get(node.getClassIri());
			node.setParentClassIri(parentIri);
			if (parentIri == null || !nodeMap.containsKey(parentIri)) {
				roots.add(node);
			}
			else {
				ClassHierarchyNodeVO parent = nodeMap.get(parentIri);
				List<ClassHierarchyNodeVO> children = parent.getChildren();
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
