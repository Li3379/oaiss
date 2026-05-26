import { test, expect, type Page, type Request } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { loginViaApi } from '../fixtures/auth'

const BASE_URL = process.env.BASE_URL || 'http://localhost:5173'
const OUT_DIR = path.join(process.cwd(), 'test-results', 'ad-hoc-probes')

type TableSnapshot = {
  rows: number
  totalText: string
  tableTipText: string
  firstTradeNo: string
  tradeNos: string[]
  emptyVisible: boolean
}

function ensureOutDir() {
  fs.mkdirSync(OUT_DIR, { recursive: true })
}

function writeJson(name: string, data: unknown) {
  fs.writeFileSync(path.join(OUT_DIR, name), JSON.stringify(data, null, 2), 'utf8')
}

async function settle(page: Page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 5000 }).catch(() => {})
  await page.waitForTimeout(800)
}

async function captureTradeRequests(page: Page, action: () => Promise<void>): Promise<string[]> {
  const urls: string[] = []
  const handler = (request: Request) => {
    const url = request.url()
    if (url.includes('/api/v1/trade/my-trades')) {
      urls.push(url)
    }
  }
  page.on('request', handler)
  try {
    await action()
    await page.waitForTimeout(1200)
    return urls
  } finally {
    page.off('request', handler)
  }
}

async function waitForTradeTableOrEmpty(page: Page) {
  await page.waitForFunction(() => {
    const rows = document.querySelectorAll('.el-table__body tbody tr').length
    const empty = Array.from(document.querySelectorAll('.el-table__empty-text'))
      .some((node) => (node.textContent || '').trim().length > 0)
    return rows > 0 || empty
  }, undefined, { timeout: 15000 })
}

async function readTotalText(page: Page): Promise<string> {
  const candidates = [
    '.el-pagination__total',
    '.el-pagination .is-first + *',
  ]
  for (const selector of candidates) {
    const locator = page.locator(selector).first()
    if (await locator.count()) {
      const text = (await locator.textContent())?.trim()
      if (text) return text
    }
  }
  return ''
}

async function snapshotTable(page: Page): Promise<TableSnapshot> {
  const rows = page.locator('.el-table__body tbody tr')
  const rowCount = await rows.count()
  const tradeNoCells = page.locator('.el-table__body tbody tr td:nth-child(2), .el-table__body tbody tr td:nth-child(3)')
  const texts = (await tradeNoCells.allTextContents())
    .map((item) => item.trim())
    .filter((item) => /^TR|^\d{6,}|^trade_/i.test(item) || item.includes('TRADE') || item.includes('-'))
  const tradeNos = [...new Set(texts)].slice(0, 8)
  return {
    rows: rowCount,
    totalText: await readTotalText(page),
    tableTipText: ((await page.locator('.table-tip').first().textContent().catch(() => '')) || '').trim(),
    firstTradeNo: tradeNos[0] || '',
    tradeNos,
    emptyVisible: await page.locator('.el-table__empty-text').first().isVisible().catch(() => false),
  }
}

async function fillFirstSearchInput(page: Page, value: string) {
  const input = page.locator('.search-form input:not([readonly])').first()
  await input.click()
  await input.fill(value)
}

async function clickSearch(page: Page) {
  await page.getByRole('button', { name: /搜索|查 询|查询|search/i }).first().click()
}

