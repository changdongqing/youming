/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.entity;

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
 * 实例数据属性值。
 *
 * @author youming
 */
@Data
@TableName("ont_instance_data_value")
@Schema(description = "实例数据属性值")
@EqualsAndHashCode(callSuper = true)
public class OntInstanceDataValue extends Model<OntInstanceDataValue> {

	private static final long serialVersionUID = 1L;

	@TableId(type = IdType.ASSIGN_ID)
	@Schema(description = "数据值ID")
	private Long id;

	@Schema(description = "实例ID")
	private Long instanceId;

	@Schema(description = "数据属性ID")
	private Long dataPropertyId;

	@Schema(description = "规范化词法值")
	private String literalValue;

	@Schema(description = "字面量类型：STRING/URI/DATE/DATETIME/INTEGER/DECIMAL/BOOLEAN")
	private String literalType;

	@Schema(description = "单位ID，UNIT_REF必填")
	private Long unitId;

	@Schema(description = "UNIT_REF词法快照")
	private String literalSymbol;

	@Schema(description = "同属性多值排序")
	private Integer sortOrder;

	@Schema(description = "安全级别编码覆盖，NULL继承上级")
	private String securityLevelCode;

	@Schema(description = "是否加密：0明文 1密文")
	private String isEncrypted;

	@Schema(description = "AES-GCM密文（含认证标签）")
	private byte[] encryptedValue;

	@Schema(description = "密钥版本ID")
	private String cryptoKeyId;

	@Schema(description = "加密随机IV")
	private byte[] cryptoIv;

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
