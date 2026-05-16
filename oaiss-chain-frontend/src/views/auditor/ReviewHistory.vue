<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getReviewHistory } from '../../api/reviewer'
import { formatDateTime } from '../../utils/format'

const { t } = useI18n()

const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const response = await getReviewHistory({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    tableData.value = response.items || []
    total.value = response.total || 0
  } catch {
    ElMessage.error(t('reviewHistory.loadFailed'))
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

const getStatusType = (status: number) => {
  return status === 3 ? 'success' : status === 4 ? 'danger' : 'info'
}

onMounted(() => fetchData())
</script>

<template>
  <section class="history-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('reviewHistory.breadcrumbAudit') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('reviewHistory.breadcrumbHistory') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="tableData" border v-loading="loading">
        <el-table-column prop="reportNo" :label="t('reviewHistory.colReportNo')" min-width="180" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" :label="t('reviewHistory.colEnterpriseName')" min-width="180" />
        <el-table-column prop="title" :label="t('reviewHistory.colReportTitle')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reviewResult" :label="t('reviewHistory.colResult')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.reviewResult)">
              {{ row.reviewResult === 3 ? t('reviewHistory.approved') : row.reviewResult === 4 ? t('reviewHistory.rejected') : '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" :label="t('reviewHistory.colComment')" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reviewTime" :label="t('reviewHistory.colReviewTime')" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.reviewTime || row.createdAt) }}</template>
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
</template>

<style scoped>
.history-page {
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
