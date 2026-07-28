package com.pig4cloud.pig.ontology.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.api.entity.QuantityKind;
import com.pig4cloud.pig.ontology.api.entity.Unit;
import com.pig4cloud.pig.ontology.api.vo.UnitSupplyVO;
import com.pig4cloud.pig.ontology.mapper.QuantityKindMapper;
import com.pig4cloud.pig.ontology.mapper.UnitMapper;
import com.pig4cloud.pig.ontology.service.UnitService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单位 Service 实现（FR-3）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class UnitServiceImpl extends ServiceImpl<UnitMapper, Unit> implements UnitService {

	private final QuantityKindMapper quantityKindMapper;

	@Override
	public IPage<Unit> page(Page page, Unit unit) {
		return baseMapper.selectPage(page,
				Wrappers.<Unit>lambdaQuery()
					.eq(unit.getQuantityKindId() != null, Unit::getQuantityKindId, unit.getQuantityKindId())
					.and(StrUtil.isNotBlank(unit.getQudtIri()),
							w -> w.like(Unit::getQudtIri, unit.getQudtIri())
								.or()
								.like(Unit::getLabel, unit.getQudtIri())
								.or()
								.like(Unit::getSymbol, unit.getQudtIri()))
					.eq(StrUtil.isNotBlank(unit.getDeprecated()), Unit::getDeprecated, unit.getDeprecated())
					.orderByAsc(Unit::getQuantityKindId)
					.orderByAsc(Unit::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveUnit(Unit unit) {
		// 1. 量纲存在性校验（防止脏引用）
		QuantityKind qk = quantityKindMapper.selectById(unit.getQuantityKindId());
		if (qk == null) {
			return R.failed("所属量纲不存在");
		}
		// 2. qudtIri 预查重（AC-3.5）
		long count = count(Wrappers.<Unit>lambdaQuery().eq(Unit::getQudtIri, unit.getQudtIri()));
		if (count > 0) {
			return R.failed("QUDT IRI '" + unit.getQudtIri() + "' 已存在");
		}
		unit.setSource("custom");
		unit.setDeprecated("0");
		try {
			// DB 唯一约束 uk_ont_unit_iri 不受逻辑删除过滤，是权威兜底：
			// 若 qudtIri 曾被软删，预查重查不到，由约束抛 DuplicateKeyException，转友好提示
			return R.ok(save(unit));
		}
		catch (DuplicateKeyException e) {
			return R.failed("QUDT IRI '" + unit.getQudtIri() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateUnit(Unit unit) {
		Unit existing = getById(unit.getId());
		if (existing == null) {
			return R.failed("单位不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置单位不可编辑");
		}
		// qudtIri 不可改（引用稳定性，对齐 DD2 templateCode 范式）
		unit.setQudtIri(existing.getQudtIri());
		return R.ok(updateById(unit));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeUnit(Long id) {
		Unit existing = getById(id);
		if (existing == null) {
			return R.failed("单位不存在");
		}
		if ("builtin".equals(existing.getSource())) {
			return R.failed("内置单位不可删除");
		}
		// TODO: 后期接入属性模板 unitRef 引用计数（FR-1 unitRef 交汇），有引用时拒绝删除
		return R.ok(removeById(id));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R deprecate(Long id, String deprecated) {
		return R.ok(update(Wrappers.<Unit>lambdaUpdate()
			.eq(Unit::getId, id)
			.set(Unit::getDeprecated, deprecated)));
	}

	@Override
	public List<UnitSupplyVO> supplyList(String quantityKindIri, Boolean includeDeprecated) {
		// 1. 量纲 IRI -> id（为空则不过滤）
		Long qkId = null;
		Map<Long, QuantityKind> qkMap = new HashMap<>();
		if (StrUtil.isNotBlank(quantityKindIri)) {
			QuantityKind qk = quantityKindMapper.selectOne(Wrappers.<QuantityKind>lambdaQuery()
					.eq(QuantityKind::getQudtIri, quantityKindIri));
			if (qk == null) {
				return List.of();
			}
			qkId = qk.getId();
			qkMap.put(qk.getId(), qk);
		}
		else {
			// 预加载全量量纲，供 VO 填 quantityKindIri/quantityKindLabel（AC-5.4）
			List<QuantityKind> all = quantityKindMapper.selectList(null);
			for (QuantityKind q : all) {
				qkMap.put(q.getId(), q);
			}
		}
		// 2. 查单位（按量纲/弃用过滤，按量纲+id 排序）
		List<Unit> units = list(Wrappers.<Unit>lambdaQuery()
				.eq(qkId != null, Unit::getQuantityKindId, qkId)
				.eq(!includeDeprecated, Unit::getDeprecated, "0")
				.orderByAsc(Unit::getQuantityKindId)
				.orderByAsc(Unit::getId));
		// 3. 转稳定化 VO（屏蔽审计字段，AC-5.7），补量纲 IRI/label
		return units.stream().map(u -> {
			UnitSupplyVO vo = BeanUtil.copyProperties(u, UnitSupplyVO.class);
			QuantityKind q = qkMap.get(u.getQuantityKindId());
			if (q != null) {
				vo.setQuantityKindIri(q.getQudtIri());
				vo.setQuantityKindLabel(q.getLabel());
			}
			return vo;
		}).toList();
	}

}
