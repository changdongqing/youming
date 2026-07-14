/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntEntityType;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.entity.OntUnit;
import com.pig4cloud.pig.ontology.event.service.OntDomainEventPublisher;
import com.pig4cloud.pig.ontology.mapper.OntEntityTypeMapper;
import com.pig4cloud.pig.ontology.mapper.OntEntityInstanceMapper;
import com.pig4cloud.pig.ontology.mapper.OntDataPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntObjectPropertyMapper;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.mapper.OntUnitMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 命名空间与实体类型引用保护测试。
 *
 * @author youming
 */
@ExtendWith(MockitoExtension.class)
class OntNamespaceServiceImplTests {

	@BeforeAll
	static void initializeMybatisTableMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "namespace-service-tests");
		TableInfoHelper.initTableInfo(assistant, OntNamespace.class);
		TableInfoHelper.initTableInfo(assistant, OntEntityType.class);
		TableInfoHelper.initTableInfo(assistant, OntUnit.class);
	}

	@Mock
	private OntNamespaceMapper namespaceMapper;

	@Mock
	private OntUnitMapper unitMapper;

	@Mock
	private OntEntityTypeMapper entityTypeMapper;

	@Mock
	private OntDataPropertyMapper dataPropertyMapper;

	@Mock
	private OntObjectPropertyMapper objectPropertyMapper;

	@Mock
	private OntEntityInstanceMapper entityInstanceMapper;

	@Mock
	private OntDomainEventPublisher eventPublisher;

	private OntNamespaceServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OntNamespaceServiceImpl(unitMapper, entityTypeMapper, dataPropertyMapper, objectPropertyMapper,
				entityInstanceMapper, eventPublisher);
		ReflectionTestUtils.setField(service, "baseMapper", namespaceMapper);
	}

	@Test
	void shouldRejectDeletingNamespaceReferencedByEntityType() {
		OntNamespace namespace = extensionNamespace();
		when(namespaceMapper.selectById(20001L)).thenReturn(namespace);
		when(unitMapper.selectCount(any())).thenReturn(0L);
		when(entityTypeMapper.selectCount(any())).thenReturn(1L);

		R<Boolean> result = service.removeNamespace(20001L);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("实体类型引用");
		verify(namespaceMapper, never()).deleteById(20001L);
	}

	@Test
	void shouldRejectChangingReferencedNamespaceUri() {
		OntNamespace old = extensionNamespace();
		when(namespaceMapper.selectById(20001L)).thenReturn(old);
		when(entityTypeMapper.selectCount(any())).thenReturn(1L);
		OntNamespace update = extensionNamespace();
		update.setUri("http://changed.example.com/extension#");

		R<OntNamespace> result = service.updateNamespace(update);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("URI不可修改");
		verify(namespaceMapper, never()).update(any(), any());
	}

	private OntNamespace extensionNamespace() {
		OntNamespace namespace = new OntNamespace();
		namespace.setId(20001L);
		namespace.setPrefix("ext");
		namespace.setUri("http://example.com/extension#");
		namespace.setIsBuiltin("0");
		namespace.setSortOrder(0);
		return namespace;
	}

}
