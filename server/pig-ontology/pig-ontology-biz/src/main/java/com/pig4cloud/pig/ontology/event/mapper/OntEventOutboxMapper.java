/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.event.entity.OntEventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件 Outbox Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntEventOutboxMapper extends BaseMapper<OntEventOutbox> {

	/**
	 * 使用 FOR UPDATE SKIP LOCKED 领取待投递事件批次。 <p>
	 * 领取 PENDING 且到时可投递的，以及 PROCESSING 但租约已过期的行。
	 * @param batchSize 单次领取上限
	 * @return 领取到的事件 ID 列表
	 */
	@Select("SELECT id FROM ont_event_outbox "
			+ "WHERE del_flag = '0' "
			+ "AND ( "
			+ "  (status = 'PENDING' AND available_at <= now()) "
			+ "  OR (status = 'PROCESSING' AND lease_until < now()) "
			+ ") "
			+ "ORDER BY create_time "
			+ "FOR UPDATE SKIP LOCKED "
			+ "LIMIT #{batchSize}")
	List<Long> selectPollingBatch(@Param("batchSize") int batchSize);

	/**
	 * 将领取行更新为 PROCESSING 并设置租约。
	 * @param id 事件 Outbox 行 ID
	 * @param lockedBy 领取实例标识
	 * @param leaseUntil 租约到期时间
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_outbox SET status = 'PROCESSING', locked_by = #{lockedBy}, "
			+ "lease_until = #{leaseUntil}, delivery_attempt = delivery_attempt + 1, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0'")
	int lockForProcessing(@Param("id") Long id, @Param("lockedBy") String lockedBy,
			@Param("leaseUntil") LocalDateTime leaseUntil);

	/**
	 * XADD 成功后标记为 PUBLISHED。
	 * @param id 事件 Outbox 行 ID
	 * @param streamRecordId Redis Stream 返回的记录 ID
	 * @param publishedAt 发布时间
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_outbox SET status = 'PUBLISHED', stream_record_id = #{streamRecordId}, "
			+ "published_at = #{publishedAt}, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0'")
	int markPublished(@Param("id") Long id, @Param("streamRecordId") String streamRecordId,
			@Param("publishedAt") LocalDateTime publishedAt);

	/**
	 * 标记投递失败并记录错误信息。
	 * @param id 事件 Outbox 行 ID
	 * @param errorCode 错误码
	 * @param errorMessage 错误信息
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_outbox SET status = 'FAILED', last_error_code = #{errorCode}, "
			+ "last_error_message = #{errorMessage}, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0'")
	int markFailed(@Param("id") Long id, @Param("errorCode") String errorCode,
			@Param("errorMessage") String errorMessage);

	/**
	 * 重置 FAILED 行为 PENDING 以便手动重试。
	 * @param id 事件 Outbox 行 ID
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_outbox SET status = 'PENDING', available_at = now(), "
			+ "last_error_code = NULL, last_error_message = NULL, update_time = now() "
			+ "WHERE id = #{id} AND del_flag = '0' AND status = 'FAILED'")
	int resetForRetry(@Param("id") Long id);

}
