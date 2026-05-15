import { type Page, type Route } from '@playwright/test'

interface MockRouteConfig {
  method: string
  path: string
  data: unknown
}

export function mockApiResponse(
  page: Page,
  method: string,
  path: string,
  data: unknown,
): void {
  const urlPattern = `**/api/v1${path}`
  page.route(urlPattern, (route: Route) => {
    if (route.request().method() === method) {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 200, message: 'ok', data }),
      })
    } else {
      route.continue()
    }
  })
}

export function mockPaginatedList(
  page: Page,
  path: string,
  items: unknown[],
  total?: number,
): void {
  const totalElements = total ?? items.length
  const springPage = {
    content: items,
    totalElements,
    totalPages: Math.ceil(totalElements / 10),
    size: 10,
    number: 0,
    first: true,
    last: true,
    empty: items.length === 0,
  }
  mockApiResponse(page, 'GET', path, springPage)
}

export function setupSmokeMock(page: Page, role: string): void {
  const routes = SMOKE_ROUTES[role] || []
  for (const { method, path, data } of routes) {
    mockApiResponse(page, method, path, data)
  }
}

const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, size: 10, number: 0, first: true, last: true, empty: true }

const SMOKE_ROUTES: Record<string, MockRouteConfig[]> = {
  ENTERPRISE: [
    { method: 'GET', path: '/carbon/my-reports*', data: EMPTY_PAGE },
    { method: 'GET', path: '/trade/my-trades*', data: EMPTY_PAGE },
    { method: 'GET', path: '/auction/orders*', data: EMPTY_PAGE },
    { method: 'GET', path: '/auction/my-orders*', data: EMPTY_PAGE },
    { method: 'GET', path: '/auction/results*', data: EMPTY_PAGE },
    { method: 'GET', path: '/carbon-neutral/search*', data: EMPTY_PAGE },
    { method: 'GET', path: '/credit/my-score*', data: { score: 100, level: 'EXCELLENT', tradeRestricted: false, accountFrozen: false } },
    { method: 'GET', path: '/credit/history*', data: [] },
    { method: 'GET', path: '/carbon-coin/account*', data: { balance: 10000, totalRecharged: 10000, totalSpent: 0 } },
    { method: 'GET', path: '/carbon-coin/transactions*', data: EMPTY_PAGE },
    { method: 'GET', path: '/blockchain/status*', data: { connected: true, latestBlock: 100, peerCount: 5 } },
    { method: 'GET', path: '/blockchain/blocks/latest*', data: EMPTY_PAGE },
    { method: 'GET', path: '/blockchain/transactions*', data: EMPTY_PAGE },
    { method: 'GET', path: '/emission/ratings/*', data: [] },
    { method: 'GET', path: '/emission/rankings/*', data: [] },
    { method: 'GET', path: '/user/profile*', data: { id: 2, username: 'enterprise001', realName: '张三', userType: 1, email: 'enterprise001@example.com' } },
    { method: 'GET', path: '/signature/keypair*', data: null },
  ],
  ADMIN: [
    { method: 'GET', path: '/admin/users*', data: EMPTY_PAGE },
    { method: 'GET', path: '/carbon/reports*', data: EMPTY_PAGE },
    { method: 'GET', path: '/admin/statistics*', data: { totalUsers: 10, totalReports: 50, totalTrades: 20 } },
  ],
  REVIEWER: [
    { method: 'GET', path: '/carbon/reports*', data: EMPTY_PAGE },
  ],
  THIRD_PARTY: [
    { method: 'GET', path: '/third-party/carbon-reports*', data: EMPTY_PAGE },
    { method: 'GET', path: '/third-party/statistics*', data: { totalEnterprises: 5, totalReports: 30, totalEmission: 100000 } },
  ],
}
