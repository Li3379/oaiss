import request from './request'
import type { PasswordChangeRequest } from '../types/auth'
import type { UserInfoResponse, UserProfileUpdateRequest } from '../types/user'

export function getProfile(): Promise<UserInfoResponse> {
  return request.get('/user/profile')
}

export function updateProfile(data: UserProfileUpdateRequest): Promise<UserInfoResponse> {
  if (!data) return Promise.reject(new Error('Profile update payload is required'))
  return request.put('/user/profile', data)
}

export function changePassword(data: PasswordChangeRequest): Promise<void> {
  if (!data?.oldPassword) return Promise.reject(new Error('Current password is required'))
  if (!data?.newPassword || data.newPassword.length < 6) {
    return Promise.reject(new Error('New password must be at least 6 characters'))
  }
  return request.put('/user/password', data)
}

export function getUserById(userId: number): Promise<UserInfoResponse> {
  if (!userId) return Promise.reject(new Error('User ID is required'))
  return request.get(`/user/${userId}`)
}

export function checkUsername(username: string): Promise<boolean> {
  if (!username) return Promise.reject(new Error('Username is required'))
  return request.get('/user/check-username', { params: { username } })
}

export function checkEmail(email: string): Promise<boolean> {
  if (!email) return Promise.reject(new Error('Email is required'))
  return request.get('/user/check-email', { params: { email } })
}
