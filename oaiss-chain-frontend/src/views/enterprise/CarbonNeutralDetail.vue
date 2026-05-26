<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getProject,
  submitProject,
  startProject,
  updateMonitoring,
  applyCertification,
  terminateProject,
} from '../../api/carbonNeutral'
import type { CarbonNeutralProjectResponse } from '../../types'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const PROJECT_STATUS = {
  DRAFT: 0,
  PENDING: 1,
  APPROVED: 2,
  IMPLEMENTING: 3,
  COMPLETED: 4,
  TERMINATED: 5,
  REJECTED: 6,
} as const

const route = useRoute()
const projectId = computed(() => Number(route.params.id))

const loading = ref(false)
const project = ref<CarbonNeutralProjectResponse | null>(null)

const activeTab = ref('info')
const monitorForm = ref({ emissionData: '', description: '' })
const monitorLoading = ref(false)

const loadProject = async () => {
  try {
    loading.value = true
    const result = await getProject(projectId.value)
    project.value = result
  } catch {
    ElMessage.error(t('carbonNeutralDetail.loadFailed'))
  } finally {
    loading.value = false
  }
}

const currentStatus = computed(() => {
  const status = project.value?.status
  return typeof status === 'number' ? status : null
})

const getStatusTag = (status: number | null) => {
  const map: Record<number, string> = {
    [PROJECT_STATUS.DRAFT]: 'info',
    [PROJECT_STATUS.PENDING]: 'warning',
    [PROJECT_STATUS.APPROVED]: 'success',
    [PROJECT_STATUS.IMPLEMENTING]: 'primary',
    [PROJECT_STATUS.COMPLETED]: 'success',
    [PROJECT_STATUS.TERMINATED]: 'info',
    [PROJECT_STATUS.REJECTED]: 'danger',
  }
  return status !== null ? (map[status] || 'info') : 'info'
}

const fallbackStatusText = (status: number | null) => {
  const map: Record<number, string> = {
    [PROJECT_STATUS.DRAFT]: t('carbonNeutralDetail.statusDraft'),
    [PROJECT_STATUS.PENDING]: t('carbonNeutralDetail.statusPending'),
    [PROJECT_STATUS.APPROVED]: t('carbonNeutralDetail.statusApproved'),
    [PROJECT_STATUS.IMPLEMENTING]: t('carbonNeutralDetail.statusImplementing'),
    [PROJECT_STATUS.COMPLETED]: t('carbonNeutralDetail.statusCertified'),
    [PROJECT_STATUS.TERMINATED]: t('carbonNeutralDetail.statusTerminated'),
    [PROJECT_STATUS.REJECTED]: t('carbonNeutralDetail.statusRejected'),
  }
  return status !== null ? (map[status] || String(status)) : '-'
}

const statusText = computed(() => project.value?.statusText || fallbackStatusText(currentStatus.value))

const handleAction = async (action: () => Promise<void>, confirmMsg: string) => {
  try {
    await ElMessageBox.confirm(confirmMsg, t('carbonNeutralDetail.confirmAction'), { type: 'warning' })
    loading.value = true
    await action()
    ElMessage.success(t('carbonNeutralDetail.actionSuccess'))
    await loadProject()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(t('carbonNeutralDetail.actionFailed'))
    }
  } finally {
    loading.value = false
  }
}

const onSubmit = () => handleAction(
  () => submitProject(projectId.value),
  t('carbonNeutralDetail.confirmSubmitReview'),
)

const onStart = () => handleAction(
  () => startProject(projectId.value),
  t('carbonNeutralDetail.confirmStartImplement'),
)

const onApplyCertification = () => handleAction(
  () => applyCertification(projectId.value),
  t('carbonNeutralDetail.confirmApplyCertification'),
)

const onTerminate = () => handleAction(
  () => terminateProject(projectId.value, { reason: t('carbonNeutral.terminateReason') }),
  t('carbonNeutralDetail.confirmTerminate'),
)

