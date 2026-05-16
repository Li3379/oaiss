<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyProjects, createProject, submitProject, startProject, applyCertification, terminateProject } from '../../api/carbonNeutral'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const projects = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createDialogVisible = ref(false)
const createFormRef = ref(null)
const createForm = reactive({
  projectName: '',
  projectType: '',
  description: '',
  expectedReduction: '',
  startDate: '',
  endDate: '',
})

const projectTypeOptions = [
  { label: 'carbonNeutral.typeCarbonSink', value: 1 },
  { label: 'carbonNeutral.typeCCUS', value: 2 },
  { label: 'carbonNeutral.typeNewEnergy', value: 3 },
  { label: 'carbonNeutral.typeEnergySaving', value: 4 },
  { label: 'carbonNeutral.typeOther', value: 5 },
]

const createFormRules = {
  projectName: [{ required: true, message: t('carbonNeutral.enterProjectName'), trigger: 'blur' }],
  projectType: [{ required: true, message: t('carbonNeutral.selectProjectType'), trigger: 'change' }],
  description: [{ required: true, message: t('carbonNeutral.enterDescription'), trigger: 'blur' }],
  expectedReduction: [
    { required: true, message: t('carbonNeutral.enterExpectedReduction'), trigger: 'blur' },
    { pattern: /^\d+(\.\d+)?$/, message: t('carbonNeutral.invalidNumber'), trigger: 'blur' },
  ],
  startDate: [{ required: true, message: t('carbonNeutral.selectStartDate'), trigger: 'change' }],
  endDate: [{ required: true, message: t('carbonNeutral.selectEndDate'), trigger: 'change' }],
}

const loadProjects = async () => {
  try {
    loading.value = true
    const result = await getMyProjects({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    projects.value = result?.items || []
    total.value = result?.total || 0
  } catch (error) {
    ElMessage.error(t('carbonNeutral.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadProjects()
}

const onCurrentChange = (page) => {
  currentPage.value = page
  loadProjects()
}

const getProjectTypeLabel = (type) => {
  const option = projectTypeOptions.find(opt => opt.value === type)
  if (!option) return type
  return t(option.label)
}

const getProjectStatusTag = (status: number) => {
  const map: Record<number, string> = {
    0: 'info',      // DRAFT
    1: 'warning',   // PENDING
    2: 'success',   // APPROVED
    3: 'primary',   // IMPLEMENTING
    4: 'success',   // COMPLETED
    5: 'danger',    // TERMINATED
    6: 'danger',    // REJECTED
  }
  return map[status] || 'info'
}

const getProjectStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: t('carbonNeutral.statusDraft'),
    1: t('carbonNeutral.statusPending'),
    2: t('carbonNeutral.statusApproved'),
    3: t('carbonNeutral.statusImplementing'),
    4: t('carbonNeutral.statusCompleted'),
    5: t('carbonNeutral.statusTerminated'),
    6: t('carbonNeutral.statusRejected'),
  }
  return map[status] || String(status)
}

const openCreateDialog = () => {
  createDialogVisible.value = true
}

const closeCreateDialog = () => {
  createDialogVisible.value = false
  createFormRef.value?.resetFields()
  Object.assign(createForm, {
    projectName: '',
    projectType: '',
    description: '',
    expectedReduction: '',
    startDate: '',
    endDate: '',
  })
}

const handleCreate = async () => {
  try {
    await createFormRef.value.validate()
    await ElMessageBox.confirm(t('carbonNeutral.confirmCreate'), t('carbonNeutral.createProject'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    await createProject(createForm)
    ElMessage.success(t('carbonNeutral.createSuccess'))
    closeCreateDialog()
    loadProjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(t('carbonNeutral.createFailed'))
    }
  }
}

const onSubmitProject = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonNeutral.confirmSubmitProject'), t('common.confirm'), { type: 'warning' })
    await submitProject(row.id)
    ElMessage.success(t('carbonNeutral.submitSuccess'))
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonNeutral.submitFailed'))
  }
}

const onStartProject = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonNeutral.confirmStartProject'), t('common.confirm'), { type: 'warning' })
    await startProject(row.id)
    ElMessage.success(t('carbonNeutral.startSuccess'))
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonNeutral.startFailed'))
  }
}

const onApplyCertification = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonNeutral.confirmCertification'), t('common.confirm'), { type: 'warning' })
    await applyCertification(row.id)
    ElMessage.success(t('carbonNeutral.certificationApplied'))
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonNeutral.certificationFailed'))
  }
}

