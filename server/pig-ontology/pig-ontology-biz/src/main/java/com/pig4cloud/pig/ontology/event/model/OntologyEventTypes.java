/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.event.model;

/**
 * 领域事件类型常量。
 * <p>
 * 采用稳定的粗粒度事件类型，具体资源和动作放 Envelope/payload。
 *
 * @author youming
 */
public final class OntologyEventTypes {

	private OntologyEventTypes() {
	}

	/** Schema 变更：命名空间/类型/层次/属性/公理等结构变化 */
	public static final String ONTOLOGY_SCHEMA_CHANGED = "ONTOLOGY_SCHEMA_CHANGED";

	/** 实例变更：实例/值/关系事务完成 */
	public static final String ONTOLOGY_INSTANCE_CHANGED = "ONTOLOGY_INSTANCE_CHANGED";

	/** 校验完成 */
	public static final String ONTOLOGY_VALIDATION_COMPLETED = "ONTOLOGY_VALIDATION_COMPLETED";

	/** 序列化完成：导入/导出 */
	public static final String ONTOLOGY_SERIALIZATION_COMPLETED = "ONTOLOGY_SERIALIZATION_COMPLETED";

	/** 版本准备：候选版本构建 */
	public static final String ONTOLOGY_VERSION_PREPARED = "ONTOLOGY_VERSION_PREPARED";

	/** 版本发布：候选版本激活 */
	public static final String ONTOLOGY_VERSION_PUBLISHED = "ONTOLOGY_VERSION_PUBLISHED";

	/** 版本迁移完成 */
	public static final String ONTOLOGY_MIGRATION_COMPLETED = "ONTOLOGY_MIGRATION_COMPLETED";

	/** 安全事件：非敏感摘要 */
	public static final String ONTOLOGY_SECURITY_EVENT = "ONTOLOGY_SECURITY_EVENT";

	/** 扩展绑定变更 */
	public static final String EXTENSION_BINDING_CHANGED = "EXTENSION_BINDING_CHANGED";

	/** 映射版本发布：映射配置快照发布为不可变版本 */
	public static final String MAPPING_VERSION_PUBLISHED = "MAPPING_VERSION_PUBLISHED";

}
