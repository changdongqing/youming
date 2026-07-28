package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.SubclassOfSaveDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelSubclassOfMapper;
import com.pig4cloud.pig.ontology.modeling.service.HierarchySyncService;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelSubclassOfTreeVO;
import com.pig4cloud.pig.ontology.service.ClassTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 类层级 Service 实现（FR-14）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelSubclassOfServiceImpl extends ServiceImpl<ModelSubclassOfMapper, ModelSubclassOf>
		implements ModelSubclassOfService {

	private final ModelClassMapper modelClassMapper;
	private final HierarchySyncService hierarchySyncService;
	private final ClassTemplateService classTemplateService;

	@Override
	public List<ModelSubclassOfTreeVO> tree(Long projectId) {
		// 查项目下所有类
		List<ModelClass> classes = modelClassMapper.selectList(
				Wrappers.<ModelClass>lambdaQuery().eq(ModelClass::getProjectId, projectId));
		// 查项目下所有 subClassOf 边
		List<ModelSubclassOf> edges = list(Wrappers.<ModelSubclassOf>lambdaQuery()
				.eq(ModelSubclassOf::getProjectId, projectId));
		// 组装树：按 parentClassId 分组
		Map<Long, List<ModelSubclassOf>> edgeMap = new HashMap<>();
		for (ModelSubclassOf edge : edges) {
			edgeMap.computeIfAbsent(edge.getParentClassId(), k -> new ArrayList<>()).add(edge);
		}
		// 找根节点（未被任何边作为 child 的类）
		Set<Long> childIds = new HashSet<>();
		for (ModelSubclassOf edge : edges) {
			childIds.add(edge.getChildClassId());
		}
		List<ModelSubclassOfTreeVO> roots = new ArrayList<>();
		Map<Long, ModelClass> classMap = new HashMap<>();
		for (ModelClass cls : classes) {
			classMap.put(cls.getId(), cls);
			if (!childIds.contains(cls.getId())) {
				roots.add(buildTreeVO(cls, edgeMap, classMap));
			}
		}
		return roots;
	}

	private ModelSubclassOfTreeVO buildTreeVO(ModelClass cls, Map<Long, List<ModelSubclassOf>> edgeMap,
			Map<Long, ModelClass> classMap) {
		ModelSubclassOfTreeVO vo = new ModelSubclassOfTreeVO();
		vo.setClassId(cls.getId());
		vo.setClassIri(cls.getClassIri());
		vo.setLocalName(cls.getLocalName());
		vo.setLabel(cls.getLabel());
		vo.setClassificationCode(cls.getClassificationCode());
		List<ModelSubclassOf> children = edgeMap.get(cls.getId());
		if (CollUtil.isNotEmpty(children)) {
			List<ModelSubclassOfTreeVO> childNodes = new ArrayList<>();
			for (ModelSubclassOf edge : children) {
				ModelClass childCls = classMap.get(edge.getChildClassId());
				if (childCls != null) {
					childNodes.add(buildTreeVO(childCls, edgeMap, classMap));
				}
			}
			vo.setChildren(childNodes);
		}
		return vo;
	}

	@Override
	public List<String> suggestParentIris(Long classId) {
		// AC-14.2：消费治理域 suggest（方案B：同模块 ClassTemplateService.suggestParentClassIris）
		ModelClass cls = modelClassMapper.selectById(classId);
		// 手建类（空白类）无 templateCode，父类建议不可用（Supply suggest 要求 templateCode 必填）
		if (cls == null || StrUtil.isBlank(cls.getTemplateCode())) {
			return Collections.emptyList();
		}
		try {
			return classTemplateService.suggestParentClassIris(cls.getTemplateCode());
		}
		catch (Exception e) {
			return Collections.emptyList();
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveEdge(SubclassOfSaveDTO dto) {
		// 1. 自引用校验
		if (dto.getChildClassId().equals(dto.getParentClassId())) {
			return R.failed("子类与父类不能相同");
		}
		// 2. 环路检测（DFS，AC-14.1，支持多继承）
		if (wouldCreateCycle(dto.getProjectId(), dto.getChildClassId(), dto.getParentClassId())) {
			return R.failed("建立此关系会导致类层级成环");
		}
		// 3. 重复校验（uk: project_id + child_class_id + parent_class_id）
		long count = count(Wrappers.<ModelSubclassOf>lambdaQuery()
			.eq(ModelSubclassOf::getProjectId, dto.getProjectId())
			.eq(ModelSubclassOf::getChildClassId, dto.getChildClassId())
			.eq(ModelSubclassOf::getParentClassId, dto.getParentClassId()));
		if (count > 0) {
			return R.failed("该类层级关系已存在");
		}
		// 4. 保存边（sync_status='0' 待同步，retry_count=0）
		ModelSubclassOf edge = new ModelSubclassOf();
		edge.setProjectId(dto.getProjectId());
		edge.setChildClassId(dto.getChildClassId());
		edge.setParentClassId(dto.getParentClassId());
		edge.setSourceTemplateRef(dto.getSourceTemplateRef());
		edge.setSyncStatus("0");
		edge.setRetryCount(0);
		save(edge);
		// 5. 镜像回推（AC-14.3）
		R syncResult = hierarchySyncService.pushMirror(edge);
		if (syncResult.getCode() != 0) {
			// 回推失败：sync_status 保持 '0'，补偿任务重试（AC-14.5）
			// 不回滚建模域事务（建模域是权威源，镜像可异步补偿）
			return R.ok("类层级已建立，镜像同步稍后重试");
		}
		// 回推成功：更新 sync_status='1'
		edge.setSyncStatus("1");
		updateById(edge);
		return R.ok(edge);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeEdge(Long childClassId, Long parentClassId) {
		ModelSubclassOf edge = getOne(Wrappers.<ModelSubclassOf>lambdaQuery()
			.eq(ModelSubclassOf::getChildClassId, childClassId)
			.eq(ModelSubclassOf::getParentClassId, parentClassId));
		if (edge == null) {
			return R.failed("类层级关系不存在");
		}
		// 1. 软删建模域边
		removeById(edge.getId());
		// 2. 镜像失效（AC-14.4）
		hierarchySyncService.invalidateMirror(edge);
		return R.ok();
	}

	/**
	 * 环路检测：DFS 遍历 parentClassId 的所有祖先链，若遇到 childClassId 则成环（AC-14.1，支持多继承 AC-14.6）
	 * <p>
	 * 与治理域 CycleDetectorService 不同：后者针对单继承 parentId 链（while 单链向上）；
	 * 本方法针对多继承 subClassOf 图（一个子类可有多个父），用 DFS 递归遍历所有父链。
	 * 建立边 (child -> parent) 后，从 parent 向上查，若任意祖先链回到 child 即成环。
	 */
	private boolean wouldCreateCycle(Long projectId, Long childClassId, Long parentClassId) {
		Set<Long> visited = new HashSet<>();
		return dfsAncestors(projectId, parentClassId, childClassId, visited);
	}

	/**
	 * DFS 递归：从 cur 向上遍历所有父类，检查是否到达 target
	 */
	private boolean dfsAncestors(Long projectId, Long cur, Long target, Set<Long> visited) {
		if (cur == null || !visited.add(cur)) {
			return false;
		}
		if (cur.equals(target)) {
			return true;
		}
		// 查 cur 的所有父类（多继承：一个子类可有多个父）
		List<ModelSubclassOf> parents = list(Wrappers.<ModelSubclassOf>lambdaQuery()
			.eq(ModelSubclassOf::getProjectId, projectId)
			.eq(ModelSubclassOf::getChildClassId, cur));
		for (ModelSubclassOf edge : parents) {
			if (dfsAncestors(projectId, edge.getParentClassId(), target, visited)) {
				return true;
			}
		}
		return false;
	}

}
