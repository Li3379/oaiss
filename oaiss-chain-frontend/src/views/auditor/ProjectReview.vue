<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getPendingVerification, verifyProject } from '../../api/carbonNeutral'
import { deductPoints } from '../../api/credit'
import { formatDateTime } from '../../utils/format'

const { t } = useI18n()

type ProjectReviewRow = {
  id?: number
  enterpriseId?: number
  ownerId?: number
  expectedReduction?: number | string
  monitoringData?: string
  status?: number
  verificationStatus?: number
  [key: string]: unknown
}

const tableData = ref<ProjectReviewRow[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const verifyDialogVisible = ref(false)
const verifyForm = ref({
  projectId: null as number | null,
  verifiedReduction: '',
  verificationReport: '',
  monitoringData: '',
  remark: '',
})
const verifyLoading = ref(false)
const verifyErrorMessage = ref('')

const deductDialogVisible = ref(false)
const deductForm = ref({ enterpriseId: null as number | null, eventType: 1, description: '' })
const deductLoading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await getPendingVerification({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    tableData.value = (response.items || []).map((row: ProjectReviewRow) => ({
      ...row,
      status: Number(row.status ?? 0),
      verificationStatus: Number(row.verificationStatus ?? 0),
    }))
    total.value = response.total || 0
  } catch {
    ElMessage.error(t('projectReview.loadFailed'))
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

const openVerifyDialog = (row: ProjectReviewRow) => {
  verifyErrorMessage.value = ''
  verifyForm.value = {
    projectId: row.id as number,
    verifiedReduction: String(row.expectedReduction ?? ''),
    verificationReport: '',
    monitoringData: typeof row.monitoringData === 'string' ? row.monitoringData : '',
    remark: '',
  }
  verifyDialogVisible.value = true
}

const submitVerify = async () => {
  verifyErrorMessage.value = ''
  const verifiedReduction = Number(verifyForm.value.verifiedReduction)
  if (!Number.isFinite(verifiedReduction) || verifiedReduction <= 0) {
    const invalidMessage = t('projectReview.verifiedReductionInvalid')
    verifyErrorMessage.value = invalidMessage === 'projectReview.verifiedReductionInvalid'
      ? `${t('projectReview.labelVerifiedReduction')} > 0`
      : invalidMessage
    ElMessage.warning(verifyErrorMessage.value)
    return
  }

  verifyLoading.value = true
  try {
    await verifyProject({
      projectId: verifyForm.value.projectId!,
      verifiedReduction,
      verificationReport: verifyForm.value.verificationReport || undefined,
      monitoringData: verifyForm.value.monitoringData || undefined,
      remark: verifyForm.value.remark || undefined,
    })
    ElMessage.success(t('projectReview.verifyPassed'))
    verifyDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    const backendMessage = error?.response?.data?.data?.[0] || error?.response?.data?.message || error?.message
    verifyErrorMessage.value = backendMessage || t('projectReview.verifySubmitFailed')
    ElMessage.error(backendMessage || t('projectReview.verifySubmitFailed'))
  } finally {
    verifyLoading.value = false
  }
}

const openDeductDialog = (row: ProjectReviewRow) => {
  const resolvedEnterpriseId = Number(row.enterpriseId ?? row.ownerId ?? 0)
  if (!Number.isFinite(resolvedEnterpriseId) || resolvedEnterpriseId <= 0) {
    ElMessage.error(t('projectReview.deductFailed'))
    return
  }
  deductForm.value = { enterpriseId: resolvedEnterpriseId, eventType: 1, description: '' }
  deductDialogVisible.value = true
}

const submitDeduct = async () => {
  const description = deductForm.value.description.trim()
  if (!description) {
    ElMessage.warning(t('projectReview.enterDescription'))
    return
  }

  deductLoading.value = true
  try {
    await deductPoints({
      enterpriseId: deductForm.value.enterpriseId!,
      eventType: deductForm.value.eventType,
      description,
    })
    ElMessage.success(t('projectReview.deductSuccess'))
    deductDialogVisible.value = false
  } catch (error: any) {
    const backendMessage = error?.response?.data?.data?.[0] || error?.response?.data?.message || error?.message
    ElMessage.error(backendMessage || t('projectReview.deductFailed'))
  } finally {
    deductLoading.value = false
  }
}

const STATUS_MAP: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'primary',
  4: 'success',
  5: 'danger',
  6: 'danger',
}

const VERIFICATION_STATUS_MAP: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
}

const getStatusTag = (status: number) => {
  return STATUS_MAP[status] || 'info'
}

const getVerificationTag = (status: number) => {
  return VERIFICATION_STATUS_MAP[status] || 'info'
}

const canVerify = (row: ProjectReviewRow) => {
  return Number(row.verificationStatus) === 1
}

const canDeduct = (row: ProjectReviewRow) => {
  return Number(row.verificationStatus) === 1
}

const hasRows = computed(() => tableData.value.length > 0)

const queueTitle = computed(() => {
  return t('projectReview.queueVerification')
})

const queueDescription = computed(() => {
  return t('projectReview.queueVerificationDesc')
})

onMounted(() => fetchData())
</script>

<template>
  <section class="review-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('projectReview.breadcrumbAudit') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('projectReview.breadcrumbProject') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="queue-summary">
        <div>
          <div class="queue-title">{{ queueTitle }}</div>
          <div class="queue-desc">{{ queueDescription }}</div>
        </div>
      </div>
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="projectName" :label="t('projectReview.colProjectName')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ownerName" :label="t('projectReview.colEnterpriseName')" min-width="160" />
        <el-table-column prop="projectTypeName" :label="t('projectReview.colProjectType')" min-width="120" />
        <el-table-column prop="status" :label="t('common.status')" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ row.statusText || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="verificationStatus" :label="t('projectReview.colVerificationStatus')" min-width="140">
          <template #default="{ row }">
            <el-tag :type="getVerificationTag(row.verificationStatus)">{{ row.verificationStatusText || row.verificationStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedReduction" :label="t('projectReview.colExpectedReduction')" min-width="130" />
        <el-table-column prop="createdAt" :label="t('common.createTime')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canVerify(row)" link type="success" @click="openVerifyDialog(row)">{{ t('projectReview.verify') }}</el-button>
            <el-button v-if="canDeduct(row)" link type="warning" @click="openDeductDialog(row)">{{ t('projectReview.deductCredit') }}</el-button>
            <span v-if="!canVerify(row) && !canDeduct(row)" class="empty-op">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !hasRows" :description="t('projectReview.emptyText')" />

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

    <!-- Verify Dialog -->
    <el-dialog v-model="verifyDialogVisible" :title="t('projectReview.verifyDialogTitle')" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('projectReview.labelVerifiedReduction')">
          <el-input v-model="verifyForm.verifiedReduction" />
        </el-form-item>
        <el-form-item :label="t('projectReview.labelVerificationReport')">
          <el-input v-model="verifyForm.verificationReport" type="textarea" :rows="3" :placeholder="t('projectReview.enterVerificationReport')" />
        </el-form-item>
        <el-form-item :label="t('projectReview.labelMonitoringData')">
          <el-input v-model="verifyForm.monitoringData" type="textarea" :rows="3" :placeholder="t('projectReview.enterMonitoringData')" />
        </el-form-item>
        <el-form-item :label="t('projectReview.labelRemark')">
          <el-input v-model="verifyForm.remark" type="textarea" :rows="3" :placeholder="t('projectReview.enterRemark')" />
        </el-form-item>
        <div v-if="verifyErrorMessage" class="form-error-text">{{ verifyErrorMessage }}</div>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="verifyLoading" @click="submitVerify">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <!-- Credit Deduction Dialog -->
    <el-dialog v-model="deductDialogVisible" :title="t('projectReview.deductDialogTitle')" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('projectReview.labelEventType')">
          <el-select v-model="deductForm.eventType">
            <el-option :label="t('projectReview.eventTypeViolation')" :value="1" />
            <el-option :label="t('projectReview.eventTypeFraud')" :value="2" />
            <el-option :label="t('projectReview.eventTypeOther')" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('projectReview.labelDescription')">
          <el-input v-model="deductForm.description" type="textarea" :rows="3" :placeholder="t('projectReview.enterDescription')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deductDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="deductLoading" @click="submitDeduct">{{ t('projectReview.confirmDeduct') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.review-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.queue-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.queue-title {
  font-size: 16px;
  font-weight: 600;
}

.queue-desc {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.empty-op {
  color: #999;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.form-error-text {
  color: var(--el-color-danger);
  margin-top: 4px;
  padding-left: 100px;
  font-size: 13px;
}
</style>
