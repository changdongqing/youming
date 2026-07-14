<template>
	<div class="query-editor-wrapper">
		<code-editor
			ref="codeEditorRef"
			:model-value="modelValue"
			@update:model-value="$emit('update:modelValue', $event)"
			mode="sparql"
			theme="darcula"
			:height="height"
			:options="{ showCopy: true }" />
	</div>
</template>

<script lang="ts" name="SparqlQueryEditor" setup>
import { defineAsyncComponent, ref, onMounted, nextTick } from 'vue';

const CodeEditor = defineAsyncComponent(() => import('/@/components/CodeEditor/index.vue'));

defineProps<{
	modelValue: string;
	height?: string | number;
}>();

defineEmits<{
	(e: 'update:modelValue', value: string): void;
}>();

const codeEditorRef = ref();

onMounted(async () => {
	await nextTick();
	codeEditorRef.value?.refresh();
});

defineExpose({
	refresh: () => codeEditorRef.value?.refresh()
});
</script>

<style scoped>
.query-editor-wrapper {
	border: 1px solid #dcdfe6;
	border-radius: 4px;
	overflow: hidden;
}
</style>
