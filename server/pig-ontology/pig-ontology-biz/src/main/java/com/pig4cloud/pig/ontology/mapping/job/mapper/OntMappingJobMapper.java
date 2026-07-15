/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.mapping.job.entity.OntMappingJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 映射作业 Mapper（18-07 §3）。
 * <p>
 * 提供 CAS 状态转换、租约管理、进度更新和队列领取等自定义 SQL。
 *
 * @author youming
 */
@Mapper
public interface OntMappingJobMapper extends BaseMapper<OntMappingJob> {

	/**
	 * 行级悲观锁。
	 * @param id 作业ID
	 * @return 作业实体（在同一事务内持有行锁）
	 */
	@Select("SELECT * FROM ont_mapping_job WHERE id = #{id} AND del_flag = '0' FOR UPDATE")
	OntMappingJob selectForUpdate(@Param("id") Long id);

	/**
	 * 领取作业：CAS QUEUED/RECOVERING → STARTING，设置租约。
	 * @param id 作业ID
	 * @param leaseOwner 租约持有者标识
	 * @param leaseUntil 租约到期时间
	 * @param heartbeatAt 心跳时间
	 * @param startedAt 开始时间
	 * @return 受影响行数（1成功，0状态不匹配）
	 */
	@Update("UPDATE ont_mapping_job SET job_status = 'STARTING', "
			+ "lease_owner = #{leaseOwner}, lease_until = #{leaseUntil}, "
			+ "heartbeat_at = #{heartbeatAt}, started_at = #{startedAt}, "
			+ "update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0' "
			+ "AND job_status IN ('QUEUED','RECOVERING')")
	int acquireLease(@Param("id") Long id, @Param("leaseOwner") String leaseOwner,
			@Param("leaseUntil") LocalDateTime leaseUntil,
			@Param("heartbeatAt") LocalDateTime heartbeatAt,
			@Param("startedAt") LocalDateTime startedAt);

	/**
	 * 续租：更新心跳和租约到期时间，仅对运行态有效。
	 * @param id 作业ID
	 * @param leaseUntil 新的租约到期时间
	 * @param heartbeatAt 心跳时间
	 * @return 受影响行数（0表示租约已丢失）
	 */
	@Update("UPDATE ont_mapping_job SET lease_until = #{leaseUntil}, "
			+ "heartbeat_at = #{heartbeatAt}, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0' "
			+ "AND job_status IN ('STARTING','RUNNING','CANCELLING')")
	int renewLease(@Param("id") Long id, @Param("leaseUntil") LocalDateTime leaseUntil,
			@Param("heartbeatAt") LocalDateTime heartbeatAt);

	/**
	 * 通用 CAS 状态转换。
	 * @param id 作业ID
	 * @param expectedStatus 预期状态
	 * @param targetStatus 目标状态
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_job SET job_status = #{targetStatus}, update_time = now() "
			+ "WHERE id = #{id} AND job_status = #{expectedStatus} AND del_flag = '0'")
	int casUpdateStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
			@Param("targetStatus") String targetStatus);

	/**
	 * 更新进度：计数、游标、当前阶段和映射编码、页号、心跳。
	 * @param id 作业ID
	 * @param currentPhase 当前阶段
	 * @param currentMappingCode 当前映射编码
	 * @param currentPageNo 当前页号
	 * @param totalRead 总读取数
	 * @param totalCreated 新建数
	 * @param totalUpdated 更新数
	 * @param totalUnchanged 未变化数
	 * @param totalSkipped 跳过数
	 * @param totalFailed 失败数
	 * @param totalRelations 关系数
	 * @param cursorAfter 游标后值JSON
	 * @param heartbeatAt 心跳时间
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_job SET "
			+ "current_phase = #{currentPhase}, "
			+ "current_mapping_code = #{currentMappingCode}, "
			+ "current_page_no = #{currentPageNo}, "
			+ "total_read = #{totalRead}, "
			+ "total_created = #{totalCreated}, "
			+ "total_updated = #{totalUpdated}, "
			+ "total_unchanged = #{totalUnchanged}, "
			+ "total_skipped = #{totalSkipped}, "
			+ "total_failed = #{totalFailed}, "
			+ "total_relations = #{totalRelations}, "
			+ "cursor_after = CAST(#{cursorAfter} AS jsonb), "
			+ "heartbeat_at = #{heartbeatAt}, "
			+ "update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0'")
	int updateProgress(@Param("id") Long id, @Param("currentPhase") String currentPhase,
			@Param("currentMappingCode") String currentMappingCode,
			@Param("currentPageNo") Long currentPageNo, @Param("totalRead") Long totalRead,
			@Param("totalCreated") Long totalCreated, @Param("totalUpdated") Long totalUpdated,
			@Param("totalUnchanged") Long totalUnchanged, @Param("totalSkipped") Long totalSkipped,
			@Param("totalFailed") Long totalFailed, @Param("totalRelations") Long totalRelations,
			@Param("cursorAfter") String cursorAfter, @Param("heartbeatAt") LocalDateTime heartbeatAt);

	/**
	 * 标记作业完成：设置终态、完成时间、错误信息。
	 * @param id 作业ID
	 * @param expectedStatus 预期状态（RUNNING/CANCELLING）
	 * @param targetStatus 目标终态
	 * @param finishedAt 完成时间
	 * @param errorCode 错误码
	 * @param errorMessage 错误消息
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_job SET job_status = #{targetStatus}, "
			+ "finished_at = #{finishedAt}, error_code = #{errorCode}, "
			+ "error_message = #{errorMessage}, lease_owner = NULL, "
			+ "lease_until = NULL, update_time = now() "
			+ "WHERE id = #{id} AND job_status = #{expectedStatus} AND del_flag = '0'")
	int markFinished(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
			@Param("targetStatus") String targetStatus, @Param("finishedAt") LocalDateTime finishedAt,
			@Param("errorCode") String errorCode, @Param("errorMessage") String errorMessage);

	/**
	 * 请求取消：设置 cancel_requested=1 和 CANCELLING 状态。
	 * @param id 作业ID
	 * @return 受影响行数
	 */
	@Update("UPDATE ont_mapping_job SET cancel_requested = '1', "
			+ "job_status = CASE WHEN job_status IN ('STARTING','RUNNING','RECOVERING') THEN 'CANCELLING' ELSE job_status END, "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0' "
			+ "AND job_status NOT IN ('CANCELLED','SUCCEEDED','PARTIAL_SUCCESS','FAILED')")
	int requestCancel(@Param("id") Long id);

	/**
	 * 领取待处理作业（FOR UPDATE SKIP LOCKED）。
	 * @param limit 最大领取数
	 * @return 作业列表
	 */
	@Select("SELECT * FROM ont_mapping_job "
			+ "WHERE job_status IN ('QUEUED','RECOVERING') AND del_flag = '0' "
			+ "ORDER BY create_time FOR UPDATE SKIP LOCKED LIMIT #{limit}")
	List<OntMappingJob> findQueuedJobs(@Param("limit") int limit);

}
