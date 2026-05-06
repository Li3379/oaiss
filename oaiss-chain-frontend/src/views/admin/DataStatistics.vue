<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { User, OfficeBuilding, DocumentChecked, UserFilled } from '@element-plus/icons-vue'
import echarts from '../../utils/echarts'
import { getStatistics } from '../../api/admin'

const { t } = useI18n()

const statistics = ref({
  totalUsers: 0,
  enterpriseCount: 0,
  reviewerCount: 0,
  thirdPartyCount: 0,
})

const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null

const fetchData = async () => {
  loading.value = true
  try {
    const response = await getStatistics()
    statistics.value = response
  } catch (error) {
    ElMessage.error(t('dataStatistics.loadFailed'))
  } finally {
    loading.value = false
  }
}

const getUserTypeDistribution = computed(() => {
  return [
    { name: t('dataStatistics.pieEnterprise'), value: statistics.value.enterpriseCount },
    { name: t('dataStatistics.pieAuditor'), value: statistics.value.reviewerCount },
    { name: t('dataStatistics.pieThirdParty'), value: statistics.value.thirdPartyCount },
    { name: t('dataStatistics.pieAdmin'), value: statistics.value.totalUsers - statistics.value.enterpriseCount - statistics.value.reviewerCount - statistics.value.thirdPartyCount },
  ].filter(item => item.value > 0)
})

const renderChart = async () => {
  await nextTick()
  const dom = chartRef.value
  if (!dom) {
    return
  }

  if (!chartInstance) {
    chartInstance = echarts.init(dom)
  }

  const option = {
    title: {
      text: t('dataStatistics.chartUserTypePie'),
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      left: 'center',
      bottom: 10,
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '50%'],
        label: {
          formatter: '{b}: {d}%',
        },
        data: getUserTypeDistribution.value,
      },
    ],
  }

  chartInstance.setOption(option, true)
}

const onResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(statistics, () => {
  renderChart()
})

onMounted(() => {
  fetchData()
  renderChart()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chartInstance) {
    chartInstance.dispose()
  }
})
</script>

<template>
  <section class="stats-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('dataStatistics.breadcrumbDataManage') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('dataStatistics.breadcrumbStatistics') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <div class="stats-grid">
      <el-card class="stat-card" shadow="never" v-loading="loading">
        <el-statistic :title="t('dataStatistics.statTotalUsers')" :value="statistics.totalUsers">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-statistic>
      </el-card>

      <el-card class="stat-card" shadow="never" v-loading="loading">
        <el-statistic :title="t('dataStatistics.statEnterpriseCount')" :value="statistics.enterpriseCount">
          <template #prefix>
            <el-icon><OfficeBuilding /></el-icon>
          </template>
        </el-statistic>
      </el-card>

      <el-card class="stat-card" shadow="never" v-loading="loading">
        <el-statistic :title="t('dataStatistics.statAuditorCount')" :value="statistics.reviewerCount">
          <template #prefix>
            <el-icon><DocumentChecked /></el-icon>
          </template>
        </el-statistic>
      </el-card>

      <el-card class="stat-card" shadow="never" v-loading="loading">
        <el-statistic :title="t('dataStatistics.statThirdPartyCount')" :value="statistics.thirdPartyCount">
          <template #prefix>
            <el-icon><UserFilled /></el-icon>
          </template>
        </el-statistic>
      </el-card>
    </div>

    <el-card class="chart-card" shadow="never">
      <div ref="chartRef" class="chart-box" />
    </el-card>
  </section>
</template>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card,
.stat-card,
.chart-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.stat-card {
  padding: 20px;
}

.chart-box {
  width: 100%;
  height: 400px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
