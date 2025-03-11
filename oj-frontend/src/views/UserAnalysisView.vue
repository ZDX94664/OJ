<template>
  <div id="userAnalysisView">
    <a-row :gutter="[16, 16]">
      <a-col :span="24">
        <a-card title="做题进度">
          <a-progress
            :percent="completionRate"
            :color="{
              '0%': '#108ee9',
              '100%': '#87d068',
            }"
          />
          <div class="stat-info">
            已完成 {{ completedQuestions }} 题 / 总共 {{ totalQuestions }} 题
          </div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="提交统计">
          <div ref="submissionChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="语言分布">
          <div ref="languageChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
      <a-col :span="24">
        <a-card title="近期活跃度">
          <div ref="activityChartRef" style="height: 300px"></div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, onUnmounted } from 'vue';
import * as echarts from 'echarts/core';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components';
import { PieChart, BarChart, LineChart } from 'echarts/charts';
import { LabelLayout } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';
import { QuestionControllerService } from '../../generated';
import message from "@arco-design/web-vue/es/message";

// 注册必需的组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  PieChart,
  BarChart,
  LineChart,
  LabelLayout,
  CanvasRenderer
]);

const completedQuestions = ref(0);
const totalQuestions = ref(0);
const completionRate = ref(0);

// 图表引用
const submissionChartRef = ref();
const languageChartRef = ref();
const activityChartRef = ref();

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    const res = await QuestionControllerService.getUserStatsUsingGet();
    if (res.code === 0 && res.data) {
      const { totalQuestions: total, completedQuestions: completed } = res.data;
      totalQuestions.value = total;
      completedQuestions.value = completed;
      // 计算百分比，转换为小数形式并保留两位小数
      const rawPercentage = Math.min((completed / total), 1);
      completionRate.value = total === 0 ? 0 : Number(rawPercentage.toFixed(2));

   
      // 初始化各个图表
      initSubmissionChart(res.data.submissionStats);
      initLanguageChart(res.data.languageStats);
      initActivityChart(res.data.activityStats);
    } else {
      message.error('获取统计数据失败：' + res.message);
    }
  } catch (error) {
    message.error('获取统计数据失败：' + error.message);
  }
};

// 初始化提交统计图表
const initSubmissionChart = (data: any) => {
  const chart = echarts.init(submissionChartRef.value);
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '做题结果',
        type: 'pie',
        radius: '50%',
        data: [
          {
            value: data.success || 0,
            name: '正确',
            itemStyle: {
              color: '#87CEEB' // 设置正确的颜色为蓝色
            }
          },
          {
            value: data.failed || 0,
            name: '错误',
            itemStyle: {
              color: '#FFB6C1' // 设置错误的颜色为绿色
            }
          },

        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  };
  chart.setOption(option);
};

// 初始化语言分布图表
const initLanguageChart = (data: any) => {
  const chart = echarts.init(languageChartRef.value);
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: Object.keys(data)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        data: Object.values(data),
        type: 'bar'
      }
    ]
  };
  chart.setOption(option);
};

// 初始化活跃度图表
const initActivityChart = (data: any) => {
  const chart = echarts.init(activityChartRef.value);
  // 将对象格式转换为数组
  const dates = Object.keys(data);
  const counts = Object.values(data);
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        rotate: 45 // 斜着显示日期，防止重叠
      }
    },
    yAxis: {
      type: 'value',
      name: '提交次数'
    },
    series: [
      {
        data: counts,
        type: 'line',
        smooth: true,
        areaStyle: {},
        name: '提交次数'
      }
    ]
  };
  chart.setOption(option);
};

// 监听窗口大小变化，重绘图表
const handleResize = () => {
  const charts = [
    echarts.getInstanceByDom(submissionChartRef.value),
    echarts.getInstanceByDom(languageChartRef.value),
    echarts.getInstanceByDom(activityChartRef.value)
  ];
  charts.forEach(chart => chart?.resize());
};

onMounted(() => {
  loadUserStats();
  window.addEventListener('resize', handleResize);
});

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
#userAnalysisView {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.stat-info {
  text-align: center;
  margin-top: 10px;
  color: #666;
}

.arco-card {
  height: 100%;
}
</style>
