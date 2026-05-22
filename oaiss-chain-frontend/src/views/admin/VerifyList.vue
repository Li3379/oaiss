<template>
  <PageContainer :title="t('verifyList.title')" :description="t('verifyList.description')">
    <el-card class="section-card" shadow="never" v-loading="statsLoading">
      <div class="stats-row">
        <el-statistic :title="t('verifyList.statPending')" :value="stats.pending" />
        <el-statistic :title="t('verifyList.statApproved')" :value="stats.approved" />
        <el-statistic :title="t('verifyList.statRejected')" :value="stats.rejected" />
        <div class="status-cell">
          <div class="status-label">{{ t('verifyList.blockchainStatus') }}</div>
          <el-tag :type="blockchainStatus === t('verifyList.blockchainNormal') ? 'success' : 'danger'" size="large">{{ blockchainStatus }}</el-tag>
        </div>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <div class="search-row">
        <el-select v-model="statusFilter" :placeholder="t('verifyList.colStatus')" clearable style="width: 150px">
          <el-option :label="t('verifyList.statusPending')" :value="1" />
          <el-option :label="t('verifyList.statusApproved')" :value="2" />
          <el-option :label="t('verifyList.statusRejected')" :value="3" />
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
        <el-table-column prop="status" :label="t('verifyList.colStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('verifyList.colSubmitTime')" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('verifyList.colOperation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">{{ t('verifyList.btnView') }}</el-button>
            <el-button
              v-if="row.status === 2"
              type="success" link @click="onVerify(row, true)"
            >{{ t('verifyList.btnApprove') }}</el-button>
            <el-button
              v-if="row.status === 2"
              type="danger" link @click="onVerify(row, false)"
            >{{ t('verifyList.btnReject') }}</el-button>
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
        <el-descriptions-item :label="t('verifyList.colTotalEmission')">{{ currentReport.totalEmission }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelCoalEmission')">{{ currentReport.coalEmission }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelOilEmission')">{{ currentReport.oilEmission }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelGasEmission')">{{ currentReport.gasEmission }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelElectricityEmission')">{{ currentReport.electricityEmission }} {{ t('common.unit_ton') }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.colSubmitTime')">{{ formatDateTime(currentReport.createdAt) }}</el-descriptions-item>
        <el-descriptions-item :label="t('verifyList.labelReviewComment')" :span="2">{{ currentReport.reviewComment || t('verifyList.noComment') }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportList, reviewReport } from '../../api/carbon'
import { getStatus } from '../../api/blockchain'
import { formatDateTime } from '../../utils/format'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const reports = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')

const stats = ref({ pending: 0, approved: 0, rejected: 0 })
const blockchainStatus = ref('')
const statsLoading = ref(false)

const detailVisible = ref(false)
const currentReport = ref(null)

// Status codes: 0=草稿, 1=待审核, 2=审核通过, 3=已驳回, 4=认证驳回, 5=已上链
const getStatusLabel = (status) => {
  const map = { 0: t('verifyList.statusDraft'), 1: t('verifyList.statusPending'), 2: t('verifyList.statusApproved'), 3: t('verifyList.statusRejected'), 5: t('verifyList.statusOnChain') }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 5: 'primary' }
  return map[status] || 'info'
}

const loadReports = async () => {
  loading.value = true
  try {
    const result = await getReportList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    reports.value = result?.items || []
    total.value = result?.total || 0

    stats.value = {
      pending: reports.value.filter(r => r.status === 2).length,
      approved: reports.value.filter(r => r.status === 5).length,
      rejected: reports.value.filter(r => r.status === 3 || r.status === 4).length,
    }
  } catch (error) {
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

const viewDetail = (row) => {
  currentReport.value = row
  detailVisible.value = true
}

const onVerify = async (row, approved) => {
  const action = approved ? t('verifyList.btnApprove') : t('verifyList.btnReject')
  try {
    await ElMessageBox.confirm(t(approved ? 'verifyList.confirmApprove' : 'verifyList.confirmReject'), t('common.confirm'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await reviewReport({
      reportId: row.id,
      approved,
      comment: approved ? t('verifyList.approveSuccess') : t('verifyList.rejectSuccess'),
    })
    ElMessage.success(action)
    loadReports()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${t('verifyList.operationFailed')}: ${error.message || ''}`)
    }
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

onMounted(() => {
  loadReports()
  loadBlockchainStatus()
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
