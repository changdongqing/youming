package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 类层级关系 Entity（rdfs:subClassOf，建模域权威源，FR-14）
 * <p>
 * 区别于治理域 ont_class_hierarchy（只读镜像）：本表是建模域权威源，
 * 建立后经 HierarchySyncService 回推镜像到 ont_class_hierarchy。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "类层级关系")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_subclassof")
public class ModelSubclassOf extends Model<ModelSubclassOf> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "子类 ID")
	private Long childClassId;

	@Schema(description = "父类 ID")
	private Long parentClassId;

	@Schema(description = "溯源：建议来源的分类模板 template_code")
	private String sourceTemplateRef;

	@Schema(description = "0=待同步 1=已同步 2=已失效 3=同步失败")
	private String syncStatus;

	@Schema(description = "最近镜像回推时间")
	private LocalDateTime syncTime;

	@Schema(description = "镜像回推重试次数（上限3）")
	private Integer retryCount;

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
	@Schema(description = "修改时间")
	private LocalDateTime updateTime;

	@TableLogic
	@TableField(fill = FieldFill.INSERT)
	@Schema(description = "删除标记,1:已删除,0:正常")
	private String delFlag;

}
