import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { getAccessToken, getRefreshToken, setTokens, clearTokens, isTokenExpired } from '../utils/auth'
import router from '../router'
import { ElMessage } from 'element-plus'
import type { ApiResponse, SpringPage } from '../types'

const RETRY_MAX_ATTEMPTS = 2
const RETRY_BASE_DELAY_MS = 1000
const REQUEST_TIMEOUT_MS = 15000
const SUCCESS_CODES = [200, 0]

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    __retryCount?: number
  }
}

interface PendingRequest {
  resolve: (token: string) => void
  reject: (reason?: unknown) => void
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

let isRefreshing = false
let pendingRequests: PendingRequest[] = []

function onTokenRefreshed(newToken: string): void {
  pendingRequests.forEach(({ resolve }) => resolve(newToken))
  pendingRequests = []
}

function onTokenRefreshFailed(): void {
  pendingRequests.forEach(({ reject }) => reject(new Error('Token 刷新失败')))
  pendingRequests = []
}

request.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // F8: 统一分页参数名 — 前端 pageNum/pageSize → 后端 page/size
  if (config.params) {
    const { pageNum, pageSize, ...rest } = config.params as Record<string, unknown>
    if (pageNum !== undefined || pageSize !== undefined) {
      config.params = {
        ...rest,
        ...(pageNum !== undefined && { page: pageNum }),
        ...(pageSize !== undefined && { size: pageSize }),
      }
    }
  }

  const token = getAccessToken()
  if (token) {
    if (isTokenExpired(token)) {
      const refreshToken = getRefreshToken()
      if (refreshToken && !isRefreshing) {
        isRefreshing = true
        try {
          const { data } = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
            `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
            {},
            { headers: { 'Refresh-Token': refreshToken } },
          )
          setTokens(data.data!.accessToken, data.data!.refreshToken)
          onTokenRefreshed(data.data!.accessToken)
          config.headers.Authorization = `Bearer ${data.data!.accessToken}`
        } catch {
          onTokenRefreshFailed()
          clearTokens()
          router.push('/login')
          return Promise.reject(new Error('Token 刷新失败，请重新登录'))
        } finally {
          isRefreshing = false
        }
      } else if (isRefreshing) {
        return new Promise<InternalAxiosRequestConfig>((resolve, reject) => {
          pendingRequests.push({
            resolve: (newToken: string) => {
              config.headers.Authorization = `Bearer ${newToken}`
              resolve(config)
            },
            reject,
          })
        })
      }
    } else {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  return config
})

interface TransformedPage<T> {
  items: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data as ApiResponse<unknown>
    if (SUCCESS_CODES.includes(code)) {
      // F9: Spring Data Page 格式转换 — 保留完整分页元数据
      if (data && typeof data === 'object' && 'content' in data && Array.isArray((data as SpringPage<unknown>).content) && 'totalElements' in data) {
        const page = data as SpringPage<unknown>
        return {
          items: page.content,
          total: page.totalElements,
          page: page.number ?? 0,
          size: page.size ?? 10,
          totalPages: page.totalPages ?? 0,
        } as TransformedPage<unknown>
      }
      return data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const config = error.config as InternalAxiosRequestConfig | undefined
    if (!config || (config.__retryCount ?? 0) >= RETRY_MAX_ATTEMPTS) {
      // 超过重试次数或无配置，继续正常错误处理
    } else if (!error.response && (error.code === 'ECONNABORTED' || error.message?.includes('Network Error'))) {
      config.__retryCount = (config.__retryCount || 0) + 1
      await new Promise(r => setTimeout(r, RETRY_BASE_DELAY_MS * config.__retryCount!))
      return request(config)
    }

    if (error.response) {
      const { status, data } = error.response as AxiosResponse<ApiResponse<null>>
      const msg = data?.message || '服务器错误'

      if (status === 401) {
        const isLoginRequest = error.config?.url?.includes('/auth/login')
        if (!isLoginRequest) {
          onTokenRefreshFailed()
          clearTokens()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
        } else {
          ElMessage.error(msg)
        }
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else {
        ElMessage.error(msg)
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  },
)

export default request
