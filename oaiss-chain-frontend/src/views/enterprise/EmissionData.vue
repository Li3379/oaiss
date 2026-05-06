<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getEnterpriseRatings, getIndustryRankings, predictEmission } from '../../api/emission'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const activeTab = ref('ratings')

const ratings = ref([])
const ratingsLoading = ref(false)
const ratingsPage = ref(1)
const ratingsPageSize = ref(10)
const ratingsTotal = ref(0)

const rankings = ref([])
const rankingsLoading = ref(false)
const rankingsYear = ref(new Date().getFullYear())

const predictLoading = ref(false)
const predictResult = ref(null)
const predictForm = ref({
  enterpriseId: '',
  historicalData: '',
  predictionMonths: 6,
})

const loadRatings = async () => {
  try {
    ratingsLoading.value = true
    const enterpriseId = localStorage.getItem('enterpriseId') || ''
    if (!enterpriseId) {
      ratings.value = []
      return
    }
    const result = await getEnterpriseRatings(enterpriseId)
    ratings.value = result?.items || []
    ratingsTotal.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('emissionData.loadRatingFailed'))
  } finally {
    ratingsLoading.value = false
  }
}

const loadRankings = async () => {
  try {
    rankingsLoading.value = true
    const result = await getIndustryRankings(rankingsYear.value)
    rankings.value = result?.items || []
  } catch (error) {
    ElMessage.error(t('emissionData.loadRankingFailed'))
  } finally {
    rankingsLoading.value = false
  }
}

const onPredict = async () => {
  try {
    predictLoading.value = true
    let data
    try {
      data = JSON.parse(predictForm.value.historicalData || '[]')
    } catch (parseError) {
      ElMessage.error(t('emissionData.historicalDataFormatError'))
      return
    }
    if (!Array.isArray(data)) {
      ElMessage.error(t('emissionData.historicalDataMustBeArray'))
      return
    }
    const predictionResult = await predictEmission({
      enterpriseId: predictForm.value.enterpriseId || undefined,
      historicalData: data,
      predictionMonths: predictForm.value.predictionMonths,
    })
    predictResult.value = predictionResult
    ElMessage.success(t('emissionData.predictionComplete'))
  } catch (error) {
    ElMessage.error(t('emissionData.predictionFailed'))
  } finally {
    predictLoading.value = false
  }
}

const getRatingTag = (level) => {
  const map = { A: 'success', B: 'primary', C: 'warning', D: 'danger' }
  return map[level] || 'info'
}

onMounted(() => {
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
              <el-table-column prop="industryAvg" :label="t('emissionData.colIndustryAvg')" min-width="140" />
              <el-table-column prop="score" :label="t('emissionData.colScore')" min-width="100" />
              <el-table-column prop="ratingTime" :label="t('emissionData.colRatingTime')" min-width="170" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('emissionData.tabRanking')" name="rankings">
            <div style="margin-bottom: 14px; display: flex; align-items: center; gap: 12px">
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
              <el-table-column prop="enterpriseName" :label="t('emissionData.colEnterpriseName')" min-width="200" show-overflow-tooltip />
              <el-table-column prop="totalEmission" :label="t('emissionData.colTotalEmission')" min-width="140" />
              <el-table-column prop="ratingLevel" :label="t('emissionData.colRating')" min-width="80">
                <template #default="{ row }">
                  <el-tag :type="getRatingTag(row.ratingLevel)">{{ row.ratingLevel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="score" :label="t('emissionData.colScore')" min-width="100" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('emissionData.tabPrediction')" name="predict">
            <div style="padding: 20px 0; max-width: 700px">
              <el-form label-width="120px">
                <el-form-item :label="t('emissionData.predictionMonths')">
                  <el-input-number v-model="predictForm.predictionMonths" :min="1" :max="24" />
                </el-form-item>
                <el-form-item :label="t('emissionData.historicalData')">
                  <el-input
                    v-model="predictForm.historicalData"
                    type="textarea"
                    :rows="6"
                    :placeholder="t('emissionData.historicalDataFormat')"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="predictLoading" @click="onPredict">{{ t('emissionData.tabPrediction') }}</el-button>
                </el-form-item>
              </el-form>

              <el-descriptions v-if="predictResult" :title="t('emissionData.predictionResult')" :column="2" border style="margin-top: 20px">
                <el-descriptions-item :label="t('emissionData.predictionTrend')">
                  <el-tag :type="predictResult.trend === 'DOWN' ? 'success' : predictResult.trend === 'UP' ? 'danger' : 'warning'">
                    {{ predictResult.trend === 'DOWN' ? t('emissionData.trendDown') : predictResult.trend === 'UP' ? t('emissionData.trendUp') : t('emissionData.trendStable') }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item :label="t('emissionData.predictedEmission')">{{ predictResult.predictedEmission }} tCO2e</el-descriptions-item>
                <el-descriptions-item :label="t('emissionData.confidence')">{{ predictResult.confidence }}%</el-descriptions-item>
                <el-descriptions-item :label="t('emissionData.suggestion')">{{ predictResult.suggestion || '-' }}</el-descriptions-item>
              </el-descriptions>
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
</style>
