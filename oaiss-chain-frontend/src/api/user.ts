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
