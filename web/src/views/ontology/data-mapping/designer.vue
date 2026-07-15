<template>
	<div class="layout-padding mapping-designer-page">
		<div class="layout-padding-auto layout-padding-view">
			<div v-loading="loading">
				<!-- ==================== 顶部 Header ==================== -->
				<div class="designer-header" v-if="project && version">
					<div class="header-left">
						<el-button link @click="goBack">
							<el-icon><ArrowLeft /></el-icon> 返回
						</el-button>
						<el-divider direction="vertical" />
						<span class="header-title">{{ project.mappingName }}</span>
						<el-tag :type="versionStatusTagType(version.versionStatus)" size="small" class="ml8">
							{{ versionStatusLabel(version.versionStatus) }}
						</el-tag>
						<span class="header-info ml12">
							版本: <el-tag size="small">{{ version.versionNumber }}</el-tag>
						</span>
						<span class="header-info ml8"> 本体工程ID: {{ project.ontologyId }} </span>
						<span class="header-info ml8" v-if="version.configHash">
							配置哈希: <el-text size="small" type="info">{{ version.configHash.slice(0, 8) }}...</el-text>
						</span>
					</div>
					<div class="header-right">
						<el-button type="primary" @click="handleSave" :disabled="store.isReadonly || !store.isDirty" v-auth="'ontology_mapping_edit'">
							<el-icon><Check /></el-icon> 保存
						</el-button>
						<el-button @click="handleValidate" :disabled="store.isReadonly" v-auth="'ontology_mapping_validate'">
							<el-icon><Checked /></el-icon> 校验
						</el-button>
						<el-button type="success" @click="handlePublish" :disabled="store.isReadonly" v-auth="'ontology_mapping_publish'">
							<el-icon><Upload /></el-icon> 发布
						</el-button>
					</div>
				</div>

				<!-- ==================== 三栏布局 ==================== -->
				<Splitpanes class="designer-body" v-if="project && version">
					<!-- 左：源数据 -->
					<Pane :size="25" min-size="18">
						<div class="panel-container">
							<div class="panel-title">
								<el-icon><Connection /></el-icon> 源数据
							</div>
							<el-scrollbar class="panel-content">
								<el-empty description="源数据面板（第二轮实现）" :image-size="60" />
							</el-scrollbar>
						</div>
					</Pane>

					<!-- 中：映射工作区 -->
					<Pane :size="50" min-size="30">
						<div class="panel-container">
							<div class="panel-title">
								<el-icon><Grid /></el-icon> 映射工作区
							</div>
							<el-scrollbar class="panel-content">
								<el-empty description="实体映射与字段映射编辑器（第二轮实现）" :image-size="80">
									<template #description>
										<p>第二轮将实现：</p>
										<ul style="text-align: left; font-size: 12px; color: var(--el-text-color-secondary)">
											<li>EntityMappingEditor — 实体映射表单</li>
											<li>FieldMappingTable — 字段映射表格</li>
											<li>FilterDslBuilder — 过滤条件构建器</li>
											<li>RelationMappingWizard — 关系映射向导</li>
										</ul>
									</template>
								</el-empty>
							</el-scrollbar>
						</div>
					</Pane>

					<!-- 右：本体模型 -->
					<Pane :size="25" min-size="18">
						<div class="panel-container">
							<div class="panel-title">
								<el-icon><Share /></el-icon> 本体模型
							</div>
							<el-scrollbar class="panel-content">
								<el-empty description="本体模型浏览（第二轮实现）" :image-size="60" />
							</el-scrollbar>
						</div>
					</Pane>
				</Splitpanes>

				<!-- ==================== 底部抽屉预留 ==================== -->
				<div class="designer-footer" v-if="project && version">
					<el-tabs v-model="bottomTab" class="bottom-tabs">
						<el-tab-pane label="预览" name="preview">
							<el-empty description="预览抽屉（第二轮实现）" :image-size="40" />
						</el-tab-pane>
						<el-tab-pane label="校验问题" name="validation">
							<el-empty description="校验报告（第二轮实现）" :image-size="40" />
						</el-tab-pane>
						<el-tab-pane label="版本差异" name="diff">
							<el-empty description="版本差异（第二轮实现）" :image-size="40" />
						</el-tab-pane>
						<el-tab-pane label="操作日志" name="logs">
							<el-empty description="操作日志（第二轮实现）" :image-size="40" />
						</el-tab-pane>
					</el-tabs>
				</div>
			</div>
		</div>
	</div>
