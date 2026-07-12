/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.ontology.entity.OntValidationResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校验结果明细 Mapper。
 *
 * @author youming
 */
@Mapper
public interface OntValidationResultMapper extends BaseMapper<OntValidationResult> {

}
