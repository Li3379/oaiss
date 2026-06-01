---
status: draft
priority: low
created: 2026-05-10
author: claude
related:
  - Phase 2 UAT findings
  - User experience
  - Authentication
---

# SPEC: Token存储策略优化

## 问题描述

当前Token存储使用`sessionStorage`：
- 标签页关闭后Token丢失
- 刷新页面需要重新登录
- 多标签页不共享登录状态

## 当前状态

```typescript
// oaiss-chain-frontend/src/utils/auth.ts
const TOKEN_KEY = 'access_token'

export function setToken(access: string, refresh?: string): void {
  sessionStorage.setItem(TOKEN_KEY, access)  // 使用sessionStorage
  if (refresh) {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  }
}
```

## 解决方案分析

### 方案A: 使用localStorage（推荐）

**优点**：
- 持久化存储
- 刷新页面保持登录
- 多标签页共享状态

**缺点**：
- 安全性略低（XSS风险）
- 需要处理Token过期

**实施**：
```typescript
// 改用localStorage
localStorage.setItem(TOKEN_KEY, access)
```

### 方案B: localStorage + 过期时间

**优点**：
- 持久化
- 自动过期清理

**实施**：
```typescript
interface TokenData {
  token: string
  expiry: number
}

export function setToken(access: string, expiresIn: number): void {
  const data: TokenData = {
    token: access,
    expiry: Date.now() + expiresIn * 1000
  }
  localStorage.setItem(TOKEN_KEY, JSON.stringify(data))
}

export function getToken(): string | null {
  const data = localStorage.getItem(TOKEN_KEY)
  if (!data) return null

  const tokenData: TokenData = JSON.parse(data)
  if (Date.now() > tokenData.expiry) {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
  return tokenData.token
}
```

### 方案C: 保持sessionStorage + 自动刷新

**优点**：
- 安全性较高
- 符合当前设计

**缺点**：
- 用户体验差
- 需要刷新Token机制

**实施**：
```typescript
// 使用refresh token自动刷新
async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) return false

  try {
    const response = await api.post('/auth/refresh', { refreshToken })
    setToken(response.data.access, response.data.refresh)
    return true
  } catch {
    return false
  }
}
```

### 方案D: 混合策略

**优点**：
- 平衡安全与体验
- 用户可选

**实施**：
```typescript
// 提供"记住我"选项
export function setToken(access: string, rememberMe: boolean): void {
  const storage = rememberMe ? localStorage : sessionStorage
  storage.setItem(TOKEN_KEY, access)
}
```

## 推荐方案

**采用方案B + 方案D组合**：

1. 默认使用localStorage + 过期时间
2. 提供"记住我"选项
3. 自动清理过期Token

## 实施细节

### 1. 定义Token数据结构

```typescript
// types/auth.ts
export interface TokenData {
  accessToken: string
  refreshToken?: string
  expiresAt: number  // 过期时间戳
  rememberMe: boolean
}

export interface AuthState {
  token: string | null
  isAuthenticated: boolean
  user: User | null
}
```

### 2. 创建Token管理工具

```typescript
// utils/auth.ts
import type { TokenData } from '@/types/auth'

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'auth_user'

/**
 * 设置Token
 * @param accessToken 访问令牌
 * @param expiresIn 过期时间（秒）
 * @param rememberMe 是否记住登录
 * @param refreshToken 刷新令牌（可选）
 */
export function setToken(
  accessToken: string,
  expiresIn: number,
  rememberMe: boolean = true,
  refreshToken?: string
): void {
  const storage = rememberMe ? localStorage : sessionStorage

  const tokenData: TokenData = {
    accessToken,
    refreshToken,
    expiresAt: Date.now() + expiresIn * 1000,
    rememberMe
  }

  storage.setItem(TOKEN_KEY, JSON.stringify(tokenData))
}

/**
 * 获取有效Token
 * @returns 有效Token或null
 */
export function getToken(): string | null {
  // 优先从localStorage获取
  let data = localStorage.getItem(TOKEN_KEY)

  // 如果localStorage没有，从sessionStorage获取
  if (!data) {
    data = sessionStorage.getItem(TOKEN_KEY)
  }

  if (!data) return null

  try {
    const tokenData: TokenData = JSON.parse(data)

    // 检查是否过期
    if (Date.now() > tokenData.expiresAt) {
      // Token已过期，清理
      clearToken()
      return null
    }

    return tokenData.accessToken
  } catch {
    // 解析失败，清理
    clearToken()
    return null
  }
}

/**
 * 获取刷新Token
 */
export function getRefreshToken(): string | null {
  const data = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
  if (!data) return null

  try {
    const tokenData: TokenData = JSON.parse(data)
    return tokenData.refreshToken || null
  } catch {
    return null
  }
}

/**
 * 清理Token
 */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

/**
 * 检查Token是否即将过期（5分钟内）
 */
export function isTokenExpiringSoon(): boolean {
  const data = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
  if (!data) return true

  try {
    const tokenData: TokenData = JSON.parse(data)
    // 5分钟内过期
    return Date.now() > tokenData.expiresAt - 5 * 60 * 1000
  } catch {
    return true
  }
}

/**
 * 获取剩余有效时间（秒）
 */
export function getTokenRemainingTime(): number {
  const data = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY)
  if (!data) return 0

  try {
    const tokenData: TokenData = JSON.parse(data)
    const remaining = Math.floor((tokenData.expiresAt - Date.now()) / 1000)
    return Math.max(0, remaining)
  } catch {
    return 0
  }
}
```

