import { type Page } from '@playwright/test'
import { AuthMonitor } from './auth-monitor'

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080/api/v1'

export const MOCK_TOKENS: Record<string, string> = {
  ENTERPRISE:
    'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlbnRlcnByaXNlMDAxIiwidXNlcklkIjoyLCJlbnRlcnByaXNlSWQiOjEsInJvbGVzIjpbIkVOVEVSUFJJU0UiXSwidXNlclR5cGUiOjEsImV4cCI6OTk5OTk5OTk5OX0.mock',
  ADMIN:
    'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwiZW50ZXJwcmlzZUlkIjoxLCJyb2xlcyI6WyJBRE1JTiJdLCJ1c2VyVHlwZSI6NCwiZXhwIjo5OTk5OTk5OTk5fQ.mock',
  REVIEWER:
    'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyZXZpZXdlciIsInVzZXJJZCI6NCwiZW50ZXJwcmlzZUlkIjoxLCJyb2xlcyI6WyJSRVZJRVdFUiJdLCJ1c2VyVHlwZSI6MiwiZXhwIjo5OTk5OTk5OTk5fQ.mock',
  THIRD_PARTY:
    'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aGlyZHBhcnR5IiwidXNlcklkIjo1LCJlbnRlcnByaXNlSWQiOjEsInJvbGVzIjpbIlRISVJEX1BBUlRZIl0sInVzZXJUeXBlIjozLCJleHAiOjk5OTk5OTk5OTl9.mock',
}

export const TEST_USERS: Record<string, { username: string; password: string; role: string }> = {
  admin: { username: 'admin', password: 'admin123', role: 'ADMIN' },
  enterprise: { username: 'enterprise001', password: 'admin123', role: 'ENTERPRISE' },
  reviewer: { username: 'reviewer001', password: 'admin123', role: 'REVIEWER' },
  thirdParty: { username: 'thirdparty001', password: 'admin123', role: 'THIRD_PARTY' },
}

export function getToken(role: string): string {
  return MOCK_TOKENS[role] || MOCK_TOKENS.ENTERPRISE
}

export async function loginViaToken(page: Page, role: string): Promise<void> {
  const token = getToken(role)
  await page.addInitScript(
    (args) => {
      sessionStorage.setItem('access_token', args.token)
      sessionStorage.setItem('user_role', args.roleLabel)
    },
    { token, roleLabel: role },
  )
}

export async function loginViaApi(
  page: Page,
  username: string,
  password: string,
): Promise<string> {
  const response = await page.request.post(`${API_BASE}/auth/login`, {
    data: { username, password },
  })
  const body = await response.json()
  const { accessToken, refreshToken } = body.data

  // Navigate to the app first so localStorage is accessible
  await page.goto(BASE_URL)
  await page.evaluate(
    ({ accessToken, refreshToken }) => {
      // Use localStorage (consistent with default rememberMe=true)
      localStorage.setItem('access_token', accessToken)
      localStorage.setItem('refresh_token', refreshToken)
      localStorage.setItem('remember_me', 'true')

      // Parse JWT to set token_expiry
      try {
        const parts = accessToken.split('.')
        if (parts.length === 3) {
          const payload = JSON.parse(atob(parts[1]))
          if (payload.exp) {
            localStorage.setItem('token_expiry', String(payload.exp * 1000))
          }
        }
      } catch (e) {
        console.error('Failed to parse JWT:', e)
      }
    },
    { accessToken, refreshToken },
  )
  return accessToken
}

export function buildStorageState(role = 'ENTERPRISE') {
  return {
    origins: [
      {
        origin: 'http://localhost:5173',
        localStorage: [],
      },
    ],
  }
}

/**
 * Login via API and return both token and an attached AuthMonitor.
 *
 * Usage:
 * ```ts
 * const { token, monitor } = await loginWithMonitor(page, 'enterprise')
 * // ... run test operations ...
 * const report = monitor.stop()
 * ```
 */
export async function loginWithMonitor(
  page: Page,
  role: keyof typeof TEST_USERS,
): Promise<{ token: string; monitor: AuthMonitor }> {
  const user = TEST_USERS[role]
  const monitor = new AuthMonitor(page)
  monitor.start()

  const token = await loginViaApi(page, user.username, user.password)
  return { token, monitor }
}
