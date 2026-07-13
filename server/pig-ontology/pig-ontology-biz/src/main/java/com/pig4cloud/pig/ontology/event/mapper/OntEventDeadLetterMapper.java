/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.event.entity.OntEventDeadLetter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 事件死信 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntEventDeadLetterMapper extends BaseMapper<OntEventDeadLetter> {

	/**
	 * 标记死信为 REPLAYED 并记录新 replayNo。
	 * @param id 死信 ID
	 * @param replayedAsNo 回放后分配的新 replayNo
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_dead_letter SET status = 'REPLAYED', replayed_as_no = #{replayedAsNo}, "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0'")
	int markReplayed(@Param("id") Long id, @Param("replayedAsNo") Integer replayedAsNo);

	/**
	 * 标记死信为 RESOLVED（人工已处理）。
	 * @param id 死信 ID
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_dead_letter SET status = 'RESOLVED', resolved_at = now(), "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0'")
	int markResolved(@Param("id") Long id);

	/**
	 * 标记死信为 IGNORED（人工忽略）。
	 * @param id 死信 ID
	 * @return 影响行数
	 */
	@Update("UPDATE ont_event_dead_letter SET status = 'IGNORED', resolved_at = now(), "
			+ "update_time = now() WHERE id = #{id} AND del_flag = '0'")
	int markIgnored(@Param("id") Long id);

}
