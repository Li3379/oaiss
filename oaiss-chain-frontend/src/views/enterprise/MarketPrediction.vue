<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import echarts from '@/utils/echarts'
import { getMarketTrend, getMarketPrice, getSupplyDemand } from '@/api/marketPrediction'
import type { MarketForecastResponse, MarketForecastDataPoint } from '@/types/ai'

const { t } = useI18n()

const predictionType = ref('trend')
const horizonDays = ref(30)
const loading = ref(false)
const forecastResponse = ref<MarketForecastResponse | null>(null)
const chartRef = ref<HTMLElement | null>(null)
const emptyStateMessage = ref('')

let chartInstance: echarts.ECharts | null = null

const predictionTypeOptions = [
  { label: () => t('marketPrediction.typeTrend'), value: 'trend' },
  { label: () => t('marketPrediction.typePrice'), value: 'price' },
  { label: () => t('marketPrediction.typeSupplyDemand'), value: 'supply-demand' },
]

const horizonOptions = [7, 30, 90, 180]

function asNumberArray(value: number[] | null | undefined): number[] {
  return Array.isArray(value) ? value : []
}

function asStringArray(value: string[] | null | undefined): string[] {
  return Array.isArray(value) ? value : []
}

function transformToDataPoints(response: MarketForecastResponse): MarketForecastDataPoint[] {
  const forecastDates = asStringArray(response.forecastDates)
  const forecastPrices = asNumberArray(response.forecastPrices)
  const lowerBound = asNumberArray(response.lowerBound)
  const upperBound = asNumberArray(response.upperBound)

  return forecastDates.map((date, i) => ({
    date,
    price: forecastPrices[i] ?? 0,
    lowerBound: lowerBound[i] ?? 0,
    upperBound: upperBound[i] ?? 0,
  }))
}

const dataPoints = computed<MarketForecastDataPoint[]>(() => {
  if (!forecastResponse.value) return []
  return transformToDataPoints(forecastResponse.value)
})

const trendDirection = computed(() => forecastResponse.value?.trend ?? '-')
const modelVersion = computed(() => forecastResponse.value?.modelVersion ?? '-')

const trendTagType = computed(() => {
  const trend = forecastResponse.value?.trend?.toLowerCase() ?? ''
  if (trend.includes('up') || trend.includes('rise')) return 'success'
  if (trend.includes('down') || trend.includes('fall') || trend.includes('decline')) return 'danger'
  return 'warning'
})

function resetForecastState() {
  forecastResponse.value = null
  emptyStateMessage.value = ''
}

const fetchForecast = async () => {
  loading.value = true
  try {
    const fetcher: Record<string, (days: number) => Promise<MarketForecastResponse>> = {
      trend: getMarketTrend,
      price: getMarketPrice,
      'supply-demand': getSupplyDemand,
    }
    const fn = fetcher[predictionType.value]
    const result = await fn(horizonDays.value)
    forecastResponse.value = result
    emptyStateMessage.value = dataPoints.value.length === 0
      ? t('marketPrediction.insufficientData')
      : ''
    await nextTick()
    renderChart()
  } catch (error) {
    resetForecastState()
    ElMessage.error(t('marketPrediction.loadFailed'))
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const points = dataPoints.value
  if (points.length === 0) {
    chartInstance.clear()
    return
  }

  const option = {
    title: { text: t('marketPrediction.chartTitle'), left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: [t('marketPrediction.price'), t('marketPrediction.confidenceBand')], bottom: 0 },
    grid: { left: 60, right: 24, top: 50, bottom: 48 },
    xAxis: { type: 'category', data: points.map(d => d.date) },
    yAxis: { type: 'value', name: t('marketPrediction.priceUnit') },
    series: [
      {
        name: t('marketPrediction.price'),
        type: 'line',
        data: points.map(d => d.price),
        smooth: true,
        lineStyle: { width: 3 },
      },
      {
        name: t('marketPrediction.confidenceBand'),
        type: 'line',
        data: points.map(d => d.upperBound),
        lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        stack: 'confidence',
        symbol: 'none',
      },
      {
        name: t('marketPrediction.lowerBound'),
        type: 'line',
        data: points.map((d, i) => d.lowerBound - points[i].upperBound),
        lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(64,158,255,0.15)' },
        stack: 'confidence',
        symbol: 'none',
      },
    ],
  }

  chartInstance.setOption(option, true)
}

function onResize() {
  chartInstance?.resize()
}

let debounceTimer: number | null = null

function debouncedFetch() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => fetchForecast(), 300)
}

watch(predictionType, () => debouncedFetch())

watch(horizonDays, () => debouncedFetch())

onMounted(() => {
  fetchForecast()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<template>
  <section class="market-prediction-page">
    <el-card class="section-card" shadow="never">
      <div class="header-row">
        <h2 class="page-title">{{ t('marketPrediction.title') }}</h2>
        <el-select v-model="predictionType" style="width: 180px">
          <el-option
            v-for="opt in predictionTypeOptions"
            :key="opt.value"
            :label="opt.label()"
            :value="opt.value"
          />
        </el-select>
      </div>
    </el-card>

    <el-row :gutter="12" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ t('marketPrediction.trendDirection') }}</div>
          <div class="stat-value">
            <el-tag :type="trendTagType" size="large">{{ trendDirection }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ t('marketPrediction.modelVersion') }}</div>
          <div class="stat-value model-version">{{ modelVersion }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">{{ t('marketPrediction.dataPoints') }}</div>
          <div class="stat-value">{{ dataPoints.length }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" shadow="never" v-loading="loading">
      <div class="horizon-row">
        <span class="horizon-label">{{ t('marketPrediction.horizon') }}：</span>
        <el-button
          v-for="days in horizonOptions"
          :key="days"
          :type="horizonDays === days ? 'primary' : 'default'"
          size="small"
          @click="horizonDays = days"
        >
          {{ days }} {{ t('marketPrediction.days') }}
        </el-button>
      </div>
      <el-empty
        v-if="!loading && dataPoints.length === 0"
        :description="emptyStateMessage || t('marketPrediction.noData')"
        class="chart-empty"
      />
      <div v-show="dataPoints.length > 0" ref="chartRef" class="chart-box" />
    </el-card>
  </section>
</template>

<style scoped>
.market-prediction-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card,
.stat-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.stats-row {
  margin: 0;
}

.stat-card {
  text-align: center;
}

.stat-label {
  color: var(--text-secondary);
  font-size: 13px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.model-version {
  font-size: 18px;
}

.horizon-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.horizon-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.chart-box {
  width: 100%;
  height: 420px;
}

.chart-empty {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .chart-box {
    height: 300px;
  }

  .chart-empty {
    min-height: 300px;
  }
}
</style>
