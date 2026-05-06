<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getProject,
  updateProject,
  submitProject,
  startProject,
  submitVerification,
  updateMonitoring,
  applyCertification,
  terminateProject,
} from '../../api/carbonNeutral'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const route = useRoute()
const projectId = computed(() => route.params.id)

const loading = ref(false)
const project = ref(null)

const activeTab = ref('info')
const monitorForm = ref({ emissionData: '', description: '' })
const monitorLoading = ref(false)

const loadProject = async () => {
  try {
    loading.value = true
    const result = await getProject(projectId.value)
    project.value = result
  } catch (error) {
    ElMessage.error(t('carbonNeutralDetail.loadFailed'))
  } finally {
    loading.value = false
  }
}

const getStatusTag = (status) => {
  const map = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    IN_PROGRESS: 'primary',
    VERIFYING: 'warning',
    CERTIFIED: 'success',
    TERMINATED: 'info',
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    DRAFT: t('carbonNeutralDetail.statusDraft'),
    PENDING: t('carbonNeutralDetail.statusPending'),
    APPROVED: t('carbonNeutralDetail.statusApproved'),
    REJECTED: t('carbonNeutralDetail.statusRejected'),
    IN_PROGRESS: t('carbonNeutralDetail.statusImplementing'),
    VERIFYING: t('carbonNeutralDetail.statusVerifying'),
    CERTIFIED: t('carbonNeutralDetail.statusCertified'),
    TERMINATED: t('carbonNeutralDetail.statusTerminated'),
  }
  return map[status] || status
}

const handleAction = async (action, confirmMsg) => {
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

const onSubmitVerification = () => handleAction(
  () => submitVerification(projectId.value, {}),
  t('carbonNeutralDetail.confirmSubmitVerify'),
)

const onApplyCertification = () => handleAction(
  () => applyCertification(projectId.value),
  t('carbonNeutralDetail.confirmApplyCertification'),
)

const onTerminate = () => handleAction(
  () => terminateProject(projectId.value, {}),
  t('carbonNeutralDetail.confirmTerminate'),
)

const onSaveMonitoring = async () => {
  try {
    monitorLoading.value = true
    await updateMonitoring(projectId.value, monitorForm.value)
    ElMessage.success(t('carbonNeutralDetail.monitorUpdateSuccess'))
    monitorForm.value = { emissionData: '', description: '' }
    await loadProject()
  } catch (error) {
    ElMessage.error(t('carbonNeutralDetail.monitorUpdateFailed'))
  } finally {
    monitorLoading.value = false
  }
}

const canSubmit = computed(() => project.value?.status === 'DRAFT')
const canStart = computed(() => project.value?.status === 'APPROVED')
const canVerify = computed(() => project.value?.status === 'IN_PROGRESS')
const canCertify = computed(() => project.value?.status === 'VERIFYING')
const canTerminate = computed(() => !['TERMINATED', 'CERTIFIED'].includes(project.value?.status))

onMounted(() => {
  loadProject()
})
</script>

<template>
  <PageContainer :title="t('carbonNeutralDetail.title')" :description="t('carbonNeutralDetail.description')">
    <section class="detail-page" v-loading="loading">
      <el-card class="section-card" shadow="never" v-if="project">
        <template #header>
          <div class="card-header-row">
            <span class="card-header">{{ project.projectName }}</span>
            <div class="action-bar">
              <el-tag :type="getStatusTag(project.status)" size="large">{{ getStatusText(project.status) }}</el-tag>
              <el-button v-if="canSubmit" type="primary" @click="onSubmit">{{ t('carbonNeutralDetail.actionSubmitReview') }}</el-button>
              <el-button v-if="canStart" type="success" @click="onStart">{{ t('carbonNeutralDetail.actionStartImplement') }}</el-button>
              <el-button v-if="canVerify" type="warning" @click="onSubmitVerification">{{ t('carbonNeutralDetail.actionSubmitVerify') }}</el-button>
              <el-button v-if="canCertify" type="success" @click="onApplyCertification">{{ t('carbonNeutralDetail.actionApplyCertification') }}</el-button>
              <el-button v-if="canTerminate" type="danger" @click="onTerminate">{{ t('carbonNeutralDetail.actionTerminate') }}</el-button>
            </div>
          </div>
        </template>

        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('carbonNeutralDetail.tabInfo')" name="info">
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelProjectName')">{{ project.projectName }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelProjectType')">{{ project.projectType }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelExpectedReduction')">{{ project.expectedReduction }} {{ t('common.unit_ton') }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelActualReduction')">{{ project.actualReduction || '-' }} {{ t('common.unit_ton') }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelStartDate')">{{ project.startDate }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelEndDate')">{{ project.endDate }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelCreateTime')">{{ project.createdAt }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelUpdateTime')">{{ project.updatedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item :label="t('carbonNeutralDetail.labelDescription')" :span="2">{{ project.description }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <el-tab-pane :label="t('carbonNeutralDetail.tabMonitor')" name="monitoring" :disabled="!canVerify && !canTerminate">
            <div style="padding: 20px 0; max-width: 700px">
              <el-form label-width="120px">
                <el-form-item :label="t('carbonNeutralDetail.monitorEmissionData')">
                  <el-input v-model="monitorForm.emissionData" type="textarea" :rows="6" :placeholder="t('carbonNeutralDetail.monitorEmissionPlaceholder')" />
                </el-form-item>
                <el-form-item :label="t('carbonNeutralDetail.monitorDescription')">
                  <el-input v-model="monitorForm.description" type="textarea" :rows="3" :placeholder="t('carbonNeutralDetail.monitorDescriptionPlaceholder')" />
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
