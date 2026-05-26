<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getMyRating, getIndustryRankings, predictEmission } from '../../api/emission'
import { getEnterpriseInfo } from '../../api/enterprise'
import type { EmissionRating, CarbonPredictionResponse } from '../../types'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const activeTab = ref('ratings')

const ratings = ref<EmissionRating[]>([])
const ratingsLoading = ref(false)

type RankingRow = EmissionRating & { enterpriseName?: string }

const rankings = ref<RankingRow[]>([])
const rankingsLoading = ref(false)
const rankingsYear = ref(String(new Date().getFullYear()))

const predictLoading = ref(false)
const predictResult = ref<CarbonPredictionResponse | null>(null)
const predictForm = ref({
  enterpriseId: '',
  predictMonths: 6,
})

const enterpriseIdNumber = computed(() => {
  const value = Number(predictForm.value.enterpriseId)
  return Number.isFinite(value) && value > 0 ? value : null
})

const latestPrediction = computed(() => {
  const predictions = predictResult.value?.predictions || []
  return predictions.length > 0 ? predictions[predictions.length - 1] : null
})

const loadEnterpriseContext = async () => {
  try {
    const enterprise = await getEnterpriseInfo() as { id?: number }
    if (enterprise?.id) {
      predictForm.value.enterpriseId = String(enterprise.id)
    }
  } catch {
    // Keep manual input available if enterprise context is unavailable.
  }
}

const loadRatings = async () => {
  try {
    ratingsLoading.value = true
    const result = await getMyRating()
    ratings.value = Array.isArray(result) ? result : []
  } catch {
    ElMessage.error(t('emissionData.loadRatingFailed'))
  } finally {
    ratingsLoading.value = false
  }
}

const loadRankings = async () => {
  try {
    rankingsLoading.value = true
    const result = await getIndustryRankings(Number(rankingsYear.value))
    rankings.value = Array.isArray(result) ? result : (result?.items || [])
  } catch {
    ElMessage.error(t('emissionData.loadRankingFailed'))
  } finally {
    rankingsLoading.value = false
  }
}

const onPredict = async () => {
  if (!enterpriseIdNumber.value) {
    ElMessage.error(t('emissionData.enterpriseIdRequired'))
    return
  }

  try {
    predictLoading.value = true
    const predictionResult = await predictEmission({
      enterpriseId: enterpriseIdNumber.value,
      predictMonths: predictForm.value.predictMonths,
    })
    predictResult.value = predictionResult
    ElMessage.success(t('emissionData.predictionComplete'))
  } catch {
    ElMessage.error(t('emissionData.predictionFailed'))
  } finally {
    predictLoading.value = false
  }
}

const getRatingTag = (level?: string) => {
  const map: Record<string, string> = { A: 'success', B: 'primary', C: 'warning', D: 'danger' }
  return level ? (map[level] || 'info') : 'info'
}

onMounted(() => {
  loadEnterpriseContext()
  loadRatings()
  loadRankings()
})
</script>

<template>
  <PageContainer :title="t('emissionData.title')" :description="t('emissionData.description')">
    <section class="emission-page">
      <el-card class="section-card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('emissionData.tabRating')" name="ratings">
            <el-table :data="ratings" border v-loading="ratingsLoading">
              <el-table-column :label="t('common.operation')" width="80">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="ratingLevel" :label="t('emissionData.colRating')" min-width="100">
                <template #default="{ row }">
                  <el-tag :type="getRatingTag(row.ratingLevel)">{{ row.ratingLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalEmission" :label="t('emissionData.colTotalEmission')" min-width="140" />
              <el-table-column prop="emissionIntensity" :label="t('emissionData.colIndustryAvg')" min-width="140">
                <template #default="{ row }">{{ row.emissionIntensity ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="ratingScore" :label="t('emissionData.colScore')" min-width="100" />
              <el-table-column prop="ratingYear" :label="t('emissionData.colRatingTime')" min-width="170" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('emissionData.tabRanking')" name="rankings">
            <div class="toolbar-row">
              <span>{{ t('emissionData.yearPicker') }}：</span>
              <el-date-picker
                v-model="rankingsYear"
                type="year"
                value-format="YYYY"
                :placeholder="t('emissionData.selectYear')"
                @change="loadRankings"
              />
            </div>
            <el-table :data="rankings" border v-loading="rankingsLoading">
              <el-table-column :label="t('emissionData.colRank')" width="80">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="enterpriseId" :label="t('emissionData.colEnterpriseName')" min-width="200">
                <template #default="{ row }">{{ row.enterpriseName || `企业ID ${row.enterpriseId}` }}</template>
              </el-table-column>
              <el-table-column prop="totalEmission" :label="t('emissionData.colTotalEmission')" min-width="140" />
              <el-table-column prop="ratingLevel" :label="t('emissionData.colRating')" min-width="80">
                <template #default="{ row }">
                  <el-tag :type="getRatingTag(row.ratingLevel)">{{ row.ratingLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="ratingScore" :label="t('emissionData.colScore')" min-width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('emissionData.tabPrediction')" name="predict">
            <div class="predict-panel">
              <el-form label-width="140px">
                <el-form-item label="企业ID">
                  <el-input v-model="predictForm.enterpriseId" placeholder="请输入企业ID" />
                </el-form-item>
                <el-form-item :label="t('emissionData.predictionMonths')">
                  <el-input-number v-model="predictForm.predictMonths" :min="1" :max="24" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="predictLoading" @click="onPredict">{{ t('emissionData.tabPrediction') }}</el-button>
                </el-form-item>
              </el-form>

              <el-alert
                title="当前预测接口仅使用企业ID和预测月份，历史数据仅作页面说明"
                type="info"
                :closable="false"
                show-icon
                class="prediction-alert"
              />

              <el-descriptions
                v-if="predictResult"
                :title="t('emissionData.predictionResult')"
                :column="2"
                border
                class="prediction-result"
              >
                <el-descriptions-item label="企业ID">{{ predictResult.enterpriseId }}</el-descriptions-item>
                <el-descriptions-item :label="t('emissionData.confidence')">{{ predictResult.confidence }}%</el-descriptions-item>
                <el-descriptions-item label="预测说明" :span="2">
                  {{ predictResult.message || '-' }}
                </el-descriptions-item>
                <el-descriptions-item :label="t('emissionData.predictedEmission')">
                  {{ latestPrediction ? `${latestPrediction.predictedEmission} tCO2e` : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="生成时间">
                  {{ predictResult.generatedAt || '-' }}
                </el-descriptions-item>
              </el-descriptions>

              <el-table
                v-if="predictResult?.predictions?.length"
                :data="predictResult.predictions"
                border
                class="prediction-table"
              >
                <el-table-column prop="period" label="预测周期" min-width="140" />
                <el-table-column prop="predictedEmission" :label="t('emissionData.predictedEmission')" min-width="180">
                  <template #default="{ row }">{{ row.predictedEmission }} tCO2e</template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.emission-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.toolbar-row {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.predict-panel {
  padding: 20px 0;
  max-width: 720px;
}

.prediction-alert {
  margin-top: 8px;
}

.prediction-result {
  margin-top: 20px;
}

.prediction-table {
  margin-top: 16px;
}
</style>
