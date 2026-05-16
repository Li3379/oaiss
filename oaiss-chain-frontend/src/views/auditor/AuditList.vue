<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, reviewReport } from '../../api/carbon'
import { getMyReviewerQualification } from '../../api/admin'
import { getPendingReports, getStatistics, getReviewerInfo } from '../../api/reviewer'
import { formatDateTime } from '../../utils/format'

const { t } = useI18n()

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activeTab = ref('pending')
const statisticsData = ref<Record<string, unknown> | null>(null)
const reviewerInfoData = ref<Record<string, unknown> | null>(null)

const reviewDialogVisible = ref(false)
const reviewForm = ref({
  reportId: null,
  approved: true,
  comment: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    }
    const response = activeTab.value === 'pending'
      ? await getPendingReports(params)
      : await getReportList(params)
    tableData.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error(t('auditList.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchData()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  fetchData()
}

const openReviewDialog = (row) => {
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
      reportId: reviewForm.value.reportId,
      approved: reviewForm.value.approved,
      comment: reviewForm.value.comment,
    })
    
    ElMessage.success(reviewForm.value.approved ? t('auditList.approveSuccess') : t('auditList.rejectSuccess'))
    closeReviewDialog()
    fetchData()
  } catch (error) {
    ElMessage.error(t('auditList.submitFailed'))
  }
}

const getStatusType = (status) => {
  const statusMap = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info',
  }
  return statusMap[status] || 'info'
}

const qualificationStatus = ref<Record<string, unknown> | null>(null)
const qualificationLoading = ref(false)

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
  qualificationLoading.value = true
  try {
    const res = await getMyReviewerQualification()
    const list = Array.isArray(res) ? res : ((res as Record<string, unknown>)?.items as unknown[] || [])
    qualificationStatus.value = list.length > 0 ? list[0] as Record<string, unknown> : null
  } catch {
    qualificationStatus.value = null
  } finally {
    qualificationLoading.value = false
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
      <div v-if="reviewerInfoData" style="margin-top: 10px; color: #666; font-size: 13px;">
        <span v-if="reviewerInfoData.name">{{ t('auditList.reviewerName') }}: {{ reviewerInfoData.name }}</span>
      </div>
    </el-card>

    <el-card v-if="statisticsData" class="section-card" shadow="never">
      <el-space :size="30">
        <span>{{ t('auditList.totalReviews') }}: <strong>{{ statisticsData.totalReviews ?? 0 }}</strong></span>
        <span>{{ t('auditList.approvedCount') }}: <strong>{{ statisticsData.approvedCount ?? 0 }}</strong></span>
        <span>{{ t('auditList.rejectedCount') }}: <strong>{{ statisticsData.rejectedCount ?? 0 }}</strong></span>
        <span>{{ t('auditList.approvalRate') }}: <strong>{{ statisticsData.approvalRate ?? '0%' }}</strong></span>
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
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('auditList.colOperation')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReviewDialog(row)">{{ t('auditList.colOperation') }}</el-button>
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
            <el-radio :label="true">{{ t('auditList.approve') }}</el-radio>
            <el-radio :label="false">{{ t('auditList.reject') }}</el-radio>
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
