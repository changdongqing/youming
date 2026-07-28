package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.dto.ClassInstantiateDTO;
import com.pig4cloud.pig.ontology.modeling.entity.ModelClass;
import com.pig4cloud.pig.ontology.modeling.entity.ModelDatatypeProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelObjectProperty;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelClassMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelDatatypePropertyMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelObjectPropertyMapper;
import com.pig4cloud.pig.ontology.modeling.service.ClassInstantiationService;
import com.pig4cloud.pig.ontology.modeling.service.ModelClassService;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import com.pig4cloud.pig.ontology.modeling.vo.ModelClassDetailVO;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * 本体类实体 Service 实现（FR-11）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelClassServiceImpl extends ServiceImpl<ModelClassMapper, ModelClass>
		implements ModelClassService {

	private final ClassInstantiationService instantiationService;
	private final ModelDatatypePropertyMapper datatypePropertyMapper;
	private final ModelObjectPropertyMapper objectPropertyMapper;
	private final ModelProjectService modelProjectService;

	@Override
	public IPage<ModelClass> page(Page page, ModelClass cls) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelClass>lambdaQuery()
					.eq(cls.getProjectId() != null, ModelClass::getProjectId, cls.getProjectId())
					.like(StrUtil.isNotBlank(cls.getLabel()), ModelClass::getLabel, cls.getLabel())
					.eq(StrUtil.isNotBlank(cls.getTemplateCode()),
							ModelClass::getTemplateCode, cls.getTemplateCode())
					.orderByDesc(ModelClass::getCreateTime));
	}

	@Override
	public ModelClassDetailVO getDetail(Long id) {
		ModelClass cls = getById(id);
		if (cls == null) {
			return null;
		}
		ModelClassDetailVO vo = new ModelClassDetailVO();
		vo.setId(cls.getId());
		vo.setProjectId(cls.getProjectId());
		vo.setClassIri(cls.getClassIri());
		vo.setLocalName(cls.getLocalName());
		vo.setLabel(cls.getLabel());
		vo.setLabelCn(cls.getLabelCn());
		vo.setDescription(cls.getDescription());
		vo.setTemplateCode(cls.getTemplateCode());
		vo.setClassificationCode(cls.getClassificationCode());
		vo.setIcon(cls.getIcon());
		vo.setColor(cls.getColor());
		// 数据属性列表
		vo.setDatatypeProperties(datatypePropertyMapper.selectList(
				Wrappers.<ModelDatatypeProperty>lambdaQuery()
					.eq(ModelDatatypeProperty::getClassId, id)
					.orderByAsc(ModelDatatypeProperty::getSortOrder)));
		// 对象属性列表（作为 domain）
		vo.setObjectProperties(objectPropertyMapper.selectList(
				Wrappers.<ModelObjectProperty>lambdaQuery()
					.eq(ModelObjectProperty::getDomainClassId, id)
					.orderByAsc(ModelObjectProperty::getSortOrder)));
		// 父类/子类 IRI 列表（DD9 建 ont_model_subclassof 表后填充，v1 返回空列表）
		vo.setParentClassIris(new ArrayList<>());
		vo.setChildClassIris(new ArrayList<>());
		return vo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveClass(ModelClass cls) {
		// 1. localName 必填（IRI 本地名）
		if (StrUtil.isBlank(cls.getLocalName())) {
			return R.failed("本地名不能为空");
		}
		// 2. IRI 拼接与一致性校验（评审补充 P-5）
		//    IRI = project.namespace_base + localName；若前端已传 classIri 则校验一致性
		ModelProject project = modelProjectService.getById(cls.getProjectId());
		if (project == null) {
			return R.failed("项目不存在");
		}
		String expectedIri = project.getNamespaceBase() + cls.getLocalName();
		if (StrUtil.isBlank(cls.getClassIri())) {
			cls.setClassIri(expectedIri);
		}
		else if (!expectedIri.equals(cls.getClassIri())) {
			return R.failed("类 IRI 与命名空间基址/本地名不一致，期望：" + expectedIri);
		}
		// 3. 同项目 IRI 预查重
		long count = count(Wrappers.<ModelClass>lambdaQuery()
			.eq(ModelClass::getProjectId, cls.getProjectId())
			.eq(ModelClass::getClassIri, cls.getClassIri()));
		if (count > 0) {
			return R.failed("类 IRI '" + cls.getClassIri() + "' 在项目内已存在");
		}
		// 4. 保存类实体
		try {
			save(cls);
		}
		catch (DuplicateKeyException e) {
			return R.failed("类 IRI '" + cls.getClassIri() + "' 在项目内已存在");
		}
		// 5. 基于模板实例化属性（AC-11.2~11.5）
		if (StrUtil.isNotBlank(cls.getTemplateCode())) {
			R instantiateResult = instantiationService.instantiate(cls);
			if (instantiateResult.getCode() != 0) {
				// 实例化失败回滚（@Transactional 已标记 rollback）
				return R.failed("模板实例化失败：" + instantiateResult.getMsg());
			}
		}
		// 空白类创建（AC-11.3）：templateCode 为空时跳过实例化，属性列表初始为空
		return R.ok(cls);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateClass(ModelClass cls) {
		ModelClass existing = getById(cls.getId());
		if (existing == null) {
			return R.failed("类不存在");
		}
		// classIri / projectId / localName 不可改（IRI 标识锁定）
		cls.setClassIri(existing.getClassIri());
		cls.setProjectId(existing.getProjectId());
		cls.setLocalName(existing.getLocalName());
		// templateCode / classificationCode 不可改（溯源锁定）
		cls.setTemplateCode(existing.getTemplateCode());
		cls.setClassificationCode(existing.getClassificationCode());
		// 空值保护（评审补充 P-5）：前端只传部分字段时，保留 existing 的非锁定可编辑字段
		if (cls.getLabel() == null) {
			cls.setLabel(existing.getLabel());
		}
		if (cls.getDescription() == null) {
			cls.setDescription(existing.getDescription());
		}
		if (cls.getSortOrder() == null) {
			cls.setSortOrder(existing.getSortOrder());
		}
		return R.ok(updateById(cls));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R instantiateFromClass(Long id, ClassInstantiateDTO dto) {
		// 场景三：对已有空白类追加模板属性（PRD 10.2 POST /{id}/instantiate）
		ModelClass cls = getById(id);
		if (cls == null) {
			return R.failed("类不存在");
		}
		if (StrUtil.isNotBlank(cls.getTemplateCode()) && cls.getTemplateCode().equals(dto.getTemplateCode())) {
			return R.failed("该类已基于模板 " + dto.getTemplateCode() + " 创建，不可重复实例化");
		}
		// 设置模板编码后走实例化；溯源字段（templateCode/classificationCode）由 instantiate 回填
		cls.setTemplateCode(dto.getTemplateCode());
		return instantiationService.instantiate(cls);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeClass(Long id) {
		// 删除校验：是否有数据属性
		long dtCount = datatypePropertyMapper.selectCount(Wrappers.<ModelDatatypeProperty>lambdaQuery()
			.eq(ModelDatatypeProperty::getClassId, id));
		if (dtCount > 0) {
			return R.failed("类下存在 " + dtCount + " 个数据属性，无法删除");
		}
		// 删除校验：是否有对象属性（domain 或 range）
		long objCount = objectPropertyMapper.selectCount(Wrappers.<ModelObjectProperty>lambdaQuery()
			.eq(ModelObjectProperty::getDomainClassId, id)
			.or()
			.eq(ModelObjectProperty::getRangeClassId, id));
		if (objCount > 0) {
			return R.failed("类被 " + objCount + " 个对象属性引用，无法删除");
		}
		// 删除校验：是否被 subClassOf 引用（DD9 建表后启用，v1 跳过）
		return R.ok(removeById(id));
	}

}