test('probe enterprise orders filters against real backend behavior', async ({ page }) => {
  test.setTimeout(45_000)
  ensureOutDir()
  await loginViaApi(page, 'enterprise001', 'admin123')

  await page.goto(`${BASE_URL}/enterprise/orders/manage`)
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const ordersBefore = await snapshotTable(page)

  const impossibleTradeNo = `NONEXISTENT_QA_${Date.now()}`
  const ordersTradeNoUrls = await captureTradeRequests(page, async () => {
    await fillFirstSearchInput(page, impossibleTradeNo)
    await clickSearch(page)
  })
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const ordersAfterTradeNo = await snapshotTable(page)

  await page.getByRole('button', { name: /重置|reset/i }).first().click()
  await settle(page)
  await waitForTradeTableOrEmpty(page)

  const dateInput = page.locator('.search-form .el-range-input').first()
  await dateInput.click()
  await page.keyboard.press('Control+a')
  await page.keyboard.type('2099-01-01')
  const endDateInput = page.locator('.search-form .el-range-input').nth(1)
  await endDateInput.click()
  await page.keyboard.press('Control+a')
  await page.keyboard.type('2099-01-02')
  const ordersDateUrls = await captureTradeRequests(page, async () => {
    await clickSearch(page)
  })
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const ordersAfterDate = await snapshotTable(page)
  await page.screenshot({
    path: path.join(OUT_DIR, 'orders-manage-impossible-tradeno-filter-2026-05-24.png'),
    fullPage: true,
  })
  const ordersProbe = {
    probe: 'orders-manage',
    before: ordersBefore,
    impossibleTradeNo,
    afterTradeNo: ordersAfterTradeNo,
    tradeNoUrls: ordersTradeNoUrls,
    afterDateRange: ordersAfterDate,
    dateRangeUrls: ordersDateUrls,
  }
  writeJson('orders-manage-filter-probe-2026-05-24.json', ordersProbe)
  console.log(JSON.stringify(ordersProbe, null, 2))

  expect(ordersBefore.rows).toBeGreaterThan(0)
  expect(ordersBefore.tableTipText).toContain(String(ordersBefore.rows))
  expect(ordersAfterTradeNo.rows).toBe(0)
  expect(ordersAfterTradeNo.tableTipText).toContain('0')
  expect(ordersAfterDate.rows).toBe(0)
  expect(ordersTradeNoUrls.some((url) => url.includes('tradeNo='))).toBeFalsy()
  expect(ordersDateUrls.some((url) => url.includes('startDate=') || url.includes('endDate='))).toBeFalsy()
})

test('probe trading p2p filters against real backend behavior', async ({ page }) => {
  test.setTimeout(45_000)
  ensureOutDir()
  await loginViaApi(page, 'enterprise001', 'admin123')

  await page.goto(`${BASE_URL}/enterprise/trading/p2p`)
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const p2pBefore = await snapshotTable(page)

  const impossibleKeyword = `NO_MATCH_NAME_${Date.now()}`
  const p2pKeywordUrls = await captureTradeRequests(page, async () => {
    await fillFirstSearchInput(page, impossibleKeyword)
    await clickSearch(page)
  })
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const p2pAfterKeyword = await snapshotTable(page)
  await page.screenshot({
    path: path.join(OUT_DIR, 'p2p-impossible-keyword-filter-2026-05-24.png'),
    fullPage: true,
  })

  const p2pInputs = page.locator('.search-form input:not([readonly])')
  const impossibleOrderNo = `NO_MATCH_ORDER_${Date.now()}`
  if ((await p2pInputs.count()) >= 2) {
    await p2pInputs.nth(1).fill(impossibleOrderNo)
  }
  const p2pOrderNoUrls = await captureTradeRequests(page, async () => {
    await clickSearch(page)
  })
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const p2pAfterOrderNo = await snapshotTable(page)

  const identitySelect = page.locator('.search-form .el-select').first()
  if (await identitySelect.count()) {
    await identitySelect.click()
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()
  }
  const p2pIdentityUrls = await captureTradeRequests(page, async () => {
    await clickSearch(page)
  })
  await settle(page)
  await waitForTradeTableOrEmpty(page)
  const p2pAfterIdentity = await snapshotTable(page)
  await page.screenshot({
    path: path.join(OUT_DIR, 'p2p-identity-filter-no-effect-2026-05-24.png'),
    fullPage: true,
  })

  const p2pProbe = {
    probe: 'trading-p2p',
    before: p2pBefore,
    impossibleKeyword,
    afterKeyword: p2pAfterKeyword,
    keywordUrls: p2pKeywordUrls,
    impossibleOrderNo,
    afterOrderNo: p2pAfterOrderNo,
    orderNoUrls: p2pOrderNoUrls,
    afterIdentity: p2pAfterIdentity,
    identityUrls: p2pIdentityUrls,
  }
  writeJson('trading-p2p-filter-probe-2026-05-24.json', p2pProbe)
  console.log(JSON.stringify(p2pProbe, null, 2))

  expect(p2pBefore.rows).toBeGreaterThan(0)
  expect(p2pAfterKeyword.rows).toBe(0)
  expect(p2pAfterOrderNo.rows).toBe(0)
  expect(p2pKeywordUrls.some((url) => url.includes('keyword='))).toBeFalsy()
  expect(p2pOrderNoUrls.some((url) => url.includes('tradeNo='))).toBeFalsy()
})
