package com.pig4cloud.pig.ontology.modeling.controller;

import cn.hutool.core.io.IoUtil;
import com.pig4cloud.pig.common.core.util.R;
import com.pig4cloud.pig.common.log.annotation.SysLog;
import com.pig4cloud.pig.common.security.annotation.HasPermission;
import com.pig4cloud.pig.ontology.modeling.service.ConsistencyValidator;
import com.pig4cloud.pig.ontology.modeling.service.ParsingService;
import com.pig4cloud.pig.ontology.modeling.service.SerializationService;
import com.pig4cloud.pig.ontology.modeling.vo.ImportResultVO;
import com.pig4cloud.pig.ontology.modeling.vo.SerializePreviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * RDF 序列化与解析 Controller（FR-15/16）
 * <p>
 * 路径 /ont/model/serialize/**，对外 /admin/ont/model/serialize/**
 *
 * @author pig
 * @date 2026-07-28
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ont/model/serialize")
@Tag(name = "序列化与导入", description = "RDF 序列化导出 + 解析导入 + 往返校验（FR-15/16）")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class ModelSerializeController {

	private final SerializationService serializationService;
	private final ParsingService parsingService;
	private final ConsistencyValidator consistencyValidator;

	@GetMapping("/{projectId}/preview")
	@Operation(summary = "序列化预览", description = "返回文本供前端 codemirror 展示（AC-15.8）")
	@HasPermission("ont_serialize_view")
	public R<SerializePreviewVO> preview(@PathVariable Long projectId,
			@RequestParam(defaultValue = "TTL") String format) {
		return R.ok(serializationService.preview(projectId, format));
	}

	@SneakyThrows
	@GetMapping("/{projectId}/download")
	@Operation(summary = "下载序列化文件", description = "Turtle(.ttl) 或 OWL XML(.owl.xml)（AC-15.1/15.2）")
	@HasPermission("ont_serialize_view")
	public void download(@PathVariable Long projectId,
			@RequestParam(defaultValue = "TTL") String format,
			HttpServletResponse response) {
		SerializePreviewVO vo = serializationService.preview(projectId, format);
		String filename = vo.getFormat().equals("TTL") ? "ontology.ttl" : "ontology.owl.xml";
		String contentType = vo.getFormat().equals("TTL") ? "text/turtle; charset=UTF-8"
				: "application/rdf+xml; charset=UTF-8";

		byte[] data = vo.getContent().getBytes(StandardCharsets.UTF_8);
		response.reset();
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
		response.setContentLength(data.length);
		response.setContentType(contentType);
		IoUtil.write(response.getOutputStream(), false, data);
	}

	@SysLog("导入RDF本体")
	@PostMapping("/{projectId}/import")
	@Operation(summary = "导入 RDF 文件", description = "解析 Turtle/OWL XML + 溯源识别（AC-16.1~16.6）")
	@HasPermission("ont_serialize_manage")
	public R<ImportResultVO> importRdf(@PathVariable Long projectId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(defaultValue = "SKIP") String conflictStrategy) {
		if (file.isEmpty()) {
			return R.failed("文件不能为空");
		}
		if (file.getSize() > 10 * 1024 * 1024) {
			return R.failed("文件大小不能超过 10MB（NFR-S2）");
		}
		// 导入后失效序列化缓存
		serializationService.invalidateCache(projectId);
		return R.ok(parsingService.parse(projectId, file, conflictStrategy));
	}

	@GetMapping("/{projectId}/validate")
	@Operation(summary = "往返一致性校验", description = "序列化->解析->再序列化图同构（AC-16.5）")
	@HasPermission("ont_serialize_view")
	public R<Boolean> validate(@PathVariable Long projectId) {
		return R.ok(consistencyValidator.validate(projectId));
	}

}
