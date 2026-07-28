package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelCanvasState;

import java.util.Map;

/**
 * 画布状态 Service 接口（FR-17）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelCanvasService extends IService<ModelCanvasState> {

	/**
	 * 获取当前用户的画布状态（AC-17.8）
	 */
	ModelCanvasState getState(Long projectId);

	/**
	 * 保存画布状态（graph.toJSON 持久化，AC-17.8）
	 */
	R saveState(ModelCanvasState state);

	/**
	 * 构建画布图数据（类节点 + subClassOf 边 + 对象属性边，AC-17.1）
	 */
	Map<String, Object> buildGraphData(Long projectId);

}
