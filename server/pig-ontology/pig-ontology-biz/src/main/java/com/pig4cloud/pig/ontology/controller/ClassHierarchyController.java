package com.pig4cloud.pig.ontology.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.entity.ClassHierarchy;
import com.pig4cloud.pig.ontology.api.vo.ClassHierarchyNodeVO;
import com.pig4cloud.pig.ontology.service.ClassHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 类分类树镜像 Controller（FR-9，只读查询）
 * <p>
 * 路径 /ont/class-hierarchy，对外 /admin/ont/class-hierarchy（对齐 PRD 10.2）。
 * <p>
 * 边界（AC-9.3）：仅 GET，不提供 POST/PUT/DELETE 登记/删除接口。类层级写入只经
 * {@link SyncController}（建模侧权威源，权限 ont_sync_push）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/class-hierarchy")
@Tag(name = "类分类树（只读镜像）", description = "本体类分类树视图（FR-9，只读）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ClassHierarchyController {

	private final ClassHierarchyService classHierarchyService;

	@GetMapping("/tree")
	@Operation(summary = "类分类树视图", description = "已镜像 subClassOf，只读（10.2，AC-9.4）")
	@HasPermission("ont_class_tpl_view")
	public R<List<ClassHierarchyNodeVO>> tree(@RequestParam(required = false) String treeRoot) {
		return R.ok(classHierarchyService.tree(treeRoot));
	}

	@GetMapping("/page")
	@Operation(summary = "分页查询镜像边", description = "支持 syncStatus / treeRoot / childClassIri 过滤")
	@HasPermission("ont_class_tpl_view")
	public R<IPage<ClassHierarchy>> page(@ParameterObject Page page, @ParameterObject ClassHierarchy filter) {
		return R.ok(classHierarchyService.page(page, filter));
	}

}
