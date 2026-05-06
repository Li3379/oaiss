export const MOCK_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInVzZXJJZCI6MSwiZW50ZXJwcmlzZUlkIjoxLCJyb2xlcyI6WyJFTlRFUlBSSVNFIiwiQURNSU4iXSwidXNlclR5cGUiOjEsImV4cCI6OTk5OTk5OTk5OX0.mock'

export const MOCK_ADMIN_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MiwiZW50ZXJwcmlzZUlkIjoxLCJyb2xlcyI6WyJBRE1JTiJdLCJ1c2VyVHlwZSI6NCwiZXhwIjo5OTk5OTk5OTk5fQ.mock'

export const MOCK_THIRD_PARTY_TOKEN =
  'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aGlyZHBhcnR5IiwidXNlcklkIjo0LCJlbnRlcnByaXNlSWQiOjEsInJvbGVzIjpbIlRISVJEX1BBUlRZIl0sInVzZXJUeXBlIjo1LCJleHAiOjk5OTk5OTk5OTl9.mock'

export function getToken(role) {
  if (role === 'admin') return MOCK_ADMIN_TOKEN
  if (role === 'thirdParty') return MOCK_THIRD_PARTY_TOKEN
  return MOCK_TOKEN
}

export function buildStorageState(role = 'enterprise') {
  return {
    origins: [
      {
        origin: 'http://localhost:5173',
        localStorage: [],
      },
    ],
  }
}

export async function loginViaToken(page, role = 'enterprise') {
  await page.goto('/login')
  await page.evaluate((token) => {
    sessionStorage.setItem('access_token', token)
  }, getToken(role))
}

export function setupApiMock(page, data = { content: [], totalElements: 0 }) {
  // Check if data is a route map (keys contain HTTP methods)
  const isRouteMap = Object.keys(data).some(k => /^(GET|POST|PUT|DELETE|PATCH)\s/.test(k))

  if (isRouteMap) {
    // Per-route mocking
    for (const [routeKey, responseData] of Object.entries(data)) {
      const [method, path] = routeKey.split(/\s+/)
      const urlPattern = `**/api/v1${path}`
      page.route(urlPattern, (route) => {
        if (route.request().method() === method) {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ code: 200, message: 'ok', data: responseData }),
          })
        } else {
          route.continue()
        }
      })
    }
  } else {
    // Original behavior: single data for all routes
    page.route('**/api/v1/**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data }),
      }),
    )
  }
}
