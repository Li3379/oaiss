import { type Page, expect } from '@playwright/test'

export class CarbonUploadPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/enterprise/carbon/upload')
    await this.page.waitForLoadState('networkidle')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page).toHaveURL(/\/enterprise\/carbon\/upload/)
    await expect(this.page.getByRole('button', { name: '创建项目' })).toBeVisible()
  }

  async expectTableHeaders(): Promise<void> {
    const headers = ['报告编号', '报告标题', '核算周期', '总排放量(tCO2e)', '状态', '审核人', '创建时间', '操作']
    for (const h of headers) {
      await expect(this.page.getByRole('columnheader', { name: h })).toBeVisible()
    }
  }

  async expectEmptyState(): Promise<void> {
    await expect(this.page.getByText(/暂无碳核算报告/)).toBeVisible()
  }

  async expectSearchForm(): Promise<void> {
    await expect(this.page.getByPlaceholder('请输入报告标题')).toBeVisible()
    await expect(this.page.getByPlaceholder('请输入核算周期')).toBeVisible()
    await expect(this.page.getByRole('button', { name: '查询' })).toBeVisible()
  }
}
