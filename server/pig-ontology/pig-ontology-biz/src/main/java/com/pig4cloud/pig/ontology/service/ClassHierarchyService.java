package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassHierarchySyncDTO;
import com.pig4cloud.pig.ontology.api.entity.ClassHierarchy;
import com.pig4cloud.pig.ontology.api.vo.ClassHierarchyNodeVO;

import java.util.List;

/**
 * 类分类树镜像 Service（FR-9，只读查询 + 建模侧同步写入）
 * <p>
 * 边界（AC-9.2/9.3）：治理侧不提供类层级登记/删除接口；写入只经建模侧同步（权限
 * ont_sync_push）。建模侧是类层级权威源，治理侧只读镜像。
 *
 * @author pig
 * @date 2026-07-28
 */
public interface ClassHierarchyService extends IService<ClassHierarchy> {

	/**
	 * 类分类树视图（已镜像 subClassOf，只读，AC-9.4）。
	 * @param treeRoot 类树标识，null 查全部
	 * @return 类分类树节点列表（已组装父子关系）
	 */
	List<ClassHierarchyNodeVO> tree(String treeRoot);

	/**
	 * 分页查询镜像边。
	 * @param page 分页对象
	 * @param filter 过滤条件（syncStatus 等）
	 * @return 分页结果
	 */
	IPage<ClassHierarchy> page(Page page, ClassHierarchy filter);

	/**
	 * 建模侧推送 subClassOf 关系（批量），镜像入库并置 sync_status='1'（AC-9.2）。
	 * <p>
	 * 按 (child_class_iri, parent_class_iri) 去重 upsert。
	 * @param edges 建模侧推送的类层级边
	 * @return 操作结果
	 */
	R upsertMirror(List<ClassHierarchySyncDTO> edges);

	/**
	 * 建模侧删除某类层级时，镜像置 sync_status='2'（失效），不物理删（保留历史便于统计）。
	 * @param childClassIri 子类 IRI
	 * @param parentClassIri 父类 IRI
	 * @return 操作结果
	 */
	R invalidateMirror(String childClassIri, String parentClassIri);

}
