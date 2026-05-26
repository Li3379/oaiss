<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getEnterpriseInference } from '@/api/enterpriseInference'
import { useAppStore } from '@/store'
import PageContainer from '@/components/PageContainer.vue'
import type { EnterpriseInferenceResponse } from '@/types/ai'

const { t } = useI18n()
const appStore = useAppStore()

const inferenceData = ref<EnterpriseInferenceResponse | null>(null)
const loading = ref(false)

function normalizeStatus(status: string | null | undefined): string {
  return (status || '').trim().toLowerCase().replace(/[-\s]+/g, '_')
}

function clampRatio(value: number | null | undefined): number {
  if (typeof value !== 'number' || Number.isNaN(value)) return 0
  return Math.min(1, Math.max(0, value))
}

function toPercentage(value: number | null | undefined): number {
  return Math.round(clampRatio(value) * 100)
}

function formatSignedScore(value: number | null | undefined): string {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '--'
  }

  return value.toFixed(3)
}

function getConfidenceColor(value: number | null | undefined): string {
  const safeValue = clampRatio(value)
  return safeValue >= 0.8 ? '#67c23a' : safeValue >= 0.5 ? '#e6a23c' : '#f56c6c'
}

function getAnomalyScoreColor(value: number | null | undefined): string {
  if (typeof value !== 'number' || Number.isNaN(value)) return 'var(--text-secondary)'
  if (value < 0) return '#f56c6c'
  if (value < 0.1) return '#e6a23c'
  return '#67c23a'
}

function localizeRiskFactor(factor: string): string {
  const factorMap: Record<string, string> = {
    'Low credit score': t('enterpriseInference.riskLowCreditScore'),
    'High compliance flag count': t('enterpriseInference.riskHighComplianceFlags'),
    'Extended reporting gap': t('enterpriseInference.riskExtendedReportingGap'),
    'Anomalous emission pattern detected': t('enterpriseInference.riskAnomalousEmissionPattern'),
    'High average emissions per report': t('enterpriseInference.riskHighAverageEmissions'),
  }

  return factorMap[factor] || factor
}

const loadData = async () => {
  const enterpriseId = appStore.enterpriseId
  if (!enterpriseId) {
    ElMessage.error(t('enterpriseInference.noEnterpriseId'))
    return
  }
  try {
    loading.value = true
    const result = await getEnterpriseInference(enterpriseId)
    inferenceData.value = result
  } catch (error) {
    ElMessage.error(t('enterpriseInference.loadFailed'))
  } finally {
    loading.value = false
  }
}

function getStatusType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  switch (normalizeStatus(status)) {
    case 'compliant': return 'success'
    case 'warning': return 'warning'
    case 'at_risk': return 'warning'
    case 'non_compliant': return 'danger'
    case 'unknown': return 'info'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  switch (normalizeStatus(status)) {
    case 'compliant': return t('enterpriseInference.compliant')
    case 'warning': return t('enterpriseInference.warning')
    case 'at_risk': return t('enterpriseInference.atRisk')
    case 'non_compliant': return t('enterpriseInference.nonCompliant')
    case 'unknown': return t('enterpriseInference.unknown')
    default: return status
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <PageContainer :title="t('enterpriseInference.title')" :description="t('enterpriseInference.description')">
    <section class="inference-page">
      <el-card class="section-card" shadow="never" :loading="loading">
        <template #header>
          <div class="card-header-row">
            <span class="card-header">{{ t('enterpriseInference.resultTitle') }}</span>
            <el-button type="primary" :loading="loading" @click="loadData">
              {{ t('enterpriseInference.refresh') }}
            </el-button>
          </div>
        </template>

        <template v-if="inferenceData">
          <el-row :gutter="16" class="stat-row">
            <el-col :xs="24" :sm="12" :md="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-label">{{ t('enterpriseInference.complianceStatus') }}</div>
                <div class="stat-value">
                  <el-tag :type="getStatusType(inferenceData.complianceStatus)" size="large">
                    {{ getStatusLabel(inferenceData.complianceStatus) }}
                  </el-tag>
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-label">{{ t('enterpriseInference.confidenceScore') }}</div>
                <div class="stat-value">
                  <el-progress
                    type="dashboard"
                    :percentage="toPercentage(inferenceData.confidence)"
                    :color="getConfidenceColor(inferenceData.confidence)"
                  />
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-label">{{ t('enterpriseInference.anomalyScore') }}</div>
                <div class="stat-value">
                  <span
                    class="score-value"
                    :style="{ color: getAnomalyScoreColor(inferenceData.anomalyScore) }"
                  >
                    {{ formatSignedScore(inferenceData.anomalyScore) }}
                  </span>
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-label">{{ t('enterpriseInference.anomalyDetection') }}</div>
                <div class="stat-value">
                  <el-tag :type="inferenceData.isAnomaly ? 'danger' : 'success'" size="large">
                    {{ inferenceData.isAnomaly ? t('enterpriseInference.isAnomaly') : t('enterpriseInference.notAnomaly') }}
                  </el-tag>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-card class="section-card" shadow="never" style="margin-top: 16px">
            <template #header>
              <span class="card-header">{{ t('enterpriseInference.riskFactors') }}</span>
            </template>
            <div v-if="inferenceData.riskFactors && inferenceData.riskFactors.length > 0" class="risk-factors">
              <el-tag
                v-for="factor in inferenceData.riskFactors"
                :key="factor"
                type="warning"
                class="risk-tag"
              >
                {{ localizeRiskFactor(factor) }}
              </el-tag>
            </div>
            <el-alert
              v-else
              :title="t('enterpriseInference.noRiskFactors')"
              type="success"
              :closable="false"
              show-icon
            />
          </el-card>

          <div class="model-footer">
            <span class="model-version">{{ t('enterpriseInference.modelVersion') }}：{{ inferenceData.modelVersion }}</span>
          </div>
        </template>

        <el-empty v-else-if="!loading" :description="t('enterpriseInference.emptyText')" />
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.inference-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.stat-row {
  margin-top: 4px;
}

.stat-card {
  text-align: center;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  min-height: 140px;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.stat-value {
  display: flex;
  justify-content: center;
  align-items: center;
}

.score-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.risk-factors {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.risk-tag {
  font-size: 14px;
}

.model-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
  text-align: right;
}

.model-version {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>
