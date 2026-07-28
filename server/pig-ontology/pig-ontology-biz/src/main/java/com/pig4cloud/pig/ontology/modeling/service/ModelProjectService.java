package com.pig4cloud.pig.ontology.modeling.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;

/**
 * 本体项目 Service 接口（FR-10）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ModelProjectService extends IService<ModelProject> {

	/**
	 * 分页查询（AC-10.5，名称模糊 + 状态过滤）
	 */
	IPage<ModelProject> page(Page page, ModelProject project);

	/**
	 * 项目详情（AC-10.1）
	 */
	ModelProject getDetail(Long id);

	/**
	 * 新增项目（AC-10.1/10.3，策略强制 B）
	 */
	R saveProject(ModelProject project);

	/**
	 * 编辑项目（AC-10.4，archived 拒绝写）
	 */
	R updateProject(ModelProject project);

	/**
	 * 删除项目（AC-10.1，级联软删前缀；类实体校验移交 DD8）
	 */
	R removeProject(Long id);

}
