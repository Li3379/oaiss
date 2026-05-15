<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, reviewReport } from '../../api/carbon'

const { t } = useI18n()

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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
    const response = await getReportList(params)
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

onMounted(() => {
  fetchData()
})
</script>

<template>
  <section class="audit-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('auditList.breadcrumbAudit') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('auditList.breadcrumbData') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
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
        <el-table-column prop="createdAt" :label="t('auditList.colCreateTime')" min-width="170" />
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
