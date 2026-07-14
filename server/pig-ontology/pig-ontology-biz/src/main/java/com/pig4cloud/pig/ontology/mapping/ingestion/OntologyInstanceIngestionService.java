/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.mapping.ingestion;

import java.util.List;

/**
 * 统一程序化实例写入入口。
 * <p>
 * 数据源映射、未来IoT、规则和API导入统一复用此服务，将外部记录转换为
 * 可校验、可幂等、可追溯的 Ontology 实例、数据属性值和对象属性断言。
 * <p>
 * 该接口为 Java 内部 SPI。V1 不创建公网批量写 API。
 *
 * @author youming
 */
public interface OntologyInstanceIngestionService {

	/**
	 * 实体 Upsert：创建或更新一个映射来源的实例。
	 * <p>
	 * 按 18-01 §8.1 的 10 步算法执行：校验上下文 → 规范化来源身份 →
	 * 并发查找/创建来源绑定 → 创建或更新实例 → 合并数据值 → 写 Outbox 事件。
	 *
	 * @param command 实体摄入命令
	 * @return 摄入结果
	 */
	IngestionResult upsertEntity(EntityIngestionCommand command);

	/**
	 * 关系 Upsert：创建或更新一个映射来源的对象属性断言。
	 *
	 * @param command 关系摄入命令
	 * @return 关系摄入结果
	 */
	RelationIngestionResult upsertRelation(RelationIngestionCommand command);

	/**
	 * 失活/删除一个已绑定实例。
	 * <p>
	 * 当源记录在增量同步中消失时调用。
	 *
	 * @param sourceIdentity 来源身份标识
	 * @param strategy 删除策略
	 * @param context 摄入上下文
	 * @return 失活结果
	 */
	DeactivationResult deactivateEntity(SourceIdentity sourceIdentity,
										DeleteStrategy strategy,
										IngestionContext context);

	/**
	 * 批量摄入。
	 * <p>
	 * 仅适用于小批次（如重试作业的≤1000条）。大批量同步作业应逐条调用 upsertEntity。
	 *
	 * @param commands 实体摄入命令列表
	 * @param context 摄入上下文
	 * @return 批量摄入结果
	 */
	BatchIngestionResult ingestBatch(List<EntityIngestionCommand> commands,
									 IngestionContext context);

}
