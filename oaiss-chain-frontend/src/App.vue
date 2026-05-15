<template>
  <ElConfigProvider :z-index="3000">
    <ErrorBoundary>
      <router-view />
    </ErrorBoundary>
  </ElConfigProvider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import ErrorBoundary from './components/ErrorBoundary.vue'
</script>
