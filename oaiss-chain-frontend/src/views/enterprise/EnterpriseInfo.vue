<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getEnterpriseInfo, getQuotaInfo, updateContact } from '../../api/enterprise'

const { t } = useI18n()

const loading = ref(false)
const enterpriseInfo = ref<Record<string, unknown> | null>(null)
const quotaInfo = ref<Record<string, unknown> | null>(null)

const contactDialogVisible = ref(false)
const contactForm = ref({ contactPerson: '', contactPhone: '' })
const contactLoading = ref(false)

const fetchInfo = async () => {
  loading.value = true
  try {
    const [info, quota] = await Promise.all([
      getEnterpriseInfo(),
      getQuotaInfo(),
    ])
    enterpriseInfo.value = info as Record<string, unknown>
    quotaInfo.value = quota as Record<string, unknown>
    if ((info as Record<string, unknown>).contactPerson) {
      contactForm.value.contactPerson = (info as Record<string, unknown>).contactPerson as string
    }
    if ((info as Record<string, unknown>).contactPhone) {
      contactForm.value.contactPhone = (info as Record<string, unknown>).contactPhone as string
    }
  } catch {
    ElMessage.error(t('enterpriseInfo.loadFailed'))
  } finally {
    loading.value = false
  }
}

const openContactDialog = () => {
  contactDialogVisible.value = true
}

const submitContact = async () => {
  contactLoading.value = true
  try {
    await updateContact({
      contactPerson: contactForm.value.contactPerson.trim(),
      contactPhone: contactForm.value.contactPhone.trim(),
    })
    ElMessage.success(t('enterpriseInfo.contactUpdated'))
    contactDialogVisible.value = false
    fetchInfo()
  } catch {
    ElMessage.error(t('enterpriseInfo.contactUpdateFailed'))
  } finally {
    contactLoading.value = false
  }
}

onMounted(() => fetchInfo())
</script>

<template>
  <section class="info-page">
    <el-card class="section-card" shadow="never">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>{{ t('enterpriseInfo.breadcrumbEnterprise') }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ t('enterpriseInfo.breadcrumbInfo') }}</el-breadcrumb-item>
      </el-breadcrumb>
    </el-card>

    <div v-loading="loading">
      <el-card class="section-card" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>{{ t('enterpriseInfo.enterpriseInfoTitle') }}</span>
            <el-button type="primary" size="small" @click="openContactDialog">{{ t('enterpriseInfo.editContact') }}</el-button>
          </div>
        </template>
        <el-descriptions v-if="enterpriseInfo" :column="2" border>
          <el-descriptions-item :label="t('enterpriseInfo.companyName')">{{ enterpriseInfo.companyName || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.industry')">{{ enterpriseInfo.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.contactPerson')">{{ enterpriseInfo.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.contactPhone')">{{ enterpriseInfo.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.registrationDate')">{{ enterpriseInfo.registrationDate || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.address')">{{ enterpriseInfo.address || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="section-card" shadow="never" style="margin-top: 14px;">
        <template #header>
          <span>{{ t('enterpriseInfo.quotaInfoTitle') }}</span>
        </template>
        <el-descriptions v-if="quotaInfo" :column="2" border>
          <el-descriptions-item :label="t('enterpriseInfo.totalQuota')">{{ quotaInfo.totalQuota ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.usedQuota')">{{ quotaInfo.usedQuota ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.tradableQuota')">{{ quotaInfo.tradableQuota ?? '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.quotaPeriod')">{{ quotaInfo.period || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <el-dialog v-model="contactDialogVisible" :title="t('enterpriseInfo.editContactTitle')" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item :label="t('enterpriseInfo.contactPerson')">
          <el-input v-model="contactForm.contactPerson" :placeholder="t('enterpriseInfo.enterContactPerson')" />
        </el-form-item>
        <el-form-item :label="t('enterpriseInfo.contactPhone')">
          <el-input v-model="contactForm.contactPhone" :placeholder="t('enterpriseInfo.enterContactPhone')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="contactLoading" @click="submitContact">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.info-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}
</style>
