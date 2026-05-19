<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getCarbonReports, getStatistics } from '../../api/thirdParty'
import { formatDateTime } from '../../utils/format'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const statistics = ref({
  totalReports: 0,
  pendingReports: 0,
  approvedReports: 0,
  rejectedReports: 0,
})

const approvalRate = computed(() => {
  const total = statistics.value.approvedReports + statistics.value.rejectedReports
  if (total === 0) return '0%'
  return (statistics.value.approvedReports / total * 100).toFixed(1) + '%'
})

const statsLoading = ref(false)

const reports = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const result = await getStatistics()
    statistics.value = {
      totalReports: result?.totalReports || 0,
      pendingReports: result?.pendingReports || 0,
      approvedReports: result?.approvedReports || 0,
      rejectedReports: result?.rejectedReports || 0,
    }
  } catch (error) {
    // silently handle
  } finally {
    statsLoading.value = false
  }
}

const loadReports = async () => {
  try {
    loading.value = true
    const result = await getCarbonReports({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    reports.value = result?.items || []
    total.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('monitor.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadReports()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  loadReports()
}

const statusMap = {
  0: { tag: 'info', text: t('monitor.statusDraft') },
  1: { tag: 'warning', text: t('monitor.statusPending') },
  2: { tag: 'warning', text: t('monitor.statusPending') },
  3: { tag: 'success', text: t('monitor.statusApproved') },
  4: { tag: 'danger', text: t('monitor.statusRejected') },
  5: { tag: 'success', text: t('monitor.statusOnChain') },
}

const getStatusTag = (status) => {
  return statusMap[status]?.tag || 'info'
}

const getStatusText = (status) => {
  return statusMap[status]?.text || status
}

onMounted(() => {
  loadStatistics()
  loadReports()
})
</script>

<template>
  <PageContainer :title="t('monitor.title')" :description="t('monitor.description')">
    <section class="monitor-page">
      <el-card class="section-card" shadow="never" v-loading="statsLoading">
        <template #header>
          <span class="card-header">{{ t('monitor.dataStats') }}</span>
        </template>
        <div class="stats-grid">
          <div class="stat-card total">
            <div class="stat-icon">📊</div>
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statTotalReports') }}</div>
              <div class="stat-value">{{ statistics.totalReports }}</div>
            </div>
          </div>
          <div class="stat-card pending">
            <div class="stat-icon">⏳</div>
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statPending') }}</div>
              <div class="stat-value">{{ statistics.pendingReports }}</div>
            </div>
          </div>
          <div class="stat-card approved">
            <div class="stat-icon">✅</div>
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statApproved') }}</div>
              <div class="stat-value">{{ statistics.approvedReports }}</div>
            </div>
          </div>
          <div class="stat-card rejected">
            <div class="stat-icon">❌</div>
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statRejected') }}</div>
              <div class="stat-value">{{ statistics.rejectedReports }}</div>
            </div>
          </div>
          <div class="stat-card rate">
            <div class="stat-icon">📈</div>
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statApprovalRate') || '审核通过率' }}</div>
              <div class="stat-value">{{ approvalRate }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <span class="card-header">{{ t('monitor.tableTitle') }}</span>
        </template>
        <el-table :data="reports" border v-loading="loading">
          <el-table-column label="#" width="80">
            <template #default="scope">
              {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="reportNo" :label="t('monitor.colReportNo')" min-width="150" />
          <el-table-column prop="enterpriseName" :label="t('monitor.colEnterpriseName')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="accountingPeriod" :label="t('monitor.colYear')" min-width="100" />
          <el-table-column prop="totalEmission" :label="t('monitor.colTotalEmission')" min-width="140" />
          <el-table-column prop="status" :label="t('monitor.colStatus')" min-width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" :label="t('monitor.colSubmitTime')" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.submitTime || row.createdAt) }}</template>
          </el-table-column>
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
.monitor-page {
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  padding: 10px 0;
}

.stat-card {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-card.pending {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-card.approved {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.stat-card.rejected {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
}

.stat-card.rate {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  color: white;
}

.stat-icon {
  font-size: 36px;
  line-height: 1;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  margin-bottom: 8px;
  opacity: 0.9;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
