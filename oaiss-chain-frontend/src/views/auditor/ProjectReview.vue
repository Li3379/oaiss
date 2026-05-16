<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getPendingVerification, reviewProject, verifyProject } from '../../api/carbonNeutral'
import { deductPoints } from '../../api/credit'
import { formatDateTime } from '../../utils/format'

const { t } = useI18n()

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const reviewDialogVisible = ref(false)
const reviewForm = ref({ projectId: null as number | null, approved: true, comment: '' })
const reviewLoading = ref(false)

const verifyDialogVisible = ref(false)
const verifyForm = ref({ projectId: null as number | null, verified: true, comment: '' })
const verifyLoading = ref(false)

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
    tableData.value = response.items || []
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

const openReviewDialog = (row: Record<string, unknown>) => {
  reviewForm.value = { projectId: row.id as number, approved: true, comment: '' }
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  reviewLoading.value = true
  try {
    await reviewProject(reviewForm.value.projectId!, {
      approved: reviewForm.value.approved,
      comment: reviewForm.value.comment,
    })
    ElMessage.success(reviewForm.value.approved ? t('projectReview.reviewApproved') : t('projectReview.reviewRejected'))
    reviewDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(t('projectReview.reviewFailed'))
  } finally {
    reviewLoading.value = false
  }
}

const openVerifyDialog = (row: Record<string, unknown>) => {
  verifyForm.value = { projectId: row.id as number, verified: true, comment: '' }
  verifyDialogVisible.value = true
}

const submitVerify = async () => {
  verifyLoading.value = true
  try {
    await verifyProject({
      projectId: verifyForm.value.projectId!,
      verified: verifyForm.value.verified,
      comment: verifyForm.value.comment,
    })
    ElMessage.success(verifyForm.value.verified ? t('projectReview.verifyPassed') : t('projectReview.verifyFailed'))
    verifyDialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(t('projectReview.verifySubmitFailed'))
  } finally {
    verifyLoading.value = false
  }
}

const openDeductDialog = (row: Record<string, unknown>) => {
  deductForm.value = { enterpriseId: row.enterpriseId as number, eventType: 1, description: '' }
  deductDialogVisible.value = true
}

const submitDeduct = async () => {
  deductLoading.value = true
  try {
    await deductPoints({
      enterpriseId: deductForm.value.enterpriseId!,
      eventType: deductForm.value.eventType,
      description: deductForm.value.description,
    })
    ElMessage.success(t('projectReview.deductSuccess'))
    deductDialogVisible.value = false
  } catch {
    ElMessage.error(t('projectReview.deductFailed'))
  } finally {
    deductLoading.value = false
  }
}

const getStatusTag = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    IN_PROGRESS: 'primary',
    PENDING_VERIFICATION: 'warning',
    CERTIFIED: 'success',
    TERMINATED: 'danger',
  }
  return map[status] || 'info'
}

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
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="projectName" :label="t('projectReview.colProjectName')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" :label="t('projectReview.colEnterpriseName')" min-width="160" />
        <el-table-column prop="projectType" :label="t('projectReview.colProjectType')" min-width="120" />
        <el-table-column prop="status" :label="t('common.status')" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ row.statusText || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedReduction" :label="t('projectReview.colExpectedReduction')" min-width="130" />
        <el-table-column prop="createdAt" :label="t('common.createTime')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING_REVIEW'" link type="primary" @click="openReviewDialog(row)">{{ t('projectReview.review') }}</el-button>
            <el-button v-if="row.status === 'PENDING_VERIFICATION'" link type="success" @click="openVerifyDialog(row)">{{ t('projectReview.verify') }}</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW' || row.status === 'PENDING_VERIFICATION'" link type="warning" @click="openDeductDialog(row)">{{ t('projectReview.deductCredit') }}</el-button>
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

    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" :title="t('projectReview.reviewDialogTitle')" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('projectReview.labelResult')">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :label="true">{{ t('projectReview.approve') }}</el-radio>
            <el-radio :label="false">{{ t('projectReview.reject') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('projectReview.labelComment')">
          <el-input v-model="reviewForm.comment" type="textarea" :rows="4" :placeholder="t('projectReview.enterComment')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">{{ t('common.submit') }}</el-button>
      </template>
    </el-dialog>

    <!-- Verify Dialog -->
    <el-dialog v-model="verifyDialogVisible" :title="t('projectReview.verifyDialogTitle')" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('projectReview.labelResult')">
          <el-radio-group v-model="verifyForm.verified">
            <el-radio :label="true">{{ t('projectReview.verifyPass') }}</el-radio>
            <el-radio :label="false">{{ t('projectReview.verifyFail') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('projectReview.labelComment')">
          <el-input v-model="verifyForm.comment" type="textarea" :rows="4" :placeholder="t('projectReview.enterComment')" />
        </el-form-item>
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

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
