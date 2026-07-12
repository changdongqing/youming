/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionResourceAssociateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationReport;
import com.pig4cloud.pig.ontology.extension.vo.ExtensionResourceVO;

import java.util.List;

/**
 * 扩展资源关联服务接口。
 *
 * @author youming
 */
public interface OntExtensionResourceService extends IService<OntExtensionResource> {

	/**
	 * 查询模块关联的资源列表。
	 * @param moduleId 模块ID
	 * @param resourceType 资源类型（可选过滤）
	 * @return 资源列表
	 */
	List<ExtensionResourceVO> listByModule(Long moduleId, String resourceType);

	/**
	 * 批量关联资源到模块。
	 * @param moduleId 模块ID
	 * @param request 关联请求
	 * @return 关联结果（含校验报告）
	 */
	R<AssociateResult> associateResources(Long moduleId, ExtensionResourceAssociateDTO request);

	/**
	 * 解除资源关联。
	 * @param moduleId 模块ID
	 * @param resourceId 资源ID
	 * @param resourceType 资源类型
	 * @return 解除结果
	 */
	R<Boolean> removeResource(Long moduleId, Long resourceId, String resourceType);

	/**
	 * 批量关联结果。
	 */
	class AssociateResult {

		private int associatedCount;

		private int skippedCount;

		private ExtensionValidationReport validationReport;

		public int getAssociatedCount() {
			return associatedCount;
		}

		public void setAssociatedCount(int associatedCount) {
			this.associatedCount = associatedCount;
		}

		public int getSkippedCount() {
			return skippedCount;
		}

		public void setSkippedCount(int skippedCount) {
			this.skippedCount = skippedCount;
		}

		public ExtensionValidationReport getValidationReport() {
			return validationReport;
		}

		public void setValidationReport(ExtensionValidationReport validationReport) {
			this.validationReport = validationReport;
		}

	}

}
