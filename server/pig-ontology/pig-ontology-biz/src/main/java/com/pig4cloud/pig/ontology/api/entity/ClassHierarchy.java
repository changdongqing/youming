package com.pig4cloud.pig.ontology.api.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本体类分类树镜像（subClassOf，只读，FR-9）
 * <p>
 * 对应 PRD v1.2 9.8。只读镜像建模侧类间的 subClassOf 关系。边界（AC-9.2/9.3/9.6）：
 * 数据来源是建模侧推送/同步（见 SyncController，权限 ont_sync_push），治理侧不提供
 * 主动登记/删除接口，不创建 owl:Class 实体。sync_status 标识镜像新鲜度，建模侧为类层级
 * 权威源。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "本体类分类树镜像（subClassOf，只读）")
@EqualsAndHashCode(callSuper = true)
@TableName("ont_class_hierarchy")
public class ClassHierarchy extends Model<ClassHierarchy> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "主键")
	private Long id;

	@Schema(description = "子类 IRI（建模侧类，只镜像不创建）")
	private String childClassIri;

	@Schema(description = "父类 IRI")
	private String parentClassIri;

	@Schema(description = "溯源：建议来源的分类模板 template_code")
	private String sourceTemplateRef;

	@Schema(description = "所属类树标识")
	private String treeRoot;

	@Schema(description = "0=待同步 1=已同步 2=已失效")
	private String syncStatus;

	@Schema(description = "最近同步时间")
	private LocalDateTime syncTime;

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
