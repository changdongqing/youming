/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.vo;

import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体类型详情。
 *
 * @author youming
 */
@Data
@Schema(description = "实体类型详情")
public class OntEntityTypeDetailVO {

	private OntEntityType entityType;

	private List<OntEntityTypeLabel> labels = new ArrayList<>();

	private List<Long> parentIds = new ArrayList<>();

	private List<OntEntityType> parents = new ArrayList<>();

	private List<Long> childIds = new ArrayList<>();

	private List<OntEntityType> children = new ArrayList<>();

	private List<OntEntityType> equivalents = new ArrayList<>();

	private List<OntEntityType> disjoints = new ArrayList<>();

	private OntNamespace namespace;

}
