<template>
	<div class="serialize-preview" v-show="visible">
		<div class="serialize-preview-header">
			<span>{{ t('modelCanvas.serializePreview') }}</span>
			<el-button-group>
				<el-button size="small" :type="format === 'TTL' ? 'primary' : ''" @click="format = 'TTL'">Turtle</el-button>
				<el-button size="small" :type="format === 'OWL_XML' ? 'primary' : ''" @click="format = 'OWL_XML'">OWL XML</el-button>
			</el-button-group>
			<el-button size="small" @click="visible = false">折叠</el-button>
		</div>
		<code-editor v-model="content" theme="darcula" :mode="format === 'TTL' ? 'text/turtle' : 'application/xml'" :read-only="true" :height="200" />
	</div>
</template>

<script lang="ts" setup>
import { useDebounceFn } from '@vueuse/core';
import { preview as serializePreview } from '/@/api/ontology-model/serialize';
import { useI18n } from 'vue-i18n';

const CodeEditor = defineAsyncComponent(() => import('/@/components/CodeEditor/index.vue'));

const props = defineProps<{ projectId: string }>();
const { t } = useI18n();

const visible = ref(true);
const content = ref('');
const format = ref('TTL');

// 防抖 500ms 触发序列化预览（AC-17.6）
const debouncedPreview = useDebounceFn(async () => {
	if (!props.projectId) return;
	try {
		const res = await serializePreview(props.projectId, format.value);
		content.value = res.data?.content || '';
	} catch {
		// 静默失败
	}
}, 500);

// 监听 format 变化
watch(format, debouncedPreview);

// 暴露刷新方法供画布操作后调用
defineExpose({ refresh: debouncedPreview });
</script>

<style scoped>
.serialize-preview {
	border-top: 1px solid #e4e7ed;
}
.serialize-preview-header {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 4px 12px;
	background: #f5f7fa;
	font-size: 13px;
	font-weight: 600;
}
</style>
