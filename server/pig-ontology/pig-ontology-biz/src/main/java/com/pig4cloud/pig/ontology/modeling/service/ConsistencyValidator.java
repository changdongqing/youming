package com.pig4cloud.pig.ontology.modeling.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

/**
 * 往返一致性校验 Service（FR-16.5，AC-16.5）
 * <p>
 * 序列化 -> 解析 -> 再序列化，用 Jena Model.isIsomorphicWith() 比对图同构。
 * 忽略 IRI 顺序差异（Jena Model 无序，isIsomorphicWith 天然处理）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Service
public class ConsistencyValidator {

	private final SerializationService serializationService;

	/**
	 * 校验往返一致性（AC-16.5）
	 * @return true=图同构（一致性通过）
	 */
	public boolean validate(Long projectId) {
		try {
			// 1. 第一次序列化
			String ttl1 = serializationService.preview(projectId, "TTL").getContent();

			// 2. 解析回 Model
			Model model1 = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model1, new ByteArrayInputStream(ttl1.getBytes()), RDFLanguages.TURTLE);

			// 3. 再序列化
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			RDFDataMgr.write(out, model1, RDFLanguages.TURTLE);
			String ttl2 = out.toString();

			// 4. 解析第二次序列化
			Model model2 = ModelFactory.createDefaultModel();
			RDFDataMgr.read(model2, new ByteArrayInputStream(ttl2.getBytes()), RDFLanguages.TURTLE);

			// 5. 图同构比对（忽略顺序差异）
			return model1.isIsomorphicWith(model2);
		}
		catch (Exception e) {
			log.error("往返一致性校验失败: projectId={}", projectId, e);
			return false;
		}
	}

}
