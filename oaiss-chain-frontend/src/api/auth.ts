import request from './request'
import type { LoginRequest, LoginResponse } from '../types'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function register(data: { username: string; password: string; email: string; role: string }): Promise<unknown> {
  if (!data?.username) return Promise.reject(new Error('用户名不能为空'))
  if (!data?.password) return Promise.reject(new Error('密码不能为空'))
  return request.post('/auth/register', data)
}

export function checkIp(): Promise<unknown> {
  return request.get('/auth/check-ip')
}

export function getCurrentUser(): Promise<unknown> {
  return request.get('/auth/me')
}
