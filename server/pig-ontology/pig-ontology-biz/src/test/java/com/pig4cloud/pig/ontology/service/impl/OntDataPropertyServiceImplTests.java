/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntDataPropertyUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntDataProperty;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyEnum;
import com.pig4cloud.pig.ontology.entity.OntDataPropertyLabel;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyEnumMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntAxiomRuleTargetMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
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
 * 数据属性领域服务单元测试。
 *
 * @author youming
 */
@ExtendWith(MockitoExtension.class)
class OntDataPropertyServiceImplTests {

	@BeforeAll
	static void initializeMybatisTableMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "ontology-data-property-tests");
		TableInfoHelper.initTableInfo(assistant, OntDataProperty.class);
		TableInfoHelper.initTableInfo(assistant, OntDataPropertyLabel.class);
		TableInfoHelper.initTableInfo(assistant, OntDataPropertyEnum.class);
	}

	@Mock
	private OntDataPropertyMapper dataPropertyMapper;

	@Mock
	private OntDataPropertyLabelMapper labelMapper;

	@Mock
	private OntDataPropertyEnumMapper enumMapper;

	@Mock
	private OntEntityTypeMapper entityTypeMapper;

	@Mock
	private OntEntityTypeLabelMapper entityTypeLabelMapper;

	@Mock
	private OntEntityTypeHierarchyMapper hierarchyMapper;

	@Mock
	private OntNamespaceMapper namespaceMapper;

	@Mock
	private OntOntologyProjectMapper ontologyProjectMapper;

	@Mock
	private OntUnitCategoryMapper unitCategoryMapper;

	@Mock
	private OntAxiomRuleTargetMapper axiomRuleTargetMapper;

	@Mock
	private com.pig4cloud.pig.ontology.mapper.OntInstanceDataValueMapper instanceDataValueMapper;

	private OntDataPropertyServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OntDataPropertyServiceImpl(labelMapper, enumMapper, entityTypeMapper,
			entityTypeLabelMapper, hierarchyMapper, namespaceMapper, ontologyProjectMapper, unitCategoryMapper,
			axiomRuleTargetMapper, instanceDataValueMapper);
		ReflectionTestUtils.setField(service, "baseMapper", dataPropertyMapper);
	}

	@Test
	void shouldGenerateIriAndSaveLabelWhenCreatingDataProperty() {
		stubProjectAndExtensionNamespace();
		stubDomainType();
		when(dataPropertyMapper.selectCount(any())).thenReturn(0L);
		when(dataPropertyMapper.insert(any(OntDataProperty.class))).thenAnswer(invocation -> {
			OntDataProperty prop = invocation.getArgument(0);
			prop.setId(10001L);
			return 1;
		});
		when(labelMapper.update(any(), any())).thenReturn(0);

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("vehicleWeight");
		request.setLabel("车辆重量");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("TEXT");
		request.setValueMode("FREE");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData().getIri()).isEqualTo("http://example.com/extension#vehicleWeight");
		assertThat(result.getData().getIriLocalName()).isEqualTo("vehicleWeight");
		assertThat(result.getData().getSourceType()).isEqualTo("EXTENSION");
		assertThat(result.getData().getIsBuiltin()).isEqualTo("0");
	}

	@Test
	void shouldRejectBuiltinNamespaceForExtensionProperty() {
		stubProjectAndBuiltinNamespace();

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(930001L);
		request.setName("vehicleWeight");
		request.setLabel("车辆重量");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("TEXT");
		request.setValueMode("FREE");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("扩展命名空间");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	@Test
	void shouldRejectUnitRefWithoutUnitDictionaryMode() {
		stubProjectAndExtensionNamespace();
		stubDomainType(940067L);
		when(dataPropertyMapper.selectCount(any())).thenReturn(0L);

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("customUnit");
		request.setLabel("自定义单位");
		request.setDomainEntityTypeId(940067L);
		request.setBaseType("UNIT_REF");
		request.setValueMode("FREE");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("UNIT_DICTIONARY");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	@Test
	void shouldRejectClosedEnumWithoutEnumValues() {
		stubProjectAndExtensionNamespace();
		stubDomainType();
		when(dataPropertyMapper.selectCount(any())).thenReturn(0L);

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("customEnum");
		request.setLabel("自定义枚举");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("TEXT");
		request.setValueMode("CLOSED_ENUM");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("至少需要一个枚举值");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	@Test
	void shouldRejectBooleanWithRegexPattern() {
		stubProjectAndExtensionNamespace();
		stubDomainType();
		when(dataPropertyMapper.selectCount(any())).thenReturn(0L);

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("activeFlag");
		request.setLabel("是否启用");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("BOOLEAN");
		request.setValueMode("FREE");
		request.setRegexPattern("^[01]$");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("BOOLEAN").contains("正则");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	@Test
	void shouldRejectIriNotMatchingNamespaceUriPlusLocalName() {
		stubProjectAndExtensionNamespace();
		stubDomainType();

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("vehicleWeight");
		request.setIri("http://wrong.example/vehicleWeight");
		request.setLabel("车辆重量");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("TEXT");
		request.setValueMode("FREE");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("IRI必须等于");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	@Test
	void shouldRejectBuiltinPropertySemanticFieldModification() {
		OntDataProperty builtin = new OntDataProperty();
		builtin.setId(950001L);
		builtin.setIsBuiltin("1");
		builtin.setSortOrder(10);
		when(dataPropertyMapper.selectById(950001L)).thenReturn(builtin);

		OntDataPropertyUpdateDTO request = new OntDataPropertyUpdateDTO();
		request.setId(950001L);
		request.setLabel("新编制目的");
		request.setBaseType("NUMERIC");

		R<OntDataProperty> result = service.updateDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("语义字段不可修改");
	}

	@Test
	void shouldAllowBuiltinPropertySortOrderAndRemarksModification() {
		OntDataProperty builtin = new OntDataProperty();
		builtin.setId(950001L);
		builtin.setIsBuiltin("1");
		builtin.setSortOrder(10);
		when(dataPropertyMapper.selectById(950001L)).thenReturn(builtin);
		when(labelMapper.update(any(), any())).thenReturn(1);

		OntDataPropertyUpdateDTO request = new OntDataPropertyUpdateDTO();
		request.setId(950001L);
		request.setLabel("编制目的");
		request.setSortOrder(20);
		request.setRemarks("更新排序");

		R<OntDataProperty> result = service.updateDataProperty(request);

		assertThat(result.isOk()).isTrue();
	}

	@Test
	void shouldRejectDomainTypeFromDifferentOntology() {
		stubProjectAndExtensionNamespace();
		OntEntityType domainType = new OntEntityType();
		domainType.setId(940001L);
		domainType.setOntologyId(999999L);
		when(entityTypeMapper.selectById(940001L)).thenReturn(domainType);

		OntDataPropertyCreateDTO request = new OntDataPropertyCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("crossOntology");
		request.setLabel("跨工程");
		request.setDomainEntityTypeId(940001L);
		request.setBaseType("TEXT");
		request.setValueMode("FREE");

		R<OntDataProperty> result = service.saveDataProperty(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("同一本体工程");
		verify(dataPropertyMapper, never()).insert(any(OntDataProperty.class));
	}

	private void stubProjectAndExtensionNamespace() {
		OntOntologyProject project = new OntOntologyProject();
		project.setId(935001L);
		when(ontologyProjectMapper.selectById(935001L)).thenReturn(project);

		OntNamespace namespace = new OntNamespace();
		namespace.setId(20001L);
		namespace.setUri("http://example.com/extension#");
		namespace.setIsBuiltin("0");
		when(namespaceMapper.selectById(20001L)).thenReturn(namespace);
	}

	private void stubProjectAndBuiltinNamespace() {
		OntOntologyProject project = new OntOntologyProject();
		project.setId(935001L);
		when(ontologyProjectMapper.selectById(935001L)).thenReturn(project);

		OntNamespace namespace = new OntNamespace();
		namespace.setId(930001L);
		namespace.setUri("http://example.org/standard-ontology#");
		namespace.setIsBuiltin("1");
		when(namespaceMapper.selectById(930001L)).thenReturn(namespace);
	}

	private void stubDomainType() {
		stubDomainType(940001L);
	}

	private void stubDomainType(Long domainTypeId) {
		OntEntityType domainType = new OntEntityType();
		domainType.setId(domainTypeId);
		domainType.setOntologyId(935001L);
		when(entityTypeMapper.selectById(domainTypeId)).thenReturn(domainType);
	}

}
