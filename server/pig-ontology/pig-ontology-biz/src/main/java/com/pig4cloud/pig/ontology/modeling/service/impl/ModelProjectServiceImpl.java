package com.pig4cloud.pig.ontology.modeling.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.ontology.modeling.entity.ModelPrefix;
import com.pig4cloud.pig.ontology.modeling.entity.ModelProject;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelPrefixMapper;
import com.pig4cloud.pig.ontology.modeling.mapper.ModelProjectMapper;
import com.pig4cloud.pig.ontology.modeling.service.ModelProjectService;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 本体项目 Service 实现（FR-10）
 *
 * @author pig
 * @date 2026-07-28
 */
@AllArgsConstructor
@Service
public class ModelProjectServiceImpl extends ServiceImpl<ModelProjectMapper, ModelProject>
		implements ModelProjectService {

	private static final Set<String> VALID_STATUS = Set.of("draft", "active", "archived");

	private static final Set<String> VALID_FORMAT = Set.of("TTL", "OWL_XML");

	private final ModelPrefixMapper modelPrefixMapper;

	@Override
	public IPage<ModelProject> page(Page page, ModelProject project) {
		return baseMapper.selectPage(page,
				Wrappers.<ModelProject>lambdaQuery()
					.like(StrUtil.isNotBlank(project.getName()), ModelProject::getName, project.getName())
					.eq(StrUtil.isNotBlank(project.getStatus()), ModelProject::getStatus, project.getStatus())
					.orderByDesc(ModelProject::getCreateTime));
	}

	@Override
	public ModelProject getDetail(Long id) {
		return getById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R saveProject(ModelProject project) {
		// 1. 状态校验（默认 draft）
		if (StrUtil.isBlank(project.getStatus())) {
			project.setStatus("draft");
		}
		if (!VALID_STATUS.contains(project.getStatus())) {
			return R.failed("无效的项目状态：" + project.getStatus());
		}
		// 2. 格式校验（默认 TTL）
		if (StrUtil.isBlank(project.getDefaultFormat())) {
			project.setDefaultFormat("TTL");
		}
		if (!VALID_FORMAT.contains(project.getDefaultFormat())) {
			return R.failed("无效的序列化格式：" + project.getDefaultFormat());
		}
		// 3. 策略强制 B（v1 禁用 A，AC-10.3）
		project.setSerializationStrategy("B");
		// 4. projectCode 预查重
		long count = count(Wrappers.<ModelProject>lambdaQuery()
			.eq(ModelProject::getProjectCode, project.getProjectCode()));
		if (count > 0) {
			return R.failed("项目编码 '" + project.getProjectCode() + "' 已存在");
		}
		// 5. namespaceBase 末尾补 /（IRI 规范）
		if (!project.getNamespaceBase().endsWith("/")) {
			project.setNamespaceBase(project.getNamespaceBase() + "/");
		}
		try {
			return R.ok(save(project));
		}
		catch (DuplicateKeyException e) {
			return R.failed("项目编码 '" + project.getProjectCode() + "' 已存在");
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R updateProject(ModelProject project) {
		ModelProject existing = getById(project.getId());
		if (existing == null) {
			return R.failed("项目不存在");
		}
		// archived 拒绝写（AC-10.4）
		if ("archived".equals(existing.getStatus())) {
			return R.failed("已归档项目不可编辑");
		}
		// 策略强制 B（v1 禁用 A，AC-10.3）
		project.setSerializationStrategy("B");
		// projectCode 不可改（锁定）
		project.setProjectCode(existing.getProjectCode());
		// namespaceBase 末尾补 /
		if (StrUtil.isNotBlank(project.getNamespaceBase())
				&& !project.getNamespaceBase().endsWith("/")) {
			project.setNamespaceBase(project.getNamespaceBase() + "/");
		}
		// 状态校验
		if (StrUtil.isNotBlank(project.getStatus()) && !VALID_STATUS.contains(project.getStatus())) {
			return R.failed("无效的项目状态：" + project.getStatus());
		}
		return R.ok(updateById(project));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R removeProject(Long id) {
		ModelProject existing = getById(id);
		if (existing == null) {
			return R.failed("项目不存在");
		}
		// 1. 级联软删项目下前缀（前缀是项目从属资源，删项目应一并清理，避免孤儿）
		//    本 DD 已建 ont_model_prefix，可直接操作
		modelPrefixMapper.delete(Wrappers.<ModelPrefix>lambdaQuery()
			.eq(ModelPrefix::getProjectId, id));
		// 2. 类实体关联校验：移交 DD8
		//    DD8 建 ont_model_class 后，在 ModelClass 删除链路或此处补充：
		//    long classCount = modelClassMapper.selectCount(...projectId=id);
		//    if (classCount > 0) return R.failed("项目下存在 N 个类实体，无法删除");
		//    本 DD 不预埋引用未存在 Mapper 的代码（避免编译/语义混淆）
		return R.ok(removeById(id));
	}

}
