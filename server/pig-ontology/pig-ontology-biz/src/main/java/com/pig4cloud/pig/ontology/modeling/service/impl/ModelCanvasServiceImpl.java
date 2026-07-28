package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.ontology.modeling.entity.ModelCanvasState;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelCanvasStateMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelSubclassOfMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelCanvasService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 画布状态 Service 实现（FR-17.8）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelCanvasServiceImpl extends ServiceImpl<ModelCanvasStateMapper, ModelCanvasState>
		implements ModelCanvasService {

	private final ModelClassMapper modelClassMapper;
	private final ModelSubclassOfMapper subclassOfMapper;
	private final ModelObjectPropertyMapper objectPropertyMapper;

	@Override
	public ModelCanvasState getState(Long projectId) {
		// 当前用户名（与 createBy 审计字段一致，P-1 评审修正）
		String username = getCurrentUsername();
		return getOne(Wrappers.<ModelCanvasState>lambdaQuery()
			.eq(ModelCanvasState::getProjectId, projectId)
			.eq(ModelCanvasState::getUserId, username));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveState(ModelCanvasState state) {
		String username = getCurrentUsername();
		state.setUserId(username);
		ModelCanvasState existing = getOne(Wrappers.<ModelCanvasState>lambdaQuery()
			.eq(ModelCanvasState::getProjectId, state.getProjectId())
			.eq(ModelCanvasState::getUserId, username));
		if (existing != null) {
			existing.setGraphData(state.getGraphData());
			return R.ok(updateById(existing));
		}
		return R.ok(save(state));
	}

	/**
	 * 构建画布图数据（AC-17.1）：类节点 + subClassOf 继承边 + 对象属性关联边
	 */
	@Override
	public Map<String, Object> buildGraphData(Long projectId) {
		// 查所有类
		List<ModelClass> classes = modelClassMapper.selectList(
				Wrappers.<ModelClass>lambdaQuery().eq(ModelClass::getProjectId, projectId));

		List<Map<String, Object>> nodes = new ArrayList<>();
		List<Map<String, Object>> edges = new ArrayList<>();

		// 类节点
		for (ModelClass cls : classes) {
			Map<String, Object> node = new HashMap<>();
			node.put("id", String.valueOf(cls.getId()));
			node.put("shape", "class-node");
			Map<String, Object> data = new HashMap<>();
			data.put("classId", cls.getId());
			data.put("classIri", cls.getClassIri());
			data.put("label", StrUtil.isNotBlank(cls.getLabel()) ? cls.getLabel() : cls.getLocalName());
			data.put("classificationCode", cls.getClassificationCode());
			data.put("icon", cls.getIcon());
			data.put("color", cls.getColor());
			data.put("templateCode", cls.getTemplateCode());
			node.put("data", data);
			nodes.add(node);
		}

		// subClassOf 继承边
		List<ModelSubclassOf> subEdges = subclassOfMapper.selectList(
				Wrappers.<ModelSubclassOf>lambdaQuery().eq(ModelSubclassOf::getProjectId, projectId));
		for (ModelSubclassOf edge : subEdges) {
			Map<String, Object> e = new HashMap<>();
			e.put("source", String.valueOf(edge.getChildClassId()));
			e.put("target", String.valueOf(edge.getParentClassId()));
			e.put("shape", "subclass-edge");
			e.put("label", "subClassOf");
			e.put("data", Map.of("type", "subclass"));
			edges.add(e);
		}

		// 对象属性关联边
		List<ModelObjectProperty> objProps = objectPropertyMapper.selectList(
				Wrappers.<ModelObjectProperty>lambdaQuery().eq(ModelObjectProperty::getProjectId, projectId));
		for (ModelObjectProperty prop : objProps) {
			if (prop.getRangeClassId() == null) {
				continue; // range 未补全的跳过
			}
			Map<String, Object> e = new HashMap<>();
			e.put("source", String.valueOf(prop.getDomainClassId()));
			e.put("target", String.valueOf(prop.getRangeClassId()));
			e.put("shape", "association-edge");
			e.put("label", StrUtil.isNotBlank(prop.getLabel()) ? prop.getLabel() : prop.getLocalName());
			Map<String, Object> data = new HashMap<>();
			data.put("type", "association");
			data.put("propertyId", prop.getId());
			data.put("templateCode", prop.getTemplateCode());
			e.put("data", data);
			edges.add(e);
		}

		Map<String, Object> result = new HashMap<>();
		result.put("nodes", nodes);
		result.put("edges", edges);
		return result;
	}

	private String getCurrentUsername() {
		// 取当前登录用户名（与 createBy 审计字段一致，ontology-biz 首个主动取用户的服务，P-1 评审修正）
		// SecurityUtils.getUser().getUsername() 返回 username 字符串，存入 user_id varchar(64)
		return SecurityUtils.getUser().getUsername();
	}

}
