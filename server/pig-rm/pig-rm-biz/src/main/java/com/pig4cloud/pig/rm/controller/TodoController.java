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

package com.pig4cloud.pig.rm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.rm.api.vo.TodoItemVO;
import com.pig4cloud.pig.rm.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

/**
 * 待办中心
 *
 * @author youming
 * @date 2026-07-29
 */
@RestController
@AllArgsConstructor
@Tag(name = "待办中心", description = "我的待办/计数/标记完成")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class TodoController {

	private final TodoService todoService;

	@GetMapping("/rm/todo/my")
	@Operation(summary = "我的待办（分页）")
	@HasPermission("rm_todo")
	public R<IPage<TodoItemVO>> myTodo(@ParameterObject Page page,
			@RequestParam(required = false) String todoType,
			@RequestParam(required = false) String billType) {
		return R.ok(todoService.myTodo(page, todoType, billType));
	}

	@GetMapping("/rm/todo/count")
	@Operation(summary = "我的待办计数（角标）")
	@HasPermission("rm_todo")
	public R myTodoCount() {
		return todoService.myTodoCount();
	}

	@PutMapping("/rm/todo/done/{id}")
	@Operation(summary = "标记待办完成")
	@HasPermission("rm_todo")
	public R markDone(@PathVariable Long id) {
		return todoService.markDone(id);
	}

}
