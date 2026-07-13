/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.event.entity.OntEventConsumeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 事件消费记录 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntEventConsumeRecordMapper extends BaseMapper<OntEventConsumeRecord> {

	/**
	 * 按幂等键查询当前消费记录状态。
	 * @param consumerGroup 消费者组
	 * @param eventId 事件 ID
	 * @param replayNo 回放序号
	 * @return 消费记录（可能不存在）
	 */
	@Select("SELECT * FROM ont_event_consume_record "
			+ "WHERE consumer_group = #{consumerGroup} AND event_id = #{eventId} "
			+ "AND replay_no = #{replayNo} AND del_flag = '0'")
	OntEventConsumeRecord findByIdempotencyKey(@Param("consumerGroup") String consumerGroup,
			@Param("eventId") String eventId, @Param("replayNo") Integer replayNo);

	/**
	 * 更新消费状态为 SUCCEEDED。
	 * @param id 记录 ID
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_consume_record SET status = 'SUCCEEDED', completed_at = now(), "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0'")
	int markSucceeded(@Param("id") Long id);

	/**
	 * 更新消费状态为 FAILED 并记录错误。
	 * @param id 记录 ID
	 * @param errorCode 错误码
	 * @param errorMessage 错误信息
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_consume_record SET status = 'FAILED', last_error_code = #{errorCode}, "
			+ "last_error_message = #{errorMessage}, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0'")
	int markFailed(@Param("id") Long id, @Param("errorCode") String errorCode,
			@Param("errorMessage") String errorMessage);

	/**
	 * 续租：延长 PROCESSING 记录的租约时间。
	 * @param id 记录 ID
	 * @param leaseUntil 新租约到期时间
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_consume_record SET lease_until = #{leaseUntil}, "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0' AND status = 'PROCESSING'")
	int renewLease(@Param("id") Long id, @Param("leaseUntil") LocalDateTime leaseUntil);

}
