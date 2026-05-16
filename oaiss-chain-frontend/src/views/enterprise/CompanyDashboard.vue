<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import echarts from '../../utils/echarts'
import { getMyReports } from '../../api/carbon'
import { getMyTrades } from '../../api/trade'
import { getMyScore } from '../../api/credit'
import { getMyEnterpriseAdmission } from '../../api/enterprise'

const { t } = useI18n()

const loading = ref(false)
const queryForm = reactive({
  assetNo: '',
  otherCondition: '',
  keyword: '',
})

const appliedFilters = ref({
  assetNo: '',
  otherCondition: '',
  keyword: '',
})

const timeDimension = ref('month')

const assetPool = ref([])
const userProfile = ref(null)
const tradeData = ref([])
const carbonReports = ref([])
const admissionStatus = ref<Record<string, unknown> | null>(null)

const chartTradeBarRef = ref(null)
const chartTrendLineRef = ref(null)
const chartSuggestBarRef = ref(null)
const chartEmissionPieRef = ref(null)
const chartTradePieRef = ref(null)
const chartCreditLineRef = ref(null)

const chartInstances = []

const filteredAssets = computed(() => {
  const f = appliedFilters.value
  const assetNo = f.assetNo.trim().toLowerCase()
  const otherCondition = f.otherCondition.trim().toLowerCase()
  const keyword = f.keyword.trim().toLowerCase()

  return assetPool.value.filter((item) => {
    const matchAssetNo = !assetNo || item.assetNo.toLowerCase().includes(assetNo)
    const matchOther = !otherCondition || item.other.toLowerCase().includes(otherCondition)
    const matchKeyword = !keyword || item.keyword.toLowerCase().includes(keyword)
    return matchAssetNo && matchOther && matchKeyword
  })
})

const filteredFactor = computed(() => {
  if (filteredAssets.value.length === 0) {
    return 0
  }
  return filteredAssets.value.length / (assetPool.value.length || 1)
})

const currentData = computed(() => {
  const factor = filteredFactor.value
  const round = (n) => Math.round(n)

  const trades = tradeData.value || []
  const reports = carbonReports.value || []

  const labels = trades.map(t => {
    if (t.createdAt) {
      const d = new Date(t.createdAt)
      return `${d.getMonth() + 1}/${d.getDate()}`
    }
    return ''
  })
  const tradeCount = trades.map(t => Number(t.quantity || t.totalAmount || 0))
  const aiTrend = reports.map(r => Number(r.totalEmission || 0))
  const aiSuggest = reports.map(r => Number(r.scope1Emission || 0) + Number(r.scope2Emission || 0))
  const creditTrend = trades.map((_, i) => 60 + Math.min(i * 5, 40))

  const summary = {
    carbonCoinTotal: userProfile.value?.carbonCoins || 0,
    carbonQuotaTotal: userProfile.value?.carbonQuota || userProfile.value?.quota || 0,
    creditScore: userProfile.value?.score || userProfile.value?.creditScore || 0,
  }

  return {
    labels,
    tradeCount: tradeCount.map((v) => round(v * factor)),
    aiTrend: aiTrend.map((v) => round(v * factor)),
    aiSuggest: aiSuggest.map((v) => round(v * factor)),
    creditTrend: creditTrend.map((v) => round(v * (0.9 + factor * 0.1))),
    summary,
    emissionPie: [
      { name: t('companyDashboard.pieGasEmission'), value: reports.reduce((sum, r) => sum + Number(r.scope1Emission || 0), 0) },
      { name: t('companyDashboard.pieWaterEmission'), value: reports.reduce((sum, r) => sum + Number(r.scope2Emission || 0), 0) },
      { name: t('companyDashboard.pieSolidEmission'), value: reports.reduce((sum, r) => sum + Number(r.scope3Emission || 0), 0) },
      { name: t('companyDashboard.piePredictedEmission'), value: reports.reduce((sum, r) => sum + Number(r.totalEmission || 0) * 0.1, 0) },
      { name: t('companyDashboard.pieRatedEmission'), value: reports.reduce((sum, r) => sum + Number(r.totalEmission || 0), 0) },
    ].filter(item => item.value > 0),
    tradePie: [
      { name: t('companyDashboard.pieTransactionExpense'), value: trades.reduce((sum, t) => sum + Number(t.totalAmount || 0), 0) },
      { name: t('companyDashboard.pieCarbonCoinConvert'), value: trades.reduce((sum, t) => sum + Number(t.quantity || 0) * 0.5, 0) },
      { name: t('companyDashboard.pieBlockchainGenerate'), value: trades.reduce((sum, t) => sum + Number(t.quantity || 0) * 0.3, 0) },
      { name: t('companyDashboard.pieOriginal'), value: trades.reduce((sum, t) => sum + Number(t.quantity || 0) * 0.15, 0) },
      { name: t('companyDashboard.pieQuotaPurchase'), value: trades.reduce((sum, t) => sum + Number(t.quantity || 0) * 0.05, 0) },
    ].filter(item => item.value > 0),
  }
})

