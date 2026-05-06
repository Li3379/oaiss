<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '../../api/user'
import PageContainer from '../../components/PageContainer.vue'

const { t } = useI18n()

const activeTab = ref('info')
const profileLoading = ref(false)
const profile = ref(null)

const pwdFormRef = ref(null)
const pwdLoading = ref(false)
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules = {
  oldPassword: [{ required: true, message: t('userProfile.enterCurrentPassword'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('userProfile.enterNewPassword'), trigger: 'blur' },
    { min: 6, max: 20, message: t('userProfile.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('userProfile.confirmNewPassword'), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error(t('userProfile.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
}

// 当新密码变更时，重新校验确认密码
watch(() => pwdForm.value.newPassword, () => {
  if (pwdForm.value.confirmPassword) {
    pwdFormRef.value?.validateField('confirmPassword')
  }
})

const editFormRef = ref(null)
const editLoading = ref(false)
const editForm = ref({
  realName: '',
  email: '',
  phone: '',
  company: '',
  address: '',
})

const editRules = {
  realName: [{ required: true, message: t('userProfile.enterRealName'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('userProfile.invalidEmail'), trigger: 'blur' }],
}

const loadProfile = async () => {
  try {
    profileLoading.value = true
    const result = await getProfile()
    profile.value = result
    editForm.value = {
      realName: result?.realName || '',
      email: result?.email || '',
      phone: result?.phone || '',
      company: result?.company || '',
      address: result?.address || '',
    }
  } catch (error) {
    ElMessage.error(t('userProfile.loadUserFailed'))
  } finally {
    profileLoading.value = false
  }
}

const onSaveProfile = async () => {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    editLoading.value = true
    await updateProfile(editForm.value)
    ElMessage.success(t('userProfile.updateSuccess'))
    loadProfile()
  } catch (error) {
    ElMessage.error(t('userProfile.updateFailed'))
  } finally {
    editLoading.value = false
  }
}

const onChangePassword = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    pwdLoading.value = true
    await changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword,
    })
    ElMessage.success(t('userProfile.passwordChangeSuccess'))
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (error) {
    ElMessage.error(t('userProfile.passwordChangeFailed'))
  } finally {
    pwdLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <PageContainer :title="t('userProfile.title')" :description="t('userProfile.description')">
    <section class="profile-page">
      <el-card class="section-card" shadow="never">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('userProfile.basicInfo')" name="info">
            <div v-loading="profileLoading" style="padding: 20px 0">
              <el-form
                ref="editFormRef"
                :model="editForm"
                :rules="editRules"
                label-width="100px"
                class="profile-form"
              >
                <el-form-item :label="t('userProfile.username')">
                  <el-input :model-value="profile?.username" disabled />
                </el-form-item>
                <el-form-item :label="t('userProfile.role')">
                  <el-input :model-value="profile?.roleName || profile?.role" disabled />
                </el-form-item>
                <el-form-item :label="t('userProfile.realName')" prop="realName">
                  <el-input v-model="editForm.realName" :placeholder="t('userProfile.enterRealName')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.email')" prop="email">
                  <el-input v-model="editForm.email" :placeholder="t('userProfile.enterEmail')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.phone')">
                  <el-input v-model="editForm.phone" :placeholder="t('userProfile.enterPhone')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.company')">
                  <el-input v-model="editForm.company" :placeholder="t('userProfile.enterCompany')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.address')">
                  <el-input v-model="editForm.address" :placeholder="t('userProfile.enterAddress')" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="editLoading" @click="onSaveProfile">{{ t('userProfile.saveChanges') }}</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('userProfile.changePassword')" name="password">
            <div style="padding: 20px 0">
              <el-form
                ref="pwdFormRef"
                :model="pwdForm"
                :rules="pwdRules"
                label-width="100px"
                class="pwd-form"
              >
                <el-form-item :label="t('userProfile.currentPassword')" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password :placeholder="t('userProfile.enterCurrentPassword')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.newPassword')" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password :placeholder="t('userProfile.enterNewPassword')" />
                </el-form-item>
                <el-form-item :label="t('userProfile.confirmPassword')" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password :placeholder="t('userProfile.confirmNewPassword')" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="pwdLoading" @click="onChangePassword">{{ t('userProfile.changePassword') }}</el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </section>
  </PageContainer>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.profile-form {
  max-width: 600px;
}

.pwd-form {
  max-width: 500px;
}
</style>
