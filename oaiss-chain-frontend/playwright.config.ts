import { defineConfig, devices } from '@playwright/test'

const isFlowMode = process.env.TEST_MODE === 'flow'
const isV11Mode = process.env.TEST_MODE === 'v1.1'

export default defineConfig({
  testDir: isV11Mode
    ? './tests/e2e/v1.1'
    : isFlowMode
      ? './tests/e2e/flows'
      : './tests/e2e/smoke',
  timeout: isV11Mode ? 45000 : isFlowMode ? 30000 : 15000,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [
    [
      'html',
      {
        open: 'never',
        outputFolder: isV11Mode ? 'playwright-report/v1.1' : 'playwright-report',
      },
    ],
    ['list'],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: isFlowMode || isV11Mode ? 'on-first-retry' : 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer:
    isFlowMode || isV11Mode
      ? undefined
      : {
          command: 'npx vite --port 5173',
          url: 'http://localhost:5173',
          reuseExistingServer: !process.env.CI,
          timeout: 30000,
        },
})