const overviewCards = computed(() => [
  { label: t('companyDashboard.cardCarbonCoin'), value: currentData.value.summary.carbonCoinTotal },
  { label: t('companyDashboard.cardCarbonQuota'), value: currentData.value.summary.carbonQuotaTotal },
  { label: t('companyDashboard.cardCreditScore'), value: currentData.value.summary.creditScore },
])

const percentFormatter = (params, sourceArr) => {
  const total = sourceArr.reduce((sum, n) => sum + n, 0) || 1
  const percent = ((params.value / total) * 100).toFixed(2)
  return `${params.name}<br/>${t('companyDashboard.tooltipValue')}：${params.value}<br/>${t('companyDashboard.tooltipRatio')}：${percent}%`
}

const buildBarOption = (title, labels, data, color) => ({
  title: { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } },
  tooltip: {
    trigger: 'axis',
    formatter: (params) => percentFormatter(params[0], data),
  },
  grid: { left: 50, right: 24, top: 50, bottom: 28 },
  xAxis: { type: 'category', data: labels },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data,
      barMaxWidth: 30,
      itemStyle: {
        color,
        borderRadius: [5, 5, 0, 0],
      },
    },
  ],
})

const buildLineOption = (title, labels, data, color) => ({
  title: { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } },
  tooltip: {
    trigger: 'axis',
    formatter: (params) => percentFormatter(params[0], data),
  },
  grid: { left: 50, right: 24, top: 50, bottom: 28 },
  xAxis: { type: 'category', data: labels },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      data,
      symbolSize: 7,
      lineStyle: { color, width: 3 },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(24, 169, 154, 0.35)' },
          { offset: 1, color: 'rgba(24, 169, 154, 0.02)' },
        ]),
      },
    },
  ],
})

const buildPieOption = (title, data) => ({
  title: { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } },
  tooltip: {
    trigger: 'item',
    formatter: `{b}<br/>${t('companyDashboard.tooltipValue')}：{c}<br/>${t('companyDashboard.tooltipRatio')}：{d}%`,
  },
  legend: {
    bottom: 4,
    left: 'center',
  },
  series: [
    {
      type: 'pie',
      radius: ['38%', '64%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: true,
      label: {
        formatter: '{b}: {d}%',
      },
      data,
    },
  ],
})

const getChartConfigs = () => {
  const d = currentData.value
  return [
    {
      ref: chartTradeBarRef,
      option: buildBarOption(t('companyDashboard.chartTransactionBar'), d.labels, d.tradeCount, '#2fb38f'),
    },
    {
      ref: chartTrendLineRef,
      option: buildLineOption(t('companyDashboard.chartAIPrediction'), d.labels, d.aiTrend, '#22a49a'),
    },
    {
      ref: chartSuggestBarRef,
      option: buildBarOption(t('companyDashboard.chartAISuggestion'), d.labels, d.aiSuggest, '#5ec97f'),
    },
    {
      ref: chartEmissionPieRef,
      option: buildPieOption(t('companyDashboard.chartEmissionPie'), d.emissionPie),
    },
    {
      ref: chartTradePieRef,
      option: buildPieOption(t('companyDashboard.chartTransactionPie'), d.tradePie),
    },
    {
      ref: chartCreditLineRef,
      option: buildLineOption(t('companyDashboard.chartCreditLine'), d.labels, d.creditTrend, '#4fa7d6'),
    },
  ]
}

const renderCharts = async () => {
  await nextTick()
  const configs = getChartConfigs()

  configs.forEach(({ ref: domRef, option }, index) => {
    const dom = domRef.value
    if (!dom) {
      return
    }

    if (!chartInstances[index]) {
      chartInstances[index] = echarts.init(dom)
    }

    chartInstances[index].setOption(option, true)
  })
}

const fetchUserProfile = async () => {
  try {
    const result = await getMyScore()
    userProfile.value = result
  } catch (error) {
    ElMessage.error(t('companyDashboard.loadUserFailed'))
  }
}

const fetchTradeData = async () => {
  try {
    const result = await getMyTrades({
      pageNum: 1,
      pageSize: 100,
      timeDimension: timeDimension.value,
    })
    tradeData.value = result?.items || []
  } catch (error) {
    ElMessage.error(t('companyDashboard.loadTradeFailed'))
    tradeData.value = []
  }
}

const fetchCarbonReports = async () => {
  try {
    const result = await getMyReports({
      pageNum: 1,
      pageSize: 100,
      timeDimension: timeDimension.value,
    })
    carbonReports.value = result?.items || []

    assetPool.value = (result?.items || []).map((report, index) => ({
      assetNo: report.assetNo || report.reportNo || 'AST-' + (1001 + index),
      other: report.category || report.reportType || '',
      keyword: report.region || report.accountingPeriod || '',
    }))
  } catch (error) {
    ElMessage.error(t('companyDashboard.loadEmissionFailed'))
    carbonReports.value = []
    assetPool.value = []
  }
}

