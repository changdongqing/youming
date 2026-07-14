/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.version.serialization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.entity.OntOntologyProject;
import com.pig4cloud.pig.ontology.mapper.OntOntologyProjectMapper;
import com.pig4cloud.pig.ontology.version.entity.OntOntologyVersion;
import com.pig4cloud.pig.ontology.version.mapper.OntOntologyVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;

/**
 * 模块 10 版本声明贡献器。
 * <p>
 * 在 OntologyModelExporter 完成 Model 组装后，输出本体头声明：
 * owl:Ontology、owl:versionIRI、owl:priorVersion、owl:backwardCompatibleWith、owl:incompatibleWith。
 *
 * @author youming
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OntologyVersionDeclarationContributor {

	private final OntOntologyProjectMapper projectMapper;

	private final OntOntologyVersionMapper versionMapper;

	/**
	 * 向 Model 添加版本声明。
	 * @param model Jena Model
	 * @param ontologyId 本体工程ID
	 * @param versionId 版本ID（null 表示当前工作区版本）
	 */
	public void contribute(Model model, Long ontologyId, Long versionId) {
		OntOntologyProject project = projectMapper.selectById(ontologyId);
		if (project == null || project.getOntologyIri() == null) {
			return;
		}

		String ontologyIri = project.getOntologyIri();
		Resource ontologyResource = model.createResource(ontologyIri);
		model.add(ontologyResource, RDF.type, OWL.Ontology);

		// 确定使用哪个版本
		OntOntologyVersion version = null;
		if (versionId != null) {
			version = versionMapper.selectById(versionId);
		}
		else if (project.getCurrentVersionId() != null) {
			version = versionMapper.selectById(project.getCurrentVersionId());
		}

		if (version == null) {
			// 无已发布版本，只输出 owl:Ontology，不伪造 versionIRI
			return;
		}

		// 输出 owl:versionIRI
		if (version.getVersionIri() != null) {
			model.add(ontologyResource, OWL.versionIRI, model.createResource(version.getVersionIri()));
		}

		// 输出 owl:priorVersion
		if (version.getPriorVersionId() != null) {
			OntOntologyVersion priorVersion = versionMapper.selectById(version.getPriorVersionId());
			if (priorVersion != null && priorVersion.getVersionIri() != null) {
				model.add(ontologyResource, OWL.priorVersion, model.createResource(priorVersion.getVersionIri()));
			}
		}

		// 兼容性谓词
		String compatibility = version.getCompatibility();
		if ("BACKWARD_COMPATIBLE".equals(compatibility) && version.getPriorVersionId() != null) {
			OntOntologyVersion priorVersion = versionMapper.selectById(version.getPriorVersionId());
			if (priorVersion != null && priorVersion.getVersionIri() != null) {
				model.add(ontologyResource, OWL.backwardCompatibleWith,
						model.createResource(priorVersion.getVersionIri()));
			}
		}
		else if ("BREAKING".equals(compatibility) && version.getPriorVersionId() != null) {
			OntOntologyVersion priorVersion = versionMapper.selectById(version.getPriorVersionId());
			if (priorVersion != null && priorVersion.getVersionIri() != null) {
				model.add(ontologyResource, OWL.incompatibleWith,
						model.createResource(priorVersion.getVersionIri()));
			}
		}
		// PATCH_ONLY 不自动等同于某个 OWL 兼容性谓词
	}

}
