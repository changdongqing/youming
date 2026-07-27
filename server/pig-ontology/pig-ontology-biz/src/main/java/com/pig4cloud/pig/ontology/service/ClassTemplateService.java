package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassTemplateSaveDTO;
import com.pig4cloud.pig.ontology.api.entity.ClassTemplate;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateDetailVO;
import com.pig4cloud.pig.ontology.api.vo.ClassTemplateNodeVO;
import com.pig4cloud.pig.ontology.api.vo.InheritedViewVO;

import java.util.List;

/**
 * 分类模板 Service（FR-2/FR-8）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ClassTemplateService extends IService<ClassTemplate> {

	/**
	 * 分类模板树（含编码/继承预览/外观合并）。
	 * @param treeRoot 分类树标识
	 * @param includeDeprecated 是否包含弃用
	 * @return 树节点列表
	 */
	List<ClassTemplateNodeVO> tree(String treeRoot, Boolean includeDeprecated);

	/**
	 * 分类模板详情（外观合并 + 本节点 refs，AC-2.2）。
	 * @param id 分类模板 id
	 * @return 详情
	 */
	ClassTemplateDetailVO getDetail(Long id);

	/**
	 * 继承视图（父链合并属性 + 外观，AC-2.3/2.5，NFR-12/14）。
	 * @param id 分类模板 id
	 * @return 继承视图
	 */
	InheritedViewVO inheritedView(Long id);

	/**
	 * 分页查询（支持 classificationCode 前缀查子树 AC-8.4 + templateCode 模糊）。
	 * @param page 分页对象
	 * @param template 查询条件
	 * @return 分页结果
	 */
	IPage<ClassTemplate> page(Page page, ClassTemplate template);

	/**
	 * 新增分类模板（含 parent_id；classification_code 自动生成或校验，FR-8）。
	 * @param dto 新增请求
	 * @return 操作结果
	 */
	R saveTemplate(ClassTemplateSaveDTO dto);

	/**
	 * 编辑分类模板（builtin 拒绝；改 parent 校验不成环 AC-2.6）。
	 * @param dto 编辑请求
	 * @return 操作结果
	 */
	R updateTemplate(ClassTemplateSaveDTO dto);

	/**
	 * 删除分类模板（有子节点拒绝 AC-2.7；custom 软删）。
	 * @param id 分类模板 id
	 * @return 操作结果
	 */
	R removeTemplate(Long id);

	/**
	 * 弃用/恢复分类模板。
	 * @param id 分类模板 id
	 * @param deprecated '1'=弃用 '0'=恢复
	 * @return 操作结果
	 */
	R deprecate(Long id, String deprecated);

	/**
	 * 编码预览（据 parentId + 规则生成下一编码，AC-8.2）。
	 * @param parentId 父分类模板 id，null 表示根
	 * @return 生成的编码
	 */
	String previewClassificationCode(Long parentId);

	/**
	 * 据 templateCode 查询分类模板（供给接口 suggest 用）。
	 * @param templateCode 模板标识
	 * @return 分类模板，不存在返回 null
	 */
	ClassTemplate getByCode(String templateCode);

	/**
	 * 类层级建议供给（据模板父链推荐 subClassOf 父类 IRI，FR-9 AC-9.1）。
	 * @param templateCode 模板标识
	 * @return 建议的父类 IRI 列表（来自 ont_class_hierarchy 已镜像的 parent_class_iri）
	 */
	List<String> suggestParentClassIris(String templateCode);

}
