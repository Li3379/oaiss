<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEnterpriseAdmissionList,
  issueEnterpriseAdmission,
  revokeEnterpriseAdmission,
  getReviewerQualificationList,
  issueReviewerQualification,
  revokeReviewerQualification,
} from '../../api/admin'

const { t } = useI18n()

const activeTab = ref('admission')

// Admission state
const admissionList = ref<Record<string, unknown>[]>([])
const admissionLoading = ref(false)
const admissionPage = ref(1)
const admissionPageSize = ref(10)
const admissionTotal = ref(0)

// Qualification state
const qualificationList = ref<Record<string, unknown>[]>([])
const qualificationLoading = ref(false)
const qualificationPage = ref(1)
const qualificationPageSize = ref(10)
const qualificationTotal = ref(0)

// Issue dialog
const issueDialogVisible = ref(false)
const issueType = ref<'admission' | 'qualification'>('admission')
const issueForm = reactive({ enterpriseId: '', reviewerId: '' })

watch(issueType, () => {
  issueForm.enterpriseId = ''
  issueForm.reviewerId = ''
})

const fetchAdmissionData = async () => {
  admissionLoading.value = true
  try {
    const res = await getEnterpriseAdmissionList({ pageNum: admissionPage.value, pageSize: admissionPageSize.value }) as Record<string, unknown>
    admissionList.value = (res.items as Record<string, unknown>[]) || []
    admissionTotal.value = (res.total as number) || 0
  } catch {
    ElMessage.error(t('certificateManage.loadFailed'))
  } finally {
    admissionLoading.value = false
  }
}

const fetchQualificationData = async () => {
  qualificationLoading.value = true
  try {
    const res = await getReviewerQualificationList({ pageNum: qualificationPage.value, pageSize: qualificationPageSize.value }) as Record<string, unknown>
    qualificationList.value = (res.items as Record<string, unknown>[]) || []
    qualificationTotal.value = (res.total as number) || 0
  } catch {
    ElMessage.error(t('certificateManage.loadFailed'))
  } finally {
    qualificationLoading.value = false
  }
}

const onTabChange = (tab: string) => {
  if (tab === 'admission') fetchAdmissionData()
  else fetchQualificationData()
}

const handleIssueAdmission = async () => {
  const id = Number(issueForm.enterpriseId)
  if (!id || id <= 0) {
    ElMessage.warning(t('certificateManage.enterEnterpriseId'))
    return
  }
  try {
    await issueEnterpriseAdmission(id)
    ElMessage.success(t('certificateManage.issueSuccess'))
    issueDialogVisible.value = false
    fetchAdmissionData()
  } catch {
    ElMessage.error(t('certificateManage.issueFailed'))
  }
}

const handleIssueQualification = async () => {
  const id = Number(issueForm.reviewerId)
  if (!id || id <= 0) {
    ElMessage.warning(t('certificateManage.enterReviewerId'))
    return
  }
  try {
    await issueReviewerQualification(id)
    ElMessage.success(t('certificateManage.issueSuccess'))
    issueDialogVisible.value = false
    fetchQualificationData()
  } catch {
    ElMessage.error(t('certificateManage.issueFailed'))
  }
}

