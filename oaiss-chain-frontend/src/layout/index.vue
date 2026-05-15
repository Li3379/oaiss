<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Expand, Fold, UserFilled, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { logout as apiLogout } from '../api/auth'
import { useAppStore } from '../store'
import { MENU_BY_ROLE } from '../config/menu'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const { t } = useI18n()

const MOBILE_BREAKPOINT = 768
let mobileQuery = null

const onMobileChange = (e) => {
  if (e.matches) {
    appStore.sidebarCollapsed = true
  }
}

onMounted(() => {
  mobileQuery = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT}px)`)
  mobileQuery.addEventListener('change', onMobileChange)
  if (mobileQuery.matches) appStore.sidebarCollapsed = true
})

onUnmounted(() => {
  mobileQuery?.removeEventListener('change', onMobileChange)
})

const menuTree = computed(() => MENU_BY_ROLE[appStore.role] || [])

const activeMenu = computed(() => route.path)

const defaultOpeneds = computed(() => {
  const openList = []

  menuTree.value.forEach((group) => {
    group.children.forEach((secondLevel) => {
      const matchCurrent = secondLevel.children.some((leaf) => leaf.path === route.path)

      if (matchCurrent) {
        openList.push(group.label, `${group.label}-${secondLevel.label}`)
      }
    })
  })

  return openList
})

const onSelectMenu = (index) => {
  if (index && index.startsWith('/') && index !== route.path) {
    router.push(index)
  }
}

const onLogout = async () => {
  try {
    await apiLogout()
  } catch {
    // 退出登录接口失败不影响本地退出
  }
  appStore.logout()
  ElMessage.success(t('layout.loggedOut'))
  router.replace('/login')
}

const isDark = ref(document.documentElement.classList.contains('dark'))

const toggleDark = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

onMounted(() => {
  const saved = localStorage.getItem('theme')
  if (saved === 'dark' || (!saved && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark')
    isDark.value = true
  }
})
</script>

<template>
  <el-container class="app-shell">
    <el-aside class="side-panel" :width="appStore.sidebarCollapsed ? '70px' : '260px'">
      <div class="brand-area">
        <div class="logo-dot" />
        <span v-show="!appStore.sidebarCollapsed" class="brand-title">{{ t('layout.brand') }}</span>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu
          class="side-menu"
          :default-active="activeMenu"
          :default-openeds="defaultOpeneds"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          unique-opened
          @select="onSelectMenu"
        >
          <el-sub-menu v-for="group in menuTree" :key="group.label" :index="group.label">
            <template #title>
              <span>{{ t(group.label) }}</span>
            </template>

            <el-sub-menu
              v-for="secondLevel in group.children"
              :key="`${group.label}-${secondLevel.label}`"
              :index="`${group.label}-${secondLevel.label}`"
            >
              <template #title>
                <span>{{ t(secondLevel.label) }}</span>
              </template>

              <el-menu-item v-for="leaf in secondLevel.children" :key="leaf.path" :index="leaf.path">
                {{ t(leaf.label) }}
              </el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="top-header">
        <div class="header-left">
          <el-button class="collapse-btn" circle plain @click="appStore.toggleSidebar">
            <el-icon>
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>

          <div class="system-meta">
            <div class="system-title">{{ t(appStore.systemTitle) }}</div>
            <div class="system-subtitle">{{ t('layout.systemDesc') }}</div>
          </div>
        </div>

        <div class="header-right">
          <el-tag class="status-tag" effect="dark">{{ t('layout.monitorMode') }}</el-tag>
          <el-avatar :size="36" class="avatar-icon">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
          <div class="user-block">
            <div class="user-name">{{ appStore.username || t('layout.systemUser') }}</div>
            <div class="user-role">{{ t(appStore.roleLabel) }}</div>
          </div>
          <el-button class="theme-toggle" text @click="toggleDark">
            <el-icon><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
          </el-button>
          <LanguageSwitcher />
          <el-button class="logout-btn" text @click="onLogout">{{ t('layout.logout') }}</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  height: 100%;
}

.side-panel {
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, #0d3236 0%, #125850 62%, #198369 100%);
}

.brand-area {
  height: 66px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #eef7f6;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(130deg, #34d5bf, #90f0b1);
  box-shadow: 0 0 0 6px rgba(73, 193, 153, 0.15);
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.6px;
}

.menu-scrollbar {
  height: calc(100vh - 66px);
}

.side-menu {
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu) {
  background: transparent;
  border-right: none;
}

.side-menu :deep(.el-sub-menu__title),
.side-menu :deep(.el-menu-item) {
  color: rgba(235, 248, 247, 0.87);
}

.side-menu :deep(.el-sub-menu__title:hover),
.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(38, 204, 162, 0.34);
}

.top-header {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: linear-gradient(90deg, #17363a 0%, #1e4d49 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.collapse-btn {
  border-color: rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
  color: #ecf7f4;
  flex-shrink: 0;
}

.collapse-btn:hover {
  border-color: rgba(255, 255, 255, 0.26);
  background: rgba(255, 255, 255, 0.14);
  color: #ecf7f4;
}

.system-meta {
  color: #f0fbf6;
}

.system-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.system-subtitle {
  font-size: 12px;
  opacity: 0.78;
  margin-top: 3px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logout-btn {
  color: #e8faf2;
  padding: 0 6px;
}

.logout-btn:hover {
  color: #ffffff;
}

.theme-toggle {
  color: #e8faf2;
  padding: 0 6px;
  font-size: 18px;
}

.theme-toggle:hover {
  color: #ffffff;
}

.status-tag {
  background: linear-gradient(120deg, #18a99a, #42c977);
  border: none;
  color: #ffffff;
}

.avatar-icon {
  color: #1f4748;
  background: linear-gradient(120deg, #d8fbef 0%, #e8f4fb 100%);
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: #f0fbf6;
}

.user-role {
  font-size: 12px;
  color: rgba(240, 251, 246, 0.75);
  margin-top: 2px;
}

.main-content {
  min-height: calc(100vh - 70px);
  max-width: 1600px;
  margin: 0 auto;
}

@media (max-width: 768px) {
  .side-panel {
    position: fixed;
    z-index: 100;
    height: 100vh;
  }

  .top-header {
    height: auto;
    padding: 8px 12px;
    flex-wrap: wrap;
    gap: 8px;
  }

  .system-subtitle,
  .status-tag {
    display: none;
  }

  .user-block {
    display: none;
  }

  .header-right {
    gap: 6px;
  }

  .main-content {
    padding: 12px !important;
  }
}
</style>
