<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMyScore, getScoreHistory } from '../../api/credit'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const scoreData = ref(null)
const scoreLoading = ref(false)

const historyData = ref([])
const historyLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadScore = async () => {
  try {
    scoreLoading.value = true
    const result = await getMyScore()
    scoreData.value = result
  } catch (error) {
    ElMessage.error(t('creditScore.loadScoreFailed'))
  } finally {
    scoreLoading.value = false
  }
}

const loadHistory = async () => {
  try {
    historyLoading.value = true
    const result = await getScoreHistory({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    historyData.value = result?.items || []
    total.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('creditScore.loadHistoryFailed'))
  } finally {
    historyLoading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadHistory()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  loadHistory()
}

const getScoreLevelType = (level: string) => {
  const map: Record<string, string> = {
    'EXCELLENT': 'success',
    'GOOD': 'primary',
    'WARNING': 'warning',
    'DANGER': 'danger',
    'FROZEN': 'danger',
  }
  return map[level] || 'info'
}

const getEventTypeTag = (type) => {
  const map = {
    'INITIAL': 'info',
    'AUDIT_PASS': 'success',
    'AUDIT_FAIL': 'danger',
    'TRADE_COMPLETE': 'primary',
    'PROJECT_VERIFY': 'success',
    'MANUAL_ADJUST': 'warning',
  }
  return map[type] || 'info'
}

onMounted(() => {
  loadScore()
  loadHistory()
})
</script>

<template>
  <PageContainer :title="t('creditScore.title')" :description="t('creditScore.description')">
    <section class="credit-score-page">
      <el-card class="section-card" shadow="never" v-loading="scoreLoading">
        <template #header>
          <span class="card-header">{{ t('creditScore.currentScore') }}</span>
        </template>
        <div v-if="scoreData" class="score-display">
          <div class="score-main">
            <div class="score-value">{{ scoreData.score || 0 }}</div>
            <el-tag :type="getScoreLevelType(scoreData.level)" size="large" class="score-level">
              {{ scoreData.level || 'N/A' }}
            </el-tag>
          </div>
          <div class="score-meta">
            <span>{{ t('creditScore.updateTime') }}：{{ scoreData.lastEvaluatedAt || '-' }}</span>
          </div>
        </div>
        <el-empty v-else :description="t('creditScore.emptyText')" />
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <span class="card-header">{{ t('creditScore.scoreHistory') }}</span>
        </template>
        <el-table :data="historyData" border v-loading="historyLoading">
          <el-table-column :label="t('common.total')" width="80">
            <template #default="scope">
              {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="eventType" :label="t('creditScore.colEventType')" min-width="140">
            <template #default="{ row }">
              <el-tag :type="getEventTypeTag(row.eventType)">
                {{ row.eventTypeName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pointsChanged" :label="t('creditScore.colChangeValue')" min-width="100">
            <template #default="{ row }">
              <span :class="row.pointsChanged >= 0 ? 'positive' : 'negative'">
                {{ row.pointsChanged >= 0 ? '+' : '' }}{{ row.pointsChanged }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="eventDescription" :label="t('creditScore.colReason')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" :label="t('creditScore.colTime')" min-width="170" />
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            background
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="onSizeChange"
            @current-change="onCurrentChange"
          />
        </div>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.credit-score-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.score-display {
  padding: 20px 0;
}

.score-main {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.score-value {
  font-size: 48px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1;
}

.score-level {
  font-size: 18px;
  padding: 8px 16px;
}

.score-meta {
  color: var(--text-secondary);
  font-size: 14px;
}

.positive {
  color: var(--el-color-success);
  font-weight: 600;
}

.negative {
  color: var(--el-color-danger);
  font-weight: 600;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
