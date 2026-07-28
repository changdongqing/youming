package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.SubclassOfSaveDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.vo.ModelSubclassOfTreeVO;

import java.util.List;

/**
 * 类层级 Service 接口（FR-14）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelSubclassOfService extends IService<ModelSubclassOf> {

	/**
	 * 类树视图（AC-14.1）
	 */
	List<ModelSubclassOfTreeVO> tree(Long projectId);

	/**
	 * 父类建议（AC-14.2，消费治理域 suggest）
	 */
	List<String> suggestParentIris(Long classId);

	/**
	 * 建立 subClassOf（AC-14.1/14.3，环路检测 + 镜像回推）
	 */
	R saveEdge(SubclassOfSaveDTO dto);

	/**
	 * 删除 subClassOf（AC-14.4，镜像失效）
	 */
	R removeEdge(Long childClassId, Long parentClassId);

}
