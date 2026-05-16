import request from './request'
import type { PageRequest } from '../types'

export function getCarbonReports(params?: PageRequest): Promise<unknown> {
  return request.get('/third-party/carbon-reports', { params })
}

export function getStatistics(): Promise<unknown> {
  return request.get('/third-party/statistics')
}

export function getOrgInfo(): Promise<unknown> {
  return request.get('/third-party/org-info')
}

export function updateContact(data: { contactPerson: string; contactPhone: string }): Promise<void> {
  if (!data?.contactPerson) return Promise.reject(new Error('联系人不能为空'))
  return request.put('/third-party/contact', data)
}
