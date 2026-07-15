/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 映射工程表（18-03 §3）。
 * <p>
 * 面向一个本体工程的一组实体、字段和关系映射的容器，管理调度、安全级别和活跃版本指针。
 *
 * @author youming
 */
@Data
@TableName("ont_mapping_project")
@Schema(description = "映射工程")
@EqualsAndHashCode(callSuper = true)
public class OntMappingProject extends Model<OntMappingProject> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "工程ID")
	private Long id;

	@Schema(description = "映射编码")
	private String mappingCode;

	@Schema(description = "映射名称")
	private String mappingName;

	@Schema(description = "本体工程ID")
	private Long ontologyId;

	@Schema(description = "默认命名空间ID")
	private Long defaultNamespaceId;

	@Schema(description = "活跃版本ID")
	private Long activeVersionId;

	@Schema(description = "工程状态: DRAFT / ACTIVE / DISABLED / ARCHIVED")
	private String projectStatus;

	@Schema(description = "描述")
	private String description;

	@Schema(description = "是否启用调度: 0否 1是")
	private String scheduleEnabled;

	@Schema(description = "调度Cron表达式")
	private String scheduleCron;

	@Schema(description = "调度运行类型: FULL / INCREMENTAL")
	private String scheduleRunType;

	@Schema(description = "执行主体类型: USER / ROLE")
	private String executionSubjectType;

	@Schema(description = "执行主体ID")
	private Long executionSubjectId;

	@Schema(description = "安全级别编码")
	private String securityLevelCode;

	@Schema(description = "工程修订号（乐观锁）")
	private Long revision;

	@Schema(description = "最近作业ID")
	private Long lastJobId;

	@Schema(description = "最近安全拒绝时间")
	private LocalDateTime lastSecurityDenialAt;

	@Schema(description = "备注")
	private String remarks;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建人")
	private String createBy;

	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "创建时间")
	private LocalDateTime createTime;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "修改人")
	private String updateBy;

	@TableField(fill = FieldFill.UPDATE)
	@Schema(description = "更新时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
