/*
 * Copyright (c) 2026 youming Authors. All Rights Reserved.
 */

package com.pig4cloud.pig.ontology.validation.constant;

/**
 * 图谱边类型常量。
 * <p>
 * 收口 {@code GraphEdgeVO.edgeType} 的硬编码字符串取值。
 * 仅包含代码中实际有赋值点的 5 个取值（{@code INSTANCE_OF} 在仓库代码中无赋值点，不纳入）。
 *
 * @author youming
 */
public final class GraphEdgeType {

	private GraphEdgeType() {
	}

	/** 继承关系边（rdfs:subClassOf） */
	public static final String SUBCLASS_OF = "SUBCLASS_OF";

	/** 对象属性声明边 */
	public static final String OBJECT_PROPERTY = "OBJECT_PROPERTY";

	/** 等价关系边（owl:equivalentClass） */
	public static final String EQUIVALENT = "EQUIVALENT";

	/** 不相交关系边（owl:disjointWith） */
	public static final String DISJOINT = "DISJOINT";

	/** 实例关系断言边 */
	public static final String INSTANCE_RELATION = "INSTANCE_RELATION";

}
