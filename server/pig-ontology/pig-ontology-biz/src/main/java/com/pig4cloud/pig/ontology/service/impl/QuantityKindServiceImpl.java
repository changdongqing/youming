package com.pig4cloud.pig.ontology.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.QuantityKindNodeVO;
import com.pig4cloud.pig.ontology.mapper.QuantityKindMapper;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import com.pig4cloud.pig.ontology.service.QuantityKindService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 量纲 Service 实现（FR-3）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class QuantityKindServiceImpl extends ServiceImpl<QuantityKindMapper, QuantityKind>
		implements QuantityKindService {

	private final UnitMapper unitMapper;

	@Override
	public List<QuantityKindNodeVO> listWithCount() {
		// 1. 全量量纲（按 sort_order）
		List<QuantityKind> kinds = list(Wrappers.<QuantityKind>lambdaQuery()
				.orderByAsc(QuantityKind::getSortOrder)
				.orderByAsc(QuantityKind::getId));
		// 2. 单位计数：量纲数 <=8，单位数有限，一次性查全量后内存分组（避免 N 次 count 查询）
		List<Unit> allUnits = unitMapper
				.selectList(Wrappers.<Unit>lambdaQuery().select(Unit::getQuantityKindId));
		Map<Long, Long> countMap = new HashMap<>();
		for (Unit u : allUnits) {
			countMap.merge(u.getQuantityKindId(), 1L, Long::sum);
		}
		// 3. 组装 VO
		return kinds.stream().map(k -> {
			QuantityKindNodeVO vo = new QuantityKindNodeVO();
			vo.setId(k.getId());
			vo.setQudtIri(k.getQudtIri());
			vo.setLabel(k.getLabel());
			vo.setLabelCn(k.getLabelCn());
			vo.setDimensionVector(k.getDimensionVector());
			vo.setSortOrder(k.getSortOrder());
			vo.setUnitCount(countMap.getOrDefault(k.getId(), 0L));
			return vo;
		}).toList();
	}

}
