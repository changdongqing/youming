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

	/** 映射版本校验完成：映射版本通过校验进入 VALIDATED */
	public static final String MAPPING_VERSION_VALIDATED = "MAPPING_VERSION_VALIDATED";

	/** 映射作业启动：作业通过前置校验并开始扫描 */
	public static final String MAPPING_JOB_STARTED = "MAPPING_JOB_STARTED";

	/** 映射作业完成：SUCCEEDED/PARTIAL_SUCCESS */
	public static final String MAPPING_JOB_COMPLETED = "MAPPING_JOB_COMPLETED";

	/** 映射作业失败：作业级失败 */
	public static final String MAPPING_JOB_FAILED = "MAPPING_JOB_FAILED";

	/** 映射作业取消：安全停止 */
	public static final String MAPPING_JOB_CANCELLED = "MAPPING_JOB_CANCELLED";

	/** 映射实例摄入完成（18-08 §11.1） */
	public static final String INSTANCE_INGESTED = "INSTANCE_INGESTED";

	/** 映射实例从源更新（18-08 §11.1） */
	public static final String INSTANCE_UPDATED_FROM_SOURCE = "INSTANCE_UPDATED_FROM_SOURCE";

	/** 映射实例从源失活（18-08 §11.1） */
	public static final String INSTANCE_DEACTIVATED_FROM_SOURCE = "INSTANCE_DEACTIVATED_FROM_SOURCE";

	/** 映射来源值人工覆盖（18-08 §11.1） */
	public static final String MAPPING_VALUE_OVERRIDDEN = "MAPPING_VALUE_OVERRIDDEN";

	/** 映射关系摄入完成（18-08 §11.1） */
	public static final String RELATION_INGESTED = "RELATION_INGESTED";

	/** 映射关系从源更新（18-08 §11.1） */
	public static final String RELATION_UPDATED_FROM_SOURCE = "RELATION_UPDATED_FROM_SOURCE";

	/** 映射关系来源移除（18-08 §11.1） */
	public static final String RELATION_SOURCE_REMOVED = "RELATION_SOURCE_REMOVED";

	/** 映射安全拒绝（18-08 §11.1） */
	public static final String MAPPING_SECURITY_DENIED = "MAPPING_SECURITY_DENIED";

	/** 映射记录拒绝（18-08 §11.1） */
	public static final String MAPPING_RECORD_REJECTED = "MAPPING_RECORD_REJECTED";

}
