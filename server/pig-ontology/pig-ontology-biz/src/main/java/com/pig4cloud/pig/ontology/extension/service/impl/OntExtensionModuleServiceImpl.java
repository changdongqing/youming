/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleCreateDTO;
import com.pig4cloud.pig.ontology.extension.dto.ExtensionModuleUpdateDTO;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionModule;
import com.pig4cloud.pig.ontology.extension.entity.OntExtensionResource;
import com.pig4cloud.pig.ontology.extension.export.ExtensionExporter;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionModuleMapper;
import com.pig4cloud.pig.ontology.extension.mapper.OntExtensionResourceMapper;
import com.pig4cloud.pig.ontology.extension.service.OntExtensionModuleService;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidationReport;
import com.pig4cloud.pig.ontology.extension.validator.ExtensionValidator;
import com.pig4cloud.pig.ontology.extension.vo.ExtensionModuleDetailVO;
import com.pig4cloud.pig.ontology.mapper.OntNamespaceMapper;
import com.pig4cloud.pig.ontology.service.OntEntityTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 扩展模块服务实现。
 *
 * @author youming
 */
@Service
@RequiredArgsConstructor
public class OntExtensionModuleServiceImpl extends ServiceImpl<OntExtensionModuleMapper, OntExtensionModule>
		implements OntExtensionModuleService {

	private static final String BUILTIN = "1";

	private static final String EXTENSION = "0";

	private static final Pattern MODULE_CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]*$");

	private final OntNamespaceMapper namespaceMapper;

	private final OntExtensionResourceMapper resourceMapper;

	private final ExtensionValidator validator;

	private final ExtensionExporter exporter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntExtensionModule> saveModule(ExtensionModuleCreateDTO request) {
		if (!StringUtils.hasText(request.getModuleCode())) {
			return R.failed("模块代码不能为空");
		}
		if (!MODULE_CODE_PATTERN.matcher(request.getModuleCode()).matches()) {
			return R.failed("模块代码仅支持小写英文、数字、下划线和中划线，且必须以小写英文开头");
		}

		long codeCount = this.count(Wrappers.<OntExtensionModule>lambdaQuery()
			.eq(OntExtensionModule::getModuleCode, request.getModuleCode()));
		if (codeCount > 0) {
			return R.failed("模块代码已存在");
		}

		OntNamespace namespace = namespaceMapper.selectById(request.getNamespaceId());
		if (namespace == null) {
			return R.failed("命名空间不存在");
		}
		if (BUILTIN.equals(namespace.getIsBuiltin())) {
			return R.failed("扩展模块必须绑定扩展命名空间，不可绑定核心命名空间");
		}

		OntExtensionModule module = new OntExtensionModule();
		module.setModuleCode(request.getModuleCode());
		module.setModuleName(request.getModuleName());
		module.setNamespaceId(request.getNamespaceId());
		module.setOntologyId(request.getOntologyId() != null
				? request.getOntologyId() : OntEntityTypeService.CORE_ONTOLOGY_ID);
		module.setDescription(request.getDescription());
		module.setVersion(request.getVersion());
		module.setIsBuiltin(EXTENSION);
		module.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
		module.setRemarks(request.getRemarks());

		ExtensionValidationReport report = validator.validateModule(module);
		if (!report.getConforms()) {
			return R.failed("扩展合法性校验未通过");
		}

		this.save(module);
		return R.ok(module);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<OntExtensionModule> updateModule(ExtensionModuleUpdateDTO request) {
		if (request.getId() == null) {
			return R.failed("模块ID不能为空");
		}
		OntExtensionModule old = this.getById(request.getId());
		if (old == null) {
			return R.failed("扩展模块不存在");
		}

		if (BUILTIN.equals(old.getIsBuiltin())) {
			return R.failed("内置扩展模块不可修改");
		}

		if (request.getNamespaceId() != null && !request.getNamespaceId().equals(old.getNamespaceId())) {
			long resourceCount = resourceMapper.selectCount(Wrappers.<OntExtensionResource>lambdaQuery()
				.eq(OntExtensionResource::getModuleId, old.getId())
				.eq(OntExtensionResource::getDelFlag, EXTENSION));
			if (resourceCount > 0) {
				return R.failed("该模块已关联" + resourceCount + "个资源，命名空间不可修改（请先解除资源关联）");
			}
		}

		OntExtensionModule update = new OntExtensionModule();
		update.setId(old.getId());
		update.setModuleName(request.getModuleName());
		update.setNamespaceId(request.getNamespaceId());
		update.setDescription(request.getDescription());
		update.setVersion(request.getVersion());
		update.setSortOrder(request.getSortOrder());
		update.setRemarks(request.getRemarks());
		this.updateById(update);
		return R.ok(this.getById(old.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R<Boolean> removeModule(Long id) {
		OntExtensionModule module = this.getById(id);
		if (module == null) {
			return R.failed("扩展模块不存在");
		}
		if (BUILTIN.equals(module.getIsBuiltin())) {
			return R.failed("内置扩展模块不可删除");
		}

		// 逻辑删除关联资源（不删除资源本身）
		resourceMapper.update(null, Wrappers.<OntExtensionResource>lambdaUpdate()
			.eq(OntExtensionResource::getModuleId, id)
			.set(OntExtensionResource::getDelFlag, "1"));

		return R.ok(this.removeById(id));
	}

	@Override
	public ExtensionModuleDetailVO getDetail(Long id) {
		OntExtensionModule module = this.getById(id);
		if (module == null) {
			return null;
		}
		ExtensionModuleDetailVO vo = new ExtensionModuleDetailVO();
		vo.setId(module.getId());
		vo.setModuleCode(module.getModuleCode());
		vo.setModuleName(module.getModuleName());
		vo.setNamespaceId(module.getNamespaceId());

		OntNamespace ns = namespaceMapper.selectById(module.getNamespaceId());
		if (ns != null) {
			vo.setNamespacePrefix(ns.getPrefix());
			vo.setNamespaceUri(ns.getUri());
		}

		vo.setOntologyId(module.getOntologyId());
		vo.setDescription(module.getDescription());
		vo.setVersion(module.getVersion());
		vo.setIsBuiltin(module.getIsBuiltin());
		vo.setSortOrder(module.getSortOrder());
		vo.setRemarks(module.getRemarks());
		vo.setCreateTime(module.getCreateTime());

		long resourceCount = resourceMapper.selectCount(Wrappers.<OntExtensionResource>lambdaQuery()
			.eq(OntExtensionResource::getModuleId, id)
			.eq(OntExtensionResource::getDelFlag, EXTENSION));
		vo.setResourceCount(resourceCount);

		return vo;
	}

	@Override
	public ExtensionValidationReport validateModule(Long id) {
		OntExtensionModule module = this.getById(id);
		if (module == null) {
			return null;
		}
		return validator.validateModule(module);
	}

	@Override
	public String exportModule(Long id, String format) {
		OntExtensionModule module = this.getById(id);
		if (module == null) {
			return null;
		}
		return exporter.exportModule(module, format);
	}

	@Override
	public String getExportFilename(Long id, String format) {
		OntExtensionModule module = this.getById(id);
		if (module == null) {
			return "extension." + format.toLowerCase().replace("-", "");
		}
		String ext = switch (format) {
			case "TURTLE" -> "ttl";
			case "JSON-LD" -> "jsonld";
			case "RDF-XML" -> "rdf";
			case "N-TRIPLES" -> "nt";
			default -> "txt";
		};
		return module.getModuleCode() + "_extension." + ext;
	}

}
