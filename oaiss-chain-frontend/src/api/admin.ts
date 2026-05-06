import request from './request'
import type { PageRequest } from '../types'

export function getUserList(params?: PageRequest): Promise<unknown> {
  return request.get('/admin/users', { params })
}

export function updateUserStatus(userId: number, status: number): Promise<void> {
  return request.put(`/admin/users/${userId}/status`, null, { params: { status } })
}

export function getStatistics(): Promise<unknown> {
  return request.get('/admin/statistics')
}
