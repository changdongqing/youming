package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.UnitSupplyVO;

import java.util.List;

/**
 * 单位 Service（FR-3）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface UnitService extends IService<Unit> {

	/**
	 * 分页查询单位（按量纲/关键字/状态过滤）
	 * @param page 分页对象
	 * @param unit 查询条件
	 * @return 分页结果
	 */
	IPage<Unit> page(Page page, Unit unit);

	/**
	 * 新增 custom 单位（量纲存在性 + qudtIri 查重，AC-3.5）
	 * @param unit 单位信息
	 * @return 操作结果
	 */
	R saveUnit(Unit unit);

	/**
	 * 编辑单位（builtin 拒绝、qudtIri 不可改）
	 * @param unit 单位信息
	 * @return 操作结果
	 */
	R updateUnit(Unit unit);

	/**
	 * 删除单位（builtin 拒绝、软删）
	 * @param id 单位 ID
	 * @return 操作结果
	 */
	R removeUnit(Long id);

	/**
	 * 弃用/恢复单位
	 * @param id 单位 ID
	 * @param deprecated '1'=弃用 '0'=恢复
	 * @return 操作结果
	 */
	R deprecate(Long id, String deprecated);

	/**
	 * 供给列表（稳定化 VO，按量纲过滤，默认排除弃用，AC-5.4）
	 * @param quantityKindIri 量纲 IRI（可选，为空返回全量）
	 * @param includeDeprecated 是否包含弃用
	 * @return 单位供给视图列表
	 */
	List<UnitSupplyVO> supplyList(String quantityKindIri, Boolean includeDeprecated);

}
