<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-card class="mb10">
				<el-form :inline="true">
					<el-form-item label="开始日期"><el-date-picker v-model="startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
					<el-form-item label="结束日期"><el-date-picker v-model="endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
					<el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
				</el-form>
			</el-card>
			<el-row :gutter="10" class="mb10">
				<el-col :span="6"><el-card><div style="text-align:center"><div style="font-size:28px;font-weight:bold">{{ stat.totalCount }}</div><div style="color:#999">需求总数</div></div></el-card></el-col>
				<el-col :span="6"><el-card><div style="text-align:center"><div style="font-size:28px;font-weight:bold">{{ stat.totalDesignWorkload }}</div><div style="color:#999">设计工作量合计(人日)</div></div></el-card></el-col>
			</el-row>
			<el-row :gutter="10">
				<el-col :span="12"><el-card header="按需求来源"><v-chart :option="sourceChart" autoresize style="height:300px" /></el-card></el-col>
				<el-col :span="12"><el-card header="按月趋势"><v-chart :option="monthChart" autoresize style="height:300px" /></el-card></el-col>
			</el-row>
		</div>
	</div>
</template>

<script lang="ts" setup>
import { use } from 'echarts/core';
import VChart from 'vue-echarts';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart, LineChart } from 'echarts/charts';
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components';
import { requirementStat } from '/@/api/rm/report';
use([CanvasRenderer, PieChart, LineChart, TooltipComponent, LegendComponent, GridComponent]);

const startDate = ref(''); const endDate = ref('');
const stat = ref<any>({ bySource: [], byMonth: [], totalCount: 0, totalDesignWorkload: 0 });

const sourceLabel = (s: string) => ({ ITERATION:'迭代需求', CUSTOMER_DELIVERY:'客户需求(交付)', CUSTOMER_OPS:'客户需求(运维)', PRESALE:'售前项目需求' }[s] || s);

const sourceChart = computed(() => ({ tooltip:{trigger:'item'}, legend:{bottom:0}, series:[{ type:'pie', radius:['40%','70%'], data: stat.value.bySource?.map((i:any)=>({name:sourceLabel(i.key),value:i.count})) || [] }] }));
const monthChart = computed(() => ({ tooltip:{trigger:'axis'}, xAxis:{type:'category',data: stat.value.byMonth?.map((i:any)=>i.key) || []}, yAxis:{type:'value'}, series:[{type:'line',data: stat.value.byMonth?.map((i:any)=>i.count) || [], smooth:true}] }));

const loadData = async () => { try { const { data } = await requirementStat({ startDate: startDate.value, endDate: endDate.value }); stat.value = data; } catch {} };
onMounted(() => loadData());
</script>
