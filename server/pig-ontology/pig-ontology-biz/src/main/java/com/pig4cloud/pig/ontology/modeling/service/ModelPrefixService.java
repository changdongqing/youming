package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;

import java.util.List;

/**
 * IRI 前缀 Service 接口（FR-10.2）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelPrefixService extends IService<ModelPrefix> {

	/**
	 * 项目前缀列表（AC-10.2）
	 */
	List<ModelPrefix> listByProject(Long projectId);

	/**
	 * 新增前缀（AC-10.2，NCName 校验 + 同项目查重）
	 */
	R savePrefix(ModelPrefix prefix);

	/**
	 * 编辑前缀（AC-10.2）
	 */
	R updatePrefix(ModelPrefix prefix);

	/**
	 * 删除前缀（AC-10.2）
	 */
	R removePrefix(Long id);

}
