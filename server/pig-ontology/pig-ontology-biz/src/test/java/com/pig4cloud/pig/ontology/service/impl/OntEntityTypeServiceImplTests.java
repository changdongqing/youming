/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeCreateDTO;
import com.pig4cloud.pig.ontology.dto.OntEntityTypeUpdateDTO;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeHierarchy;
import com.pig4cloud.pig.ontology.entity.OntEntityTypeLabel;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeDisjointMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeEquivalentMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeHierarchyMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeLabelMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import com.pig4cloud.pig.ontology.vo.OntEntityTypeTreeNode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 实体类型领域服务单元测试。
 *
 * @author youming
 */
@ExtendWith(MockitoExtension.class)
class OntEntityTypeServiceImplTests {

	@BeforeAll
	static void initializeMybatisTableMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "ontology-service-tests");
		TableInfoHelper.initTableInfo(assistant, OntEntityType.class);
		TableInfoHelper.initTableInfo(assistant, OntEntityTypeLabel.class);
		TableInfoHelper.initTableInfo(assistant, OntEntityTypeHierarchy.class);
	}

	@Mock
	private OntEntityTypeMapper entityTypeMapper;

	@Mock
	private OntEntityTypeLabelMapper labelMapper;

	@Mock
	private OntEntityTypeHierarchyMapper hierarchyMapper;

	@Mock
	private OntEntityTypeDisjointMapper disjointMapper;

	@Mock
	private OntEntityTypeEquivalentMapper equivalentMapper;

	@Mock
	private OntNamespaceMapper namespaceMapper;

	@Mock
	private OntOntologyProjectMapper ontologyProjectMapper;

	private OntEntityTypeServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OntEntityTypeServiceImpl(labelMapper, hierarchyMapper, disjointMapper, equivalentMapper,
			namespaceMapper, ontologyProjectMapper);
		ReflectionTestUtils.setField(service, "baseMapper", entityTypeMapper);
	}

	@Test
	void shouldGenerateIriAndSaveChineseLabelWhenCreatingEntityType() {
		stubProjectAndExtensionNamespace();
		when(entityTypeMapper.selectCount(any())).thenReturn(0L);
		when(entityTypeMapper.insert(any(OntEntityType.class))).thenAnswer(invocation -> {
			OntEntityType entityType = invocation.getArgument(0);
			entityType.setId(10001L);
			return 1;
		});
		when(labelMapper.update(isNull(), any())).thenReturn(0);

		OntEntityTypeCreateDTO request = new OntEntityTypeCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("Vehicle");
		request.setLabel("车辆");
		request.setParentIds(List.of());

		R<OntEntityType> result = service.saveEntityType(request);

		assertThat(result.isOk()).isTrue();
		assertThat(result.getData().getIri()).isEqualTo("http://example.com/extension#Vehicle");
		assertThat(result.getData().getOntologyId()).isEqualTo(OntEntityTypeService.CORE_ONTOLOGY_ID);
		ArgumentCaptor<OntEntityTypeLabel> labelCaptor = ArgumentCaptor.forClass(OntEntityTypeLabel.class);
		verify(labelMapper).insert(labelCaptor.capture());
		assertThat(labelCaptor.getValue().getEntityTypeId()).isEqualTo(10001L);
		assertThat(labelCaptor.getValue().getLocale()).isEqualTo("zh");
		assertThat(labelCaptor.getValue().getLabel()).isEqualTo("车辆");
	}

	@Test
	void shouldRejectRequestIriThatDoesNotMatchNamespaceAndName() {
		stubProjectAndExtensionNamespace();
		OntEntityTypeCreateDTO request = new OntEntityTypeCreateDTO();
		request.setNamespaceId(20001L);
		request.setName("Vehicle");
		request.setIri("http://wrong.example/Vehicle");
		request.setLabel("车辆");

		R<OntEntityType> result = service.saveEntityType(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("IRI必须等于");
		verify(entityTypeMapper, never()).insert(any(OntEntityType.class));
	}

	@Test
	void shouldUpdateExistingLabelByCompositeKeyCondition() {
		OntEntityType old = extensionType(10001L, "Vehicle");
		when(entityTypeMapper.selectById(10001L)).thenReturn(old);
		stubProjectAndExtensionNamespace();
		when(entityTypeMapper.selectCount(any())).thenReturn(0L);
		when(entityTypeMapper.update(isNull(), any())).thenReturn(1);
		when(labelMapper.update(isNull(), any())).thenReturn(1);

		OntEntityTypeUpdateDTO request = updateRequest(10001L, "Vehicle", "车辆类型");
		R<OntEntityType> result = service.updateEntityType(request);

		assertThat(result.isOk()).isTrue();
		verify(labelMapper).update(isNull(), any());
		verify(labelMapper, never()).insert(any(OntEntityTypeLabel.class));
	}

	@Test
	void shouldRejectIndirectInheritanceCycle() {
		OntEntityType child = extensionType(10L, "Child");
		OntEntityType parent = extensionType(20L, "Parent");
		when(entityTypeMapper.selectById(10L)).thenReturn(child);
		stubProjectAndExtensionNamespace();
		when(entityTypeMapper.selectCount(any())).thenReturn(0L);
		when(entityTypeMapper.selectByIds(any())).thenReturn(List.of(parent));
		when(hierarchyMapper.selectList(isNull())).thenReturn(List.of(hierarchy(10L, 20L)));

		OntEntityTypeUpdateDTO request = updateRequest(10L, "Child", "子类");
		request.setParentIds(List.of(20L));
		R<OntEntityType> result = service.updateEntityType(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("继承关系存在环");
		verify(entityTypeMapper, never()).update(any(), any());
	}

	@Test
	void shouldFailClosedWhenInheritanceDepthExceedsLimit() {
		OntEntityType child = extensionType(10L, "Child");
		OntEntityType parent = extensionType(20L, "Parent");
		when(entityTypeMapper.selectById(10L)).thenReturn(child);
		stubProjectAndExtensionNamespace();
		when(entityTypeMapper.selectCount(any())).thenReturn(0L);
		when(entityTypeMapper.selectByIds(any())).thenReturn(List.of(parent));
		List<OntEntityTypeHierarchy> deepHierarchy = new ArrayList<>();
		for (long id = 20; id <= 121; id++) {
			deepHierarchy.add(hierarchy(id + 1, id));
		}
		when(hierarchyMapper.selectList(isNull())).thenReturn(deepHierarchy);

		OntEntityTypeUpdateDTO request = updateRequest(10L, "Child", "子类");
		request.setParentIds(List.of(20L));
		R<OntEntityType> result = service.updateEntityType(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("超过100层");
	}

	@Test
	void shouldBuildClauseTwiceWithDifferentPathKeysForMultipleInheritance() {
		OntEntityType structural = builtinType(940038L, "StructuralElement", 10);
		OntEntityType informationUnit = builtinType(940048L, "InformationUnit", 20);
		OntEntityType clause = builtinType(940040L, "Clause", 30);
		when(entityTypeMapper.selectList(any())).thenReturn(List.of(structural, informationUnit, clause));
		when(hierarchyMapper.selectList(any())).thenReturn(List.of(
			hierarchy(940038L, 940040L), hierarchy(940048L, 940040L)));
		when(labelMapper.selectList(any())).thenReturn(List.of(
			label(940038L, "层次"), label(940048L, "信息单元"), label(940040L, "条款")));

		List<OntEntityTypeTreeNode> tree = service.tree(OntEntityTypeService.CORE_ONTOLOGY_ID);

		assertThat(tree).hasSize(2);
		List<OntEntityTypeTreeNode> clauseNodes = tree.stream().flatMap(root -> root.getChildren().stream()).toList();
		assertThat(clauseNodes).extracting(OntEntityTypeTreeNode::getId).containsExactly(940040L, 940040L);
		assertThat(clauseNodes).extracting(OntEntityTypeTreeNode::getKey).doesNotHaveDuplicates();
	}

	private void stubProjectAndExtensionNamespace() {
		OntOntologyProject project = new OntOntologyProject();
		project.setId(OntEntityTypeService.CORE_ONTOLOGY_ID);
		when(ontologyProjectMapper.selectById(OntEntityTypeService.CORE_ONTOLOGY_ID)).thenReturn(project);
		OntNamespace namespace = new OntNamespace();
		namespace.setId(20001L);
		namespace.setUri("http://example.com/extension#");
		namespace.setIsBuiltin("0");
		when(namespaceMapper.selectById(20001L)).thenReturn(namespace);
	}

	private OntEntityTypeUpdateDTO updateRequest(Long id, String name, String label) {
		OntEntityTypeUpdateDTO request = new OntEntityTypeUpdateDTO();
		request.setId(id);
		request.setNamespaceId(20001L);
		request.setName(name);
		request.setLabel(label);
		request.setIsAbstract("0");
		request.setParentIds(List.of());
		request.setSortOrder(0);
		return request;
	}

	private OntEntityType extensionType(Long id, String name) {
		OntEntityType type = new OntEntityType();
		type.setId(id);
		type.setName(name);
		type.setIri("http://example.com/extension#" + name);
		type.setOntologyId(OntEntityTypeService.CORE_ONTOLOGY_ID);
		type.setNamespaceId(20001L);
		type.setIsBuiltin("0");
		type.setIsAbstract("0");
		type.setSortOrder(0);
		return type;
	}

	private OntEntityType builtinType(Long id, String name, int sortOrder) {
		OntEntityType type = extensionType(id, name);
		type.setIsBuiltin("1");
		type.setSortOrder(sortOrder);
		return type;
	}

	private OntEntityTypeHierarchy hierarchy(Long parentId, Long childId) {
		OntEntityTypeHierarchy hierarchy = new OntEntityTypeHierarchy();
		hierarchy.setParentId(parentId);
		hierarchy.setChildId(childId);
		return hierarchy;
	}

	private OntEntityTypeLabel label(Long id, String text) {
		OntEntityTypeLabel label = new OntEntityTypeLabel();
		label.setEntityTypeId(id);
		label.setLocale("zh");
		label.setLabel(text);
		return label;
	}

}
