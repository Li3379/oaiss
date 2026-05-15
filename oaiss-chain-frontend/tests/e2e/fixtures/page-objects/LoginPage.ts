import { type Page, expect } from '@playwright/test'

export class LoginPage {
  constructor(private page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto('/login')
  }

  async expectLoaded(): Promise<void> {
    await expect(this.page.getByPlaceholder('请输入账号')).toBeVisible()
    await expect(this.page.getByPlaceholder('请输入密码')).toBeVisible()
    await expect(this.page.getByRole('button', { name: '登录' })).toBeVisible()
  }

  async login(username: string, password: string): Promise<void> {
    await this.page.getByPlaceholder('请输入账号').fill(username)
    await this.page.getByPlaceholder('请输入密码').fill(password)
    await this.page.getByRole('button', { name: '登录' }).click()
  }
}
