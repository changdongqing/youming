/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleEnabledDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleTargetDTO;
import com.pig4cloud.pig.ontology.dto.OntAxiomRuleUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntAxiomRule;
import com.pig4cloud.pig.ontology.entity.OntAxiomRuleTarget;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeDisjoint;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeEquivalent;
import com.pig4cloud.pig.ontology.entity.OntObjectProperty;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitCategoryMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公理规则领域服务单元测试。
 *
 * @author youming
 */
@ExtendWith(MockitoExtension.class)
class OntAxiomRuleServiceImplTests {

	@BeforeAll
	static void initializeMybatisTableMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(),
				"ontology-axiom-rule-tests");
		TableInfoHelper.initTableInfo(assistant, OntAxiomRule.class);
		TableInfoHelper.initTableInfo(assistant, OntAxiomRuleTarget.class);
	}

	@Mock
	private OntAxiomRuleMapper axiomRuleMapper;

	@Mock
	private OntAxiomRuleTargetMapper targetMapper;

	@Mock
	private OntEntityTypeMapper entityTypeMapper;

	@Mock
	private OntEntityTypeLabelMapper entityTypeLabelMapper;

	@Mock
	private OntEntityTypeHierarchyMapper hierarchyMapper;

	@Mock
	private OntEntityTypeDisjointMapper disjointMapper;

	@Mock
	private OntEntityTypeEquivalentMapper equivalentMapper;

	@Mock
	private OntDataPropertyMapper dataPropertyMapper;

	@Mock
	private OntObjectPropertyMapper objectPropertyMapper;

	@Mock
	private OntUnitCategoryMapper unitCategoryMapper;

	private OntAxiomRuleServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OntAxiomRuleServiceImpl(targetMapper, entityTypeMapper, entityTypeLabelMapper,
				hierarchyMapper, disjointMapper, equivalentMapper, dataPropertyMapper, objectPropertyMapper,
				unitCategoryMapper);
		ReflectionTestUtils.setField(service, "baseMapper", axiomRuleMapper);
	}

	@Test
	void shouldCreateExtensionTemplateRule() {
		when(axiomRuleMapper.selectCount(any())).thenReturn(0L);
		when(axiomRuleMapper.insert(any(OntAxiomRule.class))).thenAnswer(invocation -> {
			OntAxiomRule rule = invocation.getArgument(0);
			rule.setId(980001L);
			return 1;
		});
		stubEntityType(940001L);

		OntAxiomRuleCreateDTO request = new OntAxiomRuleCreateDTO();
		request.setRuleCode("EXT_TEST_UNIQUE");
		request.setName("测试唯一性");
		request.setCategory("PROPERTY");
		request.setSubType("UNIQUENESS");
		request.setSeverity("VIOLATION");
		request.setTemplateCode("GLOBAL_UNIQUE_VALUE");
		request.setTargets(List.of(
			buildTarget("TARGET_CLASS", "ENTITY_TYPE", 940001L),
			buildTarget("KEY_PROPERTY", "DATA_PROPERTY", 950006L)));

		stubDataProperty(950006L);

		R<OntAxiomRule> result = service.saveAxiomRule(request);

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData().getStatus()).isEqualTo("ACTIVE");
		assertThat(result.getData().getIsEnabled()).isEqualTo("1");
		assertThat(result.getData().getIsBuiltin()).isEqualTo("0");
		assertThat(result.getData().getSourceType()).isEqualTo("EXTENSION");
	}

	@Test
	void shouldRejectDuplicateRuleCode() {
		when(axiomRuleMapper.selectCount(any())).thenReturn(1L);

		OntAxiomRuleCreateDTO request = new OntAxiomRuleCreateDTO();
		request.setRuleCode("GB8_STANDARD_NUMBER_UNIQUE");
		request.setName("重复代码");
		request.setCategory("PROPERTY");
		request.setSubType("UNIQUENESS");
		request.setSeverity("VIOLATION");
		request.setTemplateCode("GLOBAL_UNIQUE_VALUE");
		request.setTargets(List.of(buildTarget("TARGET_CLASS", "ENTITY_TYPE", 940001L)));

		R<OntAxiomRule> result = service.saveAxiomRule(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("规则代码已存在");
		verify(axiomRuleMapper, never()).insert(any(OntAxiomRule.class));
	}

	@Test
	void shouldRejectBuiltinRuleModification() {
		OntAxiomRule builtin = new OntAxiomRule();
		builtin.setId(970001L);
		builtin.setIsBuiltin("1");
		builtin.setSortOrder(10);
		when(axiomRuleMapper.selectById(970001L)).thenReturn(builtin);

		OntAxiomRuleUpdateDTO request = new OntAxiomRuleUpdateDTO();
		request.setId(970001L);
		request.setName("新名称");
		request.setSortOrder(20);

		R<OntAxiomRule> result = service.updateAxiomRule(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("仅允许修改排序和备注");
	}

	@Test
	void shouldAllowBuiltinRuleSortOrderModification() {
		OntAxiomRule builtin = new OntAxiomRule();
		builtin.setId(970001L);
		builtin.setIsBuiltin("1");
		builtin.setSortOrder(10);
		when(axiomRuleMapper.selectById(970001L)).thenReturn(builtin);

		OntAxiomRuleUpdateDTO request = new OntAxiomRuleUpdateDTO();
		request.setId(970001L);
		request.setSortOrder(20);
		request.setRemarks("更新排序");

		R<OntAxiomRule> result = service.updateAxiomRule(request);

		assertThat(result.isOk()).isTrue();
	}

	@Test
	void shouldRejectBuiltinRuleDeletion() {
		OntAxiomRule builtin = new OntAxiomRule();
		builtin.setId(970001L);
		builtin.setIsBuiltin("1");
		when(axiomRuleMapper.selectById(970001L)).thenReturn(builtin);

		R<Boolean> result = service.removeAxiomRule(970001L);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("内置");
	}

	@Test
	void shouldRejectEnablingBuiltinRule() {
		OntAxiomRule builtin = new OntAxiomRule();
		builtin.setId(970001L);
		builtin.setIsBuiltin("1");
		builtin.setStatus("ACTIVE");
		when(axiomRuleMapper.selectById(970001L)).thenReturn(builtin);

		OntAxiomRuleEnabledDTO request = new OntAxiomRuleEnabledDTO();
		request.setEnabled("0");

		R<OntAxiomRule> result = service.setEnabled(970001L, request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("内置规则");
	}

	@Test
	void shouldRejectCustomDraftEnabling() {
		OntAxiomRule draft = new OntAxiomRule();
		draft.setId(980001L);
		draft.setIsBuiltin("0");
		draft.setStatus("DRAFT");
		when(axiomRuleMapper.selectById(980001L)).thenReturn(draft);

		OntAxiomRuleEnabledDTO request = new OntAxiomRuleEnabledDTO();
		request.setEnabled("1");

		R<OntAxiomRule> result = service.setEnabled(980001L, request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("非ACTIVE");
	}

	@Test
	void shouldRejectDisjointBetweenBuiltinTypes() {
		stubEntityType(940001L, "1");
		stubEntityType(940010L, "1");

		var request = new com.pig4cloud.pig.ontology.dto.OntEntityTypeRelationCreateDTO();
		request.setEntityTypeAId(940001L);
		request.setEntityTypeBId(940010L);

		R<Boolean> result = service.saveDisjoint(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("核心");
	}

	@Test
	void shouldRejectSelfRelation() {
		var request = new com.pig4cloud.pig.ontology.dto.OntEntityTypeRelationCreateDTO();
		request.setEntityTypeAId(940001L);
		request.setEntityTypeBId(940001L);

		R<Boolean> result = service.saveDisjoint(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("自身");
	}

	@Test
	void shouldRejectDeleteCoreDisjoint() {
		R<Boolean> result = service.removeDisjoint(940028L, 940034L);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("核心");
	}

	private OntAxiomRuleTargetDTO buildTarget(String role, String targetType, Long targetId) {
		OntAxiomRuleTargetDTO dto = new OntAxiomRuleTargetDTO();
		dto.setBindingRole(role);
		dto.setTargetType(targetType);
		dto.setTargetId(targetId);
		return dto;
	}

	private void stubEntityType(Long id) {
		stubEntityType(id, "0");
	}

	private void stubEntityType(Long id, String isBuiltin) {
		OntEntityType type = new OntEntityType();
		type.setId(id);
		type.setOntologyId(935001L);
		type.setIsBuiltin(isBuiltin);
		when(entityTypeMapper.selectById(id)).thenReturn(type);
	}

	private void stubDataProperty(Long id) {
		OntDataProperty dp = new OntDataProperty();
		dp.setId(id);
		dp.setOntologyId(935001L);
		when(dataPropertyMapper.selectById(id)).thenReturn(dp);
	}

}