const fetchAdmissionStatus = async () => {
  try {
    const res = await getMyEnterpriseAdmission()
    const list = Array.isArray(res) ? res : ((res as Record<string, unknown>)?.items as unknown[] || [])
    admissionStatus.value = list.length > 0 ? list[0] as Record<string, unknown> : null
  } catch {
    admissionStatus.value = null
  }
}

const loadDashboardData = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchUserProfile(),
      fetchTradeData(),
      fetchCarbonReports(),
      fetchAdmissionStatus(),
    ])
    await renderCharts()
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  appliedFilters.value = {
    assetNo: queryForm.assetNo,
    otherCondition: queryForm.otherCondition,
    keyword: queryForm.keyword,
  }
  ElMessage.success(t('companyDashboard.filterComplete'))
  renderCharts()
}

const onResize = () => {
  chartInstances.forEach((instance) => {
    if (instance) {
      instance.resize()
    }
  })
}

watch(timeDimension, () => {
  loadDashboardData()
})

watch(filteredAssets, () => {
  renderCharts()
})

onMounted(() => {
  loadDashboardData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chartInstances.forEach((instance) => {
    if (instance) {
      instance.dispose()
    }
  })
})
</script>

<template>
  <section class="dashboard-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('companyDashboard.breadcrumbCompany') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('companyDashboard.breadcrumbDashboard') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="search-row">
        <div class="search-left">
          <el-input v-model="queryForm.assetNo" :placeholder="t('companyDashboard.searchAssetNo')" clearable />
          <el-input v-model="queryForm.otherCondition" :placeholder="t('companyDashboard.searchOther')" clearable />
          <el-input v-model="queryForm.keyword" :placeholder="t('companyDashboard.searchText')" clearable />
          <el-button type="primary" @click="onSearch" :loading="loading">{{ t('common.search') }}</el-button>
        </div>

        <div class="search-right">
          <span class="dimension-label">{{ t('companyDashboard.timeDimension') }}：</span>
          <el-radio-group v-model="timeDimension">
            <el-radio-button label="day">{{ t('companyDashboard.timeDay') }}</el-radio-button>
            <el-radio-button label="month">{{ t('companyDashboard.timeMonth') }}</el-radio-button>
            <el-radio-button label="year">{{ t('companyDashboard.timeYear') }}</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else class="overview-grid">
      <el-card v-for="item in overviewCards" :key="item.label" class="overview-card" shadow="hover">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
      </el-card>
    </div>

    <el-card v-if="admissionStatus !== null" class="section-card" shadow="never">
      <el-space>
        <span>{{ t('companyDashboard.admissionStatus') }}:</span>
        <el-tag :type="admissionStatus.status === 1 ? 'success' : 'danger'">
          {{ admissionStatus.status === 1 ? t('certificateManage.active') : t('certificateManage.revoked') }}
        </el-tag>
        <span v-if="admissionStatus.certificateNo" style="color: #999; font-size: 13px;">
          {{ admissionStatus.certificateNo }}
        </span>
      </el-space>
    </el-card>

    <div v-if="!loading" class="chart-grid">
      <el-card class="chart-card" shadow="never"><div ref="chartTradeBarRef" class="chart-box" /></el-card>
      <el-card class="chart-card" shadow="never"><div ref="chartTrendLineRef" class="chart-box" /></el-card>
      <el-card class="chart-card" shadow="never"><div ref="chartSuggestBarRef" class="chart-box" /></el-card>
      <el-card class="chart-card" shadow="never"><div ref="chartEmissionPieRef" class="chart-box" /></el-card>
      <el-card class="chart-card" shadow="never"><div ref="chartTradePieRef" class="chart-box" /></el-card>
      <el-card class="chart-card" shadow="never"><div ref="chartCreditLineRef" class="chart-box" /></el-card>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card,
.chart-card,
.overview-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.search-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.search-left {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 10px;
  flex: 1;
}

.search-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dimension-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.loading-container {
  padding: 20px;
  background: white;
  border-radius: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(180px, 1fr));
  gap: 12px;
}

.overview-card {
  background: linear-gradient(130deg, rgba(20, 167, 154, 0.12), rgba(69, 190, 117, 0.1));
}

.metric-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.metric-value {
  font-size: 30px;
  margin-top: 10px;
  color: #0f3d40;
  font-weight: 700;
  line-height: 1;
}

.metric-unit {
  margin-top: 8px;
  color: #2f6268;
  font-size: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(280px, 1fr));
  gap: 12px;
}

.chart-box {
  width: 100%;
  height: 330px;
}

@media (max-width: 1280px) {
  .search-left {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 768px) {
  .search-left {
    grid-template-columns: 1fr;
  }

  .overview-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .search-right {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
