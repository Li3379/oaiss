import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

const elementPlusImportStyle = process.env.VITEST ? false : 'css'

const elementPlusFormComponents = new Set([
  'autocomplete',
  'button',
  'button-group',
  'cascader',
  'checkbox',
  'checkbox-button',
  'checkbox-group',
  'date-picker',
  'form',
  'form-item',
  'input',
  'input-number',
  'option',
  'option-group',
  'radio',
  'radio-button',
  'radio-group',
  'select',
  'switch',
  'time-picker',
  'time-select',
  'upload',
])

const elementPlusDataComponents = new Set([
  'descriptions',
  'empty',
  'pagination',
  'progress',
  'table',
  'table-column',
  'table-v2',
  'tag',
])

const elementPlusShellComponents = new Set([
  'aside',
  'avatar',
  'breadcrumb',
  'breadcrumb-item',
  'card',
  'col',
  'container',
  'dialog',
  'divider',
  'drawer',
  'dropdown',
  'dropdown-item',
  'dropdown-menu',
  'footer',
  'header',
  'icon',
  'loading',
  'main',
  'menu',
  'message',
  'message-box',
  'overlay',
  'popover',
  'row',
  'scrollbar',
  'space',
  'sub-menu',
  'tab-pane',
  'tabs',
  'tooltip',
])

function getElementPlusChunkName(normalizedId) {
  const componentMatch = normalizedId.match(/element-plus\/es\/components\/([^/]+)\//)
  const styleMatch = normalizedId.match(/element-plus\/theme-chalk\/el-([^/.]+)/)
  const componentName = componentMatch?.[1] || styleMatch?.[1]

  if (!componentName) return 'element-plus-base-vendor'
  if (elementPlusFormComponents.has(componentName)) return 'element-plus-form-vendor'
  if (elementPlusDataComponents.has(componentName)) return 'element-plus-data-vendor'
  if (elementPlusShellComponents.has(componentName)) return 'element-plus-shell-vendor'

  return 'element-plus-base-vendor'
}

export default defineConfig({
  plugins: [
    vue(),
    Components({
      dts: false,
      resolvers: [
        ElementPlusResolver({
          importStyle: elementPlusImportStyle,
          directives: true,
        }),
      ],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  build: {
    rolldownOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replaceAll('\\', '/')

          if (!id.includes('node_modules')) return

          if (normalizedId.includes('/zrender/')) return 'zrender-vendor'

          if (normalizedId.includes('/echarts/lib/chart/')) return 'echarts-chart-vendor'
          if (normalizedId.includes('/echarts/lib/component/')) return 'echarts-component-vendor'
          if (normalizedId.includes('/echarts/')) return 'echarts-core-vendor'

          if (
            normalizedId.includes('/element-plus/') ||
            normalizedId.includes('/@element-plus/')
          ) {
            return getElementPlusChunkName(normalizedId)
          }

          if (normalizedId.includes('/@floating-ui/')) return 'element-plus-shell-vendor'
          if (normalizedId.includes('/lodash-es/') || normalizedId.includes('/dayjs/')) return 'element-plus-base-vendor'

          if (normalizedId.includes('/vue/') || id.includes('vue-router') || id.includes('vue-i18n') || id.includes('pinia')) {
            return 'vue-vendor'
          }

          if (id.includes('axios')) return 'network-vendor'

          return 'vendor'
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    exclude: ['tests/e2e/**', 'node_modules/**'],
  },
})
