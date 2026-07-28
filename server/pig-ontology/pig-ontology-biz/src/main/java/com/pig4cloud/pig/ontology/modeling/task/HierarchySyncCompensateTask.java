package com.pig4cloud.pig.ontology.modeling.task;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pig4cloud.pig.ontology.modeling.entity.ModelSubclassOf;
import com.pig4cloud.pig.ontology.modeling.service.HierarchySyncService;
import com.pig4cloud.pig.ontology.modeling.service.ModelSubclassOfService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 镜像回推补偿定时任务（FR-14.5，AC-14.5）
 * <p>
 * 定时扫描 sync_status='0'（待同步）且 retry_count<3 的 subClassOf 记录，重试回推。
 * 3 次失败后置 sync_status='3'（同步失败，需人工介入），不再重试。严格符合 PRD R-19/场景四"最多 3 次"。
 * 固定延迟 60 秒执行一次（避免密集重试）。
 * <p>
 * 注意：需在启动类加 @EnableScheduling（pig 框架默认未启用）。
 *
 * @author pig
 * @date 2026-07-28
 */
@Slf4j
@AllArgsConstructor
@Component
public class HierarchySyncCompensateTask {

	private final ModelSubclassOfService subclassOfService;
	private final HierarchySyncService hierarchySyncService;

	/**
	 * 每 60 秒扫描一次待同步的 subClassOf 记录，重试回推（最多 3 次，AC-14.5）
	 */
	@Scheduled(fixedDelay = 60000)
	public void retrySync() {
		// 只扫待同步且重试次数未超限的记录（sync_status='0' AND retry_count<3）
		List<ModelSubclassOf> pending = subclassOfService.list(
				Wrappers.<ModelSubclassOf>lambdaQuery()
					.eq(ModelSubclassOf::getSyncStatus, "0")
					.lt(ModelSubclassOf::getRetryCount, 3));
		if (CollUtil.isEmpty(pending)) {
			return;
		}
		log.info("镜像回推补偿任务：发现 {} 条待同步记录", pending.size());
		for (ModelSubclassOf edge : pending) {
			try {
				com.pig4cloud.pig.common.core.util.R result = hierarchySyncService.pushMirror(edge);
				if (result.getCode() == 0) {
					edge.setSyncStatus("1");
					subclassOfService.updateById(edge);
					log.info("补偿回推成功: child={}, parent={}", edge.getChildClassId(),
							edge.getParentClassId());
				}
				else {
					// 失败：retry_count+1，达 3 次置 sync_status='3'（同步失败，需人工介入）
					edge.setRetryCount((edge.getRetryCount() == null ? 0 : edge.getRetryCount()) + 1);
					if (edge.getRetryCount() >= 3) {
						edge.setSyncStatus("3");
						log.error("补偿回推 3 次耗尽，置同步失败: child={}, parent={}",
								edge.getChildClassId(), edge.getParentClassId());
					}
					else {
						log.warn("补偿回推失败({}/3): child={}, parent={}, msg={}", edge.getRetryCount(),
								edge.getChildClassId(), edge.getParentClassId(), result.getMsg());
					}
					subclassOfService.updateById(edge);
				}
			}
			catch (Exception e) {
				log.error("补偿回推异常: child={}, parent={}", edge.getChildClassId(),
						edge.getParentClassId(), e);
			}
		}
	}

}
