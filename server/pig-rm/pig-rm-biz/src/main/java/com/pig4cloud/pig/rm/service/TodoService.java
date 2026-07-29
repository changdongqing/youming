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

package com.pig4cloud.pig.rm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.util.SecurityUtils;
import com.pig4cloud.pig.rm.api.entity.Todo;
import com.pig4cloud.pig.rm.api.vo.TodoCountVO;
import com.pig4cloud.pig.rm.api.vo.TodoItemVO;
import com.pig4cloud.pig.rm.mapper.TodoMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 待办服务
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class TodoService extends ServiceImpl<TodoMapper, Todo> {

	/**
	 * 为指定用户创建待办
	 */
	@Transactional(rollbackFor = Exception.class)
	public void createTodo(String todoType, String billType, Long billId, String title, String url, Long userId) {
		// 先关闭该单据同类型的旧待办（避免重复）
		closeTodo(billType, billId, todoType);

		Todo todo = new Todo();
		todo.setUserId(userId);
		todo.setTodoType(todoType);
		todo.setBillType(billType);
		todo.setBillId(billId);
		todo.setTitle(title);
		todo.setUrl(url);
		todo.setStatus("PENDING");
		todo.setDueTime(calculateDueTime(todoType));
		save(todo);
		log.debug("待办已创建: userId={}, type={}, bill={}/{}", userId, todoType, billType, billId);
	}

	/**
	 * 为多个用户创建待办
	 */
	@Transactional(rollbackFor = Exception.class)
	public void createTodoForUsers(List<Long> userIds, String todoType, String billType, Long billId, String title,
			String url) {
		for (Long userId : userIds) {
			createTodo(todoType, billType, billId, title, url, userId);
		}
	}

	/**
	 * 关闭某单据某类型的待办
	 */
	@Transactional(rollbackFor = Exception.class)
	public void closeTodo(String billType, Long billId, String todoType) {
		update(null, Wrappers.<Todo>lambdaUpdate()
			.eq(Todo::getBillType, billType)
			.eq(Todo::getBillId, billId)
			.eq(Todo::getTodoType, todoType)
			.eq(Todo::getStatus, "PENDING")
			.set(Todo::getStatus, "CANCEL"));
	}

	/**
	 * 关闭某单据的全部待办
	 */
	@Transactional(rollbackFor = Exception.class)
	public void closeAllTodo(String billType, Long billId) {
		update(null, Wrappers.<Todo>lambdaUpdate()
			.eq(Todo::getBillType, billType)
			.eq(Todo::getBillId, billId)
			.eq(Todo::getStatus, "PENDING")
			.set(Todo::getStatus, "CANCEL"));
	}

	/**
	 * 我的待办（分页）
	 */
	public IPage<TodoItemVO> myTodo(Page page, String todoType, String billType) {
		Long userId = SecurityUtils.getUser().getId();
		return baseMapper.selectTodoPage(page, userId, todoType, billType);
	}

	/**
	 * 我的待办计数（按类型分组，用于角标）
	 */
	public R myTodoCount() {
		Long userId = SecurityUtils.getUser().getId();
		List<Todo> todos = list(Wrappers.<Todo>lambdaQuery()
			.eq(Todo::getUserId, userId)
			.eq(Todo::getStatus, "PENDING"));
		Map<String, Long> countByType = todos.stream()
			.collect(Collectors.groupingBy(Todo::getTodoType, Collectors.counting()));
		TodoCountVO vo = new TodoCountVO();
		vo.setTotal((long) todos.size());
		vo.setByType(countByType);
		return R.ok(vo);
	}

	/**
	 * 标记待办完成
	 */
	public R markDone(Long id) {
		Todo todo = getById(id);
		if (todo == null) {
			return R.failed("待办不存在");
		}
		todo.setStatus("DONE");
		updateById(todo);
		return R.ok();
	}

	/**
	 * 计算待办截止时间
	 */
	private LocalDateTime calculateDueTime(String todoType) {
		if ("APPROVE".equals(todoType) || "REVIEW".equals(todoType)) {
			return LocalDateTime.now().plusDays(2);
		}
		if ("DESIGN".equals(todoType)) {
			return LocalDateTime.now().plusDays(5);
		}
		return null;
	}

}
