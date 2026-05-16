import { request, type APIRequestContext } from '@playwright/test'

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080/api/v1'
const ML_SERVICE_BASE = process.env.ML_SERVICE_URL || 'http://localhost:8001'
const FABRIC_ENDPOINT = process.env.FABRIC_ENDPOINT || 'http://localhost:7051'

let sharedContext: APIRequestContext | null = null

async function getRequestContext(): Promise<APIRequestContext> {
  if (!sharedContext) {
    sharedContext = await request.newContext({
      baseURL: API_BASE,
      timeout: 5000,
    })
  }
  return sharedContext
}

/**
 * Check if ML service (Python FastAPI on port 8001) is available.
 * Returns true if the service responds to a GET /health request.
 */
export async function isMlServiceAvailable(): Promise<boolean> {
  try {
    const ctx = await request.newContext({ timeout: 5000 })
    const response = await ctx.get(`${ML_SERVICE_BASE}/health`)
    await ctx.dispose()
    return response.ok() || response.status() === 404
  } catch {
    return false
  }
}

/**
 * Check if Fabric network is available via the backend blockchain status endpoint.
 * Returns true if the backend reports a connected blockchain.
 */
export async function isFabricAvailable(): Promise<boolean> {
  try {
    const ctx = await getRequestContext()
    const response = await ctx.get('/blockchain/status', {
      timeout: 5000,
    })
    if (!response.ok()) return false
    const body = await response.json()
    return body?.data?.connected === true
  } catch {
    return false
  }
}

/**
 * Skip test if service is unavailable.
 * Usage with Playwright test.skip:
 *   test.skip(await !isMlServiceAvailable(), 'ML service not available')
 *
 * Or use the returned object for conditional logic:
 *   const skip = skipIfServiceUnavailable(!available, 'reason')
 *   test.skip(skip.condition, skip.reason)
 */
export function skipIfServiceUnavailable(
  condition: boolean,
  reason: string,
): { condition: boolean; reason: string } {
  return { condition, reason }
}
