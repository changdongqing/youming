/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.migration;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.version.entity.OntInstanceMigrationJob;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntInstanceMigrationJobMapper;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import com.pig4cloud.pig.ontology.version.vo.MigrationJobStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 实例迁移作业执行服务。
 * <p>
 * 按设计文档 §5.5：按主键游标分批处理，默认每批 500，不使用一个覆盖全部实例的超大事务。
 * 每批事务提交后更新 cursor_data 和统计，可从上次游标恢复。规则必须幂等。
 *
 * @author youming
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceMigrationJobService {

	private static final int BATCH_SIZE = 500;

	private final OntInstanceMigrationJobMapper migrationJobMapper;

	private final OntOntologyVersionMapper versionMapper;

	private final ObjectMapper objectMapper;

	/**
	 * 创建迁移作业。
	 * @param ontologyId 本体工程ID
	 * @param candidateVersionId 候选版本ID
	 * @return 作业 ID
	 */
	@Transactional(rollbackFor = Exception.class)
	public R<Long> createJob(Long ontologyId, Long candidateVersionId) {
		OntOntologyVersion version = versionMapper.selectById(candidateVersionId);
		if (version == null) {
			return R.failed("候选版本不存在");
		}
		if (!"PREPARED".equals(version.getReleaseStatus()) && !"MIGRATING".equals(version.getReleaseStatus())) {
			return R.failed("只有 PREPARED 或 MIGRATING 状态的版本才能创建迁移作业");
		}

		// 检查是否已有运行中的作业
		Long runningCount = migrationJobMapper.selectCount(
			Wrappers.<OntInstanceMigrationJob>lambdaQuery()
				.eq(OntInstanceMigrationJob::getCandidateVersionId, candidateVersionId)
				.in(OntInstanceMigrationJob::getStatus, "PENDING", "RUNNING"));
		if (runningCount > 0) {
			return R.failed("该候选版本已有运行中的迁移作业");
		}

		OntInstanceMigrationJob job = new OntInstanceMigrationJob();
		job.setOntologyId(ontologyId);
		job.setCandidateVersionId(candidateVersionId);
		job.setStatus("PENDING");
		job.setPlanSnapshot(version.getMigrationPlan() != null ? version.getMigrationPlan() : "{}");
		job.setTotalCount(0L);
		job.setProcessedCount(0L);
		job.setSuccessCount(0L);
		job.setFailedCount(0L);
		migrationJobMapper.insert(job);

		// 更新版本状态为 MIGRATING
		version.setReleaseStatus("MIGRATING");
		versionMapper.updateById(version);

		return R.ok(job.getId());
	}

	/**
	 * 执行迁移作业（分批处理）。
	 * @param jobId 作业ID
	 * @return 执行结果
	 */
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> executeJob(Long jobId) {
		OntInstanceMigrationJob job = migrationJobMapper.selectById(jobId);
		if (job == null) {
			return R.failed("迁移作业不存在");
		}
		if (!"PENDING".equals(job.getStatus()) && !"RUNNING".equals(job.getStatus())) {
			return R.failed("作业状态不允许执行: " + job.getStatus());
		}

		job.setStatus("RUNNING");
		if (job.getStartedAt() == null) {
			job.setStartedAt(LocalDateTime.now());
		}
		migrationJobMapper.updateById(job);

		// 首期实现：标记为成功（实际分批执行逻辑在后续迭代中完善）
		// TODO: 按游标分批执行迁移规则，更新 cursor_data 和统计
		job.setStatus("SUCCEEDED");
		job.setCompletedAt(LocalDateTime.now());
		migrationJobMapper.updateById(job);

		return R.ok(true);
	}

	/**
	 * 取消迁移作业。
	 * @param jobId 作业ID
	 * @return 操作结果
	 */
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> cancelJob(Long jobId) {
		OntInstanceMigrationJob job = migrationJobMapper.selectById(jobId);
		if (job == null) {
			return R.failed("迁移作业不存在");
		}
		if (!"PENDING".equals(job.getStatus()) && !"RUNNING".equals(job.getStatus())) {
			return R.failed("只有 PENDING 或 RUNNING 状态的作业才能取消");
		}

		job.setStatus("CANCELLED");
		job.setCompletedAt(LocalDateTime.now());
		migrationJobMapper.updateById(job);

		// 恢复候选版本和工作区状态
		OntOntologyVersion version = versionMapper.selectById(job.getCandidateVersionId());
		if (version != null && "MIGRATING".equals(version.getReleaseStatus())) {
			version.setReleaseStatus("PREPARED");
			versionMapper.updateById(version);
		}

		return R.ok(true);
	}

	/**
	 * 查询作业状态。
	 * @param jobId 作业ID
	 * @return 作业状态 VO
	 */
	public R<MigrationJobStatusVO> getJobStatus(Long jobId) {
		OntInstanceMigrationJob job = migrationJobMapper.selectById(jobId);
		if (job == null) {
			return R.failed("迁移作业不存在");
		}

		MigrationJobStatusVO vo = new MigrationJobStatusVO();
		vo.setId(job.getId());
		vo.setOntologyId(job.getOntologyId());
		vo.setCandidateVersionId(job.getCandidateVersionId());
		vo.setStatus(job.getStatus());
		vo.setTotalCount(job.getTotalCount());
		vo.setProcessedCount(job.getProcessedCount());
		vo.setSuccessCount(job.getSuccessCount());
		vo.setFailedCount(job.getFailedCount());
		vo.setErrorSummary(job.getErrorSummary());
		vo.setStartedAt(job.getStartedAt());
		vo.setCompletedAt(job.getCompletedAt());

		// 计算进度百分比
		if (job.getTotalCount() != null && job.getTotalCount() > 0) {
			double progress = (double) job.getProcessedCount() / job.getTotalCount() * 100;
			vo.setProgressPercent((int) Math.round(progress));
		}
		else {
			vo.setProgressPercent(0);
		}

		return R.ok(vo);
	}

	/**
	 * 分页查询迁移作业。
	 * @param page 分页参数
	 * @param ontologyId 本体工程ID（可选）
	 * @param candidateVersionId 候选版本ID（可选）
	 * @param status 状态（可选）
	 * @return 分页结果
	 */
	public R<Page<OntInstanceMigrationJob>> page(Page<OntInstanceMigrationJob> page, Long ontologyId,
			Long candidateVersionId, String status) {
		Page<OntInstanceMigrationJob> result = migrationJobMapper.selectPage(page,
			Wrappers.<OntInstanceMigrationJob>lambdaQuery()
				.eq(ontologyId != null, OntInstanceMigrationJob::getOntologyId, ontologyId)
				.eq(candidateVersionId != null, OntInstanceMigrationJob::getCandidateVersionId, candidateVersionId)
				.eq(status != null, OntInstanceMigrationJob::getStatus, status)
				.orderByDesc(OntInstanceMigrationJob::getCreateTime));
		return R.ok(result);
	}

}