</template>

<script lang="ts" name="ontologyDataMappingDesigner" setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useMessage } from '/@/hooks/message';
import { mappingProjectApi, mappingValidationApi } from '/@/api/ontology/data-mapping';
import type { MappingProjectVO, MappingVersionVO } from '/@/types/ontology/data-mapping';
import { versionStatusLabel, versionStatusTagType } from './utils/mapping-status';
import { useMappingDesignerStore } from '/@/stores/ontology/useMappingDesignerStore';
import { ArrowLeft, Check, Checked, Upload, Connection, Grid, Share } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const { success: msgSuccess, error: msgError, warning: msgWarning } = useMessage();
const store = useMappingDesignerStore();

const loading = ref(false);
const project = ref<MappingProjectVO | null>(null);
const version = ref<MappingVersionVO | null>(null);
const bottomTab = ref('preview');

const loadProjectAndVersion = async () => {
	const projectId = Number(route.query.projectId);
	const versionId = Number(route.query.versionId);
	if (!projectId || !versionId) {
		msgError('缺少 projectId 或 versionId 参数');
		return;
	}

	loading.value = true;
	store.setLoading('project', true);
	store.setLoading('version', true);
	try {
		const [{ data: projectData }, { data: versionData }] = await Promise.all([
			mappingProjectApi.getProjectDetail(projectId),
			mappingProjectApi.getVersionDetail(versionId),
		]);
		project.value = projectData;
		version.value = versionData;
		store.setProject(projectData);
		store.setVersion(versionData);
	} catch (e: any) {
		msgError(e.message || '加载工程/版本详情失败');
	} finally {
		loading.value = false;
		store.setLoading('project', false);
		store.setLoading('version', false);
	}
};

const goBack = () => {
	router.push('/ontology/data-mapping/index');
};

const handleSave = () => {
	// 子表独立保存逻辑将在第二轮实现
	msgWarning('保存功能将在第二轮实现');
};

const handleValidate = async () => {
	if (!version.value) return;
	try {
		const { data } = await mappingValidationApi.validate(version.value.id);
		msgSuccess(`校验完成：${data.violationCount} 违规 / ${data.warningCount} 警告 / ${data.infoCount} 信息`);
	} catch (e: any) {
		msgError(e.message || '校验失败');
	}
};

const handlePublish = async () => {
	if (!version.value) return;
	if (store.isDirty) {
		msgWarning('有未保存的修改，请先保存');
		return;
	}
	// 发布流程将在第二轮完善（preparePublish → 风险摘要 → 确认）
	try {
		const { data: prepare } = await mappingProjectApi.preparePublish(version.value.id);
		if (!prepare.publishable) {
			msgError(`不可发布：${prepare.violationCount} 违规未解决`);
			return;
		}
		msgSuccess('发布准备检查通过，完整发布流程将在第二轮实现');
	} catch (e: any) {
		msgError(e.message || '发布准备失败');
	}
};

onMounted(() => {
	loadProjectAndVersion();
});

onBeforeUnmount(() => {
	store.reset();
});
</script>

<style scoped>
.mapping-designer-page {
	height: 100%;
}
.designer-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 8px 12px;
	border-bottom: 1px solid var(--el-border-color-light);
	background-color: var(--el-bg-color);
}
.header-left {
	display: flex;
	align-items: center;
}
.header-title {
	font-size: 16px;
	font-weight: 600;
}
.header-info {
	font-size: 13px;
	color: var(--el-text-color-secondary);
}
.header-right {
	display: flex;
	gap: 8px;
}
.designer-body {
	height: calc(100% - 80px - 200px);
}
.panel-container {
	height: 100%;
	display: flex;
	flex-direction: column;
}
.panel-title {
	padding: 8px 12px;
	font-weight: 600;
	font-size: 13px;
	border-bottom: 1px solid var(--el-border-color-light);
	background-color: var(--el-fill-color-light);
	display: flex;
	align-items: center;
	gap: 6px;
}
.panel-content {
	flex: 1;
	overflow: auto;
}
.designer-footer {
	height: 200px;
	border-top: 1px solid var(--el-border-color-light);
	overflow: auto;
}
.bottom-tabs {
	padding: 0 12px;
}
.ml8 {
	margin-left: 8px;
}
.ml12 {
	margin-left: 12px;
}
</style>
