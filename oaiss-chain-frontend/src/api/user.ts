import request from './request'
import type { UserInfoResponse, UserProfileUpdateRequest, PasswordChangeRequest } from '../types'

export function getProfile(): Promise<UserInfoResponse> {
  return request.get('/user/profile')
}

export function updateProfile(data: UserProfileUpdateRequest): Promise<void> {
  if (!data) return Promise.reject(new Error('更新数据不能为空'))
  return request.put('/user/profile', data)
}

export function changePassword(data: PasswordChangeRequest): Promise<void> {
  if (!data?.oldPassword) return Promise.reject(new Error('请输入当前密码'))
  if (!data?.newPassword || data.newPassword.length < 6) return Promise.reject(new Error('新密码至少6位'))
  return request.put('/user/password', data)
}

export function getUserById(userId: number): Promise<unknown> {
  if (!userId) return Promise.reject(new Error('用户ID不能为空'))
  return request.get(`/user/${userId}`)
}

export function checkUsername(username: string): Promise<unknown> {
  if (!username) return Promise.reject(new Error('用户名不能为空'))
  return request.get('/user/check-username', { params: { username } })
}

export function checkEmail(email: string): Promise<unknown> {
  if (!email) return Promise.reject(new Error('邮箱不能为空'))
  return request.get('/user/check-email', { params: { email } })
}
