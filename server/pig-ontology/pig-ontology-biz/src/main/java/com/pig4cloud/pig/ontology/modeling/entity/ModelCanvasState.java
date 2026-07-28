package com.pig4cloud.pig.ontology.modeling.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 画布状态持久化 Entity（FR-17.8）
 * <p>
 * 每项目每用户独立，存储 AntV X6 graph.toJSON() 序列化的图数据。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "画布状态")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_model_canvas_state")
public class ModelCanvasState extends Model<ModelCanvasState> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "所属项目 ID")
	private Long projectId;

	@Schema(description = "用户名（与 createBy 一致，每用户独立画布状态）")
	private String userId;

	@Schema(description = "AntV X6 graph.toJSON() 序列化的图数据")
	private String graphData;

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
