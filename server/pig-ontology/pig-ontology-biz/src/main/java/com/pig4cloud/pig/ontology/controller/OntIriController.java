/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.entity.OntNamespace;
import com.pig4cloud.pig.ontology.service.OntNamespaceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * IRI 生成与唯一性校验。
 *
 * @author youming
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ontology/iris")
@Tag(description = "ontology-iri", name = "IRI管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class OntIriController {

	private static final Pattern LOCAL_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");

	private final OntNamespaceService ontNamespaceService;

	/**
	 * 校验IRI唯一性。
	 * @param iri 完整IRI
	 * @return 唯一性结果
	 */
	@GetMapping("/validate")
	@HasPermission("ontology_namespace_view")
	public R validate(@RequestParam String iri) {
		if (!StringUtils.hasText(iri)) {
			return R.failed("IRI不能为空");
		}
		// 首期校验：检查入参IRI是否与已有命名空间URI完全重复。
		// 完整全局IRI唯一性校验（实体类型/属性/实例级）随各模块上线后逐步补齐。
		long count = ontNamespaceService.count(Wrappers.<OntNamespace>lambdaQuery().eq(OntNamespace::getUri, iri));
		if (count > 0) {
			return R.ok(false, "IRI与已有命名空间URI冲突");
		}
		return R.ok(true, "IRI可用");
	}

	/**
	 * 根据命名空间和本地标识符生成IRI。
	 * @param request 生成请求
	 * @return 完整IRI
	 */
	@PostMapping("/generate")
	@HasPermission("ontology_namespace_view")
	public R<String> generate(@RequestBody IriGenerateRequest request) {
		if (request.getNamespaceId() == null) {
			return R.failed("命名空间ID不能为空");
		}
		OntNamespace namespace = ontNamespaceService.getById(request.getNamespaceId());
		if (namespace == null) {
			return R.failed("命名空间不存在");
		}
		if (!StringUtils.hasText(request.getLocalName())) {
			return R.failed("本地标识符不能为空");
		}
		if (!LOCAL_NAME_PATTERN.matcher(request.getLocalName()).matches()) {
			return R.failed("本地标识符仅支持英文字母、数字和下划线，且必须以字母开头");
		}
		String localName = request.getLocalName();
		if ("PROPERTY".equalsIgnoreCase(request.getElementType())) {
			localName = localName.substring(0, 1).toLowerCase() + localName.substring(1);
		} else if ("ENTITY_TYPE".equalsIgnoreCase(request.getElementType())) {
			localName = localName.substring(0, 1).toUpperCase() + localName.substring(1);
		}
		String iri = namespace.getUri() + localName;
		return R.ok(iri);
	}

	/**
	 * IRI生成请求。
	 */
	@Data
	public static class IriGenerateRequest {

		private Long namespaceId;

		private String localName;

		private String elementType;

	}

}
