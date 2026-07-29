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

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户查询 Mapper（审批人解析用，直接查 sys_user/sys_role/sys_user_role/sys_dept）
 *
 * @author youming
 * @date 2026-07-29
 */
@Mapper
public interface SysUserMapper {

	/**
	 * 按角色编码查询用户ID列表
	 */
	@Select("""
			SELECT ur.user_id FROM sys_user_role ur
			JOIN sys_role r ON ur.role_id = r.role_id
			WHERE r.role_code = #{roleCode} AND r.del_flag = '0'
			""")
	List<Long> getUserIdsByRoleCode(@Param("roleCode") String roleCode);

	/**
	 * 按用户ID查询用户姓名
	 */
	@Select("SELECT name FROM sys_user WHERE user_id = #{userId} AND del_flag = '0'")
	String getUserName(@Param("userId") Long userId);

	/**
	 * 按部门ID查询部门负责人ID
	 */
	@Select("SELECT leader FROM sys_dept WHERE dept_id = #{deptId} AND del_flag = '0'")
	Long getDeptLeaderId(@Param("deptId") Long deptId);

	/**
	 * 按部门ID查询部门名称
	 */
	@Select("SELECT name FROM sys_dept WHERE dept_id = #{deptId} AND del_flag = '0'")
	String getDeptName(@Param("deptId") Long deptId);

}
