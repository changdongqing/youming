<template>
	<div class="layout-padding ontology-event-page">
		<div class="layout-padding-auto layout-padding-view">
			<!-- 概览卡片 -->
			<EventOverview :overview="overview" :loading="overviewLoading" />

			<!-- Tab 切换 -->
			<el-tabs v-model="activeTab" class="mt8" @tab-change="handleTabChange">
				<!-- Outbox -->
				<el-tab-pane label="Outbox 投递" name="outbox">
					<OutboxTable ref="outboxRef" />
				</el-tab-pane>

				<!-- 消费者组 -->
				<el-tab-pane label="消费者组" name="consumers">
					<ConsumerGroupTable ref="consumerRef" />
				</el-tab-pane>

				<!-- 死信 -->
				<el-tab-pane label="死信队列" name="deadLetters">
					<DeadLetterTable ref="deadLetterRef" />
				</el-tab-pane>
			</el-tabs>
		</div>

		<!-- 回放对话框 -->
		<EventReplayDialog v-model:visible="replayDialog.visible" @success="handleReplaySuccess" />
	</div>
</template>

<script lang="ts" name="ontologyEvent" setup>
import { ref, reactive, onMounted } from 'vue';
import { fetchEventOverview } from '/@/api/ontology/event';
import type { EventOverviewVO } from '/@/types/ontology/event';
import EventOverview from './components/EventOverview.vue';
import OutboxTable from './components/OutboxTable.vue';
import ConsumerGroupTable from './components/ConsumerGroupTable.vue';
import DeadLetterTable from './components/DeadLetterTable.vue';
import EventReplayDialog from './components/EventReplayDialog.vue';

const activeTab = ref('outbox');
const overview = ref<EventOverviewVO>({});
const overviewLoading = ref(false);

const outboxRef = ref();
const consumerRef = ref();
const deadLetterRef = ref();

const replayDialog = reactive({ visible: false });

const loadOverview = async () => {
	overviewLoading.value = true;
	try {
		const res = await fetchEventOverview();
		overview.value = res.data || {};
	} finally {
		overviewLoading.value = false;
	}
};

const handleTabChange = (name: string) => {
	if (name === 'consumers' && consumerRef.value) {
		consumerRef.value?.load();
	}
};

const handleReplaySuccess = () => {
	loadOverview();
	if (deadLetterRef.value) deadLetterRef.value?.load();
};

onMounted(() => {
	loadOverview();
});
</script>

<style scoped>
.ontology-event-page {
	--event-tag-w: 90px;
}
.mt8 {
	margin-top: 8px;
}
</style>
