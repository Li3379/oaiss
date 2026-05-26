<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getCarbonReports, getOrgInfo, getStatistics, updateContact } from '../../api/thirdParty'
import { formatDateTime } from '../../utils/format'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const statistics = ref({
  totalReports: 0,
  pendingReports: 0,
  approvedReports: 0,
  rejectedReports: 0,
})

const orgInfo = ref<Record<string, any> | null>(null)
const orgLoading = ref(false)
const savingContact = ref(false)

const searchForm = reactive({
  enterpriseId: '',
  keyword: '',
  status: '' as '' | number,
})

const contactForm = reactive({
  contactPerson: '',
  contactPhone: '',
})

const approvalRate = computed(() => {
  const total = statistics.value.approvedReports + statistics.value.rejectedReports
  if (total === 0) return '0%'
  return `${((statistics.value.approvedReports / total) * 100).toFixed(1)}%`
})

const statsLoading = ref(false)

const reports = ref<Record<string, any>[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loadStatistics = async () => {
  try {
    statsLoading.value = true
    const result = await (getStatistics() as Promise<Record<string, any>>)
    statistics.value = {
      totalReports: result?.totalReports || 0,
      pendingReports: result?.pendingReports || 0,
      approvedReports: result?.approvedReports || 0,
      rejectedReports: result?.rejectedReports || 0,
    }
  } catch (error) {
    console.error('Failed to load statistics:', error)
    ElMessage.error(t('monitor.loadFailed'))
  } finally {
    statsLoading.value = false
  }
}

const loadOrgInfo = async () => {
  try {
    orgLoading.value = true
    const result = await (getOrgInfo() as Promise<Record<string, any>>)
    orgInfo.value = result
    contactForm.contactPerson = result?.contactPerson || ''
    contactForm.contactPhone = result?.contactPhone || ''
  } catch {
    orgInfo.value = null
    ElMessage.error(t('monitor.loadOrgFailed'))
  } finally {
    orgLoading.value = false
  }
}

const loadReports = async () => {
  try {
    loading.value = true
    const enterpriseIdValue = searchForm.enterpriseId.trim()
    const keyword = searchForm.keyword.trim()
    const enterpriseId = enterpriseIdValue ? Number(enterpriseIdValue) : undefined
    const result = await getCarbonReports({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      enterpriseId: Number.isFinite(enterpriseId) ? enterpriseId : undefined,
      keyword: keyword || undefined,
      status: searchForm.status || undefined,
    } as any)
    reports.value = (result as any)?.items || []
    total.value = (result as any)?.total || 0
  } catch {
    ElMessage.error(t('monitor.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  currentPage.value = 1
  loadReports()
}

const onReset = () => {
  searchForm.enterpriseId = ''
  searchForm.keyword = ''
  searchForm.status = ''
  currentPage.value = 1
  loadReports()
}

const saveContact = async () => {
  const contactPerson = contactForm.contactPerson.trim()
  const contactPhone = contactForm.contactPhone.trim()

  if (!contactPerson || !contactPhone) {
    ElMessage.warning(t('monitor.contactSaveFailed'))
    return
  }

  if (!/^\d{7,20}$/.test(contactPhone)) {
    ElMessage.warning(t('monitor.contactSaveFailed'))
    return
  }

  try {
    savingContact.value = true
    await updateContact({
      contactPerson,
      contactPhone,
    })
    ElMessage.success(t('monitor.contactSaveSuccess'))
    await loadOrgInfo()
  } catch (error: any) {
    const backendMessage = error?.response?.data?.data?.[0] || error?.response?.data?.message || error?.message
    ElMessage.error(backendMessage || t('monitor.contactSaveFailed'))
  } finally {
    savingContact.value = false
  }
}

const onSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadReports()
}

const onCurrentChange = (page: number) => {
  currentPage.value = page
  loadReports()
}

const statusMap = computed<Record<number, { tag: string; text: string }>>(() => ({
  0: { tag: 'info', text: t('monitor.statusDraft') },
  1: { tag: 'warning', text: t('monitor.statusPending') },
  2: { tag: 'warning', text: t('monitor.statusPending') },
  3: { tag: 'success', text: t('monitor.statusApproved') },
  4: { tag: 'danger', text: t('monitor.statusRejected') },
  5: { tag: 'success', text: t('monitor.statusOnChain') },
}))

const getStatusTag = (status: number) => statusMap.value[status]?.tag || 'info'
const getStatusText = (status: number) => statusMap.value[status]?.text || String(status)

onMounted(() => {
  loadStatistics()
  loadOrgInfo()
  loadReports()
})
</script>

<template>
  <PageContainer :title="t('monitor.title')" :description="t('monitor.description')">
    <section class="monitor-page">
      <el-card class="section-card" shadow="never" v-loading="orgLoading">
        <template #header>
          <span class="card-header">{{ t('monitor.orgInfoTitle') }}</span>
        </template>

        <div v-if="orgInfo" class="org-grid">
          <div><strong>{{ t('monitor.orgName') }}:</strong> {{ orgInfo.orgName || '-' }}</div>
          <div><strong>{{ t('monitor.accessLevel') }}:</strong> {{ orgInfo.accessLevel || '-' }}</div>
          <div><strong>{{ t('monitor.address') }}:</strong> {{ orgInfo.address || '-' }}</div>
          <div><strong>{{ t('monitor.contactPerson') }}:</strong> {{ orgInfo.contactPerson || '-' }}</div>
          <div><strong>{{ t('monitor.contactPhone') }}:</strong> {{ orgInfo.contactPhone || '-' }}</div>
        </div>

        <el-form :inline="true" class="contact-form">
          <el-form-item :label="t('monitor.contactPerson')">
            <el-input v-model="contactForm.contactPerson" />
          </el-form-item>
          <el-form-item :label="t('monitor.contactPhone')">
            <el-input v-model="contactForm.contactPhone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingContact" @click="saveContact">
              {{ t('common.save') }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="section-card" shadow="never" v-loading="statsLoading">
        <template #header>
          <span class="card-header">{{ t('monitor.dataStats') }}</span>
        </template>
        <div class="stats-grid">
          <div class="stat-card total">
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statTotalReports') }}</div>
              <div class="stat-value">{{ statistics.totalReports }}</div>
            </div>
          </div>
          <div class="stat-card pending">
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statPending') }}</div>
              <div class="stat-value">{{ statistics.pendingReports }}</div>
            </div>
          </div>
          <div class="stat-card approved">
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statApproved') }}</div>
              <div class="stat-value">{{ statistics.approvedReports }}</div>
            </div>
          </div>
          <div class="stat-card rejected">
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statRejected') }}</div>
              <div class="stat-value">{{ statistics.rejectedReports }}</div>
            </div>
          </div>
          <div class="stat-card rate">
            <div class="stat-content">
              <div class="stat-label">{{ t('monitor.statApprovalRate') }}</div>
              <div class="stat-value">{{ approvalRate }}</div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="section-card" shadow="never">
        <template #header>
          <span class="card-header">{{ t('monitor.filterTitle') }}</span>
        </template>
        <el-form :inline="true" class="search-form">
          <el-form-item :label="t('monitor.enterpriseIdLabel')">
            <el-input v-model="searchForm.enterpriseId" :placeholder="t('monitor.enterpriseIdPlaceholder')" clearable />
          </el-form-item>
          <el-form-item :label="t('common.enterKeyword')">
            <el-input v-model="searchForm.keyword" :placeholder="t('common.enterKeyword')" clearable />
          </el-form-item>
          <el-form-item :label="t('monitor.colStatus')">
            <el-select v-model="searchForm.status" clearable style="width: 160px">
              <el-option :label="t('monitor.statusDraft')" :value="0" />
              <el-option :label="t('monitor.statusPending')" :value="1" />
              <el-option :label="t('monitor.statusApproved')" :value="3" />
              <el-option :label="t('monitor.statusRejected')" :value="4" />
              <el-option :label="t('monitor.statusOnChain')" :value="5" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">{{ t('common.search') }}</el-button>
            <el-button @click="onReset">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>
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

.org-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px 18px;
  margin-bottom: 12px;
}

.contact-form,
.search-form {
  display: flex;
  flex-wrap: wrap;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  padding: 10px 0;
}

.stat-card {
  border-radius: 8px;
  padding: 20px;
  color: #fff;
}

.stat-card.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-card.pending {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card.approved {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-card.rejected {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-card.rate {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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
