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

package com.pig4cloud.pig.rm.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.pig4cloud.pig.rm.api.entity.Todo;
import com.pig4cloud.pig.rm.api.vo.TodoItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 待办 Mapper
 *
 * @author youming
 * @date 2026-07-29
 */
@Mapper
public interface TodoMapper extends MPJBaseMapper<Todo> {

	/**
	 * 待办分页（关联单据编号 + 超期标记）
	 */
	@Select("""
			SELECT t.id, t.todo_type, t.bill_type, t.bill_id, t.title, t.url, t.status,
			       t.due_time, t.create_time,
			       CASE
			         WHEN t.bill_type='REQUIREMENT' THEN r.req_code
			         WHEN t.bill_type='DEV_TASK' THEN d.task_code
			         WHEN t.bill_type='TEST_TASK' THEN tt.task_code
			         WHEN t.bill_type='BUG' THEN b.bug_code
			       END AS bill_code,
			       (t.due_time IS NOT NULL AND t.due_time < now() AND t.status='PENDING') AS overdue
			FROM rm_todo t
			LEFT JOIN rm_requirement r ON t.bill_type='REQUIREMENT' AND t.bill_id=r.id AND r.del_flag='0'
			LEFT JOIN rm_dev_task d ON t.bill_type='DEV_TASK' AND t.bill_id=d.id AND d.del_flag='0'
			LEFT JOIN rm_test_task tt ON t.bill_type='TEST_TASK' AND t.bill_id=tt.id AND tt.del_flag='0'
			LEFT JOIN rm_bug b ON t.bill_type='BUG' AND t.bill_id=b.id AND b.del_flag='0'
			WHERE t.user_id = #{userId} AND t.del_flag='0'
			  AND (#{todoType} IS NULL OR t.todo_type = #{todoType})
			  AND (#{billType} IS NULL OR t.bill_type = #{billType})
			ORDER BY t.create_time DESC
			""")
	IPage<TodoItemVO> selectTodoPage(Page page,
			@Param("userId") Long userId,
			@Param("todoType") String todoType,
			@Param("billType") String billType);

}