const onSaveMonitoring = async () => {
  try {
    monitorLoading.value = true
    await updateMonitoring(projectId.value, { monitoringData: monitorForm.value })
    ElMessage.success(t('carbonNeutralDetail.monitorUpdateSuccess'))
    monitorForm.value = { emissionData: '', description: '' }
    await loadProject()
  } catch {
    ElMessage.error(t('carbonNeutralDetail.monitorUpdateFailed'))
  } finally {
    monitorLoading.value = false
  }
}

const canSubmit = computed(() => currentStatus.value === PROJECT_STATUS.DRAFT)
const canStart = computed(() => currentStatus.value === PROJECT_STATUS.APPROVED)
const canCertify = computed(() => currentStatus.value === PROJECT_STATUS.IMPLEMENTING)
const canTerminate = computed(() => {
  const status = currentStatus.value
  return status !== null && ![PROJECT_STATUS.TERMINATED, PROJECT_STATUS.COMPLETED].includes(status)
})
const canEditMonitoring = computed(() => currentStatus.value === PROJECT_STATUS.IMPLEMENTING)

onMounted(() => {
  loadProject()
})
</script>

<template>
  <PageContainer :title="t('carbonNeutralDetail.title')" :description="t('carbonNeutralDetail.description')">
    <section class="detail-page" v-loading="loading">
      <el-card v-if="project" class="section-card" shadow="never">
        <template #header>
          <div class="card-header-row">
            <span class="card-header">{{ project.projectName }}</span>
            <div class="action-bar">
              <el-tag :type="getStatusTag(currentStatus)" size="large">{{ statusText }}</el-tag>
              <el-button v-if="canSubmit" type="primary" @click="onSubmit">{{ t('carbonNeutralDetail.actionSubmitReview') }}</el-button>
              <el-button v-if="canStart" type="success" @click="onStart">{{ t('carbonNeutralDetail.actionStartImplement') }}</el-button>
              <el-button v-if="canCertify" type="success" @click="onApplyCertification">{{ t('carbonNeutralDetail.actionApplyCertification') }}</el-button>
              <el-button v-if="canTerminate" type="danger" @click="onTerminate">{{ t('carbonNeutralDetail.actionTerminate') }}</el-button>
            </div>
          </div>
        </template>

        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('carbonNeutralDetail.tabInfo')" name="info">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelProjectName')">{{ project.projectName }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelProjectType')">{{ project.projectTypeName || project.projectType }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelExpectedReduction')">{{ project.expectedReduction }} {{ t('common.unit_ton') }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelActualReduction')">{{ project.actualReduction || '-' }} {{ t('common.unit_ton') }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelStartDate')">{{ project.startDate }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelEndDate')">{{ project.endDate }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelCreateTime')">{{ project.createdAt }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelUpdateTime')">{{ project.updatedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('common.status')">{{ statusText }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelDescription')" :span="2">{{ project.description }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane :label="t('carbonNeutralDetail.tabMonitor')" name="monitoring" :disabled="!canEditMonitoring">
            <div style="padding: 20px 0; max-width: 700px">
              <el-form label-width="120px">
                <el-form-item :label="t('carbonNeutralDetail.monitorEmissionData')">
                  <el-input
                    v-model="monitorForm.emissionData"
                    type="textarea"
                    :rows="6"
                    :placeholder="t('carbonNeutralDetail.monitorEmissionPlaceholder')"
                  />
                </el-form-item>
                <el-form-item :label="t('carbonNeutralDetail.monitorDescription')">
                  <el-input
                    v-model="monitorForm.description"
                    type="textarea"
                    :rows="3"
                    :placeholder="t('carbonNeutralDetail.monitorDescriptionPlaceholder')"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="monitorLoading" @click="onSaveMonitoring">{{ t('carbonNeutralDetail.monitorUpdate') }}</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>

      <el-empty v-else-if="!loading" :description="t('carbonNeutralDetail.notFound')" />
    </section>
  </PageContainer>
</template>

<style scoped>
.detail-page {
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
  flex-wrap: wrap;
  gap: 12px;
}

.card-header {
  font-weight: 600;
  font-size: 18px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
