<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { LocaleType } from '../i18n'

const { locale } = useI18n()

const languages: { label: string; value: LocaleType }[] = [
  { label: '中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' },
]

const switchLocale = (lang: LocaleType) => {
  locale.value = lang
  localStorage.setItem('locale', lang)
}
</script>

<template>
  <el-dropdown @command="switchLocale">
    <span class="language-trigger">
      {{ locale === 'zh-CN' ? '中文' : 'English' }}
      <el-icon class="el-icon--right"><arrow-down /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="lang in languages"
          :key="lang.value"
          :command="lang.value"
          :disabled="locale === lang.value"
        >
          {{ lang.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.language-trigger {
  cursor: pointer;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.language-trigger:hover {
  color: var(--el-color-primary);
}
</style>
