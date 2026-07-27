package com.pig4cloud.pig.ontology.api.vo;

import com.pig4cloud.pig.ontology.api.entity.ClassHierarchy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 类分类树节点（FR-9，AC-9.4，只读镜像）
 * <p>
 * 本体类分类树视图节点（subClassOf 镜像）。childClassIri 作节点 key，parentClassIri
 * 组装父子关系。sync_status 标识镜像新鲜度。只读，治理侧不提供写入接口。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "类分类树节点（只读镜像）")
public class ClassHierarchyNodeVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "类 IRI（节点 key）")
	private String classIri;

	@Schema(description = "父类 IRI（NULL=根，组装树用）")
	private String parentClassIri;

	@Schema(description = "溯源：建议来源的分类模板 template_code")
	private String sourceTemplateRef;

	@Schema(description = "所属类树标识")
	private String treeRoot;

	@Schema(description = "同步状态：0=待同步 1=已同步 2=已失效")
	private String syncStatus;

	@Schema(description = "最近同步时间")
	private String syncTime;

	@Schema(description = "子节点")
	private List<ClassHierarchyNodeVO> children;

	/**
	 * 从镜像实体转换（childClassIri 作节点 key，parentClassIri 作 parentId）。
	 * @param edge 镜像边
	 * @return 节点 VO
	 */
	public static ClassHierarchyNodeVO from(ClassHierarchy edge) {
		ClassHierarchyNodeVO vo = new ClassHierarchyNodeVO();
		vo.setClassIri(edge.getChildClassIri());
		vo.setParentClassIri(edge.getParentClassIri());
		vo.setSourceTemplateRef(edge.getSourceTemplateRef());
		vo.setTreeRoot(edge.getTreeRoot());
		vo.setSyncStatus(edge.getSyncStatus());
		vo.setSyncTime(edge.getSyncTime() == null ? null : edge.getSyncTime().toString());
		return vo;
	}

}
