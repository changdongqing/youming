/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the pig4cloud.com developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: lengleng (wangiegie@gmail.com)
 *
 */

package com.pig4cloud.pig.sync.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.sync.api.entity.ConversationHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会话记忆 Mapper
 * <p>
 * 幂等上行使用 PostgreSQL {@code ON CONFLICT ... DO NOTHING}（与客户端 SQLite 去重语义一致）。
 *
 * @author youming
 * @date 2026-07-31
 */
@Mapper
public interface ConversationHistoryMapper extends MPJBaseMapper<ConversationHistory> {

	/**
	 * 幂等插入：与 (session_id, dedup_key, user_id) 冲突时跳过。
	 * <p>
	 * 返回值：1=实际插入，0=冲突跳过（用于统计 accepted/deduped）。
	 */
	@Insert("""
			INSERT INTO qwenpaw_conversation_history
			(seq, session_id, agent_id, kind, role, name, content, tool_call_id,
			 tool_input, tool_state, headline, blocks, metadata, created_at,
			 dedup_key, user_id, client_seq, create_time, update_time, del_flag)
			VALUES
			(#{seq}, #{sessionId}, #{agentId}, #{kind}, #{role}, #{name}, #{content}, #{toolCallId},
			 #{toolInput}, #{toolState}, #{headline}, #{blocks}, #{metadata}, #{createdAt},
			 #{dedupKey}, #{userId}, #{clientSeq}, now(), now(), '0')
			ON CONFLICT (session_id, dedup_key, user_id) DO NOTHING
			""")
	int insertIfAbsent(ConversationHistory entry);

	/**
	 * 查询该用户 seq > since 的增量记录数（判断 has_more）。
	 */
	@Select("SELECT count(*) FROM qwenpaw_conversation_history "
			+ "WHERE user_id = #{userId} AND seq > #{since} AND del_flag = '0'")
	long countAfter(@Param("userId") String userId, @Param("since") long since);

	/**
	 * 查询该用户最大 seq（对账用）。
	 */
	@Select("SELECT COALESCE(max(seq), 0) FROM qwenpaw_conversation_history "
			+ "WHERE user_id = #{userId} AND del_flag = '0'")
	long maxSeq(@Param("userId") String userId);

}
