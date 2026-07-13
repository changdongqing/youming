/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.event.entity.OntEventReplayLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 事件回放日志 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntEventReplayLogMapper extends BaseMapper<OntEventReplayLog> {

	/**
	 * 查询指定事件面向目标消费者组的最大 replayNo。
	 * @param eventId 事件 ID
	 * @param targetConsumerGroup 目标消费者组
	 * @return 最大 replayNo，无记录时返回 null
	 */
	@Select("SELECT COALESCE(MAX(target_replay_no), 0) FROM ont_event_replay_log "
			+ "WHERE event_id = #{eventId} AND target_consumer_group = #{targetConsumerGroup} "
			+ "AND del_flag = '0'")
	Integer selectMaxReplayNo(@Param("eventId") String eventId,
			@Param("targetConsumerGroup") String targetConsumerGroup);

}
