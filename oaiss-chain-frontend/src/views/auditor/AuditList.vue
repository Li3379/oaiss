<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { reviewReport } from '../../api/carbon'
import { getMyReviewerQualification, getPendingReports, getReviewHistory, getReviewerInfo, getStatistics } from '../../api/reviewer'
import { formatDateTime } from '../../utils/format'

const { t } = useI18n()

type ReviewerTableRow = Record<string, any>

const tableData = ref<ReviewerTableRow[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref<'pending' | 'all'>('pending')
const statisticsData = ref<Record<string, unknown> | null>(null)
const reviewerInfoData = ref<Record<string, unknown> | null>(null)

const reviewDialogVisible = ref(false)
const reviewForm = ref({
  reportId: null as number | null,
  approved: true,
  comment: '',
})

const REVIEWABLE_STATUSES = new Set([1, 2])

const normalizedStatistics = computed(() => {
  const stats = statisticsData.value || {}
  const totalReviews = Number(stats.completedReviews ?? stats.totalReviews ?? 0)
  const approvedCount = Number(stats.passedCount ?? stats.approvedCount ?? 0)
  const rejectedCount = Number(stats.rejectedCount ?? 0)
  const approvalRate = totalReviews > 0
    ? `${((approvedCount / totalReviews) * 100).toFixed(1)}%`
    : '0%'

  return {
    totalReviews,
    approvedCount,
    rejectedCount,
    approvalRate: String(stats.approvalRate ?? approvalRate),
  }
})

function getStatusText(status?: number, statusText?: string) {
  if (statusText) return statusText
  const fallbackMap: Record<number, string> = {
    0: '草稿',
    1: '已提交',
    2: '审核中',
    3: '审核通过',
    4: '审核拒绝',
    5: '已上链',
  }
  if (typeof status === 'number') return fallbackMap[status] || String(status)
  return '-'
}

function normalizeRow(row: ReviewerTableRow): ReviewerTableRow {
  const status = typeof row.status === 'number' ? row.status : Number(row.status ?? 0)
  const enterpriseName = row.enterpriseName || row.enterprise || (row.enterpriseId ? `企业#${row.enterpriseId}` : '-')
  return {
    ...row,
    enterpriseName,
    status,
    statusText: getStatusText(status, row.statusText),
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const response = activeTab.value === 'pending'
      ? await getPendingReports(params)
      : await getReviewHistory(params)
    tableData.value = (response.items || []).map(normalizeRow)
    total.value = response.total || 0
  } catch {
    ElMessage.error(t('auditList.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

const onCurrentChange = (page: number) => {
  currentPage.value = page
  fetchData()
}

const canReviewRow = (row: ReviewerTableRow) => {
  return activeTab.value === 'pending' && REVIEWABLE_STATUSES.has(Number(row.status))
}

const openReviewDialog = (row: ReviewerTableRow) => {
  if (!canReviewRow(row)) return
  reviewForm.value = {
    reportId: row.id,
    approved: true,
    comment: '',
  }
  reviewDialogVisible.value = true
}

const closeReviewDialog = () => {
  reviewDialogVisible.value = false
  reviewForm.value = {
    reportId: null,
    approved: true,
    comment: '',
  }
}

const submitReview = async () => {
  try {
    await reviewReport({
      reportId: reviewForm.value.reportId as number,
      approved: reviewForm.value.approved,
      comment: reviewForm.value.comment,
    })

    ElMessage.success(reviewForm.value.approved ? t('auditList.approveSuccess') : t('auditList.rejectSuccess'))
    closeReviewDialog()
    await Promise.all([fetchData(), fetchStatistics()])
  } catch {
    ElMessage.error(t('auditList.submitFailed'))
  }
}

const getStatusType = (status?: number) => {
  const statusMap: Record<number, string> = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'danger',
    5: 'success',
  }
  return typeof status === 'number' ? (statusMap[status] || 'info') : 'info'
}

const qualificationStatus = ref<Record<string, unknown> | null>(null)

const fetchStatistics = async () => {
  try {
    statisticsData.value = await getStatistics() as Record<string, unknown>
  } catch {
    statisticsData.value = null
  }
}

const fetchReviewerInfo = async () => {
  try {
    reviewerInfoData.value = await getReviewerInfo() as Record<string, unknown>
  } catch {
    reviewerInfoData.value = null
  }
}

const onTabChange = () => {
  currentPage.value = 1
  fetchData()
}

const fetchQualificationStatus = async () => {
  try {
    const res = await getMyReviewerQualification()
    const list = Array.isArray(res) ? res : ((res as Record<string, unknown>)?.items as unknown[] || [])
    qualificationStatus.value = list.length > 0 ? list[0] as Record<string, unknown> : null
  } catch {
    qualificationStatus.value = null
  }
}

const qualificationStatusType = computed(() => {
  if (!qualificationStatus.value) return 'info'
  return qualificationStatus.value.status === 1 ? 'success' : 'danger'
})

const qualificationStatusText = computed(() => {
  if (!qualificationStatus.value) return t('certificateManage.notIssued')
  return qualificationStatus.value.status === 1 ? t('certificateManage.active') : t('certificateManage.revoked')
})

const reviewerIdentityLine = computed(() => {
  const info = reviewerInfoData.value
  if (!info) return ''
  const parts = [
    info.name ? `${t('auditList.reviewerName')}: ${info.name}` : '',
    info.organization ? `机构: ${info.organization}` : '',
    info.qualificationNo ? `${t('certificateManage.colCertificateNo')}: ${info.qualificationNo}` : '',
  ].filter(Boolean)
  return parts.join(' | ')
})

onMounted(() => {
  fetchData()
  fetchQualificationStatus()
  fetchStatistics()
  fetchReviewerInfo()
})
</script>

<template>
  <section class="audit-page">
    <el-card class="section-card" shadow="never">
      <el-space>
        <span>{{ t('certificateManage.myQualification') }}:</span>
        <el-tag :type="qualificationStatusType">{{ qualificationStatusText }}</el-tag>
        <span v-if="qualificationStatus" style="color: #999; font-size: 13px;">
          {{ qualificationStatus.certificateNo }}
        </span>
      </el-space>
      <div v-if="reviewerIdentityLine" style="margin-top: 10px; color: #666; font-size: 13px;">
        {{ reviewerIdentityLine }}
      </div>
    </el-card>

    <el-card v-if="statisticsData" class="section-card" shadow="never">
      <el-space :size="30">
        <span>{{ t('auditList.totalReviews') }}: <strong>{{ normalizedStatistics.totalReviews }}</strong></span>
        <span>{{ t('auditList.approvedCount') }}: <strong>{{ normalizedStatistics.approvedCount }}</strong></span>
        <span>{{ t('auditList.rejectedCount') }}: <strong>{{ normalizedStatistics.rejectedCount }}</strong></span>
        <span>{{ t('auditList.approvalRate') }}: <strong>{{ normalizedStatistics.approvalRate }}</strong></span>
      </el-space>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('auditList.breadcrumbAudit') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('auditList.breadcrumbData') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane :label="t('auditList.tabPending')" name="pending" />
        <el-tab-pane :label="t('auditList.tabAllReports')" name="all" />
      </el-tabs>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="reportNo" :label="t('auditList.colReportNo')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" :label="t('auditList.colEnterpriseName')" min-width="180" />
        <el-table-column prop="title" :label="t('auditList.colReportTitle')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="totalEmission" :label="t('auditList.colTotalEmission')" min-width="130">
          <template #default="{ row }">{{ row.totalEmission }} tCO2e</template>
        </el-table-column>
        <el-table-column prop="statusText" :label="t('auditList.colStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('auditList.colCreateTime')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.reviewedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('auditList.colOperation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canReviewRow(row)"
              link
              type="primary"
              @click="openReviewDialog(row)"
            >
              {{ t('auditList.colOperation') }}
            </el-button>
            <span v-else style="color: #999;">-</span>
          </template>
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

    <el-dialog v-model="reviewDialogVisible" :title="t('auditList.dialogTitle')" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('auditList.labelResult')">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">{{ t('auditList.approve') }}</el-radio>
            <el-radio :value="false">{{ t('auditList.reject') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('auditList.labelComment')">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="4"
            :placeholder="t('auditList.enterComment')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeReviewDialog">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitReview">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
