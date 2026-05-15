import { type Page, expect } from '@playwright/test'

export class Layout {
  constructor(private page: Page) {}

  async expectSidebar(expectedItems: string[]): Promise<void> {
    for (const item of expectedItems) {
      await expect(this.page.getByRole('menuitem', { name: item })).toBeVisible()
    }
  }

  async expectUserInfo(username: string, roleLabel: string): Promise<void> {
    await expect(this.page.getByText(username)).toBeVisible()
    await expect(this.page.locator('header').getByText(roleLabel)).toBeVisible()
  }

  async expectBreadcrumb(...segments: string[]): Promise<void> {
    const nav = this.page.getByRole('navigation', { name: 'Breadcrumb' })
    for (const segment of segments) {
      await expect(nav).toContainText(segment)
    }
  }
}
