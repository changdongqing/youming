/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobCreateRequest;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobQuery;
import com.pig4cloud.pig.ontology.mapping.job.dto.JobRetryRequest;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJob;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJobRecord;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobRecordVO;
import com.pig4cloud.pig.ontology.mapping.job.vo.MappingJobVO;

/**
 * 映射作业服务接口（18-07 §8/§9/§10）。
 * <p>
 * 管理作业创建、同步执行、取消、重试、查询和调度配置。
 *
 * @author youming
 */
public interface MappingJobService extends IService<OntMappingJob> {

	/**
	 * 创建并执行映射作业。
	 * @param versionId 映射版本ID
	 * @param request 创建请求
	 * @return 作业详情
	 */
	MappingJobVO createJob(Long versionId, JobCreateRequest request);

	/**
	 * 同步执行作业（已创建的 QUEUED 作业）。
	 * @param jobId 作业ID
	 */
	void executeJob(Long jobId);

	/**
	 * 取消作业。
	 * @param jobId 作业ID
	 * @return 作业详情
	 */
	MappingJobVO cancelJob(Long jobId);

	/**
	 * 重试失败记录。
	 * @param jobId 原作业ID
	 * @param request 重试请求
	 * @return 新建的重试作业详情
	 */
	MappingJobVO retryJob(Long jobId, JobRetryRequest request);

	/**
	 * 获取作业详情。
	 * @param jobId 作业ID
	 * @return 作业详情
	 */
	MappingJobVO getJobDetail(Long jobId);

	/**
	 * 分页查询作业。
	 * @param page 分页参数
	 * @param query 查询条件
	 * @return 分页结果
	 */
	Page<MappingJobVO> page(Page<OntMappingJob> page, JobQuery query);

	/**
	 * 分页查询作业记录。
	 * @param jobId 作业ID
	 * @param page 分页参数
	 * @param recordStatus 记录状态过滤（可选）
	 * @return 分页结果
	 */
	Page<MappingJobRecordVO> getJobRecords(Long jobId, Page<OntMappingJobRecord> page, String recordStatus);

	/**
	 * 获取映射工程的生产游标。
	 * @param projectId 工程ID
	 * @return 游标JSON
	 */
	String getProjectCursor(Long projectId);

	/**
	 * 更新映射工程调度配置。
	 * @param projectId 工程ID
	 * @param scheduleEnabled 是否启用
	 * @param scheduleCron Cron表达式
	 * @param scheduleRunType 运行类型
	 */
	void updateSchedule(Long projectId, boolean scheduleEnabled, String scheduleCron, String scheduleRunType);

}
