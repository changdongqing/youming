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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.rm.api.entity.Todo;
import com.pig4cloud.pig.rm.mapper.TodoMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 待办服务（阶段 1.3 占位实现，阶段 1.6 补全角色级分发与超期预警）
 *
 * @author youming
 * @date 2026-07-29
 */
@Slf4j
@Service
@AllArgsConstructor
public class TodoService {

	private final TodoMapper todoMapper;

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
		todoMapper.insert(todo);
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
	 * 关闭某单据某类型的待办（状态流转后旧待办批量标记 CANCEL）
	 */
	@Transactional(rollbackFor = Exception.class)
	public void closeTodo(String billType, Long billId, String todoType) {
		todoMapper.update(null, Wrappers.<Todo>lambdaUpdate()
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
		todoMapper.update(null, Wrappers.<Todo>lambdaUpdate()
			.eq(Todo::getBillType, billType)
			.eq(Todo::getBillId, billId)
			.eq(Todo::getStatus, "PENDING")
			.set(Todo::getStatus, "CANCEL"));
	}

}