const onTerminateProject = async (row) => {
  try {
    await ElMessageBox.confirm(t('carbonNeutral.confirmTerminate'), t('common.confirm'), { type: 'warning' })
    await terminateProject(row.id, { reason: t('carbonNeutral.terminateReason') })
    ElMessage.success(t('carbonNeutral.terminateSuccess'))
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('carbonNeutral.terminateFailed'))
  }
}

onMounted(() => {
  loadProjects()
})
</script>

<template>
  <PageContainer :title="t('carbonNeutral.title')" :description="t('carbonNeutral.description')">
    <section class="carbon-neutral-page">
      <el-card class="section-card" shadow="never">
        <template #header>
          <div class="card-header-row">
            <span class="card-header">{{ t('carbonNeutral.projectList') }}</span>
            <el-button type="primary" @click="openCreateDialog">{{ t('carbonNeutral.createProject') }}</el-button>
          </div>
        </template>
        <el-table :data="projects" border v-loading="loading" :empty-text="t('carbonNeutral.emptyText')">
          <el-table-column :label="t('tradingMarket.colIndex')" width="80">
            <template #default="scope">
              {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>
          <el-table-column prop="projectName" :label="t('carbonNeutral.colProjectName')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="projectType" :label="t('carbonNeutral.colProjectType')" min-width="120">
            <template #default="{ row }">
              {{ getProjectTypeLabel(row.projectType) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" :label="t('carbonNeutral.colDescription')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="expectedReduction" :label="t('carbonNeutral.colExpectedReduction')" min-width="140" />
          <el-table-column prop="status" :label="t('carbonNeutral.colStatus')" min-width="100">
            <template #default="{ row }">
              <el-tag :type="getProjectStatusTag(row.status)">{{ getProjectStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startDate" :label="t('carbonNeutral.colStartDate')" min-width="120" />
          <el-table-column prop="endDate" :label="t('carbonNeutral.colEndDate')" min-width="120" />
          <el-table-column prop="createdAt" :label="t('carbonNeutral.colCreateTime')" min-width="170" />
          <el-table-column :label="t('common.operation')" width="280" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="onSubmitProject(row)">{{ t('carbonNeutral.submit') }}</el-button>
              <el-button v-if="row.status === 'APPROVED'" link type="primary" @click="onStartProject(row)">{{ t('carbonNeutral.start') }}</el-button>
              <el-button v-if="row.status === 'IN_PROGRESS'" link type="success" @click="onApplyCertification(row)">{{ t('carbonNeutral.applyCert') }}</el-button>
              <el-button v-if="['DRAFT', 'APPROVED', 'IN_PROGRESS'].includes(row.status)" link type="danger" @click="onTerminateProject(row)">{{ t('carbonNeutral.terminate') }}</el-button>
              <router-link :to="`/enterprise/carbon-neutral/projects/${row.id}`">
                <el-button link type="primary">{{ t('carbonNeutral.viewDetail') }}</el-button>
              </router-link>
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

      <el-dialog v-model="createDialogVisible" :title="t('carbonNeutral.title')" width="600px">
        <el-form :model="createForm" :rules="createFormRules" ref="createFormRef" label-width="120px">
          <el-form-item :label="t('carbonNeutral.colProjectName')" prop="projectName">
            <el-input v-model="createForm.projectName" :placeholder="t('carbonNeutral.enterProjectName')" />
          </el-form-item>
          <el-form-item :label="t('carbonNeutral.colProjectType')" prop="projectType">
            <el-select v-model="createForm.projectType" :placeholder="t('carbonNeutral.selectProjectType')" style="width: 100%">
              <el-option v-for="item in projectTypeOptions" :key="item.value" :label="t(item.label)" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('carbonNeutral.colDescription')" prop="description">
            <el-input v-model="createForm.description" type="textarea" :rows="3" :placeholder="t('carbonNeutral.enterDescription')" />
          </el-form-item>
          <el-form-item :label="t('carbonNeutral.colExpectedReduction')" prop="expectedReduction">
            <el-input v-model="createForm.expectedReduction" :placeholder="t('carbonNeutral.enterExpectedReduction')">
              <template #append>{{ t('common.unit_ton') }}</template>
            </el-input>
          </el-form-item>
          <el-form-item :label="t('carbonNeutral.colStartDate')" prop="startDate">
            <el-date-picker v-model="createForm.startDate" type="date" :placeholder="t('carbonNeutral.selectStartDate')" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('carbonNeutral.colEndDate')" prop="endDate">
            <el-date-picker v-model="createForm.endDate" type="date" :placeholder="t('carbonNeutral.selectEndDate')" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeCreateDialog">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="handleCreate">{{ t('common.confirm') }}</el-button>
        </template>
      </el-dialog>
    </section>
  </PageContainer>
</template>

<style scoped>
.carbon-neutral-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
}

.pagination-row {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