### 3. 修改登录逻辑

```typescript
// api/auth.ts
export async function login(
  username: string,
  password: string,
  rememberMe: boolean = true
): Promise<LoginResponse> {
  const response = await api.post<ApiResponse<LoginResponse>>('/auth/login', {
    username,
    password
  })

  if (response.data.code === 1000) {
    const { accessToken, refreshToken, expiresIn, user } = response.data.data

    // 使用新的Token管理
    setToken(accessToken, expiresIn, rememberMe, refreshToken)

    // 存储用户信息
    const storage = rememberMe ? localStorage : sessionStorage
    storage.setItem(USER_KEY, JSON.stringify(user))

    return response.data.data
  }

  throw new Error(response.data.message)
}
```

### 4. 修改登录表单

```vue
<!-- views/login/index.vue -->
<template>
  <el-form>
    <!-- ... 其他表单项 ... -->

    <el-form-item>
      <el-checkbox v-model="rememberMe">
        记住我（7天内免登录）
      </el-checkbox>
    </el-form-item>

    <el-button @click="handleLogin">登录</el-button>
  </el-form>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login } from '@/api/auth'

const rememberMe = ref(true)

const handleLogin = async () => {
  try {
    await login(form.username, form.password, rememberMe.value)
    // ...
  } catch (error) {
    // ...
  }
}
</script>
```

### 5. 添加Token刷新机制

```typescript
// api/interceptors.ts
import { getToken, getRefreshToken, setToken, clearToken, isTokenExpiringSoon } from '@/utils/auth'

// 请求拦截器：添加Token
api.interceptors.request.use(async (config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：处理Token过期
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Token过期，尝试刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      const refreshToken = getRefreshToken()
      if (refreshToken) {
        try {
          const response = await api.post('/auth/refresh', { refreshToken })
          const { accessToken, expiresIn } = response.data.data

          setToken(accessToken, expiresIn, true, refreshToken)

          // 重试原请求
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return api(originalRequest)
        } catch {
          // 刷新失败，清理并跳转登录
          clearToken()
          window.location.href = '/login'
        }
      }
    }

    return Promise.reject(error)
  }
)

// 定时检查Token过期
setInterval(() => {
  if (isTokenExpiringSoon() && getRefreshToken()) {
    // 自动刷新Token
    // ...
  }
}, 60000) // 每分钟检查一次
```

### 6. 修改Pinia Store

```typescript
// store/index.ts
import { defineStore } from 'pinia'
import { getToken, clearToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
  },

  actions: {
    logout() {
      clearToken()
      this.token = null
      this.userInfo = null
    },

    // 初始化时检查Token
    init() {
      const token = getToken()
      if (token) {
        this.token = token
        // 获取用户信息
        this.fetchUserInfo()
      }
    }
  }
})
```

## 安全考虑

| 风险 | 缓解措施 |
|------|----------|
| XSS攻击 | 使用HttpOnly Cookie（需要后端配合） |
| Token泄露 | 设置合理过期时间，支持主动登出 |
| 本地存储限制 | localStorage约5MB，足够使用 |

## 验证清单

- [ ] auth.ts工具函数更新
- [ ] 登录逻辑修改
- [ ] 登录表单添加"记住我"
- [ ] 请求拦截器更新
- [ ] Pinia Store更新
- [ ] Token刷新机制测试
- [ ] 过期自动清理测试

## 测试用例

```typescript
// __tests__/utils/auth.test.ts
describe('Token Management', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  test('should store token in localStorage when rememberMe is true', () => {
    setToken('test-token', 3600, true)
    expect(localStorage.getItem('auth_token')).toBeTruthy()
  })

  test('should store token in sessionStorage when rememberMe is false', () => {
    setToken('test-token', 3600, false)
    expect(sessionStorage.getItem('auth_token')).toBeTruthy()
  })

  test('should return null for expired token', () => {
    setToken('test-token', -1, true) // 已过期
    expect(getToken()).toBeNull()
  })

  test('should clear all tokens', () => {
    setToken('test-token', 3600, true)
    clearToken()
    expect(getToken()).toBeNull()
  })
})
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| XSS攻击 | 高 | 输入过滤，CSP策略 |
| Token泄露 | 中 | 短过期时间，刷新机制 |
| 兼容性问题 | 低 | 充分测试 |

## 回滚方案

如果出现问题：
1. 恢复使用sessionStorage
2. 移除"记住我"选项
3. 恢复原有auth.ts
