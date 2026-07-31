/*
 *
 *      Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 *  Redistribution and use in source and binary, with or without
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

package com.pig4cloud.pig.sync.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 技能 manifest 条目
 *
 * @author youming
 * @date 2026-07-31
 */
@Data
@Schema(description = "企业技能 manifest 条目")
public class SkillManifestItemVO {

	@Schema(description = "技能名")
	private String name;

	@Schema(description = "技能自身版本（如 2.1.0）")
	private String version;

	@Schema(description = "技能内容哈希（sha256:xxx）")
	private String mdHash;

	@Schema(description = "企业分发版本号（增量同步依据）")
	private Integer enterpriseVersion;

	@Schema(description = "技能描述")
	private String description;

}
