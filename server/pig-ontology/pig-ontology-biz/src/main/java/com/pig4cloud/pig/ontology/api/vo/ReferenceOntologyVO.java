package com.pig4cloud.pig.ontology.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 参考本体元信息（FR-7，AC-7.1）
 * <p>
 * 列出平台内置的三套只读参考本体（QUDT/Brick/CCO）的元信息，供建模侧/治理员了解可浏览的本体。
 *
 * @author pig
 * @date 2026-07-28
 */
@Data
@Schema(description = "参考本体元信息")
public class ReferenceOntologyVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "本体标识：qudt/brick/cco")
	private String key;

	@Schema(description = "显示名")
	private String name;

	@Schema(description = "TTL 文件路径")
	private String file;

	@Schema(description = "版本")
	private String version;

	@Schema(description = "条目数（单位/类/注释属性）")
	private Long itemCount;

}
