<template>
  <PageContainer :title="t('verifyList.title')" :description="t('verifyList.description')">
    <el-card class="section-card" shadow="never" v-loading="statsLoading">
      <div class="stats-row">
        <el-statistic :title="t('verifyList.statPending')" :value="stats.pending" />
        <el-statistic :title="t('verifyList.statApproved')" :value="stats.approved" />
        <el-statistic :title="t('verifyList.statRejected')" :value="stats.rejected" />
        <div class="status-cell">
          <div class="status-label">{{ t('verifyList.blockchainStatus') }}</div>
          <el-tag :type="blockchainHealthy ? 'success' : 'danger'" size="large">
            {{ blockchainStatus }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="search-row">
        <el-select v-model="statusFilter" :placeholder="t('verifyList.colStatus')" clearable style="width: 180px">
          <el-option :label="t('verifyList.statusPendingCertification')" :value="3" />
          <el-option :label="t('verifyList.statusApproved')" :value="5" />
          <el-option :label="t('verifyList.statusRejected')" :value="4" />
          <el-option :label="t('verifyList.statusPending')" :value="1" />
        </el-select>
        <el-input v-model="keyword" :placeholder="t('common.enterKeyword')" clearable style="width: 300px" />
        <el-button type="primary" @click="loadReports">{{ t('common.search') }}</el-button>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="reports" :loading="loading" border :empty-text="t('verifyList.emptyText')">
        <el-table-column :label="t('verifyList.colIndex')" width="80">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="reportNo" :label="t('verifyList.colReportNo')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" :label="t('verifyList.colEnterpriseName')" min-width="150" />
        <el-table-column prop="accountingPeriod" :label="t('verifyList.colAccountingPeriod')" min-width="120" />
        <el-table-column prop="totalEmission" :label="t('verifyList.colTotalEmission')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" :label="t('verifyList.colStatus')" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('verifyList.colSubmitTime')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('verifyList.colOperation')" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">{{ t('verifyList.btnView') }}</el-button>
            <el-button
              v-if="canCertify(row)"
              type="success"
              link
              @click="onVerify(row, true)"
            >
              {{ t('verifyList.btnApprove') }}
            </el-button>
            <el-button
              v-if="canCertify(row)"
              type="danger"
              link
              @click="onVerify(row, false)"
            >
              {{ t('verifyList.btnReject') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
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

    <el-dialog v-model="detailVisible" :title="t('verifyList.title')" width="700px">
      <el-descriptions v-if="currentReport" :column="2" border>
        <el-descriptions-item :label="t('verifyList.colReportNo')">{{ currentReport.reportNo }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colEnterpriseName')">{{ currentReport.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colAccountingPeriod')">{{ currentReport.accountingPeriod }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colStatus')">
          <el-tag :type="getStatusType(currentReport.status)">{{ getStatusLabel(currentReport.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colTotalEmission')">
          {{ currentReport.totalEmission }} {{ t('common.unit_ton') }}
        </el-descriptions-item>
        <el-descriptions-item label="Scope 1">{{ currentReport.scope1Emission ?? '-' }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item label="Scope 2">{{ currentReport.scope2Emission ?? '-' }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item label="Scope 3">{{ currentReport.scope3Emission ?? '-' }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colSubmitTime')">{{ formatDateTime(currentReport.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelReviewComment')" :span="2">
          {{ currentReport.reviewComment || t('verifyList.noComment') }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { certifyReport, getReportList } from '../../api/carbon'
import { getStatus } from '../../api/blockchain'
import { formatDateTime } from '../../utils/format'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

type ReportRow = Record<string, any>

const reports = ref<ReportRow[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<number | ''>('')

const stats = ref({ pending: 0, approved: 0, rejected: 0 })
const blockchainStatus = ref('')
const statsLoading = ref(false)

const detailVisible = ref(false)
const currentReport = ref<ReportRow | null>(null)

const blockchainHealthy = computed(() => {
  const normalized = blockchainStatus.value.trim().toLowerCase()
  return normalized === '' || normalized === 'normal' || normalized === 'online' || normalized === 'healthy' || normalized === '姝ｅ父'
})

const STATUS_LABEL_MAP: Record<number, string> = {
  0: 'statusDraft',
  1: 'statusPending',
  2: 'statusInReview',
  3: 'statusPendingCertification',
  4: 'statusRejected',
  5: 'statusOnChain',
}

const STATUS_FALLBACK_LABEL_MAP: Record<number, string> = {
  0: 'Draft',
  1: 'Pending',
  2: 'In Review',
  3: 'Pending Certification',
  4: 'Rejected',
  5: 'On Chain',
}

const STATUS_TYPE_MAP: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'primary',
  3: 'success',
  4: 'danger',
  5: 'success',
}

const getStatusLabel = (status?: number) => {
  const key = typeof status === 'number' ? STATUS_LABEL_MAP[status] : ''
  if (!key) return String(status ?? '-')
  const translated = t(`verifyList.${key}`)
  return translated === `verifyList.${key}`
    ? (typeof status === 'number' ? STATUS_FALLBACK_LABEL_MAP[status] : translated)
    : translated
}

const getStatusType = (status?: number) => {
  return typeof status === 'number' ? (STATUS_TYPE_MAP[status] || 'info') : 'info'
}

const canCertify = (row: ReportRow) => Number(row.status) === 3

const recomputeStats = (rows: ReportRow[]) => {
  stats.value = {
    pending: rows.filter((row) => Number(row.status) === 3).length,
    approved: rows.filter((row) => Number(row.status) === 5).length,
    rejected: rows.filter((row) => Number(row.status) === 4).length,
  }
}

const loadReports = async () => {
  loading.value = true
  try {
    const result = await getReportList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
    })
    reports.value = result?.items || []
    total.value = result?.total || 0
    recomputeStats(reports.value)
  } catch {
    ElMessage.error(t('verifyList.loadFailed'))
  } finally {
    loading.value = false
  }
}

const loadBlockchainStatus = async () => {
  statsLoading.value = true
  try {
    const result = await getStatus()
    blockchainStatus.value = result?.status || t('verifyList.blockchainNormal')
  } catch {
    blockchainStatus.value = t('verifyList.blockchainNormal')
  } finally {
    statsLoading.value = false
  }
}

const viewDetail = (row: ReportRow) => {
  currentReport.value = row
  detailVisible.value = true
}

const onVerify = async (row: ReportRow, approved: boolean) => {
  const actionText = approved ? t('verifyList.btnApprove') : t('verifyList.btnReject')
  try {
    await ElMessageBox.confirm(
      t(approved ? 'verifyList.confirmApprove' : 'verifyList.confirmReject'),
      t('common.confirm'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
    await certifyReport({
      reportId: Number(row.id),
      approved,
      comment: approved ? t('verifyList.approveSuccess') : t('verifyList.rejectSuccess'),
    })
    ElMessage.success(actionText)
    await loadReports()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(`${t('verifyList.operationFailed')}: ${error?.message || ''}`)
    }
  }
}

const onSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  void loadReports()
}

const onCurrentChange = (page: number) => {
  currentPage.value = page
  void loadReports()
}

onMounted(() => {
  void loadReports()
  void loadBlockchainStatus()
})
</script>

<style scoped>
.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 14px;
}

.stats-row {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
  align-items: flex-start;
}

.status-cell {
  text-align: center;
}

.status-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.search-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pager-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
