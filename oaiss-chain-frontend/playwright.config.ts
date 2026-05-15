import { defineConfig, devices } from '@playwright/test'

const isFlowMode = process.env.TEST_MODE === 'flow'

export default defineConfig({
  testDir: isFlowMode ? './tests/e2e/flows' : './tests/e2e/smoke',
  timeout: isFlowMode ? 30000 : 15000,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: isFlowMode ? 'on-first-retry' : 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: isFlowMode
    ? undefined
    : {
        command: 'npx vite --port 5173',
        url: 'http://localhost:5173',
        reuseExistingServer: !process.env.CI,
        timeout: 30000,
      },
})
