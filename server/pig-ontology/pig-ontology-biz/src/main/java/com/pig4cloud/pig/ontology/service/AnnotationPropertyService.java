package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.AnnotationProperty;
import com.pig4cloud.pig.ontology.api.vo.AnnotationPropertySupplyVO;

import java.util.List;

/**
 * 注释属性注册表 Service（FR-4）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface AnnotationPropertyService extends IService<AnnotationProperty> {

	/**
	 * 全量列表（按 appliesTo 过滤，AC-4.3）
	 * @param appliesTo 作用对象（可选，为空返回全量）
	 * @return 注释属性列表（按 sort_order + id 排序）
	 */
	List<AnnotationProperty> list(String appliesTo);

	/**
	 * 新增 custom 注释属性（appliesTo 枚举校验 + localName 查重，AC-4.2/4.5）
	 * @param ap 注释属性信息
	 * @return 操作结果
	 */
	R saveAp(AnnotationProperty ap);

	/**
	 * 编辑注释属性（builtin 拒绝、localName 不可改，AC-4.2）
	 * @param ap 注释属性信息
	 * @return 操作结果
	 */
	R updateAp(AnnotationProperty ap);

	/**
	 * 删除注释属性（builtin 拒绝、软删）
	 * @param id 注释属性 ID
	 * @return 操作结果
	 */
	R removeAp(Long id);

	/**
	 * 供给列表（稳定化 VO，按 appliesTo 过滤，AC-5.5）
	 * @param appliesTo 作用对象（可选，为空返回全量）
	 * @return 注释属性供给视图列表
	 */
	List<AnnotationPropertySupplyVO> supplyList(String appliesTo);

	/**
	 * 导出 Markdown 清单（含 localName/label/rangeXsd/appliesTo/description，AC-4.4）
	 * @param appliesTo 作用对象（可选，为空导出全量）
	 * @return Markdown 表格字符串
	 */
	String exportMarkdown(String appliesTo);

}
