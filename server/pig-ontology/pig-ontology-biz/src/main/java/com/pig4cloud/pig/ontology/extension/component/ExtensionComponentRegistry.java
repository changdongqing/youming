/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.extension.component;

import com.pig4cloud.pig.ontology.extension.vo.ExtensionComponentVO;
import com.pig4cloud.pig.ontology.validation.reasoner.ReasonerAdapter;
import com.pig4cloud.pig.ontology.validation.reasoner.ReasonerCapability;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扩展组件注册表（只读）。
 *
 * <p>从Spring容器查询所有已注册的 ReasonerAdapter 实现，
 * 展示推理引擎扩展组件信息。本期不实现在线热注册。
 *
 * <p>对应 PRD §4.7.4 推理引擎适配层扩展点。
 *
 * @author youming
 */
@Component
@RequiredArgsConstructor
public class ExtensionComponentRegistry {

	private final ApplicationContext applicationContext;

	/**
	 * 列出已注册的推理引擎适配器。
	 * @return 组件列表
	 */
	public List<ExtensionComponentVO> listRegisteredReasoners() {
		Map<String, ReasonerAdapter> beans = applicationContext.getBeansOfType(ReasonerAdapter.class);
		List<ExtensionComponentVO> result = new ArrayList<>();

		beans.forEach((beanName, adapter) -> {
			ExtensionComponentVO vo = new ExtensionComponentVO();
			vo.setBeanName(beanName);
			vo.setReasonerName(adapter.getReasonerName());
			vo.setCapabilities(adapter.getReasonerCapabilities().stream()
				.map(ReasonerCapability::name)
				.toArray(String[]::new));
			vo.setCheckConsistencySupported(true);
			// inferEntailments 默认抛出 UnsupportedOperationException，本期不支持
			vo.setInferEntailmentsSupported(false);
			result.add(vo);
		});

		return result;
	}

}
