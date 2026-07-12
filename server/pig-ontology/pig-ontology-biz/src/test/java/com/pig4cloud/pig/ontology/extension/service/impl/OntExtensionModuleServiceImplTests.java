/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleCreateDTO;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleUpdateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.export.ExtensionExporter;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationReport;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidator;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
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
 * 扩展模块服务单元测试。
 *
 * @author youming
 */
@ExtendWith(MockitoExtension.class)
class OntExtensionModuleServiceImplTests {

	@BeforeAll
	static void initializeMybatisTableMetadata() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(),
				"extension-module-tests");
		TableInfoHelper.initTableInfo(assistant, OntExtensionModule.class);
		TableInfoHelper.initTableInfo(assistant, OntExtensionResource.class);
		TableInfoHelper.initTableInfo(assistant, OntNamespace.class);
	}

	@Mock
	private OntExtensionModuleMapper moduleMapper;

	@Mock
	private OntNamespaceMapper namespaceMapper;

	@Mock
	private OntExtensionResourceMapper resourceMapper;

	@Mock
	private ExtensionValidator validator;

	@Mock
	private ExtensionExporter exporter;

	private OntExtensionModuleServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new OntExtensionModuleServiceImpl(namespaceMapper, resourceMapper, validator, exporter);
		ReflectionTestUtils.setField(service, "baseMapper", moduleMapper);
	}

	@Test
	void shouldRejectCoreNamespace() {
		ExtensionModuleCreateDTO request = new ExtensionModuleCreateDTO();
		request.setModuleCode("test-module");
		request.setModuleName("测试模块");
		request.setNamespaceId(930001L);

		OntNamespace coreNs = new OntNamespace();
		coreNs.setId(930001L);
		coreNs.setIsBuiltin("1");
		when(namespaceMapper.selectById(930001L)).thenReturn(coreNs);

		R<OntExtensionModule> result = service.saveModule(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("扩展命名空间");
		verify(validator, never()).validateModule(any());
	}

	@Test
	void shouldRejectDuplicateModuleCode() {
		ExtensionModuleCreateDTO request = new ExtensionModuleCreateDTO();
		request.setModuleCode("medical");
		request.setModuleName("医疗扩展");
		request.setNamespaceId(930007L);

		when(service.count(any())).thenReturn(1L);

		R<OntExtensionModule> result = service.saveModule(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("模块代码已存在");
	}

	@Test
	void shouldRejectInvalidModuleCodeFormat() {
		ExtensionModuleCreateDTO request = new ExtensionModuleCreateDTO();
		request.setModuleCode("Medical");
		request.setModuleName("医疗扩展");
		request.setNamespaceId(930007L);

		R<OntExtensionModule> result = service.saveModule(request);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("模块代码");
	}

	@Test
	void shouldRejectDeletingBuiltinModule() {
		OntExtensionModule module = new OntExtensionModule();
		module.setId(980001L);
		module.setIsBuiltin("1");
		when(moduleMapper.selectById(980001L)).thenReturn(module);

		R<Boolean> result = service.removeModule(980001L);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("内置");
	}

	@Test
	void shouldRejectNamespaceChangeWhenResourcesExist() {
		OntExtensionModule old = new OntExtensionModule();
		old.setId(980001L);
		old.setIsBuiltin("0");
		old.setNamespaceId(930007L);

		ExtensionModuleUpdateDTO update = new ExtensionModuleUpdateDTO();
		update.setId(980001L);
		update.setNamespaceId(930008L);

		when(moduleMapper.selectById(980001L)).thenReturn(old);
		when(resourceMapper.selectCount(any())).thenReturn(3L);

		R<OntExtensionModule> result = service.updateModule(update);

		assertThat(result.isOk()).isFalse();
		assertThat(result.getMsg()).contains("命名空间不可修改");
	}

}