const handleRevokeAdmission = async (row: Record<string, unknown>) => {
  try {
    await ElMessageBox.confirm(
      t('certificateManage.confirmRevoke'),
      t('certificateManage.revokeTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
    await revokeEnterpriseAdmission(row.enterpriseId as number)
    ElMessage.success(t('certificateManage.revokeSuccess'))
    fetchAdmissionData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(t('certificateManage.revokeFailed'))
  }
}

const handleRevokeQualification = async (row: Record<string, unknown>) => {
  try {
    await ElMessageBox.confirm(
      t('certificateManage.confirmRevoke'),
      t('certificateManage.revokeTitle'),
      { confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning' }
    )
    await revokeReviewerQualification(row.reviewerId as number)
    ElMessage.success(t('certificateManage.revokeSuccess'))
    fetchQualificationData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(t('certificateManage.revokeFailed'))
  }
}

const getStatusType = (status: number) => (status === 1 ? 'success' : 'danger')
const getStatusText = (status: number) =>
  status === 1 ? t('certificateManage.active') : t('certificateManage.revoked')

const onAdmissionSizeChange = (size: number) => {
  admissionPageSize.value = size
  admissionPage.value = 1
  fetchAdmissionData()
}
const onAdmissionCurrentChange = (page: number) => {
  admissionPage.value = page
  fetchAdmissionData()
}
const onQualificationSizeChange = (size: number) => {
  qualificationPageSize.value = size
  qualificationPage.value = 1
  fetchQualificationData()
}
const onQualificationCurrentChange = (page: number) => {
  qualificationPage.value = page
  fetchQualificationData()
}

onMounted(() => {
  fetchAdmissionData()
})
</script>

<template>
  <section class="certificate-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('certificateManage.breadcrumbAdmin') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('certificateManage.breadcrumbCertificate') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <el-card class="section-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- Admission Tab -->
        <el-tab-pane :label="t('certificateManage.tabAdmission')" name="admission">
          <div style="margin-bottom: 14px;">
            <el-button type="primary" @click="issueDialogVisible = true; issueType = 'admission'">
              {{ t('certificateManage.issue') }}
            </el-button>
          </div>
          <el-table :data="admissionList" border v-loading="admissionLoading">
            <el-table-column prop="id" :label="t('certificateManage.colId')" width="80" />
            <el-table-column prop="enterpriseId" :label="t('certificateManage.colEnterpriseId')" min-width="120" />
            <el-table-column prop="certificateNo" :label="t('certificateManage.colCertificateNo')" min-width="180" />
            <el-table-column prop="issuedDate" :label="t('certificateManage.colIssuedDate')" min-width="120" />
            <el-table-column prop="status" :label="t('certificateManage.colStatus')" min-width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" :label="t('certificateManage.colCreateTime')" min-width="170" />
            <el-table-column :label="t('certificateManage.colOperation')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" link type="danger" @click="handleRevokeAdmission(row)">
                  {{ t('certificateManage.revoke') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="admissionPage"
              v-model:page-size="admissionPageSize"
              background
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="admissionTotal"
              @size-change="onAdmissionSizeChange"
              @current-change="onAdmissionCurrentChange"
            />
          </div>
        </el-tab-pane>

        <!-- Qualification Tab -->
        <el-tab-pane :label="t('certificateManage.tabQualification')" name="qualification">
          <div style="margin-bottom: 14px;">
            <el-button type="primary" @click="issueDialogVisible = true; issueType = 'qualification'">
              {{ t('certificateManage.issue') }}
            </el-button>
          </div>
          <el-table :data="qualificationList" border v-loading="qualificationLoading">
            <el-table-column prop="id" :label="t('certificateManage.colId')" width="80" />
            <el-table-column prop="reviewerId" :label="t('certificateManage.colReviewerId')" min-width="120" />
            <el-table-column prop="certificateNo" :label="t('certificateManage.colCertificateNo')" min-width="180" />
            <el-table-column prop="qualificationType" :label="$t('certificateManage.colQualificationType')" min-width="150" />
            <el-table-column prop="issuedDate" :label="t('certificateManage.colIssuedDate')" min-width="120" />
            <el-table-column prop="status" :label="t('certificateManage.colStatus')" min-width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" :label="t('certificateManage.colCreateTime')" min-width="170" />
            <el-table-column :label="t('certificateManage.colOperation')" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 1" link type="danger" @click="handleRevokeQualification(row)">
                  {{ t('certificateManage.revoke') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="qualificationPage"
              v-model:page-size="qualificationPageSize"
              background
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="qualificationTotal"
              @size-change="onQualificationSizeChange"
              @current-change="onQualificationCurrentChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Issue Dialog -->
    <el-dialog v-model="issueDialogVisible" :title="t('certificateManage.issue')" width="400px">
      <el-form v-if="issueType === 'admission'">
        <el-form-item :label="t('certificateManage.colEnterpriseId')">
          <el-input v-model="issueForm.enterpriseId" :placeholder="t('certificateManage.enterEnterpriseId')" />
        </el-form-item>
      </el-form>
      <el-form v-else>
        <el-form-item :label="t('certificateManage.colReviewerId')">
          <el-input v-model="issueForm.reviewerId" :placeholder="t('certificateManage.enterReviewerId')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="issueType === 'admission' ? handleIssueAdmission() : handleIssueQualification()">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.certificate-page {
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
