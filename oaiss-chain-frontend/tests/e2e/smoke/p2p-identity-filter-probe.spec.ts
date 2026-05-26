import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { loginViaToken } from '../fixtures/auth'

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173'
const OUT_DIR = path.join(process.cwd(), 'test-results', 'ad-hoc-probes')

function ensureOutDir() {
  fs.mkdirSync(OUT_DIR, { recursive: true })
}

function writeJson(name: string, data: unknown) {
  fs.writeFileSync(path.join(OUT_DIR, name), JSON.stringify(data, null, 2), 'utf8')
}

async function readRowCount(page: import('@playwright/test').Page) {
  return page.locator('table tbody tr').count()
}

async function applyIdentityFilter(
  page: import('@playwright/test').Page,
  optionText: RegExp,
) {
  await page.goto(`${BASE_URL}/enterprise/trading/p2p`)
  await page.waitForLoadState('domcontentloaded')
  await page.waitForTimeout(1000)

  const select = page.locator('.search-form .el-select').first()
  await select.click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: optionText }).first().click()
  await page.getByRole('button', { name: /Search|查询|搜索/ }).first().click()
  await page.waitForTimeout(600)

  const rowCount = await readRowCount(page)
  const bodyText = (await page.locator('.el-table__body').first().textContent()) || ''
  return { rowCount, bodyText }
}

test('probe p2p identity filter works on current user role', async ({ page }) => {
  ensureOutDir()
  await loginViaToken(page, 'ENTERPRISE')

  await page.route('**/api/v1/trade/my-trades**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        message: 'ok',
        data: {
          content: [
            {
              id: 801,
              tradeNo: 'P2P-BUYER',
              buyerId: 2,
              buyerName: 'Current User',
              sellerId: 9,
              sellerName: 'Seller A',
              quantity: 8,
              unitPrice: 70,
              totalAmount: 560,
              status: 0,
              statusText: '待处理',
              createdAt: '2026-05-24T09:00:00',
            },
            {
              id: 802,
              tradeNo: 'P2P-SELLER',
              buyerId: 10,
              buyerName: 'Buyer B',
              sellerId: 2,
              sellerName: 'Current User',
              quantity: 5,
              unitPrice: 90,
              totalAmount: 450,
              status: 0,
              statusText: '待处理',
              createdAt: '2026-05-24T10:00:00',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          size: 10,
          number: 0,
          first: true,
          last: true,
          empty: false,
        },
      }),
    })
  })

  await page.goto(`${BASE_URL}/enterprise/trading/p2p`)
  await page.waitForLoadState('domcontentloaded')
  await page.waitForTimeout(1000)

  const beforeRows = await readRowCount(page)
  const bodyTable = page.locator('.el-table__body').first()
  await expect(bodyTable).toContainText('P2P-BUYER')
  await expect(bodyTable).toContainText('P2P-SELLER')

  const buyerResult = await applyIdentityFilter(page, /Buyer|买方/)
  const sellerResult = await applyIdentityFilter(page, /Seller|卖方/)

  await page.screenshot({
    path: path.join(OUT_DIR, 'p2p-identity-filter-fixed-2026-05-24.png'),
    fullPage: true,
  })

  writeJson('p2p-identity-filter-probe-fixed-2026-05-24.json', {
    beforeRows,
    buyerRows: buyerResult.rowCount,
    sellerRows: sellerResult.rowCount,
    buyerHasBuyerTrade: buyerResult.bodyText.includes('P2P-BUYER'),
    buyerHasSellerTrade: buyerResult.bodyText.includes('P2P-SELLER'),
    sellerHasBuyerTrade: sellerResult.bodyText.includes('P2P-BUYER'),
    sellerHasSellerTrade: sellerResult.bodyText.includes('P2P-SELLER'),
  })
})
