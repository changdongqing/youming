package com.pig4cloud.pig.ontology.modeling.service;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.dto.ClassHierarchySyncDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.service.ClassHierarchyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 镜像回推核心 Service（FR-14.3/14.4）
 * <p>
 * 方案 B（同模块只读/调用 Service 注入）：注入治理域 {@link ClassHierarchyService}，
 * 调用其公开方法 upsertMirror/invalidateMirror 回推镜像到 ont_class_hierarchy。
 * 写入由治理域 Service 自己执行，建模域不直接操作治理域表/Mapper，符合 R-23 "调 Sync API" 的本质。
 * 回推失败时 sync_status='0'，由 HierarchySyncCompensateTask 补偿重试（AC-14.5）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class HierarchySyncService {

	private final ClassHierarchyService classHierarchyService;
	private final ModelClassMapper modelClassMapper;

	/**
	 * 回推镜像：建立 subClassOf 后调 ClassHierarchyService.upsertMirror（AC-14.3）
	 */
	public R pushMirror(ModelSubclassOf edge) {
		ModelClass childClass = modelClassMapper.selectById(edge.getChildClassId());
		ModelClass parentClass = modelClassMapper.selectById(edge.getParentClassId());
		if (childClass == null || parentClass == null) {
			return R.failed("子类或父类不存在");
		}
		// 组装强类型 ClassHierarchySyncDTO（4 字段：childClassIri/parentClassIri 必填，
		// sourceTemplateRef/treeRoot 可空）
		ClassHierarchySyncDTO dto = new ClassHierarchySyncDTO();
		dto.setChildClassIri(childClass.getClassIri());
		dto.setParentClassIri(parentClass.getClassIri());
		dto.setSourceTemplateRef(edge.getSourceTemplateRef());
		dto.setTreeRoot(childClass.getTemplateCode()); // treeRoot 用类的模板溯源标识（手建类为 null，可空）
		try {
			return classHierarchyService.upsertMirror(Collections.singletonList(dto));
		}
		catch (Exception e) {
			log.error("镜像回推异常: child={}, parent={}", childClass.getClassIri(),
					parentClass.getClassIri(), e);
			return R.failed("镜像回推异常：" + e.getMessage());
		}
	}

	/**
	 * 镜像失效：删除 subClassOf 后调 ClassHierarchyService.invalidateMirror（AC-14.4）
	 */
	public R invalidateMirror(ModelSubclassOf edge) {
		ModelClass childClass = modelClassMapper.selectById(edge.getChildClassId());
		ModelClass parentClass = modelClassMapper.selectById(edge.getParentClassId());
		if (childClass == null || parentClass == null) {
			return R.failed("子类或父类不存在");
		}
		try {
			return classHierarchyService.invalidateMirror(childClass.getClassIri(),
					parentClass.getClassIri());
		}
		catch (Exception e) {
			log.error("镜像失效异常: child={}, parent={}", childClass.getClassIri(),
					parentClass.getClassIri(), e);
			return R.failed("镜像失效异常：" + e.getMessage());
		}
	}

}
