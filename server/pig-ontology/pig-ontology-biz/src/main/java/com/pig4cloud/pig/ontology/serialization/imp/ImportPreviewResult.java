/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.serialization.imp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入预检结果。
 *
 * @author youming
 */
@Data
public class ImportPreviewResult {

	private Long ontologyId;

	private List<ImportInstance> instances = new ArrayList<>();

	private int schemaSkipped;

	private int conflictCount;

	private List<ImportIssue> errors = new ArrayList<>();

	private List<ImportIssue> warnings = new ArrayList<>();

	public void incrementSchemaSkipped() {
		schemaSkipped++;
	}

	public void incrementConflictCount() {
		conflictCount++;
	}

	public void addError(String instanceIri, String message) {
		errors.add(new ImportIssue(instanceIri, message));
	}

	public void addWarning(String instanceIri, String message) {
		warnings.add(new ImportIssue(instanceIri, message));
	}

	/**
	 * 导入实例（内部模型）。
	 */
	@Data
	public static class ImportInstance {

		private String iri;

		private String label;

		private Long rdfTypeId;

		private String rdfTypeIri;

		/** null=无冲突, IRI_EXISTS=IRI已存在 */
		private String conflictType;

		private List<ImportDataValue> dataValues = new ArrayList<>();

		private List<ImportObjectRelation> objectRelations = new ArrayList<>();

		public void addDataValue(ImportDataValue dv) {
			dataValues.add(dv);
		}

		public void addObjectRelation(ImportObjectRelation rel) {
			objectRelations.add(rel);
		}

		public int getDataValueCount() {
			return dataValues.size();
		}

		public int getObjectRelationCount() {
			return objectRelations.size();
		}

	}

	@Data
	public static class ImportDataValue {

		private Long dataPropertyId;

		private String predicateIri;

		private String literalValue;

		private String literalType;

		private Long unitId;

		private String unitSymbol;

	}

	@Data
	public static class ImportObjectRelation {

		private Long objectPropertyId;

		private String predicateIri;

		private String objectIri;

		private String objectKind;

	}

	@Data
	public static class ImportIssue {

		private String instanceIri;

		private String message;

		public ImportIssue(String instanceIri, String message) {
			this.instanceIri = instanceIri;
			this.message = message;
		}

	}

}
