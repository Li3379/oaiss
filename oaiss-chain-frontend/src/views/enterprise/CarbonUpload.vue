<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getMyReports, createReport, deleteReport, submitReport } from '../../api/carbon'

const { t } = useI18n()

const searchForm = reactive({ title: '', accountingPeriod: '' })
const tableData = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const formRef = ref()
const formModel = reactive({
  accountingPeriod: '',
  title: '',
  reportType: 1,
  emissionData: '',
  calculationMethod: '',
  attachments: '',
})

const reportTypeOptions = [
  { label: t('carbonUpload.reportTypeQuarterly'), value: 1 },
  { label: t('carbonUpload.reportTypeAnnual'), value: 2 },
]

const formRules = {
  accountingPeriod: [{ required: true, message: t('carbonUpload.enterAccountingPeriod'), trigger: 'blur' }],
  title: [{ required: true, message: t('carbonUpload.enterReportTitle'), trigger: 'blur' }],
  reportType: [{ required: true, message: t('carbonUpload.selectReportType'), trigger: 'change' }],
  emissionData: [{ required: true, message: t('carbonUpload.enterEmissionData'), trigger: 'blur' }],
}

const statusTagType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const data = await getMyReports({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      title: searchForm.title || undefined,
      accountingPeriod: searchForm.accountingPeriod || undefined,
    })
    tableData.value = data.items || []
    total.value = data.total || 0
  } catch {
    ElMessage.error(t('carbonUpload.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onQuery = () => { currentPage.value = 1; fetchData() }
const onSizeChange = (size) => { pageSize.value = size; currentPage.value = 1; fetchData() }
const onCurrentChange = (page) => { currentPage.value = page; fetchData() }

const openAddDialog = () => {
  formModel.accountingPeriod = ''
  formModel.title = ''
  formModel.reportType = 1
  formModel.emissionData = ''
  formModel.calculationMethod = ''
  formModel.attachments = ''
  dialogVisible.value = true
}

const onSubmitForm = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    await createReport({
      accountingPeriod: formModel.accountingPeriod.trim(),
      title: formModel.title.trim(),
      reportType: formModel.reportType,
      emissionData: formModel.emissionData,
      calculationMethod: formModel.calculationMethod || undefined,
      attachments: formModel.attachments || undefined,
    })
    ElMessage.success(t('carbonUpload.createSuccess'))
    dialogVisible.value = false
    fetchData()
  } catch {
    ElMessage.error(t('carbonUpload.createFailed'))
  }
}

const onSubmitReport = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonUpload.confirmSubmit') + ' ' + row.reportNo + '？', t('common.confirm'), { type: 'warning' })
    await submitReport(row.id)
    ElMessage.success(t('carbonUpload.submitSuccess'))
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonUpload.submitFailed'))
  }
}

const onDeleteReport = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonUpload.confirmDelete') + ' ' + row.reportNo + '？', t('common.confirm'), { type: 'warning' })
    await deleteReport(row.id)
    ElMessage.success(t('carbonUpload.deleteSuccess'))
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonUpload.deleteFailed'))
  }
}

onMounted(() => fetchData())
</script>

<template>
  <section class="upload-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('carbonUpload.breadcrumbAccounting') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('carbonUpload.breadcrumbUpload') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item :label="t('carbonUpload.colReportTitle')">
          <el-input v-model="searchForm.title" :placeholder="t('carbonUpload.enterReportTitle')" clearable />
        </el-form-item>
        <el-form-item :label="t('carbonUpload.colAccountingPeriod')">
          <el-input v-model="searchForm.accountingPeriod" :placeholder="t('carbonUpload.enterAccountingPeriod')" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onQuery">{{ t('common.search') }}</el-button>
          <el-button type="success" plain @click="openAddDialog">{{ t('carbonNeutral.createProject') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-table :data="tableData" border v-loading="loading" :empty-text="t('carbonUpload.emptyText')">
        <el-table-column prop="reportNo" :label="t('carbonUpload.colReportNo')" min-width="180" />
        <el-table-column prop="title" :label="t('carbonUpload.colReportTitle')" min-width="200" />
        <el-table-column prop="accountingPeriod" :label="t('carbonUpload.colAccountingPeriod')" min-width="120" />
        <el-table-column prop="totalEmission" :label="t('carbonUpload.colTotalEmission')" min-width="140" />
        <el-table-column prop="statusText" :label="t('common.status')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewerName" :label="t('carbonUpload.colReviewer')" min-width="120" />
        <el-table-column prop="createdAt" :label="t('common.createTime')" min-width="180" />
        <el-table-column :label="t('common.operation')" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" @click="onSubmitReport(row)">{{ t('common.submit') }}</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="onDeleteReport(row)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card" shadow="never">
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="t('carbonUpload.dialogCreate')" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="120px">
        <el-form-item :label="t('carbonUpload.labelAccountingPeriod')" prop="accountingPeriod">
          <el-input v-model="formModel.accountingPeriod" :placeholder="t('carbonUpload.placeholderPeriod')" />
        </el-form-item>
        <el-form-item :label="t('carbonUpload.labelReportTitle')" prop="title">
          <el-input v-model="formModel.title" :placeholder="t('carbonUpload.enterReportTitle')" />
        </el-form-item>
        <el-form-item :label="t('carbonUpload.labelReportType')" prop="reportType">
          <el-select v-model="formModel.reportType" style="width:100%">
            <el-option v-for="item in reportTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('carbonUpload.labelEmissionData')" prop="emissionData">
          <el-input v-model="formModel.emissionData" type="textarea" :rows="6" :placeholder="t('carbonUpload.placeholderEmissionData')" />
        </el-form-item>
        <el-form-item :label="t('carbonUpload.labelMethod')">
          <el-input v-model="formModel.calculationMethod" type="textarea" :rows="3" :placeholder="t('carbonUpload.placeholderMethod')" />
        </el-form-item>
        <el-form-item :label="t('carbonUpload.labelAttachments')">
          <el-input v-model="formModel.attachments" type="textarea" :rows="3" :placeholder="t('carbonUpload.placeholderAttachments')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="onSubmitForm">{{ t('common.create') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.upload-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .upload-page {
    gap: 12px;
  }

  .search-form :deep(.el-input) {
    width: 100%;
  }
}
</style>
