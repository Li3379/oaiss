<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useAppStore } from '../store'
import { ROLE_HOME } from '../config/menu'
import { login } from '../api/auth'
import { generateCaptcha } from '../api/captcha'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const { t } = useI18n()

const LOGIN_FORM_STORAGE_KEY = 'carbon-admin-login-form'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  captchaInput: '',
  rememberPassword: true,
})

const captchaKey = ref('')
const captchaImage = ref('')

const formRules = computed(() => ({
  account: [{ required: true, message: t('login.enterAccount'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.enterPassword'), trigger: 'blur' }],
  captchaInput: [{ required: true, message: t('login.enterCaptcha'), trigger: 'blur' }],
}))

const refreshCaptcha = async () => {
  try {
    const data = await generateCaptcha()
    captchaKey.value = data.captchaKey
    captchaImage.value = data.captchaImage
  } catch {
    ElMessage.error(t('login.captchaLoadFailed'))
  }
}

const restoreForm = () => {
  try {
    const raw = localStorage.getItem(LOGIN_FORM_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    form.account = parsed.account || ''
    form.rememberPassword = Boolean(parsed.rememberPassword)
  } catch {
    localStorage.removeItem(LOGIN_FORM_STORAGE_KEY)
  }
}

onMounted(() => {
  restoreForm()
  refreshCaptcha()
})

const onSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning(t('login.incompleteForm'))
    return
  }

  loading.value = true
  try {
    const data = await login({
      username: form.account,
      password: form.password,
      captchaKey: captchaKey.value,
      captcha: form.captchaInput.trim(),
    })

    appStore.login({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
    })

    if (form.rememberPassword) {
      localStorage.setItem(LOGIN_FORM_STORAGE_KEY, JSON.stringify({
        account: form.account,
        rememberPassword: true,
      }))
    } else {
      localStorage.removeItem(LOGIN_FORM_STORAGE_KEY)
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const target = redirect || appStore.homePath

    ElMessage.success(t('login.loginSuccess'))
    router.replace(target)
  } catch {
    refreshCaptcha()
    form.captchaInput = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class=login-page>
    <div class=login-bg-layer />

    <el-card class=login-card shadow=never>
      <div class=login-header>
        <div class=logo-dot />
        <div>
          <h1>{{ t('login.title') }}</h1>
          <p>{{ t('login.subtitle') }}</p>
        </div>
      </div>

      <el-form ref=formRef :model=form :rules=formRules label-position=top @submit.prevent>
        <el-form-item :label="t('login.account')" prop=account>
          <el-input v-model=form.account :placeholder="t('login.enterAccount')" clearable />
        </el-form-item>

        <el-form-item :label="t('login.password')" prop=password>
          <el-input
            v-model=form.password
            type=password
            show-password
            :placeholder="t('login.enterPassword')"
          />
        </el-form-item>

        <el-form-item :label="t('login.captcha')" prop=captchaInput>
          <div class=captcha-row>
            <el-input v-model=form.captchaInput :placeholder="t('login.enterCaptcha')" />
            <img
              v-if=captchaImage
              :src=captchaImage
              class=captcha-image
              :alt="t('login.captcha')"
              @click=refreshCaptcha
            />
            <div v-else class=captcha-image captcha-placeholder @click=refreshCaptcha>
              {{ t('login.captchaPlaceholder') }}
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model=form.rememberPassword>{{ t('login.rememberMe') }}</el-checkbox>
        </el-form-item>

        <el-button class=submit-btn type=primary size=large :loading=loading @click=onSubmit>
          {{ t('login.login') }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 20px;
}

.login-bg-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 22%, rgba(20, 167, 154, 0.3), transparent 42%),
    radial-gradient(circle at 82% 78%, rgba(69, 190, 117, 0.28), transparent 40%),
    linear-gradient(180deg, #163237 0%, #1f4a4d 100%);
}

.login-card {
  width: 100%;
  max-width: 520px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 250, 249, 0.96));
  box-shadow: 0 24px 50px rgba(9, 34, 37, 0.24);
  position: relative;
  z-index: 2;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.logo-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: linear-gradient(130deg, #23baa8, #6adf85);
  box-shadow: 0 0 0 6px rgba(35, 186, 168, 0.12);
}

.login-header h1 {
  margin: 0;
  font-size: 24px;
  color: #1a2c30;
}

.login-header p {
  margin: 8px 0 0;
  color: #607579;
  font-size: 14px;
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 110px;
  gap: 10px;
}

.captcha-image {
  width: 110px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #c3dfd7;
  cursor: pointer;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  border: none;
  background: linear-gradient(120deg, #18a99a 0%, #42c977 100%);
}

@media (max-width: 600px) {
  .login-card {
    max-width: 100%;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }
}
</style>
