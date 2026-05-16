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
  switch (status) {
    case 'compliant': return 'success'
    case 'warning': return 'warning'
    case 'non-compliant': return 'danger'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  switch (status) {
    case 'compliant': return t('enterpriseInference.compliant')
    case 'warning': return t('enterpriseInference.warning')
    case 'non-compliant': return t('enterpriseInference.nonCompliant')
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
                    :percentage="Math.round(inferenceData.confidence * 100)"
                    :color="inferenceData.confidence >= 0.8 ? '#67c23a' : inferenceData.confidence >= 0.5 ? '#e6a23c' : '#f56c6c'"
                  />
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :md="6">
              <el-card shadow="hover" class="stat-card">
                <div class="stat-label">{{ t('enterpriseInference.anomalyScore') }}</div>
                <div class="stat-value">
                  <el-progress
                    type="dashboard"
                    :percentage="Math.round(inferenceData.anomalyScore * 100)"
                    :color="inferenceData.anomalyScore > 0.7 ? '#f56c6c' : inferenceData.anomalyScore > 0.4 ? '#e6a23c' : '#67c23a'"
                  />
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
                {{ factor }}
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
