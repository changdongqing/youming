/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.validation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationIssue;
import com.pig4cloud.pig.ontology.mapping.validation.entity.OntMappingValidationReport;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationReportVO;
import com.pig4cloud.pig.ontology.mapping.validation.vo.ValidationIssueVO;

/**
 * 映射校验服务接口（18-06 §7, §12, §13）。
 * <p>
 * 负责编排校验器、管理校验报告和问题、执行发布门禁检查和 WARNING 确认。
 *
 * @author youming
 */
public interface MappingValidationService extends IService<OntMappingValidationReport> {

	/**
	 * 执行映射版本校验。
	 * <p>
	 * 流程：CAS DRAFT→VALIDATING → 加载上下文 → 执行校验器 → 写报告和问题 →
	 * PASSED 则 CAS VALIDATING→VALIDATED 并绑定报告；FAILED 则 CAS VALIDATING→DRAFT。
	 * @param versionId 映射版本ID
	 * @param triggerType 触发类型：MANUAL / PUBLISH_RECHECK / SYSTEM
	 * @return 校验报告
	 */
	ValidationReportVO validate(Long versionId, String triggerType);

	/**
	 * 获取校验报告详情。
	 * @param reportId 报告ID
	 * @return 报告 VO
	 */
	ValidationReportVO getReport(Long reportId);

	/**
	 * 分页查询报告下的校验问题。
	 * @param reportId 报告ID
	 * @param page 分页参数
	 * @param severity 严重级别过滤（可选）
	 * @return 问题分页
	 */
	Page<ValidationIssueVO> getIssues(Long reportId, Page<OntMappingValidationIssue> page, String severity);

	/**
	 * 确认 WARNING 问题（18-06 §13）。
	 * <p>
	 * 只有 ontology_mapping_publish 权限且拥有工程 PUBLISH ACL 的用户可确认。
	 * VIOLATION 没有确认接口。
	 * @param issueId 问题ID
	 * @return 更新后的问题 VO
	 */
	ValidationIssueVO acknowledgeIssue(Long issueId);

	/**
	 * 检查发布门禁是否通过（18-06 §12）。
	 * <p>
	 * 发布门禁条件：
	 * <ol>
	 *   <li>版本状态 VALIDATED</li>
	 *   <li>报告 PASSED 且 violationCount=0</li>
	 *   <li>所有必须确认的 WARNING 已 acknowledged</li>
	 *   <li>config revision 与报告一致</li>
	 * </ol>
	 * @param versionId 映射版本ID
	 * @throws IllegalStateException 如果门禁失败
	 */
	void assertPublishGate(Long versionId);

}
