package com.pig4cloud.pig.ontology.controller;

import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.api.dto.ClassHierarchySyncDTO;
import com.pig4cloud.pig.ontology.service.ClassHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 建模侧同步接口（FR-9，10.7，仅 ont_sync_push）
 * <p>
 * 供建模侧推送类层级关系到治理侧镜像表 ont_class_hierarchy。建模侧是类层级权威源，
 * 治理侧只读镜像。环路校验在建模侧执行（本接口不校验，避免与建模侧权威源冲突）。
 * <p>
 * 路径 /ont/sync，对外 /admin/ont/sync（对齐 PRD 10.7）。
 * <p>
 * 权限 ont_sync_push 仅授予建模侧服务账号/角色，治理员不持有（AC-9.2/9.3）。
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/sync")
@Tag(name = "建模侧同步接口", description = "建模侧推送类层级到镜像表（FR-9，10.7，仅 ont_sync_push）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class SyncController {

	private final ClassHierarchyService classHierarchyService;

	@SysLog("建模侧推送类层级镜像")
	@PostMapping("/class-hierarchy")
	@Operation(summary = "推送 subClassOf 关系（批量）", description = "镜像入库并置 sync_status='1'（10.7，AC-9.2）")
	@HasPermission("ont_sync_push")
	public R pushClassHierarchy(@Valid @RequestBody List<ClassHierarchySyncDTO> edges) {
		return classHierarchyService.upsertMirror(edges);
	}

	@SysLog("建模侧删除类层级镜像")
	@DeleteMapping("/class-hierarchy")
	@Operation(summary = "删除某类层级", description = "镜像置 sync_status='2'（失效），不物理删（10.7）")
	@HasPermission("ont_sync_push")
	public R deleteClassHierarchy(@RequestParam String childClassIri, @RequestParam String parentClassIri) {
		return classHierarchyService.invalidateMirror(childClassIri, parentClassIri);
	}

}
