package com.pig4cloud.pig.ontology.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.vo.QuantityKindNodeVO;

import java.util.List;

/**
 * 量纲 Service（FR-3）
 *
 * @author pig
 * @date 2026-07-28
 */
public interface QuantityKindService extends IService<QuantityKind> {

	/**
	 * 量纲列表（含每量纲下单位数）
	 * @return 量纲节点列表（按 sort_order）
	 */
	List<QuantityKindNodeVO> listWithCount();

}
