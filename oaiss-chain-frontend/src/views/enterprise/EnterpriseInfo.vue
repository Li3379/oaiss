<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { getEnterpriseInfo, getQuotaInfo, updateContact } from '../../api/enterprise'
import { getMyAccount } from '../../api/carbonCoin'
import type { EnterpriseQuotaResponse, EnterpriseResponse } from '../../types'

const { t } = useI18n()

const loading = ref(false)
const enterpriseInfo = ref<EnterpriseResponse | null>(null)
const quotaInfo = ref<EnterpriseQuotaResponse | null>(null)
const carbonCoinBalance = ref<number | null>(null)

const enterpriseName = computed(() => {
  return (enterpriseInfo.value?.enterpriseName as string) || '-'
})

const registrationTime = computed(() => {
  return (enterpriseInfo.value?.createdAt as string) || (enterpriseInfo.value?.registrationDate as string) || '-'
})

const remainingQuota = computed(() => {
  const value = quotaInfo.value?.remainingQuota
  return value ?? '-'
})

const usageRate = computed(() => {
  const value = Number(quotaInfo.value?.usageRate)
  return Number.isFinite(value) ? `${value.toFixed(2)}%` : '-'
})

const contactDialogVisible = ref(false)
const contactForm = ref({ contactPerson: '', contactPhone: '' })
const contactLoading = ref(false)

const fetchInfo = async () => {
  loading.value = true
  try {
    const [info, quota, account] = await Promise.all([
      getEnterpriseInfo(),
      getQuotaInfo(),
      getMyAccount(),
    ])
    enterpriseInfo.value = info
    quotaInfo.value = quota
    carbonCoinBalance.value = Number((account as { balance?: number })?.balance || 0)
    if (info.contactPerson) {
      contactForm.value.contactPerson = info.contactPerson
    }
    if (info.contactPhone) {
      contactForm.value.contactPhone = info.contactPhone
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
    await fetchInfo()
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
          <el-descriptions-item :label="t('enterpriseInfo.companyName')">{{ enterpriseName }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.industry')">{{ enterpriseInfo.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.contactPerson')">{{ enterpriseInfo.contactPerson || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.contactPhone')">{{ enterpriseInfo.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.registrationDate')">{{ registrationTime }}</el-descriptions-item>
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
          <el-descriptions-item :label="t('enterpriseInfo.remainingQuota')">{{ remainingQuota }}</el-descriptions-item>
          <el-descriptions-item :label="t('enterpriseInfo.usageRate')">{{ usageRate }}</el-descriptions-item>
          <el-descriptions-item :label="t('carbonCoin.balance')">{{ carbonCoinBalance ?? '-' }}</el-descriptions-item>
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
